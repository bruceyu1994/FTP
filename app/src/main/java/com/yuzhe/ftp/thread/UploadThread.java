package com.yuzhe.ftp.thread;

import android.content.Context;
import android.widget.Toast;

import com.yuzhe.ftp.data.File;
import com.yuzhe.ftp.util.FTPUtils;

import java.io.IOException;

/**
 * Created by Bruce Yu on 2016/6/11.
 */
public class UploadThread extends Thread {
    private FTPUtils ftpUtil = null;   //ftp工具对象
    private File file = null;
    private Boolean pause = false;
    private Context context;

    public UploadThread(FTPUtils ftpUtil, File file, Context context) {
        this.ftpUtil = ftpUtil;
        this.file = file;
        this.context = context;
    }

    public void setPause(Boolean pause) {
        this.pause = pause;
    }

    public Boolean getPause() {
        return pause;
    }


    @Override
    public void run() {
        while(!pause) {
            try {
                if(ftpUtil.login()) {
                    ftpUtil.upload(file);
                } else {
                    Toast.makeText(context, "还未登录成功",Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
