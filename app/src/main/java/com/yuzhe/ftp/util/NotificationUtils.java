package com.yuzhe.ftp.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.yuzhe.ftp.R;

import java.io.File;

/**
 * Created by Bruce Yu on 2016/6/11.
 */
public class NotificationUtils {
    public Context context;
    private int notifyId = 0;
    public  NotificationManager mNotificationManager;
    public NotificationCompat.Builder mBuilder;
    private String fileName = null;
    private String filePath = null;
    private File file;
    public NotificationUtils(Context context, int notifyId, String fileName, String filePath, File file) {
        this.context = context;
        this.notifyId = notifyId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.file = file;
    }

    public void showNotification() {
        //第一步：获取状态通知栏管理：
        mNotificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        //第二步：实例化通知栏构造器NotificationCompat.Builder：
        mBuilder = new NotificationCompat.Builder(context);
        //第三步：对Builder进行配置：
        mBuilder.setContentTitle(fileName)//设置通知栏标题
                .setContentText(filePath)
                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL)) //设置通知栏点击意图
//	.setNumber(number) //设置通知集合的数量
                .setTicker("测试通知来啦") //通知首次出现在通知栏，带上升动画效果的
                .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
                .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
//	.setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                .setDefaults(Notification.DEFAULT_VIBRATE)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合
                //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
                .setSmallIcon(R.drawable.host)//设置通知小ICON
        .setProgress(100,0,false);//进度条

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "*");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        mBuilder.setContentIntent(pendingIntent);

        //发送通知请求
        mNotificationManager.notify(notifyId, mBuilder.build());
    }

    public PendingIntent getDefalutIntent(int flags){
        PendingIntent pendingIntent= PendingIntent.getActivity(context, 1, new Intent(), flags);
        return pendingIntent;
    }
}
