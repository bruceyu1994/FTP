package com.yuzhe.ftp.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

import com.yuzhe.ftp.R;
import com.yuzhe.ftp.adapter.DownloadThreadListAdapter;
import com.yuzhe.ftp.data.File;
import com.yuzhe.ftp.data.FileFlag;
import com.yuzhe.ftp.db.DBHelper;

import java.util.ArrayList;

public class DownloadList extends AppCompatActivity {
    private ListView lstView = null;
    private DownloadThreadListAdapter adapter = null;
    public static Handler handler = null;
    private String TAG = "DownloadList";
    private ArrayList<File> files;
    private DBHelper dbHelper;
    public static final int DOWNLOAD_OPERATE = 0;
    public static final int DELETE_OPERATE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);

        //初始化
        init();
    }

    private void init() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case DELETE_OPERATE:
                        adapter.notifyDataSetChanged();
                        Toast.makeText(DownloadList.this, "删除成功", Toast.LENGTH_SHORT).show();
                        break;
                    case DOWNLOAD_OPERATE:
//                        File file = (File) msg.obj;
//                        dbHelper.update(DBHelper.TBL_NAME_FILE, file.get_id(), FileBrowse.getFileContentValue(file));
                        adapter.notifyDataSetChanged();
                        break;
                }
            }
        };

        dbHelper = DBHelper.getDbHelper(this);
        this.files = new ArrayList<>();
        lstView = (ListView) findViewById(R.id.lstView_downloadList);
        //从数据库中获取文件数据
        getData();
        adapter = new DownloadThreadListAdapter(this, files, dbHelper, handler);
        lstView.setAdapter(adapter);

    }

    private void getData() {
        if (this.files.size() > 0)
            this.files.clear();
        Cursor cursor = dbHelper.query(DBHelper.TBL_NAME_FILE, new String[]{"flag"},
                new String[]{FileFlag.Download_Flag+""});
        while (cursor.moveToNext()) {
            int _id = cursor.getInt(cursor.getColumnIndex("_id"));
            String localPath = cursor.getString(cursor.getColumnIndex("localPath"));
            String remotePath = cursor.getString(cursor.getColumnIndex("remotePath"));
            String fileName = cursor.getString(cursor.getColumnIndex("fileName"));
            long remoteSize = cursor.getInt(cursor.getColumnIndex("remoteSize"));  //本地文件大小，也就是下载进度
            long localSize = cursor.getInt(cursor.getColumnIndex("localSize"));
            long process = cursor.getInt(cursor.getColumnIndex("process"));  //下载进度
            int flag = cursor.getInt(cursor.getColumnIndex("flag"));
            //下载文件或上传文件标志
            String domainName = cursor.getString(cursor.getColumnIndex("domainName"));
            String userName = cursor.getString(cursor.getColumnIndex("userName"));
            String password = cursor.getString(cursor.getColumnIndex("password"));
            int port = cursor.getInt(cursor.getColumnIndex("port"));
            File file = new File(domainName,userName, password, remotePath, localPath,
                    fileName,remoteSize, localSize, process, port, flag);
            file.set_id(_id);

            files.add(file);
        }
    }
}
