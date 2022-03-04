package me.heartalborada.bilidownloader.gui;

import com.google.zxing.WriterException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.heartalborada.bilidownloader.utils.qrcode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;

import static me.heartalborada.bilidownloader.utils.login.qr.IsLogin;
import static me.heartalborada.bilidownloader.utils.login.qr.getOauthKey;
import static me.heartalborada.bilidownloader.utils.login.sms.getSmsLocationMap;

public class login extends Application implements Initializable{
    //captcha begin
    protected static String gt=null,challenge=null,validate=null,seccode,key=null;
    //captcha end
    //sms begin
    protected static String captcha_key;
    protected static LinkedHashMap<String, Integer> map;
    //sms end
    //qr begin
    protected static String oauthKey=null;
    //qr end
    private static Stage stage;
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root= FXMLLoader.load(Objects.requireNonNull(getClass().getResource("Login.fxml")));
        primaryStage.setTitle("登录bilibili");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        stage= primaryStage;
    }

    @FXML
    private Label qrlogin_state;
    @FXML
    private TextField acc_input,phone_number,sms_code;
    @FXML
    private PasswordField pw_input;
    @FXML
    private ChoiceBox<String> sms_choice;
    @FXML
    private VBox password,sms,qr,captcha_login;
    @FXML
    private ImageView qrcode_show;
    public static int flag=0;//flag 为判断登录方式变量
    private static int timeout_180=180;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getText();
            if (text.matches("[0-9]*")) {
                return change;
            }
            return null;
        };
        map = getSmsLocationMap();
        Iterator<Map.Entry<String, Integer>> iterator= map.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry<? extends String, ? extends Integer> entry = iterator.next();
            //System.out.println(entry.getKey()+":"+entry.getValue());
            sms_choice.getItems().add(entry.getKey().toString());
        }
        sms_choice.getSelectionModel().select(0);
        phone_number.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("[0-9]*")) {
                long value = Long.parseLong(newValue);
            } else {
                phone_number.setText(oldValue);
            }
        });
        sms_code.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) {
                long value = Long.parseLong(newValue);
            } else {
                sms_code.setText(oldValue);
            }
        });
    }

    public void getQRcode(ActionEvent event){
        timeout_180=180;
        oauthKey= getOauthKey();
        Timeline an = new Timeline(new KeyFrame(Duration.millis(1000), e -> qrLogin((Button) event.getSource())));
        an.setCycleCount(timeout_180+1);
        an.play();
        byte[] bytes={};
        try {
            bytes=qrcode.getQRCodeImage("https://passport.bilibili.com/qrcode/h5/login?oauthKey="+oauthKey,100,100);
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        qrcode_show.setImage(new Image(new ByteArrayInputStream(bytes)));
    }
    public void getCaptcha(ActionEvent event){
        try {
            new captcha().start(new Stage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int st_s=60;

    public void sixteens_cooldown(Button bt){
        st_s=60;
        Timeline an = new Timeline(new KeyFrame(Duration.millis(1000), e -> timelabel(bt)));
        an.setCycleCount(st_s+1);
        an.play();
    }

    public void qrLogin(Button bt) {
        if(timeout_180==0){
            qrlogin_state.setText("状态: 未生成二维码/二维码已过期");
            bt.setText("获取登录二维码");
            oauthKey=null;
            bt.setDisable(false);
            return;
        }
        String data[]={};
        try {
            data=IsLogin(oauthKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(data[0].equals("0")){
            qrlogin_state.setText("状态: "+data[1]);
        } else {
            qrlogin_state.setText("状态: 登录成功");
        }
        bt.setText(timeout_180+" s后登录二维码超时，需重新获取");
        bt.setDisable(true);
        --timeout_180;
    }

    public void timelabel(Button bt) {
        if(st_s==0){
            bt.setText("获取验证码");
            gt=null;
            bt.setDisable(false);
            return;
        }
        bt.setText(st_s+" s后可重新获取验证码");
        bt.setDisable(true);
        --st_s;
    }

    public void getSmsCode(ActionEvent event){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.titleProperty().set("错误");
        if(login.key == null || login.challenge == null || login.validate == null || login.seccode == null) {
            alert.setHeaderText("未获取人机验证验证码/获取失败");
            alert.showAndWait();
            return;
        }
        int cid= map.get(sms_choice.getValue());
        sixteens_cooldown((Button) event.getSource());
        long pn=Long.parseLong(phone_number.getText());
        String tmp = me.heartalborada.bilidownloader.utils.login.sms.SendSmsCaptcha(
                pn,
                cid,
                new String[]{
                    login.key,
                    login.challenge,
                    login.validate,
                    login.seccode
        });
        if(!tmp.equals("")){
            captcha_key=tmp;
        }
    }

    public void changeLoginMode(ActionEvent event){
        String bu_id = ((Button) event.getSource()).getId();
        if(bu_id.equals("pw_login")){
            flag=0;
            password.setVisible(true);
            sms.setVisible(false);
            qr.setVisible(false);
            captcha_login.setVisible(true);
        } else if(bu_id.equals("sms_login")){
            flag=1;
            password.setVisible(false);
            sms.setVisible(true);
            qr.setVisible(false);
            captcha_login.setVisible(true);
        } else if(bu_id.equals("qr_login")){
            flag=2;
            password.setVisible(false);
            sms.setVisible(false);
            qr.setVisible(true);
            captcha_login.setVisible(false);
        }
    }

    public static void close(){
        stage.close();
    }

    public void doLogin(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.titleProperty().set("错误");
        if(login.key == null || login.challenge == null || login.validate == null || login.seccode == null) {
            alert.setHeaderText("未获取人机验证验证码/获取失败");
            alert.showAndWait();
            return;
        }
        if(flag==0) {
            String acc,pw_none;
            acc=acc_input.getText();
            pw_none=pw_input.getText();
            if(pw_none.equals("")){
                alert.setHeaderText("你还没有填写密码");
                alert.show();
                return;
            }
            if(acc.equals("")){
                alert.setHeaderText("你还没有填写手机号/邮箱");
                alert.show();
                return;
            }
            String pw = me.heartalborada.bilidownloader.utils.login.password.getPw(pw_none);
            me.heartalborada.bilidownloader.utils.login.password.doLogin(
                    acc,
                    pw,
                    new String[]{
                            me.heartalborada.bilidownloader.gui.login.key,
                            me.heartalborada.bilidownloader.gui.login.challenge,
                            me.heartalborada.bilidownloader.gui.login.validate,
                            me.heartalborada.bilidownloader.gui.login.seccode
                    });
        } else if (flag==1){
            if(phone_number.getText().equals("")){
                alert.setHeaderText("您还没输入手机号");
                alert.showAndWait();
                return;
            }
            if(sms_code.getText().equals("")){
                alert.setHeaderText("您还没输入短信验证码");
                alert.showAndWait();
                return;
            }
            int cid= map.get(sms_choice.getValue());
            long pn=Long.parseLong(phone_number.getText());
            long code=Long.parseLong(sms_code.getText());
            me.heartalborada.bilidownloader.utils.login.sms.doLogin(cid,pn,code,captcha_key);
        }
    }
}
