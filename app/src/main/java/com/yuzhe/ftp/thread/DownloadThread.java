package com.yuzhe.ftp.thread;

import android.content.Context;
import android.widget.Toast;

import com.yuzhe.ftp.data.File;
import com.yuzhe.ftp.util.FTPUtils;

import java.io.IOException;

/**
 * Created by Bruce Yu on 2016/6/11.
 * 下载线程类
 */
public class DownloadThread extends Thread {
    private FTPUtils ftpUtil = null;   //ftp工具对象
    private File file = null;  //需要下载的文件信息
    private Boolean pause = false;  //是否暂停标志
    private Context context;   //上下文

    public DownloadThread(FTPUtils ftpUtil, File file, Context context) {
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
//        super.run();
        while(!pause) {
            try {
                if(ftpUtil.login()) {
                    if(ftpUtil.download(file) == 100) {
                        this.pause = true;
                    }
                } else {
                    Toast.makeText(context, "还未登录成功",Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
