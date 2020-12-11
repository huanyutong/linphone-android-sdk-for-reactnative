package com.hyt.meeyisipsdk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.linphone.core.TransportType;

/*
 * All rights Reserved, Designed By www.huanyutong.com
 * <p>项目名称: MeeyiSipSdk</p>
 * <p>包　　名: com.hyt.meeyisipsdk</p>
 * <p>文件名称: SiphoneService</p>
 * <p>描　　述: [用一句话描述该文件做什么]</p>
 * <p>创建时间: 2020-12-10 18:12</p>
 * <p>公司信息: 福建环宇通信息科技股份公司 研发部</p>
 * @author <a href="mailto:344572231@qq.com">LinShiJing</a>
 * @version v1.0
 * @Copyright: 2020 www.huanyutong.com Inc. All rights reserved.
 * 注意：本内容仅限于福建环宇通信息科技股份公司内部传阅，禁止外泄以及用于其他的商业目的
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 * @update [1][2020-12-10] [LinShiJing][变更描述]
 */
public class SiphoneService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SiphoneManager.getInstance().init(this);
        SiphoneManager.getInstance().configureAccount("192.168.111.172","1010","12345","12345", TransportType.Tcp,"abc");
    }

}
