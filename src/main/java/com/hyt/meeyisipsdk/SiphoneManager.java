package com.hyt.meeyisipsdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.TextureView;
import android.widget.Toast;

import com.hyt.meeyisipsdk.activitys.SipCallActivity;
import com.hyt.meeyisipsdk.settings.LinphonePreferences;
import com.hyt.meeyisipsdk.utils.AndroidAudioManager;
import com.hyt.meeyisipsdk.utils.FileUtils;
import com.hyt.meeyisipsdk.utils.IdWorker;
import com.hyt.meeyisipsdk.utils.LinphoneUtils;

import org.linphone.core.AccountCreator;
import org.linphone.core.AccountCreatorListenerStub;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.ConfiguringState;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.Friend;
import org.linphone.core.FriendList;
import org.linphone.core.GlobalState;
import org.linphone.core.MediaEncryption;
import org.linphone.core.PayloadType;
import org.linphone.core.ProxyConfig;
import org.linphone.core.StreamType;
import org.linphone.core.TransportType;
import org.linphone.core.VersionUpdateCheckResult;
import org.linphone.core.VideoActivationPolicy;
import org.linphone.core.tools.H264Helper;
import org.linphone.core.tools.Log;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;

import java.util.Timer;
import java.util.TimerTask;

import static com.hyt.meeyisipsdk.SiPhoneConstants.PHONE_TID_KEY;

/*
 * All rights Reserved, Designed By www.huanyutong.com
 * <p>项目名称: MeeyiSipSdk</p>
 * <p>包　　名: com.hyt.meeyisipsdk</p>
 * <p>文件名称: SiphoneManager</p>
 * <p>描　　述: [用一句话描述该文件做什么]</p>
 * <p>创建时间: 2020-12-02 09:32</p>
 * <p>公司信息: 福建环宇通信息科技股份公司 研发部</p>
 * @author <a href="mailto:344572231@qq.com">LinShiJing</a>
 * @version v1.0
 * @Copyright: 2020 www.huanyutong.com Inc. All rights reserved.
 * 注意：本内容仅限于福建环宇通信息科技股份公司内部传阅，禁止外泄以及用于其他的商业目的
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 * @update [1][2020-12-02] [LinShiJing][变更描述]
 */
public class SiphoneManager {
    private String TAG = SiphoneManager.class.getSimpleName();

    private static SiphoneManager mInstance;//单例
    private Context mContext;//上下文

    private String deviceSN;//设备sn
//    private String PHONE_TID_KEY = "MEEYI";//tid标识
    private Core mCore;//Linphone核心类
    private Call mCall;//当前通话实例
    private AccountCreator mAccountCreator;//账号管理类
    private AndroidAudioManager mAudioManager;
    //    private CallManager mCallManager;//通话管理类
    private PhoneStateListener mPhoneStateListener;//状态监听器
    private CoreListenerStub mCoreListener;//全局监听器
    private AccountCreatorListenerStub mAccountCreatorListener;//账号监听器
    private LinphonePreferences mPrefs;//Linphone配置
    private Runnable mIterateRunnable;//启动线程
    private Timer mTimer;//计时器
    private SiphoneCallCallback callback;//通话界面回调
    private SiphoneInCallCallback incallback;//通话界面回调

    private String mBasePath;
    private String mRingSoundFile;
    private String mCallLogDatabaseFile;
    private String mFriendsDatabaseFile;
    private String mUserCertsPath;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mProximityWakelock;

    public Core getCore() {
        return this.mCore;
    }

    public Call getCall() {
        return mCall;
    }

    public SiphoneCallCallback getCallback() {
        return callback;
    }

    public void setCallback(SiphoneCallCallback callback) {
        this.callback = callback;
    }

    public void setIncallback(SiphoneInCallCallback incallback) {
        this.incallback = incallback;
    }

    public SiphoneInCallCallback getIncallback() {
        return incallback;
    }

    public void removeIncallback(SiphoneInCallCallback incallback) {
        this.incallback = null;
    }

    /**
     * 构建Siphone
     */
    private SiphoneManager() {
    }

    /**
     * 获取Siphone单例
     *
     * @return
     */
    public static SiphoneManager getInstance() {
        if (mInstance == null) {
            synchronized (SiphoneManager.class) {
                mInstance = new SiphoneManager();
            }
        }
        return mInstance;
    }

    /**
     * 初始化参数
     */
    public void init(Context context) {
        this.mContext = context;
        this.mPrefs = LinphonePreferences.instance();
        this.mPrefs.setContext(context);
        mBasePath = this.mContext.getFilesDir().getAbsolutePath();
        mCallLogDatabaseFile = mBasePath + "/linphone-log-history.db";
        mFriendsDatabaseFile = mBasePath + "/linphone-friends.db";
        mRingSoundFile = mBasePath + "/share/sounds/linphone/rings/notes_of_the_optimistic.mkv";
        mUserCertsPath = mBasePath + "/user-certs";
        mPowerManager = (PowerManager) this.mContext.getSystemService(Context.POWER_SERVICE);
        //        this.mCallManager = new CallManager(context);
        createPhoneStateListener();//创建状态监听器
        createCoreListener();//创建全局监听器
        createAccountCreatorListener();//创建账号监听器
        startLibLinphone(true);//
    }

    /**
     * 初始化账号管理类
     */
    private void initAccountCreator() {
        if (mAccountCreator == null) {
            this.mCore.loadConfigFromXml(LinphonePreferences.instance().getDefaultDynamicConfigFile());
            mAccountCreator = this.mCore.createAccountCreator(null);
        }
    }

    /**
     * 创建状态监听器
     */
    private void createPhoneStateListener() {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "==CALL_STATE_OFFHOOK");
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "==CALL_STATE_RINGING");
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(TAG, "==CALL_STATE_IDLE");
                        break;
                }
            }
        };
    }

    /**
     * 创建全局监听器
     */
    private void createCoreListener() {
        mCoreListener = new CoreListenerStub() {
            @Override
            public void onGlobalStateChanged(final Core core, final GlobalState state, final String message) {
                Log.d(TAG, "==onGlobalStateChanged");
                if (state == GlobalState.On) {
                    try {
                        initLiblinphone(core);
                    } catch (Exception e) {
                        Log.d(TAG, "==onGlobalStateChanged 异常" + e.getMessage());
                    }
                }
            }

            @Override
            public void onConfiguringStatus(Core core, ConfiguringState state, String message) {
                Log.d(TAG, "==onConfiguringStatus state=" + message);
                if (state == ConfiguringState.Successful) {
                }
            }

            @SuppressLint("Wakelock")
            @Override
            public void onCallStateChanged(final Core core, final Call call, final Call.State state, final String message) {
                Log.d(TAG, "==onCallStateChanged state=" + message);
                if (state == Call.State.Idle) {

                } else if (state == Call.State.IncomingReceived) {
                    onIncomingReceived(core, call, state, message);//监听呼入
                } else if (state == Call.State.OutgoingInit) {
                    onOutgoingInit(core, call, state, message);//监听呼叫初始化
                } else if (state == Call.State.OutgoingProgress) {
                    onOutgoingProgress(core, call, state, message);//监听正在呼出
                } else if (state == Call.State.OutgoingRinging) {
                    onOutgoingRinging(core, call, state, message);//监听正在呼出
                } else if (state == Call.State.OutgoingEarlyMedia) {
                    onOutgoingEarlyMedia(core, call, state, message);//监听呼出前的媒体
                } else if (state == Call.State.Connected) {
                    onConnected(core, call, state, message);//监听电话接通
                } else if (state == Call.State.StreamsRunning) {
                    onStreamsRunning(core, call, state, message);//监听数据流传输
                } else if (state == Call.State.Pausing) {
                    onPausing(core, call, state, message);//监听正在暂停
                } else if (state == Call.State.Paused) {
                    onPaused(core, call, state, message);//监听通话暂停成功
                } else if (state == Call.State.Resuming) {
                    onResuming(core, call, state, message);//监听通话被恢复
                } else if (state == Call.State.Referred) {
                    onReferred(core, call, state, message);//监听呼叫转移
                } else if (state == Call.State.Error) {
                    onError(core, call, state, message);//监听通话错误
                } else if (state == Call.State.End) {
                    onEnd(core, call, state, message);//监听通话结束
                } else if (state == Call.State.PausedByRemote) {
                    Log.d(TAG, "PausedByRemote" + message);
                } else if (state == Call.State.UpdatedByRemote) {
                    Log.d(TAG, "UpdatedByRemote" + message);
                } else if (state == Call.State.IncomingEarlyMedia) {
                    Log.d(TAG, "IncomingEarlyMedia" + message);
                } else if (state == Call.State.Updating) {
                    Log.d(TAG, "Updating" + message);
                } else if (state == Call.State.Released) {
                    Log.d(TAG, "Released" + message);
                } else if (state == Call.State.EarlyUpdatedByRemote) {
                    Log.d(TAG, "EarlyUpdatedByRemote" + message);
                } else if (state == Call.State.EarlyUpdating) {
                    Log.d(TAG, "EarlyUpdating" + message);
                }
            }

            @Override
            public void onVersionUpdateCheckResultReceived(Core core, VersionUpdateCheckResult result, String version, String url) {
                Log.d(TAG, "==onVersionUpdateCheckResultReceived state=" + url);
            }

            @Override
            public void onFriendListCreated(Core core, FriendList list) {
                Log.d(TAG, "==onFriendListCreated list=" + list);
            }

            @Override
            public void onFriendListRemoved(Core core, FriendList list) {
                Log.d(TAG, "==onFriendListRemoved list=" + list);
            }
        };
    }

    /**
     * 初始化Liphone
     *
     * @param core
     */
    private void initLiblinphone(Core core) {
        this.mCore = core;
        mAudioManager = new AndroidAudioManager(mContext);

        this.mCore.setZrtpSecretsFile(mContext.getFilesDir().getAbsolutePath() + "/zrtp_secrets");

        String deviceName = "hua wei";
        String appName = mContext.getResources().getString(com.hyt.meeyisipsdk.R.string.user_agent);
        String androidVersion = BuildConfig.VERSION_NAME;
        String userAgent = appName + "/" + androidVersion + " (" + deviceName + ") LinphoneSDK";

        //        this.mCore.setUserAgent(userAgent, "4.3.0-beta.16+a991920(remotes/origin/release/4.3)");

        // mCore.setChatDatabasePath(mChatDatabaseFile);
        this.mCore.setCallLogsDatabasePath(mCallLogDatabaseFile);
        this.mCore.setFriendsDatabasePath(mFriendsDatabaseFile);
        this.mCore.setUserCertificatesPath(mUserCertsPath);
        // mCore.setCallErrorTone(Reason.NotFound, mErrorToneFile);
        enableDeviceRingtone(true);

        int availableCores = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "==[Manager] MediaStreamer : " + availableCores + " cores detected and configured");

        this.mCore.migrateLogsFromRcToDb();

        // Migrate existing linphone accounts to have conference factory uri and LIME X3Dh url set
        String uri = "sip:conference-factory@sip.linphone.org";
        for (ProxyConfig lpc : this.mCore.getProxyConfigList()) {
            if (lpc.getIdentityAddress().getDomain().equals("sip.linphone.org")) {
                if (lpc.getConferenceFactoryUri() == null) {
                    lpc.edit();
                    Log.d(TAG, "==[Manager] Setting conference factory on proxy config " + lpc.getIdentityAddress().asString() + " to default value: " + uri);
                    lpc.setConferenceFactoryUri(uri);
                    lpc.done();
                }

                if (this.mCore.limeX3DhAvailable()) {
                    String url = mCore.getLimeX3DhServerUrl();
                    if (url == null || url.isEmpty()) {
                        url = "https://lime.linphone.org/lime-server/lime-server.php";
                        Log.d(TAG, "==[Manager] Setting LIME X3Dh server url to default value: " + url);
                        this.mCore.setLimeX3DhServerUrl(url);
                    }
                }
            }
        }

        //        if (mContext.getResources().getBoolean(R.bool.enable_push_id)) {
        //            PushNotificationUtils.init(mContext);
        //        }

        mProximityWakelock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, mContext.getPackageName() + ";manager_proximity_sensor");

        resetCameraFromPreferences();//设置摄像头
        //        setMimeType();//设置编码格式

        String url = Factory.instance().createConfig(mBasePath + "/.linphonerc").getString("assistant", "xmlrpc_url", null);
        mAccountCreator = this.mCore.createAccountCreator(url);
        mAccountCreator.setListener(mAccountCreatorListener);
    }

    /**
     * 设置编码格式
     */
    public void setMimeType() {
        for (final PayloadType pt : mCore.getVideoPayloadTypes()) {
            if ("H264".equals(pt.getMimeType())) {
                pt.enable(true);
            } else {
                pt.enable(false);
            }
        }
    }

    /**
     * 设置摄像头
     */
    private void resetCameraFromPreferences() {
        boolean useFrontCam = false;
        int camId = 0;
        AndroidCameraConfiguration.AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
        for (AndroidCameraConfiguration.AndroidCamera androidCamera : cameras) {
            if (androidCamera.frontFacing == useFrontCam) {
                camId = androidCamera.id;
                break;
            }
        }
        String[] devices = mCore.getVideoDevicesList();
        if (camId >= devices.length) {
            Log.d(TAG, "==[Manager] Trying to use a camera id that's higher than the linphone's devices list, using 0 to prevent crash...");
            camId = 0;
        }
        String newDevice = devices[camId];
        this.mCore.setVideoDevice(newDevice);
    }

    /**
     * 启用摄像头 - 本地预览窗口生效
     */
    public void enableCamera() {
        Call call = mCore.getCurrentCall();
        if (call != null) {
            call.enableCamera(true);
        }
    }

    /**
     * 启用视频 - 远程视频窗口生效
     */
    public void enableVideo() {
        Call call = mCore.getCurrentCall();
        if (call != null) {
            CallParams params = mCore.createCallParams(call);
            params.enableVideo(true);
            params.setAudioBandwidthLimit(40);
            call.update(params);
        }
    }

    /**
     * 设置远程视频窗口
     */
    public void setVideoWindow(TextureView mRemoteVideo) {
        mCore.setNativeVideoWindowId(mRemoteVideo);
    }

    /**
     * 设置本地预览窗口
     */
    public void setPreviewWindow(TextureView mLocalPreview) {
        mCore.setNativePreviewWindowId(mLocalPreview);
    }

    /**
     * 设置远程视频/本地预览窗口
     *
     * @param mRemoteVideo
     * @param mLocalPreview
     */
    public void setWindows(TextureView mRemoteVideo, TextureView mLocalPreview) {
        setVideoWindow(mRemoteVideo);
        setPreviewWindow(mLocalPreview);
    }

    /**
     * 设置铃声
     *
     * @param use
     */
    public void enableDeviceRingtone(boolean use) {
        if (use) {
            this.mCore.setRing(null);
        } else {
            this.mCore.setRing(mRingSoundFile);
        }
    }

    /**
     * 跳转通话界面
     */
    private void goCallActivity(int direct) {
        Intent intent = new Intent(mContext, SipCallActivity.class);
        intent.putExtra("direct", direct);
        //        intent.putExtra("bean", JSON.toJSONString(call));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 监听监听呼入
     */
    private void onIncomingReceived(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onIncomingReceived message=" + message);
        if (this.mCall == null) {
            this.mCall = call;
        }
        if (this.callback != null) {
            this.callback.onIncomingReceived(core, call, state, message);
        }
        refuse();//拒接通话 - 根据当前持有通话数量是否大于15，如是拒接，并发送拒接原因
        isCanAccept();//是否接听 - 根据当前通话是否已呼出，判断是否可接听
        preventRepeatCalls();//根据当前通话是否已存在于通话列表，防止重复接听
        goCallActivity(0);//跳转通话界面
        if (this.incallback != null) {
            this.incallback.onIncomingReceived(core, call, state, message);
        }
    }

    /**
     * 拒接通话 - 根据当前持有通话数量是否大于15，如是拒接，并发送拒接原因
     */
    private void refuse() {

    }

    /**
     * 是否接听 - 根据当前通话是否已呼出，判断是否可接听
     */
    private void isCanAccept() {

    }

    /**
     * 防止重复接听 - 根据当前通话是否已存在于通话列表
     */
    private void preventRepeatCalls() {

    }

    /**
     * 监听呼叫初始化
     */
    private void onOutgoingInit(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onOutgoingInit message=" + message);
        if (this.callback != null) {
            this.callback.onOutgoingInit(core, call, state, message);
        }
        if (this.incallback != null) {
            this.incallback.onOutgoingInit(core, call, state, message);
        }
        goCallActivity(1);//跳转通话界面
    }

    /**
     * 监听正在呼出
     */
    private void onOutgoingProgress(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onOutgoingProgress message=" + message);
        if (this.callback != null) {
            this.callback.onOutgoingProgress(core, call, state, message);
        }
    }

    /**
     * 监听呼叫正在振铃
     */
    private void onOutgoingRinging(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onOutgoingRinging message=" + message);
        if (this.callback != null) {
            this.callback.onOutgoingRinging(core, call, state, message);
        }
    }

    /**
     * 监听呼出前的媒体
     */
    private void onOutgoingEarlyMedia(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onOutgoingEarlyMedia message=" + message);
        if (this.callback != null) {
            this.callback.onOutgoingEarlyMedia(core, call, state, message);
        }
    }

    /**
     * 监听电话接通
     */
    private void onConnected(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onConnected message=" + message);
        if (this.callback != null) {
            this.callback.onConnected(core, call, state, message);
        }
        if (this.incallback != null) {
            this.incallback.onConnected(core, call, state, message);
        }
    }

    /**
     * 监听数据流传输
     */
    private void onStreamsRunning(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onStreamsRunning message=" + message);
        if (this.callback != null) {
            this.callback.onStreamsRunning(core, call, state, message);
        }
        if (this.incallback != null) {
            this.incallback.onStreamsRunning(core, call, state, message);
        }
    }

    /**
     * 监听正在暂停
     */
    private void onPausing(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onPausing message=" + message);
        if (this.callback != null) {
            this.callback.onPausing(core, call, state, message);
        }
        if (this.incallback != null) {
            this.incallback.onPausing(core, call, state, message);
        }
    }

    /**
     * 监听通话暂停成功
     */
    private void onPaused(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onPaused message=" + message);
        if (this.callback != null) {
            this.callback.onPaused(core, call, state, message);
        }
        if (this.incallback != null) {
            this.incallback.onPaused(core, call, state, message);
        }
    }

    /**
     * 监听通话被恢复
     */
    private void onResuming(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onResuming message=" + message);
        if (this.callback != null) {
            this.callback.onResuming(core, call, state, message);
        }
        if (this.incallback != null) {
            this.incallback.onResuming(core, call, state, message);
        }
    }

    /**
     * 监听呼叫转移
     */
    private void onReferred(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onReferred message=" + message);
        if (this.callback != null) {
            this.callback.onReferred(core, call, state, message);
        }
    }

    /**
     * 监听通话错误
     */
    private void onError(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onError message=" + message);
        if (this.callback != null) {
            this.callback.onError(core, call, state, message);
        }
    }

    /**
     * 监听通话结束
     */
    private void onEnd(Core core, Call call, Call.State state, String message) {
        Log.d(TAG, "==onEnd message=" + message);
        if (this.callback != null) {
            this.callback.onEnd(core, call, state, message);
        }
        if (this.mCall != null) {
            this.mCall = null;
        }
        if (this.incallback != null) {
            this.incallback.onEnd(core, call, state, message);
        }
    }

    /**
     * 创建账号监听器
     */
    private void createAccountCreatorListener() {
        mAccountCreatorListener = new AccountCreatorListenerStub() {
            @Override
            public void onIsAccountExist(AccountCreator accountCreator, AccountCreator.Status status, String resp) {
                Log.d(TAG, "==onIsAccountExist resp=" + resp);
                if (status.equals(AccountCreator.Status.AccountExist)) {
                    accountCreator.isAccountLinked();
                }
            }

            @Override
            public void onLinkAccount(AccountCreator accountCreator, AccountCreator.Status status, String resp) {
                Log.d(TAG, "==onLinkAccount resp=" + resp);
                if (status.equals(AccountCreator.Status.AccountNotLinked)) {
                }
            }

            @Override
            public void onIsAccountLinked(AccountCreator accountCreator, AccountCreator.Status status, String resp) {
                Log.d(TAG, "==onIsAccountLinked resp=" + resp);
                if (status.equals(AccountCreator.Status.AccountNotLinked)) {
                }
            }
        };
    }

    /**
     * 启动LipLinphone
     *
     * @param isPush
     */
    public void startLibLinphone(boolean isPush) {
        try {
            this.mCore =
                    Factory.instance()
                            .createCore(
                                    mPrefs.getLinphoneDefaultConfig(),
                                    mPrefs.getLinphoneFactoryConfig(),
                                    mContext);
            this.mCore.addListener(mCoreListener);

            if (isPush) {
                Log.d(TAG, "==[Manager] We are here because of a received push notification, enter background mode before starting the Core");
                this.mCore.enterBackground();
            }

            this.mCore.start();

            mIterateRunnable =
                    new Runnable() {
                        @Override
                        public void run() {
                            if (mCore != null) {
                                mCore.iterate();
                            }
                        }
                    };
            TimerTask lTask =
                    new TimerTask() {
                        @Override
                        public void run() {
                            LinphoneUtils.dispatchOnUIThread(mIterateRunnable);
                        }
                    };
            /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
            mTimer = new Timer("Linphone scheduler");
            mTimer.schedule(lTask, 0, 20);

            initAccountCreator();
        } catch (Exception e) {
            Log.d(TAG, "==[Manager] Cannot start linphone");
        }

        // H264 codec Management - set to auto mode -> MediaCodec >= android 5.0 >= OpenH264
        H264Helper.setH264Mode(H264Helper.MODE_AUTO, this.mCore);
    }

    /**
     * 配置账户
     *
     * @param domain
     * @param name
     * @param password
     * @param displayName
     * @param transportType
     */
    public void configureAccount(String domain, String name, String password, String displayName, TransportType transportType, String deviceSN) {
        mAccountCreator.setUsername(name);
        mAccountCreator.setDomain(domain);
        mAccountCreator.setPassword(password);
        mAccountCreator.setDisplayName(displayName);
        mAccountCreator.setTransport(transportType);
        this.deviceSN = deviceSN;
        ProxyConfig cfg = mAccountCreator.createProxyConfig();
        if (null != cfg) {
            this.mCore.setDefaultProxyConfig(cfg);
        }
        //        createProxyConfigAndLeaveAssistant();
    }

    /**
     * 删除账户
     */
    public void removeAccount() {
        ProxyConfig config = this.mCore.getDefaultProxyConfig();
        if (config != null) {
            this.mCore.removeProxyConfig(config);
        }
    }

    /**
     * 普通呼叫
     */
    public void call(String to, String displayName) {
        newOutgoingCall(to,displayName,0);
    }

    /**
     * 紧急呼叫
     */
    public void sos(String to, String displayName) {
        newOutgoingCall(to,displayName,1);
    }

    /**
     * 紧急增援
     */
    public void emergencyReinforcement(String to, String displayName) {
        newOutgoingCall(to,displayName,2);
    }

    /**
     * 拨打电话
     */
    public void newOutgoingCall(String to, String displayName, int level) {
        if (to == null)
            return;

        Address address;
        address = this.mCore.interpretUrl(to); // InterpretUrl does normalizePhoneNumber
        if (address == null) {
            Log.d(TAG, "==[Call Manager] Couldn't convert to String to Address : " + to);
            return;
        }

        ProxyConfig lpc = this.mCore.getDefaultProxyConfig();
        if (mContext.getResources().getBoolean(com.hyt.meeyisipsdk.R.bool.forbid_self_call) && lpc != null && address.weakEqual(lpc.getIdentityAddress())) {
            return;
        }
        address.setDisplayName(displayName);

        boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(mContext);

        if (this.mCore.isNetworkReachable()) {
            if (Version.isVideoCapable()) {
                inviteAddress(address, true, isLowBandwidthConnection, false,level);
            } else {
                inviteAddress(address, false, isLowBandwidthConnection, false,level);
            }
        } else {
            Toast.makeText(mContext, mContext.getString(com.hyt.meeyisipsdk.R.string.error_network_unreachable), Toast.LENGTH_LONG).show();
            Log.d(TAG, "==[Call Manager] Error: " + mContext.getString(com.hyt.meeyisipsdk.R.string.error_network_unreachable));
        }
    }

    /**
     * 建立通话
     *
     * @param address
     * @param forceZRTP
     */
    private void inviteAddress(Address address, boolean videoEnabled, boolean lowBandwidth, boolean forceZRTP,int level) {
        CallParams params = this.mCore.createCallParams(null);
        //        mBandwidthManager.updateWithProfileSettings(params);

        if (videoEnabled) {
            params.enableVideo(true);
            this.mCore.enableVideoCapture(true);
            this.mCore.enableVideoDisplay(true);
            this.mCore.enableVideoPreview(true);
        } else {
            params.enableVideo(false);
        }

        IdWorker idWorker = new IdWorker(0, 0);
        String tid = "T=" + idWorker.nextId() + "&L=" + level + "&P=" + this.deviceSN;
        params.addCustomSdpAttribute(PHONE_TID_KEY, tid);
        params.addCustomSdpMediaAttribute(StreamType.Audio, PHONE_TID_KEY, tid);

        VideoActivationPolicy vap = this.mCore.getVideoActivationPolicy();
        vap.setAutomaticallyInitiate(true);
        vap.setAutomaticallyAccept(true);
        this.mCore.setVideoActivationPolicy(vap);

        if (lowBandwidth) {
            params.enableLowBandwidth(true);
            Log.d(TAG, "==[Call Manager] Low bandwidth enabled in call params");
        }

        if (forceZRTP) {
            params.setMediaEncryption(MediaEncryption.ZRTP);
        }

        String recordFile = FileUtils.getCallRecordingFilename(mContext, address);
        params.setRecordFile(recordFile);

        this.mCore.inviteAddressWithParams(address, params);
    }

    /**
     * 接听电话
     */
    //    public boolean acceptCall(Call call) {
    //        if (call == null)
    //            return false;
    //
    //        CallParams params = mCore.createCallParams(call);
    //
    //        boolean isLowBandwidthConnection = !LinphoneUtils.isHighBandwidthConnection(mContext);
    //
    //        if (params != null) {
    //            params.enableVideo(true);
    //            params.enableLowBandwidth(isLowBandwidthConnection);
    //            params.setRecordFile(FileUtils.getCallRecordingFilename(mContext, call.getRemoteAddress()));
    //        } else {
    //            Log.d(TAG, "==[Call Manager] Could not create call params for call");
    //            return false;
    //        }
    //
    //        call.acceptWithParams(params);
    //        //        call.accept();
    //        return true;
    //    }

    /**
     * 接听电话
     */
    public boolean acceptCall(Call call) {
        if (call == null)
            return false;

        call.accept();
        return true;
    }

    /**
     * 接听电话
     */
    //    public boolean acceptCall() {
    //        for (Call call : this.mCore.getCalls()) {
    //            if (Call.State.IncomingReceived == call.getState() || Call.State.IncomingEarlyMedia == call.getState()) {
    //                this.mCall = call;
    //                break;
    //            }
    //        }
    //        return acceptCall(this.mCall);
    //    }

    /**
     * 挂断电话
     */
    public void terminateCall() {
        if (mCore == null) {
            return;
        }
        Call call = mCore.getCurrentCall();
        if (call != null) {
            call.terminate();
        } else if (mCore.isInConference()) {
            mCore.terminateConference();
        } else {
            mCore.terminateAllCalls();
        }
    }

    /**
     * 挂断所有电话
     */
    public void terminateAllCalls() {
        if (mCore == null) {
            return;
        }
        mCore.terminateAllCalls();
    }

    /**
     * 挂断电话
     */
    public void terminateCall(Call call) {
        if (call != null) {
            call.terminate();
        }
    }

    /**
     * 转移通话
     *
     * @param call
     * @param referTo
     */
    public void transferCall(Call call, String referTo) {
        //        if (this.mCore != null) {
        //            this.mCore.transferCall(call, referTo);
        //        }
        if (call != null) {
            call.transfer(referTo);
        }
    }

    /**
     * 转移通话
     *
     * @param referTo
     */
    public void transfer(String referTo) {
        if (this.mCore != null) {
            this.mCore.getCurrentCall().transfer(referTo);
        }
    }

    /**
     * 暂停通话
     */
    public void pauseCall(Call call) {
        //        if (this.mCore != null) {
        //            this.mCore.pauseCall(call);
        //        }
        //        if(call == this.mCore.getCurrentCall()){
        call.pause();
        //        }
    }

    /**
     * 恢复通话
     */
    public void resumeCall(Call call) {
        //        if (this.mCore != null) {
        //            this.mCore.resumeCall(call);
        //        }
        if (call != null && call.getState() == Call.State.Paused) {
            call.resume();
        }
    }

    /**
     * 切换静音
     *
     * @param isMicMuted 是否静音
     */
    public void enableMic(boolean isMicMuted) {
        if (this.mCore != null) {
            this.mCore.enableMic(isMicMuted);
        }
    }

    /**
     * 切换免提
     *
     * @param isSpeakerEnabled 是否免提
     */
    public void toggleSpeaker(boolean isSpeakerEnabled) {
        if (isSpeakerEnabled) {
            mAudioManager.routeAudioToSpeaker();
        } else {
            mAudioManager.routeAudioToEarPiece();
        }
    }

    /**
     * 设置铃声音量
     *
     * @param volume 音量大小，范围 0 - 90
     */
    public static void setVolume(int volume) {
        int result = 100 - volume;
        int count = 60;
        int set = (int) (result / 100f * count);
        //        JniLibSpeech.LibSpeechAecCommand(2, set);//设置系统播放音量
        //        JniLibSpeech.LibSpeechAecCommand(4,set);//设置系统录音音量
    }

    public String findContactFromAddress(Address address) {
        if (address == null)
            return null;
        Friend lf = mCore.findFriend(address);
        if (lf != null) {
            return (String) lf.getUserData();
        }

        return null;
    }
}
