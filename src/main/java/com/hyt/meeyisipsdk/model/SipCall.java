package com.hyt.meeyisipsdk.model;

import org.linphone.core.Call;
import org.linphone.core.StreamType;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.hyt.meeyisipsdk.SiPhoneConstants.PHONE_TID_KEY;

/*
 * All rights Reserved, Designed By www.huanyutong.com
 * <p>项目名称: MeeyiSipSdk</p>
 * <p>包　　名: com.hyt.meeyisipsdk.model</p>
 * <p>文件名称: SipCall</p>
 * <p>描　　述: [通话实体]</p>
 * <p>创建时间: 2020-12-07 17:52</p>
 * <p>公司信息: 福建环宇通信息科技股份公司 研发部</p>
 * @author <a href="mailto:344572231@qq.com">LinShiJing</a>
 * @version v1.0
 * @Copyright: 2020 www.huanyutong.com Inc. All rights reserved.
 * 注意：本内容仅限于福建环宇通信息科技股份公司内部传阅，禁止外泄以及用于其他的商业目的
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 * @update [1][2020-12-07] [LinShiJing][变更描述]
 */
public class SipCall {
    private Call call;//通话实例
    //    private String number;//电话号码
    //    private String name;//通话用户
    private int direct;//通话方向 0:呼入 1:呼出
    private String level;//呼叫等级 0:普通呼叫 1:紧急呼叫 2:紧急增援
    private int state;//通话状态 0:呼入状态 1:正在通话 2:暂停
    private boolean isCurrent;//是否为当前通话

    //******* 会话属性   *******
    public String id;//会话id
    public String tid;//事务id
    public String inComingSN;//sn
    public String displayName;//分机名称
    public String from;//主叫号码
    public String to;//被叫号码
    public String inviteTime;//呼入/呼出的时间
    public String acceptTime;//接听时间

    public String direction;//Callincoming,CallOutgoing

    public SipCall(Call call, int direct, int state, boolean isCurrent) {
        this.call = call;
        this.direct = direct;
        this.state = state;
        this.isCurrent = isCurrent;
        this.direction = call.getCallLog().getDir().name();
        if ("Incoming".equals(direction)) {
            this.from = call.getCallLog().getFromAddress().getUsername();
            this.to = call.getCallLog().getToAddress().getUsername();
        } else {
            this.from = call.getCallLog().getLocalAddress().getUsername();
            this.to = call.getCallLog().getRemoteAddress().getUsername();
        }
        this.displayName = call.getRemoteAddress().getDisplayName();
        this.inviteTime = getTimeNow();
        getCallAttr(call);//获取tid、level、sn
    }

    /**
     * 获取tid、level、sn - 解析通话属性信息
     *
     * @param call
     */
    public void getCallAttr(Call call) {
        String attribute = call.getRemoteParams().getCustomSdpMediaAttribute(StreamType.Audio, PHONE_TID_KEY);
        String[] attrs = attribute.split("&");
        if (null != attrs && attrs.length > 0) {
            for (int i = 0; i < attrs.length; i++) {
                String[] keyArr = attrs[i].split("=");
                if (null != keyArr && keyArr.length > 1) {
                    if (keyArr[0].equals("T")) {
                        this.tid = keyArr[1];
                    } else if (keyArr[0].equals("L")) {
                        if("0".equals(keyArr[1])){
                            level = "普通呼叫";
                        }else if("1".equals(keyArr[1])){
                            level = "紧急呼叫";
                        }else if("2".equals(keyArr[1])){
                            level = "紧急增援";
                        }
                    } else if (keyArr[0].equals("P")) {
                        this.inComingSN = keyArr[1];
                    }
                }
            }
        }
    }

    /**
     * 获取当前通话时间
     */
    public String getTimeNow() {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_str = df.format(date);
        return date_str;
    }

    public Call getCall() {
        return call;
    }

    public void setCall(Call call) {
        this.call = call;
    }

    public int getDirect() {
        return direct;
    }

    public void setDirect(int direct) {
        this.direct = direct;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getInComingSN() {
        return inComingSN;
    }

    public void setInComingSN(String inComingSN) {
        this.inComingSN = inComingSN;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getInviteTime() {
        return inviteTime;
    }

    public void setInviteTime(String inviteTime) {
        this.inviteTime = inviteTime;
    }

    public String getAcceptTime() {
        return acceptTime;
    }

    public void setAcceptTime(String acceptTime) {
        this.acceptTime = acceptTime;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
