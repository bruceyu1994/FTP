package com.yuzhe.ftp.data;

import java.io.Serializable;

/**
 * Created by Bruce Yu on 2016/6/7.
 */
public class FTPSetting implements Serializable{
    private int _id;
    private String domainName;
    private int port;
    private String userName;
    private String password;

    public FTPSetting(String domainName, int port, String userName, String password) {
        this.userName = userName;
        this.port = port;
        this.domainName = domainName;
        this.password = password;
    }

    public FTPSetting(int _id, String domainName, int port, String userName, String password) {
        this._id = _id;
        this.userName = userName;
        this.port = port;
        this.domainName = domainName;
        this.password = password;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {

        this._id = _id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }
}
