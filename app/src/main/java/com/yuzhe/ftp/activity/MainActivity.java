package com.yuzhe.ftp.activity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.yuzhe.ftp.R;
import com.yuzhe.ftp.data.FTPSetting;
import com.yuzhe.ftp.db.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int REQUEST_CODE_ADD_FTP = 0;
    public static final int RESULT_CODE_ADD_FTP = 1;
    public static final int REQUEST_CODE_BROWSER_FTP = 2;
    public static final int REQUEST_CODE_EDIT_FTP = 3;
    public static final int DEFAULT_PORT = 21;

    private static final String TAG = "test";
    private final int MENU_MODIFY = 1;
    private final int MENU_DELETE = 2;

    private Button btn_OK;
    private GridView gridView = null;
    private SimpleAdapter adapter = null;
    private List<Map<String, Object>> lst = null;
    private List<FTPSetting> settingList = null;
    private DBHelper dbHelper = null;
    private AdapterView.AdapterContextMenuInfo menuInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化
        init();
    }

    public void init() {
//        btn_OK = (Button) findViewById(R.id.btn_OK);
//        btn_OK.setOnClickListener(this);

        gridView = (GridView) findViewById(R.id.gridView_main);
        this.registerForContextMenu(gridView);  //注册上下文菜单
        //注册点击事件
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, FileBrowse.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("ftpSetting", settingList.get(position));
                intent.putExtras(bundle);
                startActivityForResult(intent, REQUEST_CODE_BROWSER_FTP);
            }
        });

        lst = new ArrayList<>();
        settingList = new ArrayList<>();
        dbHelper = DBHelper.getDbHelper(this);

        Cursor cursor = dbHelper.query(DBHelper.TBL_NAME_FTP);
        while (cursor.moveToNext()) {
            int _id = cursor.getInt(cursor.getColumnIndex("_id"));
            String domainName = cursor.getString(cursor.getColumnIndex("domainName"));
            int port = cursor.getInt(cursor.getColumnIndex("port"));
            String userName = cursor.getString(cursor.getColumnIndex("userName"));
            String password = cursor.getString(cursor.getColumnIndex("password"));
            FTPSetting ftpSetting = new FTPSetting(_id, domainName, port, userName, password);
            settingList.add(ftpSetting);
        }

        createHost(settingList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.add:
                intent = new Intent(MainActivity.this, AddFTP.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_FTP);
                break;
            case R.id.download_list:
                intent = new Intent(this, DownloadList.class);
                startActivity(intent);
                break;
            case R.id.upload_list:
                intent = new Intent(this, UploadList.class);
                startActivity(intent);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_ADD_FTP:
                if (resultCode == RESULT_CODE_ADD_FTP) {
                    FTPSetting ftpSetting = (FTPSetting) data.getSerializableExtra("ftpSetting");
                    if (ftpSetting.getUserName().equals("")) {
                        ftpSetting.setUserName("anonymous");
                        ftpSetting.setPassword("anonymous");
                    }

                    dbHelper.insert(DBHelper.TBL_NAME_FTP, getContentValue(ftpSetting));
                    Cursor cursor = dbHelper.query(DBHelper.TBL_NAME_FTP);
                    cursor.moveToLast();
                    //设置id
                    ftpSetting.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
                    settingList.add(ftpSetting);

                    createHost(ftpSetting);
                    if (ftpSetting != null)
                        Log.d(TAG, ftpSetting.getDomainName() + " " + ftpSetting.getUserName() +
                                " " + ftpSetting.getPassword());
                    else
                        Log.d(TAG, "ftpSetting is null");
                }
                break;
            case REQUEST_CODE_EDIT_FTP:
                FTPSetting ftpSetting = null;
                if(data != null)
                    ftpSetting = (FTPSetting) data.getSerializableExtra("ftpSetting");
                if(ftpSetting != null) {
                    if ((dbHelper.update(DBHelper.TBL_NAME_FTP, ftpSetting.get_id(), getContentValue(ftpSetting)) > 0) &&
                            (lst.set(menuInfo.position, getHostMap(ftpSetting)) != null) &&
                            (settingList.set(menuInfo.position, ftpSetting) != null)) {
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "修改成功");
                        Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Log.d(TAG, "修改失败");
                        Toast.makeText(this, "修改失败", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Log.d(TAG, "修改失败");
                    Toast.makeText(this, "修改失败", Toast.LENGTH_SHORT).show();

                }
                break;
            default:
                break;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_OK:
                Log.d(TAG, "R.id.btn_OK onClicked");
//                NotificationUtils notificationUtils = new NotificationUtils(this, 0);
//                notificationUtils.showNotification();
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()) {
            case R.id.gridView_main:
                menu.add(0, MENU_MODIFY, 0, "修改");
                menu.add(0, MENU_DELETE, 0, "删除");
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case MENU_MODIFY:
                modifyHost();
                break;
            case MENU_DELETE:
                delHost();
                break;
        }
        return super.onContextItemSelected(item);
    }

    public List<Map<String, Object>> getData(FTPSetting ftpSetting) {
        lst.add(getHostMap(ftpSetting));
        return lst;
    }

    public List<Map<String, Object>> getData(List<FTPSetting> settingList) {

        for (int i = 0; i < settingList.size(); i++) {
            lst.add(getHostMap(settingList.get(i)));
        }
        return lst;
    }

    public void createHost(FTPSetting ftpSetting) {
        if (adapter == null) {
            adapter = new SimpleAdapter(this, getData(ftpSetting), R.layout.layout_main, new String[]{"imgView_host",
                    "tv_hostRemark"}, new int[]{R.id.imgView_host, R.id.tv_hostRemark});
            gridView.setAdapter(adapter);
            Log.d(TAG, "gridView.setAdapter(adapter);");
        } else {
            lst = getData(ftpSetting);
            adapter.notifyDataSetChanged();
        }
    }

    public void createHost(List<FTPSetting> settingList) {
        if (adapter == null) {
            adapter = new SimpleAdapter(this, getData(settingList), R.layout.layout_main, new String[]{"imgView_host",
                    "tv_hostRemark"}, new int[]{R.id.imgView_host, R.id.tv_hostRemark});
            gridView.setAdapter(adapter);
            Log.d(TAG, "gridView.setAdapter(adapter);");
        } else {
            lst = getData(settingList);
            adapter.notifyDataSetChanged();
        }
    }

    public ContentValues getContentValue(FTPSetting ftpSetting) {
        //ContentValues以键值对的形式存放数据
        ContentValues cv = new ContentValues();
        cv.put("domainName", ftpSetting.getDomainName());
        cv.put("port", ftpSetting.getPort() + "");
        cv.put("userName", ftpSetting.getUserName());
        cv.put("password", ftpSetting.getPassword());
        return cv;
    }

    public void modifyHost() {
        Intent intent = new Intent(this, AddFTP.class);
        FTPSetting ftpSetting = settingList.get(menuInfo.position);
        Bundle bundle = new Bundle();
        bundle.putSerializable("ftpSetting", ftpSetting);
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_EDIT_FTP);
    }

    public void delHost() {
        int position = menuInfo.position;
        FTPSetting ftpSetting = settingList.get(position);
        //删除数据库中对应的内容,FTPSetting中的内容,GridView中的数据
        if((dbHelper.del(DBHelper.TBL_NAME_FTP, ftpSetting.get_id()) > 0)) {
            if((settingList.remove(position) != null) && (lst.remove(position) != null)) {
                //并更新UI
                adapter.notifyDataSetChanged();
                Log.d(TAG, "删除成功");
            } else {
                Log.d(TAG, "删除失败");
            }
        } else {
            Log.d(TAG, "删除失败");
        }
    }

    public Map<String, Object> getHostMap(FTPSetting ftpSetting) {
        Map<String, Object> map = new HashMap<>();
        map.put("imgView_host", R.drawable.host);
        map.put("tv_hostRemark", ftpSetting.getDomainName() + ":" + ftpSetting.getPort());
        return map;
    }

    //创建Intent数组
    Intent[] makeIntentStack(Context context) {
        Intent[] intents = new Intent[2];
        //创建activity栈的根activity
        intents[0] = Intent.makeRestartActivityTask(new ComponentName(context, com.yuzhe.ftp.activity.MainActivity.class));
        intents[1] = new Intent(context,  DownloadList.class);
        return intents;
    }
}
