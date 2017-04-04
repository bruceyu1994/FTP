package com.yuzhe.ftp.activity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.yuzhe.ftp.R;
import com.yuzhe.ftp.data.FTPSetting;
import com.yuzhe.ftp.data.File;
import com.yuzhe.ftp.data.FileFlag;
import com.yuzhe.ftp.db.DBHelper;
import com.yuzhe.ftp.thread.DownloadThread;
import com.yuzhe.ftp.util.FTPUtils;
import com.yuzhe.ftp.util.FileUtils;
import com.yuzhe.ftp.util.NotificationUtils;

import org.apache.commons.net.ftp.FTPFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBrowse extends AppCompatActivity {
    public static final String TAG = "FileBrowse";
    public static final int MESSAGE_UPDATE_UI = 0;
    public static final int FILE_SELECT_CODE = 1;
    public static final int FILE_SELECT_SUCCESS = 2;
    public static final int MESSAGE_UPDATE_NOTIFICATION = 3;
    private FTPUtils ftpUtil = null;
    private FTPSetting ftpSetting = null;
    private List<Map<String, Object>> data = null;
    //    private MyAdapter myAdapter = null;
    private SimpleAdapter adapter = null;
    private ListView lstView_File;
    private FTPFile[] files = null;
    private Handler myHandler = null;
    private String localPath = null; //上传文件时，保存本地文件完整路径
    public static List<File> downloadFiles = null;
    public static List<DownloadThread> downloadThreads = null;
    public List<FTPUtils> ftpUtilses = null;
    private Context context = this;
    private int count = 0;
    private NotificationUtils notificationUtils = null;
    private DBHelper dbHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browse);

        //初始化
        init();
    }

    //初始化
    private void init() {
        downloadThreads = new ArrayList<>();
        downloadFiles = new ArrayList<>();
        ftpUtilses = new ArrayList<>();
        dbHelper = DBHelper.getDbHelper(this);

        //Handler对象
        myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MESSAGE_UPDATE_UI:
//                        getData();  //获取数据源
                        if (adapter == null) {
                            adapter = new SimpleAdapter(FileBrowse.this, data, R.layout.layout_file, new String[]{"img", "title", "info","size","longSize"},
                                    new int[]{R.id.imgBtn_pause, R.id.title, R.id.info, R.id.size,R.id.tv_longSize});
                            lstView_File.setAdapter(adapter);
                        } else {
                            Log.d(TAG, "更新UI");
                            adapter.notifyDataSetChanged();
                            //更新当前目录显示
                            FileBrowse.this.setTitle(ftpUtil.getCurrentPath());
                        }
                        break;
                    case FILE_SELECT_SUCCESS:
                        uploadFile();
                        break;
                    case MESSAGE_UPDATE_NOTIFICATION:
                        // 更新进度
                        Log.d(TAG, ""+msg.arg2);
                        notificationUtils.mBuilder.setProgress(100,msg.arg2,false);
                        notificationUtils.mNotificationManager.notify(msg.arg1, notificationUtils.mBuilder.build());
                        break;
                }
            }
        };

        lstView_File = (ListView) findViewById(R.id.lstView_File);
        Intent intent = getIntent();
        ftpSetting = (FTPSetting) intent.getSerializableExtra("ftpSetting");

        if (ftpUtil == null) {
            ftpUtil = new FTPUtils(ftpSetting.getDomainName(), ftpSetting.getPort(), ftpSetting.getUserName(),
                    ftpSetting.getPassword(), myHandler, this);
        }

        //更新当前目录显示
        FileBrowse.this.setTitle(ftpUtil.getCurrentPath());

        //
        final MyThread thread = new MyThread(ftpUtil, "/");
        thread.start();

        //为lstView_File注册Click事件
        lstView_File.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: ");
                if (position == 0) {
                    //返回根目录
                    ftpUtil.setCurrentPath("//");
                    doKeyBack();
                } else if (position == 1) {
                    //返回上级目录
                    doKeyBack();
                } else {
                    //如果是文件夹，则进入下一级目录，否则就提示用户是否下载
                    if (data.get(position).get("type").equals("dir")) {
                        String path = data.get(position).get("title").toString();
                        MyThread thread = new MyThread(ftpUtil, path);
                        thread.start();
                    } else if (data.get(position).get("type").equals("file")) {
                        if (avaiableMedia()) {
                            String fileName = (String) data.get(position).get("title");
                            String str_size = data.get(position).get("size").toString();
                            long remoteSize = (long) data.get(position).get("longSize");
                            String remotePath = ftpUtil.getCurrentPath() + fileName;
                            String localPath = Environment.getExternalStorageDirectory().getPath() + "/" + fileName;
                            Log.d(TAG, remotePath + " " + localPath);
                            File file = new File(ftpSetting.getDomainName(),ftpSetting.getUserName(), ftpSetting.getPassword(), remotePath, localPath,
                                    fileName,remoteSize,0, 0, ftpSetting.getPort(), FileFlag.Download_Flag);
                            dbHelper.insert(DBHelper.TBL_NAME_FILE,getFileContentValue(file));
//                            Cursor cursor = dbHelper.query(DBHelper.TBL_NAME_FILE);
//                            cursor.moveToLast();
//                            //设置id
//                            file.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
//                            FTPUtils utils = new FTPUtils(ftpSetting.getDomainName(), ftpSetting.getPort(), ftpSetting.getUserName(),
//                                    ftpSetting.getPassword());
//                            DownloadThread downloadThread = new DownloadThread(utils, file);
//                            downloadThreads.add(downloadThread);
//                            java.io.File file = new java.io.File(localPath);
//                            notificationUtils = new NotificationUtils(context, 0, fileName, remotePath, file);
//                            notificationUtils.showNotification();
//                            utils.setId(downloadThreads.size() - 1);
//                            downloadThread.start();
                        } else {
                            Log.d(TAG, "无SD卡可用");
                        }
                    }
                }
            }
        });

    }


    public void getData() {
        if (data == null) {
            data = new ArrayList<>();
        }
        if (data.size() > 0)
            data.clear();
        String[] menuTitle = {".", ".."};
        String[] menuInfo = {"返回根目录", "返回上一级目录"};

        for (int i = 0; i < 2; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("type", "dir");
            map.put("img", R.drawable.folder);
            map.put("title", menuTitle[i]);
            map.put("info", menuInfo[i]);
            data.add(map);
        }
        for (FTPFile file : files) {
            if (file.isDirectory()) {
                if (!file.getName().equals(".") && !file.getName().equals("..")) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "dir");
                    map.put("img", R.drawable.folder);
                    map.put("title", file.getName());
                    ftpUtil.changeDirectory(file.getName());
                    FTPFile[] files1 = ftpUtil.getFile();
                    int size = files1.length;
                    if(size >= 2) {
                        if(files1[0].getName().equals("."))
                            size --;
                        if(files1[1].getName().equals(".."))
                            size --;
                    }
                    String path = ftpUtil.getCurrentPath();
                    path = path.substring(0, path.length() - 1);
                    path = path.substring(0, path.lastIndexOf('/') + 1);
                    ftpUtil.setCurrentPath(path);

                    map.put("size", size + "个文件");
                    map.put("info", file.getName());
                    data.add(map);
                }
                Log.d(TAG, file.getName() + " 是文件夹");
            } else if (file.isFile()) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", "file");
                map.put("img", R.drawable.file);
                map.put("title", file.getName());
                double size = file.getSize();
                map.put("longSize", file.getSize());
                DecimalFormat df = new DecimalFormat("#.00");
                String str_size = df.format(size) + "K";
                if(size > 1024) {
                    size /= 1024;   //得到KB大小
                    str_size = df.format(size) + "KB";
                    if(size > 1024) {
                        size /= 1024;  //得到M大小
                        str_size = df.format(size) + "M";
                        if(size > 1024) {
                            size /= 1024;  //得到G大小
                            str_size = df.format(size) + "G";
                        }
                    }
                }
                map.put("size", str_size);
                map.put("info", file.getName());
                data.add(map);

                Log.d(TAG, file.getName() + " 是文件");
            }
        }
    }

    class MyThread extends Thread {
        public FTPUtils ftpUtil = null;
        public String path = null;
        public List<Map<String, Object>> data = null;

        public MyThread(FTPUtils ftpUtil, String path) {
            this.ftpUtil = ftpUtil;
            this.path = path;
        }

        @Override
        public void run() {
            super.run();
            if (!ftpUtil.isLogin())
                ftpUtil.login();
            if (ftpUtil.isLogin()) {
                ftpUtil.changeDirectory(path);
                files = ftpUtil.getFile();
                Log.d(TAG, String.valueOf(files.length));
                getData();
                Message message = new Message();
                message.what = MESSAGE_UPDATE_UI;
                myHandler.sendMessage(message);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu_file_browse, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload:
                showFileChooser();  //选择本地文件
                break;
            case R.id.download_list:
                showDownloadList();
                break;
            case R.id.upload_list:
                Intent intent = new Intent(this, UploadList.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDownloadList() {
        Intent intent = new Intent(this, DownloadList.class);
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("downloadThreads", (Serializable) downloadThreads);
//        intent.putExtras(bundle);
        startActivity(intent);
    }

    //上传文件，路径带有文件名
    private void uploadFile() {
        String remotePath = ftpUtil.getCurrentPath();
        String fileName = localPath.substring(localPath.lastIndexOf("/")+1);
        Log.d(TAG, remotePath + " " + localPath + fileName);
        File file = new File(ftpSetting.getDomainName(),ftpSetting.getUserName(), ftpSetting.getPassword(),
                remotePath + fileName, localPath,
                fileName,0, getFileSize(localPath), 0, ftpSetting.getPort(), FileFlag.Upload_Flag);
        dbHelper.insert(DBHelper.TBL_NAME_FILE,getFileContentValue(file));
        
//        UploadThread uploadThread = new UploadThread(ftpUtil, localPath, remotePath);
//        uploadThread.start();
    }

    /**
     * 获取指定文件大小
     *
     * @param filePath
     * @return
     * @throws Exception
     */
    private static long getFileSize(String filePath) {
        long size = 0;
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                size = fis.available();
            } else {
    //            file.createNewFile();
                Log.e("获取文件大小", "文件不存在!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            doKeyBack();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public void doKeyBack() {
        String path = ftpUtil.getCurrentPath();
        if (path.equals("/")) {
            finish();
        } else {
            Log.d(TAG, "返回上一级目录");
            path = path.substring(0, path.length() - 1);
            path = path.substring(0, path.lastIndexOf('/') + 1);
            ftpUtil.setCurrentPath(path);
        }
        MyThread thread = new MyThread(ftpUtil, path);
        thread.start();
    }

    /*判断是否有SD卡
    ture:有SD卡
    false:没有SD卡*/
    public boolean avaiableMedia() {
        String status = Environment.getExternalStorageState();  //获取SD卡状态
        if (status.equals(Environment.MEDIA_MOUNTED)) {  //判断是否正常挂载
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    localPath = FileUtils.getPath(this, uri);
                    String fileName = localPath.substring(localPath.lastIndexOf("/")+1);
                    Toast.makeText(this, "已选择 "+ fileName , Toast.LENGTH_SHORT).show();

                    Message msg = new Message();
                    msg.what = FILE_SELECT_SUCCESS;
                    myHandler.sendMessage(msg);
                } else {
                    Toast.makeText(this, "选择文件失败", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    //选择本地文件
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    //创建Intent数组
    Intent[] makeIntentStack(Context context) {
        Intent[] intents = new Intent[2];
        //创建activity栈的根activity
        intents[0] = Intent.makeRestartActivityTask(new ComponentName(context, com.yuzhe.ftp.activity.MainActivity.class));
        intents[1] = new Intent(context,  DownloadList.class);
        return intents;
    }

    public static ContentValues getFileContentValue(File file) {
        //ContentValues以键值对的形式存放数据
        ContentValues cv = new ContentValues();
        cv.put("localPath", file.getLocalPath());
        cv.put("remotePath", file.getRemotePath());
        cv.put("fileName", file.getFileName());
        cv.put("remoteSize", file.getRemoteSize());
        cv.put("process", file.getProcess());
        cv.put("localSize", file.getLocalSize());
        cv.put("flag", file.getFlag()+"");
        cv.put("domainName", file.getDomainName());
        cv.put("userName", file.getUserName());
        cv.put("password", file.getPassword());
        cv.put("port", file.getPort());
        return cv;
    }
}
