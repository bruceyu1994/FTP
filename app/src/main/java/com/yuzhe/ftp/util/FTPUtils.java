package com.yuzhe.ftp.util;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yuzhe.ftp.activity.DownloadList;
import com.yuzhe.ftp.activity.FileBrowse;
import com.yuzhe.ftp.activity.UploadList;
import com.yuzhe.ftp.data.DownloadStatus;
import com.yuzhe.ftp.data.UploadStatus;
import com.yuzhe.ftp.db.DBHelper;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Created by Bruce Yu on 2016/6/8.
 */
public class FTPUtils {
    private static final String TAG = "test";
    private FTPClient ftpClient = null;
    private String domainName, userName, password;
    private int port;
    private String currentPath = "/";
    private boolean isLogin = false;
    private Handler handler = null;
    private DBHelper dbHelper = null;
    private Context context;

    public FTPUtils(String domainName, int port, String userName, String password, Handler handler, Context context) {
        this.domainName = domainName;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.handler = handler;
        this.context = context;
        //初始化
        init();
    }

    public String getUserName() {

        return userName;
    }

    public String getPassword() {
        return password;
    }

    private void init() {
        ftpClient = new FTPClient();
        dbHelper = DBHelper.getDbHelper(context);
//        ftpClient.setConnectTimeout(2000);
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getDomainName() {
        return domainName;
    }

    public int getPort() {
        return port;
    }

    public boolean isLogin() {
        return isLogin;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    //登陆
    public boolean login() {
        if (!isLogin()) {
            try {
                //连接指定服务器，默认端口为21
                ftpClient.connect(domainName, port);
                ftpClient.setKeepAlive(true);
                Log.d(TAG, "connect to server");
                //获取响应字符串（FTP服务器上可设置）
                String replyString = ftpClient.getReplyString();
                Log.d(TAG, "\"replyString: \" + replyString");
                //获取响应码用于验证是否连接成功
                int reply = ftpClient.getReplyCode();

                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    Log.d(TAG, "连接失败");
                }
                //设置链接编码，windows主机UTF-8会乱码，需要使用GBK或gb2312编码
                ftpClient.setControlEncoding("GBK");
                //登录服务器
                isLogin = ftpClient.login(this.userName, this.password);
                if (isLogin) {
                    Log.d(TAG, "登录成功!");
                } else {
                    Log.d(TAG, "登录失败!");
                }
            } catch (IOException e) {
                e.printStackTrace();

            } finally {

            }
        }
        return isLogin;
    }

    public FTPFile[] getFile() {
        FTPFile[] files = null;
        try {
            ////设置被动模式,即每次数据连接之前，ftp client告诉ftp server开通一个端口来传输数据
            ftpClient.enterLocalPassiveMode();
            //获取所有文件和文件夹的名字
            Log.d(TAG + 2, ftpClient.printWorkingDirectory());
            Log.d(TAG + 3, currentPath);
            files = ftpClient.listFiles(new String(currentPath.getBytes("GBK"),
                    FTP.DEFAULT_CONTROL_ENCODING));

            for (int i = 0; i < files.length; i++) {
                Log.d(TAG, files[i].getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    public void changeDirectory(String path) {
        try {
            Log.d(TAG, path);
            //若条件成立则是进入下一级目录，否则进入上一级目录
            if (!path.equals("/") && !path.startsWith("/")) {
                currentPath = currentPath + path + "/";
            }

            if (ftpClient.changeWorkingDirectory(new String(currentPath.getBytes("GBK"),
                    FTP.DEFAULT_CONTROL_ENCODING))) {
                Log.d(TAG, "更新目录成功");
            } else {
                Log.d(TAG, "更新目录失败");
            }
            Log.d(TAG + 0, currentPath);
            Log.d(TAG + 1, ftpClient.printWorkingDirectory());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从FTP服务器上下载文件,支持断点续传，上传百分比汇报
     *
     * @param remote 远程文件路径
     * @param local  本地文件路径
     * @return 上传的状态
     * @throws IOException
     */
    public DownloadStatus download(String remote, String local) throws IOException {
        //设置被动模式
        ftpClient.enterLocalPassiveMode();
        //设置以二进制方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        DownloadStatus result;

        //检查远程文件是否存在
        FTPFile[] files = ftpClient.listFiles(new String(remote.getBytes("GBK"), "iso-8859-1"));
        if (files.length != 1) {
            System.out.println("远程文件不存在");
            return DownloadStatus.Remote_File_Noexist;
        }

        long lRemoteSize = files[0].getSize();
        File f = new File(local);
        //本地存在文件，进行断点下载
        if (f.exists()) {
            long localSize = f.length();
            //判断本地文件大小是否大于远程文件大小
            if (localSize >= lRemoteSize) {
                System.out.println("本地文件大于远程文件，下载中止");
                return DownloadStatus.Local_Bigger_Remote;
            }

            //进行断点续传，并记录状态
            FileOutputStream out = new FileOutputStream(f, true);
            ftpClient.setRestartOffset(localSize);
            InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes("GBK"), "iso-8859-1"));
            byte[] bytes = new byte[1024];
            long step = lRemoteSize / 100;
            long process = localSize / step;
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
                localSize += c;
                long nowProcess = localSize / step;
                if (nowProcess > process) {
                    process = nowProcess;
                    Log.d(TAG, "" + process);
                    Log.d(TAG, "Message");
                    System.out.println("下载进度：" + process);
                    //TODO 更新文件下载进度,值存放在process变量中
                }
            }
            in.close();
            out.close();
            boolean isDo = ftpClient.completePendingCommand();
            if (isDo) {
                result = DownloadStatus.Download_From_Break_Success;
            } else {
                result = DownloadStatus.Download_From_Break_Failed;
            }
        } else {
            FileOutputStream out = new FileOutputStream(f);
            InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes("GBK"), "iso-8859-1"));
            byte[] bytes = new byte[1024];
            long step = lRemoteSize / 100;
            long process = 0;
            long localSize = 0L;
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
                localSize += c;
                long nowProcess = localSize / step;
                if (nowProcess > process) {
                    process = nowProcess;
                    Log.d(TAG, "Message");
//                    bar.setProgress((int) process);
//                    if (process % 10 == 0)
                    System.out.println("下载进度：" + process);
                    //TODO 更新文件下载进度,值存放在process变量中
                }
            }
            in.close();
            out.close();
            boolean upNewStatus = ftpClient.completePendingCommand();
            if (upNewStatus) {
                result = DownloadStatus.Download_New_Success;
            } else {
                result = DownloadStatus.Download_New_Failed;
            }
        }
        return result;
    }

    public int download(com.yuzhe.ftp.data.File file) throws IOException {
        String remotePath = file.getRemotePath();
        String localPath = file.getLocalPath();
        DownloadStatus result = DownloadStatus.Download_From_Break_Failed;
        boolean isDo = false;
        //设置被动模式
        ftpClient.enterLocalPassiveMode();
        //设置以二进制方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        long remoteSize = file.getRemoteSize();
        //本地存在文件，进行断点下载
        long localSize = file.getLocalSize();
        //判断本地文件大小是否大于远程文件大小
        if (localSize >= remoteSize) {
            System.out.println("本地文件大于远程文件，下载中止");
//            return DownloadStatus.Local_Bigger_Remote;
        }
        Boolean append = false;
        if (localSize > 0) {
            append = true;
        }
        File f = new File(localPath);

        //进行断点续传，并记录状态
        FileOutputStream out = new FileOutputStream(f, append);
        ftpClient.setRestartOffset(localSize);
        InputStream in = ftpClient.retrieveFileStream(new String(remotePath.getBytes("GBK"), "iso-8859-1"));  //获得远程文件的输入流
        byte[] bytes = new byte[1024];
        long step = remoteSize / 100;
        long process = localSize / step;
        int c = in.read(bytes);
        if (c != -1) {
            out.write(bytes, 0, c);
            localSize += c;
            file.setLocalSize(localSize);
            long nowProcess = localSize / step;
            if (nowProcess > process) {
                process = nowProcess;
                file.setProcess(process);
                Log.d(TAG, "Message");
                System.out.println("下载进度：" + process);

                dbHelper.update(DBHelper.TBL_NAME_FILE, file.get_id(), FileBrowse.getFileContentValue(file));
                Message message = new Message();
                message.what = DownloadList.DOWNLOAD_OPERATE;
//                message.arg1 = (int) process;
//                message.obj = file;
                handler.sendMessage(message);
                //TODO 更新文件下载进度,值存放在process变量中
            }
        }

        in.close();
        out.close();

        isDo = ftpClient.completePendingCommand();
        if (isDo) {
            result = DownloadStatus.Download_From_Break_Success;
        } else {
            result = DownloadStatus.Download_From_Break_Failed;
        }
        return (int) process;
//        return result;
    }

    /**
     * 上传文件到FTP服务器，支持断点续传
     *
     * @param local  本地文件名称，绝对路径
     * @param remote 远程文件路径，使用/home/directory1/subdirectory/file.ext 按照Linux上的路径指定方式，支持多级目录嵌套，支持递归创建不存在的目录结构
     * @return 上传结果
     * @throws IOException
     */

    public UploadStatus upload(String local, String remote) throws IOException {
        //设置PassiveMode传输
        ftpClient.enterLocalPassiveMode();
        //设置以二进制流的方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("GBK");
        UploadStatus result;
        //对远程目录的处理
        String remoteFileName = remote;
        if (remote.contains("/")) {
            remoteFileName = remote + local.substring(local.lastIndexOf("/") + 1);
            //创建服务器远程目录结构，创建失败直接返回
            if (CreateDirecroty(remote, ftpClient) == UploadStatus.Create_Directory_Fail) {
                return UploadStatus.Create_Directory_Fail;
            }
        }

        //检查远程是否存在文件
        FTPFile[] files = ftpClient.listFiles(new String(remoteFileName.getBytes("GBK"), "iso-8859-1"));
        if (files.length == 1) {
            long remoteSize = files[0].getSize();
            File f = new File(local);
            long localSize = f.length();
            if (remoteSize == localSize) {
                return UploadStatus.File_Exits;
            } else if (remoteSize > localSize) {
                return UploadStatus.Remote_Bigger_Local;
            }

            //尝试移动文件内读取指针,实现断点续传
            result = uploadFile(remoteFileName, f, ftpClient, remoteSize);

            //如果断点续传没有成功，则删除服务器上文件，重新上传
            if (result == UploadStatus.Upload_From_Break_Failed) {
                if (!ftpClient.deleteFile(remoteFileName)) {
                    return UploadStatus.Delete_Remote_Faild;
                }
                result = uploadFile(remoteFileName, f, ftpClient, 0);
            }
        } else {
            result = uploadFile(remoteFileName, new File(local), ftpClient, 0);
        }
        return result;
    }

    /**
     * 上传文件到服务器,新上传和断点续传
     *
     * @param remoteFile 远程文件名，在上传之前已经将服务器工作目录做了改变
     * @param localFile  本地文件File句柄，绝对路径
     * @param remoteSize 需要显示的处理进度步进值
     * @param ftpClient  FTPClient引用
     * @return
     * @throws IOException
     */
    public UploadStatus uploadFile(String remoteFile, File localFile, FTPClient ftpClient, long remoteSize) throws IOException {
        UploadStatus status;
        //显示进度的上传
        long step = localFile.length() / 100;
        long process = 0;
        long localreadbytes = 0L;
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        OutputStream out = ftpClient.appendFileStream(new String(remoteFile.getBytes("GBK"), "iso-8859-1"));
        //断点续传
        if (remoteSize > 0) {
            ftpClient.setRestartOffset(remoteSize);
            process = remoteSize / step;
            raf.seek(remoteSize);
            localreadbytes = remoteSize;
        }
        byte[] bytes = new byte[1024];
        int c;
        while ((c = raf.read(bytes)) != -1) {
            out.write(bytes, 0, c);
            localreadbytes += c;
            if (localreadbytes / step != process) {
                process = localreadbytes / step;
                System.out.println("上传进度:" + process);
                //TODO 汇报上传状态
            }
        }
        out.flush();
        raf.close();
        out.close();
        boolean result = ftpClient.completePendingCommand();
        if (remoteSize > 0) {
            status = result ? UploadStatus.Upload_From_Break_Success : UploadStatus.Upload_From_Break_Failed;
        } else {
            status = result ? UploadStatus.Upload_New_File_Success : UploadStatus.Upload_New_File_Failed;
        }
        return status;
    }

    //上传文件1
    public boolean upload(com.yuzhe.ftp.data.File file) throws IOException {
        //设置PassiveMode传输
        ftpClient.enterLocalPassiveMode();
        //设置以二进制流的方式传输
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("GBK");
        UploadStatus result;
        //对远程目录的处理
        String remoteFileName = file.getRemotePath();
//        if (remote.contains("/")) {
//            remoteFileName = remote + local.substring(local.lastIndexOf("/") + 1);
//            //创建服务器远程目录结构，创建失败直接返回
//            if (CreateDirecroty(remote, ftpClient) == UploadStatus.Create_Directory_Fail) {
//                return false;
//            }
//        }

        //检查远程是否存在文件
        FTPFile[] files = ftpClient.listFiles(new String(remoteFileName.getBytes("GBK"), "iso-8859-1"));
        if (files.length == 1) {
            long remoteSize = files[0].getSize();
            File f = new File(file.getLocalPath());
            long localSize = f.length();
            if (remoteSize == localSize) {
                return true;
            } else if (remoteSize > localSize) {
                return false;
            }

            //尝试移动文件内读取指针,实现断点续传
            result = uploadFile(remoteFileName, f, ftpClient, remoteSize, file);
            //如果断点续传没有成功，则删除服务器上文件，重新上传
            if (result == UploadStatus.Upload_From_Break_Failed) {
                if (!ftpClient.deleteFile(remoteFileName)) {
                    return false;
                }
                result = uploadFile(remoteFileName, f, ftpClient, 0, file);
            }
        } else {
            result = uploadFile(remoteFileName, new File(file.getLocalPath()), ftpClient, 0, file);
        }
        return true;
    }

    //上传文件2
    public UploadStatus uploadFile(String remoteFile, File localFile, FTPClient ftpClient, long remoteSize,com.yuzhe.ftp.data.File file) throws IOException {
        UploadStatus status;
        //显示进度的上传
        long step = localFile.length() / 100;
        long process = 0;
        long localreadbytes = 0L;
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        OutputStream out = ftpClient.appendFileStream(new String(remoteFile.getBytes("GBK"), "iso-8859-1"));
        //断点续传
        if (remoteSize > 0) {
            ftpClient.setRestartOffset(remoteSize);
            process = remoteSize / step;
            raf.seek(remoteSize);
            localreadbytes = remoteSize;
        }
        byte[] bytes = new byte[1024];
        int c;
        if ((c = raf.read(bytes)) != -1) {
            out.write(bytes, 0, c);
            localreadbytes += c;
            if (localreadbytes / step != process) {
                process = localreadbytes / step;
                System.out.println("上传进度显示:" + process);
                file.setProcess(process);
                //TODO 汇报上传状态
            }
            file.setRemoteSize(localreadbytes);
            dbHelper.update(DBHelper.TBL_NAME_FILE,file.get_id(),FileBrowse.getFileContentValue(file));


            Message m = new Message();
            m.what = UploadList.UPLOAD_OPERATE;
            m.arg1 = (int) process;
            handler.sendMessage(m);
        }
        out.flush();
        raf.close();
        out.close();
        boolean result = ftpClient.completePendingCommand();
        if (remoteSize > 0) {
            status = result ? UploadStatus.Upload_From_Break_Success : UploadStatus.Upload_From_Break_Failed;
        } else {
            status = result ? UploadStatus.Upload_New_File_Success : UploadStatus.Upload_New_File_Failed;
        }
        return status;
    }

    /**
     * 递归创建远程服务器目录
     *
     * @param remote    远程服务器文件绝对路径
     * @param ftpClient FTPClient对象
     * @return 目录创建是否成功
     * @throws IOException
     */
    public UploadStatus CreateDirecroty(String remote, FTPClient ftpClient) throws IOException {
        UploadStatus status = UploadStatus.Create_Directory_Success;
        String directory = remote.substring(0, remote.lastIndexOf("/") + 1);
        if (!directory.equalsIgnoreCase("/") && !ftpClient.changeWorkingDirectory(new String(directory.getBytes("GBK"), "iso-8859-1"))) {
            //如果远程目录不存在，则递归创建远程服务器目录
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            } else {
                start = 0;
            }
            end = directory.indexOf("/", start);
            while (true) {
                String subDirectory = new String(remote.substring(start, end).getBytes("GBK"), "iso-8859-1");
                if (!ftpClient.changeWorkingDirectory(subDirectory)) {
                    if (ftpClient.makeDirectory(subDirectory)) {
                        ftpClient.changeWorkingDirectory(subDirectory);
                    } else {
                        System.out.println("创建目录失败");
                        return UploadStatus.Create_Directory_Fail;
                    }
                }

                start = end + 1;
                end = directory.indexOf("/", start);

                //检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        }
        return status;
    }

    //注销
    public void logOut() {

    }

    public int getFileCount(String path) {
        try {

            return ftpClient.listFiles(path).length;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
