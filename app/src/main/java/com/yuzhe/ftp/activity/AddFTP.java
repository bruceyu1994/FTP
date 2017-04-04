package com.yuzhe.ftp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.yuzhe.ftp.data.FTPSetting;
import com.yuzhe.ftp.R;

public class AddFTP extends AppCompatActivity implements View.OnClickListener {
    private Button btn_OK, btn_Cancel;
    private EditText et_domainName,et_port, et_userName, et_password;
    private int _id;  //修改表中的值时用到的_id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ftp);

        //初始化
        init();
    }


    private void init() {
        //从xml文件中获取按钮和EditText等控件
        btn_OK = (Button) findViewById(R.id.btn_OK);
        btn_Cancel = (Button) findViewById(R.id.btn_Cancel);
        btn_OK.setOnClickListener(this);
        btn_Cancel.setOnClickListener(this);

        et_domainName = (EditText) findViewById(R.id.et_domainName);
        et_port = (EditText) findViewById(R.id.et_port);
        et_userName = (EditText) findViewById(R.id.et_userName);
        et_password = (EditText) findViewById(R.id.et_password);

        Intent intent = getIntent();
        FTPSetting ftpSetting = (FTPSetting) intent.getSerializableExtra("ftpSetting");
        if(ftpSetting != null) {
            _id = ftpSetting.get_id();
            et_domainName.setText(ftpSetting.getDomainName());
            et_port.setText(ftpSetting.getPort()+"");
            et_userName.setText(ftpSetting.getUserName());
            et_password.setText(ftpSetting.getPassword());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_OK:
                doOKButtonClick();
                finish();
                break;
            default:
                finish();
                break;
        }
    }


    public void doOKButtonClick(){
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        Bundle bundle = new Bundle();

        String domainName = et_domainName.getText().toString();
        int port = MainActivity.DEFAULT_PORT;
        if(!et_port.getText().toString().equals(""))
            port = Integer.parseInt(et_port.getText().toString());
        String userName = et_userName.getText().toString();
        String password = et_password.getText().toString();

        FTPSetting ftpSetting = new FTPSetting(_id, domainName,port, userName, password);
        bundle.putSerializable("ftpSetting", ftpSetting);
        intent.putExtras(bundle);
        setResult(MainActivity.RESULT_CODE_ADD_FTP, intent);
    }
}
