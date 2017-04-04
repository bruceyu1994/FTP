package com.yuzhe.ftp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Bruce Yu on 2016/6/9.
 */
public class DBHelper extends SQLiteOpenHelper {
    //数据库名称
    private static final String DB_NAME = "ftp.db";
    //表名称
    public static final String TBL_NAME_FTP = "tb_ftp";
    public static final String TBL_NAME_FILE = "tb_file";
    private static final int VERSION = 2;
    //创建表SQL语句
    private static final String CREATE_TBL_FTP = "create table if not exists " + TBL_NAME_FTP
            + "(_id integer primary key autoincrement,domainName text,port integer, userName text," +
            " password text, remark text,picUrl text,desc text)";

    private static final String CREATE_TBL_FILE = "create table if not exists " + TBL_NAME_FILE
            + "(_id integer primary key autoincrement,localPath text,remotePath text, fileName text," +
            " remoteSize text, localSize text,process bigint,flag integer,domainName text,userName text,password text," +
            "port integer)";

    //SQLiteDatabase实例
    private SQLiteDatabase db = null;
    private static DBHelper dbHelper = null;

    private DBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        db.execSQL(CREATE_TBL_FTP);
        db.execSQL(CREATE_TBL_FILE);
        Log.i("测试", "表创建成功");
    }

    public static DBHelper getDbHelper(Context context) {
        if(dbHelper == null)
            dbHelper = new DBHelper(context);
        return dbHelper;
    }

    /*
         * 插入方法
         */
    public long insert(String tableName, ContentValues values) {
        //获得SQLiteDatabase实例
        SQLiteDatabase db = getWritableDatabase();
        //插入
        long result = db.insert(tableName, null, values);

        return result;
    }

    /*
     * 查询方法
     */
    public Cursor query(String tableName) {
        //获得SQLiteDatabase实例
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(tableName, null, null, null, null, null, null);
        return c;
    }

    /*
 * 查询方法
 */
    public Cursor query(String tableName, String[] wheres, String[] whereArgs) {
        //获得SQLiteDatabase实例
        SQLiteDatabase db = getWritableDatabase();
        /* query(String table, String[] columns, String selection,
        String[] selectionArgs, String groupBy, String having,
                        String orderBy)*/
        String where = wheres[0]+ "=?";
        for(int i=1; i<wheres.length; i++){
            where = where + " and " + wheres[i] + "=?";
        }
        Cursor c = db.query(tableName, null, where, whereArgs, null, null, null);
        return c;
    }

    /*
     * 删除方法
     */
    public int del(String tableName, int id) {
        if (db == null) {
            //获得SQLiteDatabase实例
            db = getWritableDatabase();
        }
        //执行删除
        return db.delete(tableName, "_id=?", new String[]{String.valueOf(id)});
    }

    /*
     *更新方法
     */
    public int update(String tableName, int id, ContentValues cv) {
        if (db == null) {
            //获得SQLiteDatabase实例
            db = getWritableDatabase();
        }
        //执行更新
        return db.update(tableName, cv, "_id=?", new String[]{id+""});
    }

    /*
     * 关闭数据库
     */
    public void colse() {
        if (db != null) {
            db.close();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
