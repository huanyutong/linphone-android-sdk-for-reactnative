package com.hyt.meeyisipsdk.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyt.meeyisipsdk.model.SipCall;

import java.util.List;

/*
 * All rights Reserved, Designed By www.huanyutong.com
 * <p>项目名称: MeeyiSipSdk</p>
 * <p>包　　名: com.hyt.meeyisipsdk.adapter</p>
 * <p>文件名称: SipCallAdapter</p>
 * <p>描　　述: [通话列表适配器]</p>
 * <p>创建时间: 2020-12-07 18:02</p>
 * <p>公司信息: 福建环宇通信息科技股份公司 研发部</p>
 * @author <a href="mailto:344572231@qq.com">LinShiJing</a>
 * @version v1.0
 * @Copyright: 2020 www.huanyutong.com Inc. All rights reserved.
 * 注意：本内容仅限于福建环宇通信息科技股份公司内部传阅，禁止外泄以及用于其他的商业目的
 * @update [序号][日期YYYY-MM-DD] [更改人姓名][变更描述]
 * @update [1][2020-12-07] [LinShiJing][变更描述]
 */
public class SipCallAdapter extends BaseAdapter {
    private List<SipCall> mCalls;
    private Context mContext;
    private int selectIndex;

    public SipCallAdapter(Context context, List<SipCall> calls) {
        mContext = context;
        mCalls = calls;
    }

    public void select(int i, int state) {
        for (SipCall call : mCalls) {
            call.setCurrent(false);
        }
        selectIndex = i;
        mCalls.get(i).setCurrent(true);
        mCalls.get(i).setState(state);
        notifyDataSetChanged();
    }

    public void updateState(int i, int state) {
        if(i>=mCalls.size()){
            return;
        }
        mCalls.get(i).setState(state);
    }

    public int getSelectIndex() {
        return selectIndex;
    }

    public void setSelectIndex(int selectIndex) {
        this.selectIndex = selectIndex;
    }

    @Override
    public int getCount() {
        return mCalls.size();
    }

    @Override
    public Object getItem(int i) {
        return mCalls.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SipCall sipCall = mCalls.get(position); //获取当前项的SipCall实例

        // 加个判断，以免ListView每次滚动时都要重新加载布局，以提高运行效率
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            // 避免ListView每次滚动时都要重新加载布局，以提高运行效率
            view = LayoutInflater.from(mContext).inflate(com.hyt.meeyisipsdk.R.layout.activity_call_item, parent, false);
            // 避免每次调用getView()时都要重新获取控件实例
            viewHolder = new ViewHolder();
            viewHolder.callinfo = view.findViewById(com.hyt.meeyisipsdk.R.id.callinfo);
            viewHolder.callin = view.findViewById(com.hyt.meeyisipsdk.R.id.callin);
            viewHolder.more = view.findViewById(com.hyt.meeyisipsdk.R.id.more);

            // 将ViewHolder存储在View中（即将控件的实例存储在其中）
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        // 获取控件实例，并调用set...方法使其显示出来
        viewHolder.callinfo.setText(sipCall.getCall().getCallLog().getRemoteAddress().getUsername() + sipCall.getLevel());
        viewHolder.callin.setVisibility(sipCall.isCurrent() || sipCall.getState() != 0 ? View.VISIBLE : View.GONE);
        if (sipCall.getState() == 1) {
            viewHolder.callin.setImageResource(com.hyt.meeyisipsdk.R.mipmap.back);
        } else {
            viewHolder.callin.setImageResource(sipCall.isCurrent() ? com.hyt.meeyisipsdk.R.mipmap.call_pause_red : com.hyt.meeyisipsdk.R.mipmap.call_pause_yellow);
        }
        viewHolder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        return view;
    }

    class ViewHolder {
        TextView callinfo;//呼入用户信息
        ImageView callin;//当前通话标识
        ImageView more;//更多
    }
}
