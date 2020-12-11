package com.hyt.meeyisipsdk.activitys;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.hyt.meeyisipsdk.SiphoneInCallCallback;
import com.hyt.meeyisipsdk.SiphoneManager;
import com.hyt.meeyisipsdk.adapter.SipCallAdapter;
import com.hyt.meeyisipsdk.model.SipCall;

import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SipCallActivity extends Activity implements SiphoneInCallCallback {
    private String TAG = SipCallActivity.class.getSimpleName();
    private TextureView mLocalPreview;//本地视频
    private TextureView mRemoteVideo;//远程视频
    private LinearLayout local_preview;//本地视频显示控件
    private Call mCall;//当前通话实例
    private Call mOutCall;//当前呼出通话实例
    private TextView mState;//状态信息
    private TextView mCalluser;//通话用户
    private ListView mCalls;//呼入用户列表
    private LinearLayout mContacts;//联系人列表视图
    private LinearLayout mTerminateCall;//挂断
    private LinearLayout mAcceptCall;//接听
    private LinearLayout mTransferCall;//转移
    private ImageView server_icon;//服务图标
    private Chronometer mWaitTimer;//等待计时器
    private Chronometer mCallTimer;//通话计时器
    private int direct = -1;//方向 0:呼入 1:呼出
    private SipCallAdapter mCallAdapter;//呼入用户列表适配器
    private List<SipCall> mCallList = new CopyOnWriteArrayList<>();//呼入用户列表数据
    private boolean AutoAccept = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(com.hyt.meeyisipsdk.R.layout.activity_call);//设置布局
        initView();//初始化视图
    }

    @Override
    protected void onDestroy() {
        removeVideo();//移除视频视图
        cancelAndHiddenCallTime();//取消隐藏通话时间
        super.onDestroy();
    }

    private CoreListener mListener;

    /**
     * 初始化视图
     */
    private void initView() {
        mRemoteVideo = findViewById(com.hyt.meeyisipsdk.R.id.remote_video_texture);//远程视频控件
        mLocalPreview = findViewById(com.hyt.meeyisipsdk.R.id.local_preview_texture);//本地预览控件
        local_preview = findViewById(com.hyt.meeyisipsdk.R.id.local_preview);//本地预览显示控件
        mState = findViewById(com.hyt.meeyisipsdk.R.id.state);//状态信息
        mCalluser = findViewById(com.hyt.meeyisipsdk.R.id.name);//呼入用户
        mTerminateCall = findViewById(com.hyt.meeyisipsdk.R.id.terminateCall);//挂断按钮
        mAcceptCall = findViewById(com.hyt.meeyisipsdk.R.id.acceptCall);//接听按钮
        mTransferCall = findViewById(com.hyt.meeyisipsdk.R.id.transferCall);//转移按钮
        server_icon = findViewById(com.hyt.meeyisipsdk.R.id.server_icon);//服务图标
        mWaitTimer = findViewById(com.hyt.meeyisipsdk.R.id.waittime);//等待计时器
        mCallTimer = findViewById(com.hyt.meeyisipsdk.R.id.calltime);//通话计时器
        mCalls = findViewById(com.hyt.meeyisipsdk.R.id.calls);//呼入用户列表
        mContacts = findViewById(com.hyt.meeyisipsdk.R.id.contacts);//联系人列表视图

        direct = getIntent().getIntExtra("direct", -1);
        getCurrentCall();//获取当前通话
        showWaitTime();//显示等待时间
        showCallBeforeInfo();//显示通话前(呼入/呼叫)信息
        setOnclick();//设置点击事件
        SiphoneManager.getInstance().setIncallback(this);//设置通话监听
        updateCurrentCall();//更新当前通话
        initCalls();//初始化呼入用户列表
    }

    /**
     * 初始化呼入用户列表
     */
    private void initCalls() {
        if (mCallList.size() != 0 && SiphoneManager.getInstance().getCore().getCalls().length != mCallList.size()) {
            for (Call call : SiphoneManager.getInstance().getCore().getCalls()) {
                int direction = call.getCallLog().getFromAddress().getUsername() == mCall.getCallLog().getLocalAddress().getUsername() ? direct : 0;
                addCall(call, direction, call == mCall);//添加通话
            }
        } else {
            if (mCall != null && direct == 0) {
                addCall(mCall, direct, false);//添加通话
            }
        }
        mCallAdapter = new SipCallAdapter(this, mCallList);

        // 将适配器上的数据传递给listView
        mCalls.setAdapter(mCallAdapter);
        mCalls.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Call selectCall = mCallList.get(i).getCall();
//                SiphoneManager.getInstance().acceptCall(selectCall);//接听通话

                if(Call.State.OutgoingProgress == mCall.getState()) {
                    AutoAccept = false;
                }
                onChangeCall(i);//监听切换通话
            }
        });
    }

    /**
     * 监听切换通话
     */
    public void onChangeCall(int i) {
        Call selectCall = mCallList.get(i).getCall();//会因未接通通话挂断导致呼入通话数量与当前通话数量不一致，索引溢出问题
        String selectName = selectCall.getCallLog().getRemoteAddress().getUsername();
        String currentName = mCall.getCallLog().getRemoteAddress().getUsername();
        if (!selectName.equals(currentName)) {//切换通话
            if (Call.State.IncomingReceived == mCall.getState() || Call.State.IncomingEarlyMedia == mCall.getState()) {//接听已呼入的通话（未接听的）
                acceptCall(mCall);//接听通话
                updateState(mCallAdapter.getSelectIndex(), 1);//刷新呼入用户列表
            } else{
                SiphoneManager.getInstance().pauseCall(mCall);//暂停通话 - 不能及时暂停通话
                updateState(mCallAdapter.getSelectIndex(), 2);//刷新呼入用户列表
            }

            removeVideo();//移除视频视图
            Log.d(TAG, "当前通话：" + selectName + " 状态：" + selectCall.getState());
            mState.append("当前通话：" + selectName + " 状态：" + selectCall.getState() + "\n");
            if (Call.State.IncomingReceived == selectCall.getState() || Call.State.IncomingEarlyMedia == selectCall.getState()) {//接听已呼入的通话（未接听的）
                acceptCall(selectCall);//接听通话
            } else if (Call.State.Paused == selectCall.getState()) {//恢复已暂停的通话
                resumeCall(selectCall);//恢复通话
            }
            showVideo();//显示视频视图
            mCall = selectCall;//设置当前通话为选中的通话
            refreshListIncomingUsers(i, 1);//刷新呼入用户列表
        } else {
            if (Call.State.IncomingReceived == mCall.getState() || Call.State.IncomingEarlyMedia == mCall.getState()) {//接听当前通话（未接听的）
                acceptCall(mCall);//接听电话
                refreshListIncomingUsers(0, 1);//刷新呼入用户列表
            } else if (Call.State.StreamsRunning == mCall.getState()) {//暂停通话
                SiphoneManager.getInstance().pauseCall(mCall);//暂停通话
                refreshListIncomingUsers(i, 2);//刷新呼入用户列表
            } else if (Call.State.Paused == mCall.getState()) {//恢复通话
                resumeCall(mCall);//恢复通话
                refreshListIncomingUsers(i, 1);//刷新呼入用户列表
            }
        }
    }

    /**
     * 刷新呼入用户列表
     *
     * @param index
     */
    public void refreshListIncomingUsers(int index, int state) {
        mCallAdapter.select(index, state);//切换通话并刷新
    }

    /**
     * 修改呼入用户列表状态
     *
     * @param index
     */
    public void updateState(int index, int state) {
        mCallAdapter.updateState(index, state);//修改呼入用户列表状态
    }

    /**
     * 添加通话
     */
    private void addCall(Call call, int direct, boolean isCurrent) {
        SipCall sipCall = new SipCall(call, direct, 0, isCurrent);
        mCallList.add(sipCall);
        if (mCallAdapter != null) {
            mCallAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 移除通话
     */
    private void removeCall(Call call) {
        String number = call.getCallLog().getFromAddress().getUsername();
        for (int i = 0; i < mCallList.size(); i++) {
            if (number.equals(mCallList.get(i).getCall().getCallLog().getRemoteAddress().getUsername())) {
                mCallList.remove(i);
                break;
            }
        }
        if (mCallAdapter != null) {
            mCallAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 显示视频视图
     */
    public void showVideo() {
        SiphoneManager.getInstance().setWindows(mRemoteVideo, mLocalPreview);//设置视频窗口
        SiphoneManager.getInstance().setMimeType();//设置编码格式
        SiphoneManager.getInstance().enableCamera();//启用摄像头
        SiphoneManager.getInstance().enableVideo();//启用视频
        mRemoteVideo.setVisibility(View.VISIBLE);
        mLocalPreview.setVisibility(View.VISIBLE);
    }

    /**
     * 移除视频视图
     */
    public void removeVideo() {
        Core core = SiphoneManager.getInstance().getCore();
        if (core != null) {
            core.setNativeVideoWindowId(null);
            core.setNativePreviewWindowId(null);
        }
    }

    /**
     * 获取当前通话
     */
    private void getCurrentCall() {
        if (SiphoneManager.getInstance().getCore() != null) {
            for (Call call : SiphoneManager.getInstance().getCore().getCalls()) {
                if (direct == 0 && Call.State.IncomingReceived == call.getState() || Call.State.IncomingEarlyMedia == call.getState()) {//新的通话 - 呼出的电话
                    mCall = call;//设置呼入通话实例
                    String number = call.getCallLog().getFromAddress().getUsername();
                    String username = call.getCallLog().getFromAddress().getDisplayName();
                    Log.d(TAG, "IncomingReceived number=" + number + " username=" + username);
                    break;
                } else if (direct == 1 && Call.State.OutgoingInit == call.getState() || Call.State.OutgoingProgress == call.getState()) {//新的通话 - 呼叫的电话
                    mCall = call;//设置呼入通话实例
                    mOutCall = call;//设置呼出通话实例
                    String number = call.getCallLog().getRemoteAddress().getUsername();
                    String username = call.getCallLog().getRemoteAddress().getDisplayName();
                    Log.d(TAG, "OutgoingInit number=" + number + " username=" + username);
                    break;
                }
            }
        }
    }

    /**
     * 更新当前通话
     */
    private void updateCurrentCall() {
        if (SiphoneManager.getInstance().getCore() != null) {
            for (Call call : SiphoneManager.getInstance().getCore().getCalls()) {
                if (Call.State.StreamsRunning == call.getState()) {//已有通话
                    mCall = call;
                    onAcceptCall(mCall);//监听接听事件
                    break;
                }
            }
        }
    }

    /**
     * 监听接听事件
     */
    public void onAcceptCall(Call call) {
        mAcceptCall.setVisibility(View.GONE);//隐藏接听按钮
        server_icon.setVisibility(View.GONE);//隐藏服务图标
        cancelAndHiddenWaitTime();//取消隐藏等待时间
        showCallTime();//显示通话时间
        showVideo();//显示视频视图
        showCallConnectedInfo(call);//显示通话接通信息
    }

    /**
     * 接听通话
     */
    public void acceptCall(Call call) {
        SiphoneManager.getInstance().acceptCall(call);//接听通话
        onAcceptCall(call);//监听接听事件
    }

    /**
     * 恢复通话
     */
    public void resumeCall(Call call) {
        SiphoneManager.getInstance().resumeCall(call);//接听通话
        onAcceptCall(call);//监听接听事件
    }

    /**
     * 挂断所有呼入通话
     */
    public void terminateCallInCalls() {
        for (SipCall sipCall : mCallList) {
            if (sipCall.getCall() != null && mOutCall != sipCall) {
                sipCall.getCall().terminate();
            }
        }
    }

    /**
     * 挂断通话
     */
    public void terminateCall() {
        if (mOutCall != null && mCallList.size() != 0) {
            terminateCallInCalls();//挂断所有呼入通话
        } else {
            SiphoneManager.getInstance().terminateAllCalls();//挂断所有通话
        }
    }

    /**
     * 设置点击事件
     */
    private void setOnclick() {
        //设置挂断事件
        mTerminateCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                terminateCall();
            }
        });
        //设置接听事件
        mAcceptCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptCall(mCall);//接听电话
                refreshListIncomingUsers(0, 1);//刷新呼入用户列表
            }
        });
        //设置转移事件
        mTransferCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mContacts.setVisibility(mContacts.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                //                SiphoneManager.getInstance().transferCall(mCall, "1030");//接听通话
            }
        });
        //设置远程视频事件
        mRemoteVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
        //设置本地预览事件
        mLocalPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLocalPreview.setVisibility(mLocalPreview.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                local_preview.setVisibility(mLocalPreview.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        //设置本地预览事件
        local_preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                local_preview.setVisibility(local_preview.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mLocalPreview.setVisibility(local_preview.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
    }

    /**
     * 显示等待时间
     */
    private void showWaitTime() {
        if (mCall != null) {
            mWaitTimer.setBase(SystemClock.elapsedRealtime() - 1000 * mCall.getDuration());
            mWaitTimer.setFormat("%s");
            mWaitTimer.start();
            mWaitTimer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 取消隐藏等待时间
     */
    private void cancelAndHiddenWaitTime() {
        if (mWaitTimer != null) {
            mWaitTimer.stop();
            mWaitTimer.setVisibility(View.GONE);//隐藏等待计时器
        }
    }

    /**
     * 显示通话时间
     */
    private void showCallTime() {
        if (mCall != null) {
            mCallTimer.setBase(SystemClock.elapsedRealtime() - 1000 * mCall.getDuration());
            mCallTimer.setFormat("%s");
            mCallTimer.start();
            mCallTimer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 取消隐藏通话时间
     */
    private void cancelAndHiddenCallTime() {
        if (mCallTimer != null) {
            mCallTimer.stop();
            mCallTimer.setVisibility(View.GONE);//隐藏等待计时器
        }
    }

    /**
     * 显示通话前(呼入/呼叫)信息
     */
    private void showCallBeforeInfo() {
        direct = getIntent().getIntExtra("direct", -1);
        Call call = SiphoneManager.getInstance().getCore().getCurrentCall();
        if (direct == 0) {//呼入
            String username = call.getCallLog().getFromAddress().getUsername();
            mCalluser.setText(username + "呼入");
        } else if (direct == 1) {//呼出
            String username = call.getCallLog().getRemoteAddress().getUsername();
            mAcceptCall.setVisibility(View.GONE);
            mTransferCall.setVisibility(View.GONE);
            mCalluser.setText("呼叫" + username);
        }
    }

    /**
     * 显示通话接通信息
     */
    private void showCallConnectedInfo(Call call) {
        direct = getIntent().getIntExtra("direct", -1);
        //        Call call = SiphoneManager.getInstance().getCore().getCurrentCall();
        String username = call.getCallLog().getRemoteAddress().getUsername();
        mCalluser.setText("正在与" + username + "通话中");
    }

    /**
     * 监听挂断事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onEnd(Core core, Call call, Call.State state, String message) {
        removeCall(call);//移除通话
        if (mOutCall == call) {
            //当呼出通话未接通而自动结束时，切换到其他呼入通话
            mOutCall = null;
        }
        if (direct == 1 && mOutCall != null && mCallList.size() == 0) {//恢复呼出通话
            mCall = mOutCall;
            resumeCall(mOutCall);//恢复通话
        } else if (mCallList.size() != 0 && AutoAccept) {
            //判断SiphoneManager.getInstance().getCore().getCalls()是否存在该电话
            onChangeCall(0);//分两种情况，一是正常挂断、二是未接通而结束
        }
        if ((direct == 0 && mCallList.size() == 0) || (direct == 1 && mOutCall == null && mCallList.size() == 0)) {
            finish();//关闭通话界面
        }
        AutoAccept = true;
    }

    //    /**
    //     * 监听接听事件
    //     *
    //     * @param core
    //     * @param call
    //     * @param state
    //     * @param message
    //     */
    //    @Override
    //    public void onAcceptCall(Core core, Call call, Call.State state, String message) {
    //        if (direct == 0) {//呼入
    //            if (SiphoneManager.getInstance().getCallback() != null) {
    //                SiphoneManager.getInstance().getCallback().acceptCall(acceptCall, core, call, state, message);//监听接听事件
    //            }
    //        } else if (direct == 1) {//呼出
    //        }
    //    }
    //
    //    /**
    //     * 监听转移事件
    //     *
    //     * @param core
    //     * @param call
    //     * @param state
    //     * @param message
    //     */
    //    @Override
    //    public void onTransferCall(Core core, Call call, Call.State state, String message) {
    //
    //    }

    /**
     * 监听呼入事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onIncomingReceived(Core core, Call call, Call.State state, String message) {
        mState.setText("呼入电话：" + call.getCallLog().getRemoteAddress().getUsername() + " 状态：" + call.getState().toString() + "\n");
        // 添加并显示呼叫列表
        addCall(call, 0, false);//添加通话
    }

    /**
     * 监听呼叫事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onOutgoingInit(Core core, Call call, Call.State state, String message) {
        mState.setText("呼叫电话：" + call.getCallLog().getRemoteAddress().getUsername() + " 状态：" + call.getState().toString() + "\n");
    }

    //    /**
    //     * 监听正在呼叫事件
    //     *
    //     * @param core
    //     * @param call
    //     * @param state
    //     * @param message
    //     */
    //    @Override
    //    public void onOutgoingProgress(Core core, Call call, Call.State state, String message) {
    //
    //    }

    /**
     * 监听电话接通事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onConnected(Core core, Call call, Call.State state, String message) {
        mState.setText("接通电话：" + call.getCallLog().getRemoteAddress().getUsername() + " 状态：" + call.getState().toString() + "\n");
        if (direct == 1) {//呼出
            mCall = call;
        }
    }

    //    /**
    //     * 监听呼叫响铃事件
    //     *
    //     * @param core
    //     * @param call
    //     * @param state
    //     * @param message
    //     */
    //    @Override
    //    public void onOutgoingRinging(Core core, Call call, Call.State state, String message) {
    //
    //    }

    /**
     * 监听数据流传输事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onStreamsRunning(Core core, Call call, Call.State state, String message) {
        if (direct == 1 && mRemoteVideo.getVisibility() == View.GONE) {
            updateCurrentCall();
        }
    }

    /**
     * 监听正在暂停事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onPausing(Core core, Call call, Call.State state, String message) {
        mState.setText("正在暂停：" + call.getCallLog().getRemoteAddress().getUsername() + " 状态：" + call.getState().toString() + "\n");

    }

    /**
     * 监听暂停事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onPaused(Core core, Call call, Call.State state, String message) {
        mState.setText("暂停电话：" + call.getCallLog().getRemoteAddress().getUsername() + " 状态：" + call.getState().toString() + "\n");
    }

    /**
     * 监听恢复事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onResuming(Core core, Call call, Call.State state, String message) {
        mState.setText("恢复电话：" + call.getCallLog().getRemoteAddress().getUsername() + " 状态：" + call.getState().toString() + "\n");
    }

    /**
     * 监听错误事件
     *
     * @param core
     * @param call
     * @param state
     * @param message
     */
    @Override
    public void onError(Core core, Call call, Call.State state, String message) {
        call.terminate();
        finish();
    }
    //
    //    /**
    //     * 监听转移事件
    //     *
    //     * @param core
    //     * @param call
    //     * @param state
    //     * @param message
    //     */
    //    @Override
    //    public void onReferred(Core core, Call call, Call.State state, String message) {
    //
    //    }
}