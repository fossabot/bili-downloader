package me.heartalborada.bilidownloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.heartalborada.bilidownloader.gui.login;
import me.heartalborada.bilidownloader.gui.viewVideo;
import me.heartalborada.bilidownloader.utlis.file;

import java.io.File;
import java.io.IOException;

import static me.heartalborada.bilidownloader.utlis.login.checkIsLogin;

public class main {
    public static String SESSDATA = null;
    public static String bili_jct = null;
    public static final String run_dir=System.getProperty("user.dir");
    public static final String config_dir=run_dir+"/config/";
    public static final String cookie_file_dir=config_dir+"bili_cookies.json";

    public void setParam() throws IOException {
        file file=new file();
        File cookieFile = new File(cookie_file_dir);
        if(!cookieFile.exists()) {
            file.write(cookie_file_dir,file.read_res(this.getClass().getResourceAsStream("bili_cookies.json")));
        }
        String data= file.read(cookie_file_dir);
        JsonObject json= JsonParser.parseString(data).getAsJsonObject();
        SESSDATA=json.get("SESSDATA").getAsString();
        bili_jct=json.get("bili_jct").getAsString();
    }

    public static void main(String[] arg){
        String java_version= System.getProperty("java.version").split("\\.")[0];
        if(Integer.parseInt(java_version)<11){
            System.err.println("本程序需要在Java 11及以上的环境运行");
            System.exit(0);
        }
        if(System.getProperty("os.name").equals("Linux")){
            System.setProperty("java.awt.headless","true");
        }
        try {
            new main().setParam();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!checkIsLogin()){
            login.main(new String[]{""});
            return;
        }
        viewVideo.main(new String[]{});
    }

}
