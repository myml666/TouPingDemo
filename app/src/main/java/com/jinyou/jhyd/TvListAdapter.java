package com.jinyou.jhyd;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.hpplay.common.utils.LeLog;
import com.hpplay.sdk.source.browse.api.LelinkServiceInfo;

import java.util.List;

/**
 * @ProjectName: TouPingDemo
 * @Package: com.jinyou.jhyd
 * @ClassName: TvListAdapter
 * @Description: java类作用描述
 * @Author: 作者名
 * @CreateDate: 2020/7/12 18:10
 * @UpdateUser: 更新者：itfitness
 * @UpdateDate: 2020/7/12 18:10
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class TvListAdapter extends BaseQuickAdapter<LelinkServiceInfo, BaseViewHolder> {
    private LelinkServiceInfo selectLelinkServiceInfo;

    public LelinkServiceInfo getSelectLelinkServiceInfo() {
        return selectLelinkServiceInfo;
    }

    public void setSelectLelinkServiceInfo(LelinkServiceInfo selectLelinkServiceInfo) {
        this.selectLelinkServiceInfo = selectLelinkServiceInfo;
    }

    public TvListAdapter(@Nullable List<LelinkServiceInfo> data) {
        super(R.layout.item_tv,data);
    }
    @Override
    protected void convert(BaseViewHolder helper, LelinkServiceInfo item) {
        CheckBox checkBox = helper.getView(R.id.item_tv_cb);
        helper.setText(R.id.item_tv_ip,item.getIp());
        helper.setText(R.id.item_tv_name,item.getName());
        helper.setText(R.id.item_tv_status,item.isOnLine()?"在线":"离线");
        checkBox.setChecked(isContains(selectLelinkServiceInfo,item));
        helper.addOnClickListener(R.id.item_tv_bt_play);
        helper.addOnClickListener(R.id.item_tv_bt_pause);
        helper.addOnClickListener(R.id.item_tv_bt_stop);
    }
    private boolean isContains(LelinkServiceInfo selectInfo, LelinkServiceInfo info) {
        try {
            if (selectInfo == null || info == null) {
                return false;
            }
            if (info.getUid() != null && selectInfo.getUid() != null && TextUtils.equals(info.getUid(), selectInfo.getUid())) {
                return true;
            } else if (TextUtils.equals(info.getIp(), selectInfo.getIp()) && TextUtils.equals(info.getName(), selectInfo.getName())) {
                return true;
            }
        } catch (Exception e) {
            LeLog.w(TAG, e);
            return false;
        }
        return false;
    }
}
