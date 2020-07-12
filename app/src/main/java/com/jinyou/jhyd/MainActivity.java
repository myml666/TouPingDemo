package com.jinyou.jhyd;


import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.dfqin.grantor.PermissionListener;
import com.github.dfqin.grantor.PermissionsUtil;
import com.hpplay.common.utils.LeLog;
import com.hpplay.sdk.source.api.IBindSdkListener;
import com.hpplay.sdk.source.api.IConnectListener;
import com.hpplay.sdk.source.api.ILelinkPlayerListener;
import com.hpplay.sdk.source.api.LelinkPlayerInfo;
import com.hpplay.sdk.source.api.LelinkSourceSDK;
import com.hpplay.sdk.source.browse.api.IAPI;
import com.hpplay.sdk.source.browse.api.IBrowseListener;
import com.hpplay.sdk.source.browse.api.IParceResultListener;
import com.hpplay.sdk.source.browse.api.LelinkServiceInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //申请的权限
    private static final String[] mPermissions = {Manifest.permission.READ_PHONE_STATE};
    private String APP_ID = "14871",APP_SECRET = "063b4f71970cc4f984a169e8315c2e9c";
    private RecyclerView rv_tvlist;
    private UIHandler mUiHandler;
    private static final int MSG_SEARCH_RESULT = 100;
    private static final int MSG_CONNECT_FAILURE = 101;
    private static final int MSG_CONNECT_SUCCESS = 102;
    private static final int MSG_UPDATE_PROGRESS = 103;
    private boolean isPause;
    private LelinkServiceInfo mSelectInfo;
    IBrowseListener iBrowseListener = new IBrowseListener() {

        @Override
        public void onBrowse(int i, List<LelinkServiceInfo> list) {
            if (i == IBrowseListener.BROWSE_ERROR_AUTH) {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "授权失败", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            if (mUiHandler != null) {
                mUiHandler.sendMessage(Message.obtain(null, MSG_SEARCH_RESULT, list));
            }
        }

    };

    IConnectListener iConnectListener = new IConnectListener() {
        @Override
        public void onConnect(LelinkServiceInfo lelinkServiceInfo, int extra) {
            if (mUiHandler != null) {
                mUiHandler.sendMessage(Message.obtain(null, MSG_CONNECT_SUCCESS, extra, 0, lelinkServiceInfo));
            }
        }
        @Override
        public void onDisconnect(LelinkServiceInfo lelinkServiceInfo, int what, int extra) {
            String text = null;
            if (what == IConnectListener.CONNECT_INFO_DISCONNECT) {
                if (null != mUiHandler) {
                    if (TextUtils.isEmpty(lelinkServiceInfo.getName())) {
                        text = "pin码连接断开";
                    } else {
                        text = lelinkServiceInfo.getName() + "连接断开";
                    }
                }
            } else if (what == IConnectListener.CONNECT_ERROR_FAILED) {
                if (extra == IConnectListener.CONNECT_ERROR_IO) {
                    text = lelinkServiceInfo.getName() + "连接失败";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_WAITTING) {
                    text = lelinkServiceInfo.getName() + "等待确认";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_REJECT) {
                    text = lelinkServiceInfo.getName() + "连接拒绝";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_TIMEOUT) {
                    text = lelinkServiceInfo.getName() + "连接超时";
                } else if (extra == IConnectListener.CONNECT_ERROR_IM_BLACKLIST) {
                    text = lelinkServiceInfo.getName() + "连接黑名单";
                }
            }
            if (null != mUiHandler) {
                mUiHandler.sendMessage(Message.obtain(null, MSG_CONNECT_FAILURE, text));
            }
        }
    };
    private TvListAdapter tvListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUiHandler = new UIHandler(this);
        requestPermissions();
        rv_tvlist = findViewById(R.id.rv_tvlist);
        rv_tvlist.setLayoutManager(new LinearLayoutManager(this));
    }
    /**
     * 请求权限
     */
    private void requestPermissions() {
        if (PermissionsUtil.hasPermission(this,mPermissions)) {
            //有访问存储空间、摄像头的权限
            initSdk();
        } else {
            PermissionsUtil.requestPermission(this, new PermissionListener() {
                @Override
                public void permissionGranted(@NonNull String[] permissions) {
                    //用户授予了访问存储空间、摄像头的权限
                    initSdk();
                }
                @Override
                public void permissionDenied(@NonNull String[] permissions) {
                    //用户拒绝了访问存储空间、摄像头的申请
                }
            }, mPermissions);
        }
    }

    private void initSdk(){
        //sdk初始化
        LelinkSourceSDK.getInstance().bindSdk(getApplicationContext(), APP_ID, APP_SECRET, new IBindSdkListener() {
            @Override
            public void onBindCallback(boolean b) {
                if (b) {
                    initSDKStatusListener();
                }
            }
        });
    }
    void initSDKStatusListener() {
        LelinkSourceSDK.getInstance().setBrowseResultListener(iBrowseListener);
        LelinkSourceSDK.getInstance().setConnectListener(iConnectListener);
        LelinkSourceSDK.getInstance().setPlayListener(lelinkPlayerListener);
        LelinkSourceSDK.getInstance().startBrowse();
        getPincodeDevice();
    }
    public void getPincodeDevice() {
        LelinkSourceSDK.getInstance().addPinCodeToLelinkServiceInfo("67875050", new IParceResultListener() {
            @Override
            public void onParceResult(int resultCode, LelinkServiceInfo info) {
                if (resultCode == IParceResultListener.PARCE_SUCCESS) {
                    mSelectInfo = info;
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<LelinkServiceInfo> lelinkServiceInfos = new ArrayList<LelinkServiceInfo>();
                            lelinkServiceInfos.add(mSelectInfo);
                            updateBrowseAdapter(lelinkServiceInfos);
                            Toast.makeText(getApplicationContext(), "获取设备成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mUiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "获取设备失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LelinkSourceSDK.getInstance().stopBrowse();
        LelinkSourceSDK.getInstance().stopPlay();
        LelinkSourceSDK.getInstance().unBindSdk();
    }

    /**
     * 更新设备列表
     * @param infos
     */
    private void updateBrowseAdapter(List<LelinkServiceInfo> infos) {
        if(tvListAdapter == null){
            tvListAdapter = new TvListAdapter(infos);
            tvListAdapter.setSelectLelinkServiceInfo(mSelectInfo);
            tvListAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
//                    mSelectInfo = tvListAdapter.getData().get(position);
//                    tvListAdapter.setSelectLelinkServiceInfo(mSelectInfo);
//                    tvListAdapter.notifyDataSetChanged();
                }
            });
            tvListAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                @Override
                public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                    switch (view.getId()){
                        case R.id.item_tv_bt_play:
                            startPlay();
                            break;
                        case R.id.item_tv_bt_pause:
                            isPause = true;
                            LelinkSourceSDK.getInstance().pause();
                            break;
                        case R.id.item_tv_bt_stop:
                            LelinkSourceSDK.getInstance().stopPlay();
                            break;
                    }
                }
            });
            rv_tvlist.setAdapter(tvListAdapter);
        }else {
            tvListAdapter.setSelectLelinkServiceInfo(mSelectInfo);
            tvListAdapter.setNewData(infos);
            tvListAdapter.notifyDataSetChanged();
        }
    }

    private void startPlay() {
        if (null == mSelectInfo) {
            Toast.makeText(getApplicationContext(), "请选择接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isPause) {
            isPause = false;
            // 暂停中
            LelinkSourceSDK.getInstance().resume();
            return;
        }
        int mediaType = LelinkSourceSDK.MEDIA_TYPE_VIDEO;//网络视频
        String url = "http://v.mifile.cn/b2c-mimall-media/ed921294fb62caf889d40502f5b38147.mp4";
        LelinkPlayerInfo lelinkPlayerInfo = new LelinkPlayerInfo();
        lelinkPlayerInfo.setUrl(url);
        lelinkPlayerInfo.setType(mediaType);
//        if (!TextUtils.isEmpty(mScreencode)) {
//            lelinkPlayerInfo.setOption(IAPI.OPTION_6, mScreencode);
//        }
        lelinkPlayerInfo.setLelinkServiceInfo(mSelectInfo);
        LelinkSourceSDK.getInstance().startPlayMedia(lelinkPlayerInfo);
    }

    private static class UIHandler extends Handler {

        private WeakReference<MainActivity> mReference;

        UIHandler(MainActivity reference) {
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mReference.get();
            if (mainActivity == null) {
                return;
            }
            switch (msg.what) {
                case MSG_SEARCH_RESULT:
                    try {
                        if (msg.obj != null) {
                            mainActivity.updateBrowseAdapter((List<LelinkServiceInfo>) msg.obj);
                        }
                    } catch (Exception e) {
                    }
                    break;
                case MSG_CONNECT_SUCCESS:
                    try {
                        if (msg.obj != null) {
                            LelinkServiceInfo serviceInfo = (LelinkServiceInfo) msg.obj;
                            String type = msg.arg1 == IConnectListener.TYPE_LELINK ? "Lelink"
                                    : msg.arg1 == IConnectListener.TYPE_DLNA ? "DLNA"
                                    : msg.arg1 == IConnectListener.TYPE_NEW_LELINK ? "NEW_LELINK" : "IM";
                            Toast.makeText(mainActivity, type + "  " + serviceInfo.getName() + "连接成功", Toast.LENGTH_SHORT).show();
                            mainActivity.mSelectInfo = serviceInfo;
                        }
                    } catch (Exception e) {
                    }
                    break;
                case MSG_CONNECT_FAILURE:
                    if (msg.obj != null) {
                        Toast.makeText(mainActivity, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MSG_UPDATE_PROGRESS:
                    break;
            }
            super.handleMessage(msg);
        }
    }
    ILelinkPlayerListener lelinkPlayerListener = new ILelinkPlayerListener() {


        @Override
        public void onLoading() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "开始加载", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStart() {
            isPause = false;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "开始播放", Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onPause() {
            isPause = true;
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "暂停播放", Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onCompletion() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "播放完成", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStop() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "播放停止", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onSeekComplete(int i) {

        }

        @Override
        public void onInfo(int i, int i1) {

        }
        String text = null;
        @Override
        public void onError(int what, int extra) {
            if (what == PUSH_ERROR_INIT) {
                if (extra == PUSH_ERRROR_FILE_NOT_EXISTED) {
                    text = "文件不存在";
                } else if (extra == PUSH_ERROR_IM_OFFLINE) {
                    text = "IM TV不在线";
                } else if (extra == PUSH_ERROR_IMAGE) {

                } else if (extra == PUSH_ERROR_IM_UNSUPPORTED_MIMETYPE) {
                    text = "IM不支持的媒体类型";
                } else {
                    text = "未知";
                }
            } else if (what == MIRROR_ERROR_INIT) {
                if (extra == MIRROR_ERROR_UNSUPPORTED) {
                    text = "不支持镜像";
                } else if (extra == MIRROR_ERROR_REJECT_PERMISSION) {
                    text = "镜像权限拒绝";
                } else if (extra == MIRROR_ERROR_DEVICE_UNSUPPORTED) {
                    text = "设备不支持镜像";
                } else if (extra == NEED_SCREENCODE) {
                    text = "请输入投屏码";
                }
            } else if (what == MIRROR_ERROR_PREPARE) {
                if (extra == MIRROR_ERROR_GET_INFO) {
                    text = "获取镜像信息出错";
                } else if (extra == MIRROR_ERROR_GET_PORT) {
                    text = "获取镜像端口出错";
                } else if (extra == NEED_SCREENCODE) {
                    text = "请输入投屏码";
//                    mUiHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            showScreenCodeDialog();
//                        }
//                    });
                    if (extra == PREEMPT_UNSUPPORTED) {
                        text = "投屏码模式不支持抢占";
                    }
                } else if (what == PUSH_ERROR_PLAY) {
                    if (extra == PUSH_ERROR_NOT_RESPONSED) {
                        text = "播放无响应";
                    } else if (extra == NEED_SCREENCODE) {
                        text = "请输入投屏码";
//                        mUiHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                showScreenCodeDialog();
//                            }
//                        });
                    } else if (extra == RELEVANCE_DATA_UNSUPPORTED) {
                        text = "老乐联不支持数据透传,请升级接收端的版本！";
                    } else if (extra == ILelinkPlayerListener.PREEMPT_UNSUPPORTED) {
                        text = "投屏码模式不支持抢占";
                    }
                } else if (what == PUSH_ERROR_STOP) {
                    if (extra == ILelinkPlayerListener.PUSH_ERROR_NOT_RESPONSED) {
                        text = "退出 播放无响应";
                    }
                } else if (what == PUSH_ERROR_PAUSE) {
                    if (extra == ILelinkPlayerListener.PUSH_ERROR_NOT_RESPONSED) {
                        text = "暂停无响应";
                    }
                } else if (what == PUSH_ERROR_RESUME) {
                    if (extra == ILelinkPlayerListener.PUSH_ERROR_NOT_RESPONSED) {
                        text = "恢复无响应";
                    }
                }

            } else if (what == MIRROR_PLAY_ERROR) {
                if (extra == MIRROR_ERROR_FORCE_STOP) {
                    text = "接收端断开";
                } else if (extra == MIRROR_ERROR_PREEMPT_STOP) {
                    text = "镜像被抢占";
                }
            } else if (what == MIRROR_ERROR_CODEC) {
                if (extra == MIRROR_ERROR_NETWORK_BROKEN) {
                    text = "镜像网络断开";
                }
            }
        }
        @Override
        public void onVolumeChanged(float v) {

        }

        @Override
        public void onPositionUpdate(long l, long l1) {
            if (mUiHandler != null) {
                Message msg = new Message();
                msg.what = MSG_UPDATE_PROGRESS;
                msg.arg1 = (int) l;
                msg.arg2 = (int) l1;
                mUiHandler.sendMessage(msg);
            }
        }
    };
}
