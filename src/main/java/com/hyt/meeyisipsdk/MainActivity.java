package com.hyt.meeyisipsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.linphone.core.TransportType;

public class MainActivity extends AppCompatActivity {
    EditText account;
    EditText number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.hyt.meeyisipsdk.R.layout.activity_main);
        initView();
        SiphoneManager.getInstance().init(this);
//        SiphoneManager.getInstance().startLibLinphone(true);
        SiphoneManager.getInstance().configureAccount("192.168.111.172","1010","12345","12345", TransportType.Tcp,"abc");
    }

    /**
     * 配置账号
     * @param view
     */
    public void configureAccount(View view){
        String user = account.getText().toString();
        SiphoneManager.getInstance().configureAccount("192.168.111.172",user,"1010",user, TransportType.Tcp,"abc");
    }

    /**
     * 初始化视图
     */
    private void initView() {
        account = findViewById(com.hyt.meeyisipsdk.R.id.account);
        number = findViewById(com.hyt.meeyisipsdk.R.id.number);
    }

    /**
     * 拨打电话
     *
     * @param view
     */
    public void callOut(View view) {
        SiphoneManager.getInstance().call(number.getText().toString(),number.getText().toString());
    }

    /**
     * 视频通话
     *
     * @param view
     */
    public void callVideoOut(View view) {
        SiphoneManager.getInstance().call(number.getText().toString(),number.getText().toString());
    }

    /**
     * 接听电话
     *
     * @param view
     */
    public void acceptCall(View view) {
//        SiphoneManager.getInstance().acceptCall();
    }

    /**
     * 挂断电话
     *
     * @param view
     */
    public void terminateCall(View view) {
        SiphoneManager.getInstance().terminateCall();
    }
}