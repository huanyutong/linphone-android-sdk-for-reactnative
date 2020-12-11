package com.hyt.meeyisipsdk;

import android.view.View;

import org.linphone.core.Call;
import org.linphone.core.Core;

/*
 * All rights Reserved, Designed By www.huanyutong.com
 * <p>项目名称: MeeyiSipSdk</p>
 * <p>包　　名: com.hyt.meeyisipsdk</p>
 * <p>文件名称: SiphoneCallCallback</p>
 * <p>描　　述: [Liphone监听回调]</p>
 * <p>创建时间: 2020-12-03 18:59</p>
 * <p>公司信息: 福建环宇通信息科技股份公司 研发部</p>
 * @author <a href="mailto:344572231@qq.com">LinShiJing</a>
 * @version v1.0
 * @Copyright: 2020 www.huanyutong.com Inc. All rights reserved.
 * 注意：本内容仅限于福建环宇通信息科技股份公司内部传阅，禁止外泄以及用于其他的商业目的
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 * @update [1][2020-12-03] [LinShiJing][变更描述]
 */
public interface SiphoneCallCallback {
    void onIncomingReceived(Core core, Call call, Call.State state, String message);//监听呼入事件
    void onOutgoingInit(Core core, Call call, Call.State state, String message);//监听呼叫初始化
    void onOutgoingProgress(Core core, Call call, Call.State state, String message);//监听正在呼出
    void onOutgoingRinging(Core core, Call call, Call.State state, String message);//监听呼叫正在振铃
    void onOutgoingEarlyMedia(Core core, Call call, Call.State state, String message);//监听呼出前的媒体
    void onConnected(Core core, Call call, Call.State state, String message);//监听电话接通
    void onStreamsRunning(Core core, Call call, Call.State state, String message);//监听数据流传输
    void onPausing(Core core, Call call, Call.State state, String message);//监听正在暂停
    void onPaused(Core core, Call call, Call.State state, String message);//监听通话暂停成功
    void onResuming(Core core, Call call, Call.State state, String message);//监听通话被恢复
    void onReferred(Core core, Call call, Call.State state, String message);//监听呼叫转移
    void onError(Core core, Call call, Call.State state, String message);//监听通话错误
    void onEnd(Core core, Call call, Call.State state, String message);//监听通话结束

    void onTerminateCall(View view, Core core, Call call, Call.State state, String message);//监听挂断事件
    void acceptCall(View view, Core core, Call call, Call.State state, String message);//监听接听事件
}
