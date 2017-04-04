package com.yuzhe.ftp.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yuzhe.ftp.R;
import com.yuzhe.ftp.activity.UploadList;
import com.yuzhe.ftp.data.File;
import com.yuzhe.ftp.db.DBHelper;
import com.yuzhe.ftp.thread.UploadThread;
import com.yuzhe.ftp.util.FTPUtils;

import java.util.ArrayList;

/**
 * Created by Bruce Yu on 2016/6/22.
 */
public class UploadThreadListAdapter extends BaseAdapter{
    private Context context;
    private LayoutInflater inflater;
    private ArrayList<File> files = null;
    private DBHelper dbHelper = null;
    Handler handler = null;

    public UploadThreadListAdapter(Context context, ArrayList<File> files, DBHelper dbHelper, Handler handler) {
        this.context = context;
        this.files = files;
        this.dbHelper = dbHelper;
        this.handler = handler;
        //初始化
        init();
    }

    private void init() {
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return this.files.size();
    }

    @Override
    public Object getItem(int position) {
        return this.files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHodler holder;
        //  获取单条item 的view对象
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_upload_list, null);
            holder = new ViewHodler();

            holder.tv_fileName = (TextView) convertView.findViewById(R.id.tv_fileName2);
            holder.tv_progress = (TextView) convertView.findViewById(R.id.tv_progress2);
            holder.bar = (ProgressBar) convertView.findViewById(R.id.progressBar2);
            holder.imgBtn_upload = (ImageButton) convertView.findViewById(R.id.imgBtn_upload);
            holder.imgBtn_pause = (ImageButton) convertView.findViewById(R.id.imgBtn_pause2);
            holder.imgBtn_delete = (ImageButton) convertView.findViewById(R.id.imgBtn_delete2);

            holder.file = files.get(position);
            convertView.setTag(holder);
        } else {
            holder = (ViewHodler) convertView.getTag();
        }

        //将数据显示到每个对应的 view上.
        holder.listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == holder.imgBtn_upload) {
                    //下载文件
                    if(holder.thread == null) {
                        FTPUtils utils = new FTPUtils(holder.file.getDomainName(), holder.file.getPort(), holder.file.getUserName(),
                                holder.file.getPassword(), handler, context);
                        holder.thread = new UploadThread(utils, holder.file, context);
                        Toast.makeText(context, "开始上传...", Toast.LENGTH_SHORT).show();
                        holder.thread.start();
                    }
                } else if (v == holder.imgBtn_pause) {
                    //暂停下载
                    if (holder.thread != null && holder.thread.isAlive()) {
                        Toast.makeText(context, "已停止上传", Toast.LENGTH_SHORT).show();
                        holder.thread.setPause(true);
                        holder.thread = null;
                    }
                } else if (v == holder.imgBtn_delete) {
                    //从下载列表中删除
                    java.io.File f = new java.io.File(files.get(position).getLocalPath());
                    f.delete();
                    files.remove(position);
                    dbHelper.del(DBHelper.TBL_NAME_FILE, holder.file.get_id());
                    holder.thread = null;
                    Message m = new Message();
                    m.what = UploadList.DELETE_OPERATE;
                    handler.sendMessage(m);
                }
            }
        };

        holder.imgBtn_upload.setOnClickListener(holder.listener);
        holder.imgBtn_pause.setOnClickListener(holder.listener);
        holder.imgBtn_delete.setOnClickListener(holder.listener);

        holder.tv_fileName.setText(this.files.get(position).getFileName());
        holder.tv_progress.setText(String.valueOf(this.files.get(position).getProcess()));
        holder.bar.setMax(100);
        holder.bar.setProgress((int) this.files.get(position).getProcess());

        return convertView;
    }

    class ViewHodler {
        TextView tv_fileName;
        TextView tv_progress;
        ProgressBar bar;
        ImageButton imgBtn_upload, imgBtn_pause, imgBtn_delete;
        View.OnClickListener listener;
        File file;
        UploadThread thread;
    }
}
