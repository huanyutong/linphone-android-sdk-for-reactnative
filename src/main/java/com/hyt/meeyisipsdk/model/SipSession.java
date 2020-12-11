package com.hyt.meeyisipsdk.model;

import org.linphone.core.Call;

/*
 * All rights Reserved, Designed By www.huanyutong.com
 * <p>项目名称: MeeyiSipSdk</p>
 * <p>包　　名: com.hyt.meeyisipsdk.model</p>
 * <p>文件名称: SipSession</p>
 * <p>描　　述: [用一句话描述该文件做什么]</p>
 * <p>创建时间: 2020-12-07 17:57</p>
 * <p>公司信息: 福建环宇通信息科技股份公司 研发部</p>
 * @author <a href="mailto:344572231@qq.com">LinShiJing</a>
 * @version v1.0
 * @Copyright: 2020 www.huanyutong.com Inc. All rights reserved.
 * 注意：本内容仅限于福建环宇通信息科技股份公司内部传阅，禁止外泄以及用于其他的商业目的
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 * @update [1][2020-12-07] [LinShiJing][变更描述]
 */
public class SipSession {
    public Call call;// 会话id
    public String id;// 会话id
    public String tid;// 事务id
    public String displayName; // 分机名称
    public String from;// 主叫号码
    public String to;// 被叫号码
    public String inviteTime;// 呼入/呼出的时间
    public String acceptTime;// 接听时间
    public String direction;//1 收到呼入 2 正在呼出 3 通话中 4 暂停 5 恢复 6 转移，7 通话错误 8 结束通话
}
