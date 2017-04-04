package com.yuzhe.ftp.data;

/**
 * Created by Bruce Yu on 2016/6/13.
 */
public class File {
    private int _id;
    private String localPath = null;
    private String remotePath = null;
    private String fileName;  //文件名
    private long remoteSize = 0;  //本地文件大小，也就是下载进度
    private long localSize = 0;
    private long process = 0;  //下载进度
    private int flag = FileFlag.Download_Flag;   //下载文件或上传文件标志
    private String domainName, userName, password;
    private int port;

    public File(String domainName,String userName, String password, String remotePath, String localPath,
                String fileName,long remoteSize, long localSize, long process, int port, int flag) {
        this.domainName = domainName;
        this.userName = userName;
        this.password = password;
        this.remotePath = remotePath;
        this.localPath = localPath;
        this.fileName = fileName;
        this.remoteSize = remoteSize;
        this.localSize = localSize;
        this.process = process;
        this.port = port;
        this.flag = flag;
    }


    public int getPort() {
        return port;
    }

    public String getPassword() {

        return password;
    }

    public String getUserName() {

        return userName;
    }

    public String getDomainName() {

        return domainName;
    }

    public int get_id() {
        return _id;
    }



    public void set_id(int _id) {

        this._id = _id;
    }

    public long getRemoteSize() {
        return remoteSize;
    }

    public void setRemoteSize(long remoteSize) {
        this.remoteSize = remoteSize;
    }

    public int getFlag() {
        return flag;
    }



    public String getFileName() {
        return fileName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public long getLocalSize() {
        return localSize;
    }

    public long getProcess() {
        return process;
    }

    public void setProcess(long process) {
        this.process = process;
    }

    public void setLocalSize(long localSize) {
        this.localSize = localSize;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
}
