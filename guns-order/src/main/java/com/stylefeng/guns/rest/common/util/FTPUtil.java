package com.stylefeng.guns.rest.common.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Jerry
 **/
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "ftp")
public class FTPUtil {
    // 地址 端口  用户名 密码
    private String hostName;
    private Integer port;
    private String userName;
    private String password;
    private FTPClient ftpClient = null;

    private void initFTPClient() {
        try {
            ftpClient = new FTPClient();
            ftpClient.setControlEncoding("utf-8");
            ftpClient.connect(hostName, port);
            ftpClient.login(userName, password);

        } catch (Exception e) {
            log.error("初始化FTPClient出错", e);
        }
    }

    // 输入一个路径 将路径里的文件转成字符串返回
    public String getFileStrByAddress(String filePath) {
        BufferedReader bufferedReader = null;
        try {

            initFTPClient();
            bufferedReader = new BufferedReader(new InputStreamReader(ftpClient.retrieveFileStream(filePath)));

            StringBuffer stringBuffer = new StringBuffer();
            while (true){
                String lineStr = bufferedReader.readLine();
                if(lineStr==null){
                    break;
                }
                stringBuffer.append(lineStr);
            }
            ftpClient.logout();
            return stringBuffer.toString();
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;

    }
}
