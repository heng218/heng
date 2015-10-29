package com.v.heng.livego.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.v.heng.livego.R;
import com.v.heng.livego.base.BaseActivity;
import com.v.heng.livego.bean.LiveInfo;
import com.v.heng.livego.net.ApiHelper;
import com.v.heng.livego.utils.Utils;
import com.v.heng.livego.webview.MyWebView;

import net.youmi.android.spot.SpotManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class LiveDetailActivity extends BaseActivity {


    private LiveInfo liveInfo;
    private MyWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBaseContentView(R.layout.activity_live_detail);

        liveInfo = (LiveInfo) getIntent().getSerializableExtra("LiveInfo");

        initTitle();

        initViews();

        initData();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= 11) {
            webView.onResume();
        }

        initAds();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Build.VERSION.SDK_INT >= 11) {
            webView.onPause();
        } else {
            webView.pauseTimers();
            webView.stopLoading();
            webView.loadData("<a></a>", "text/html", "utf-8");
        }
    }

    public void initViews() {
        webView = (MyWebView) findViewById(R.id.webView);
        webView.addJavascriptInterface(new AndroidBridge(), "android");
    }


    public void initTitle() {
//        rightTv.setVisibility(View.VISIBLE);
//        rightTv.setText("小提示：若视频卡顿 请切换普通源观看");
//        rightTv.setBackgroundResource(0);
//        rightTv.setOnClickListener(null);
    }

    public void initData() {
        if ("龙珠".equals(liveInfo.getLivePlatform())
            || "战旗".equals(liveInfo.getLivePlatform())
            || "熊猫".equals(liveInfo.getLivePlatform())) {
            // 不请求数据
            loadFlash(liveInfo);
        } else {
            Dialog dialog = showProgressDialog(this, "", false);
            ApiHelper.getLiveAddressByLivePage(dialog, handler, liveInfo);
        }
    }

    public void initAds() {
        if(Utils.getMetaData(this, "AD_CHANNEL").equals("youmi")) {
            // youmi
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpotManager.getInstance(LiveDetailActivity.this).setSpotOrientation(SpotManager.ORIENTATION_LANDSCAPE); // 屏幕方向
                    SpotManager.getInstance(LiveDetailActivity.this).showSpotAds(LiveDetailActivity.this);
                }
            }, 1000);
        }

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ApiHelper.DATA_EMPTY:
                    break;
                case ApiHelper.DATA_SUCCESS:
                    //
                    liveInfo = (LiveInfo) msg.obj;
                    loadFlash(liveInfo);
                break;
            }
        }
    };



    public void loadFlash(LiveInfo liveInfo) {
        if (checkinstallornotadobeflashapk()) {
            if(TextUtils.isEmpty(liveInfo.getLiveAddress())) {
//                String data = "<video width=\"100%\" height=\"100%\" controls=\"controls\"> <source src=\"http://dlhls.cdn.zhanqi.tv/zqlive/88050_ZFHvJ.m3u8\"> </video>";
//                webView.loadDataWithBaseURL(null, data, "text/html","UTF-8", null);
                webView.loadDataWithBaseURL(null, liveInfo.getLiveHtmlData(), "text/html","UTF-8", null);
            } else {
                webView.loadUrl(liveInfo.getLiveAddress());
            }
        } else {
            installadobeapk();
        }
    }


    // 检查机子是否安装的有Adobe Flash相关APK
    private boolean checkinstallornotadobeflashapk() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> infoList = pm.getInstalledPackages(PackageManager.GET_SERVICES);
        for (PackageInfo info : infoList) {
            if ("com.adobe.flashplayer".equals(info.packageName)) {
                return true;
            }
        }
        return false;
    }

    // 安装Adobe Flash APK
    private void installadobeapk() {
        webView.loadUrl("file:///android_asset/go_market.html");
//        installSystemApk("AdobeFlashPlayer_4.0.apk");
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void goMarket() {
            handler.post(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT < 14) { // 14--android4.0
                        installSystemApk("AdobeFlashPlayer_2.x.apk");
                    } else {
//                        try {
//                            Intent installIntent = new Intent("android.intent.action.VIEW");
//                            installIntent.setData(Uri.parse("market://details?id=com.adobe.flashplayer"));
//                            startActivity(installIntent);
//                        }catch (Exception e) {
//                            LogUtil.logERROR(LiveDetailActivity.class, e);
                        installSystemApk("AdobeFlashPlayer_4.0.apk");
//                        }
                    }
                }
            });
        }
    }

    public void installSystemApk(final String name) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    InputStream is = getAssets().open(name);
                    File file = new File(getExternalFilesDir(""), name);
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] temp = new byte[1024];
                    int i = 0;
                    while ((i = is.read(temp)) > 0) {
                        fos.write(temp, 0, i);
                    }
                    fos.close();
                    is.close();

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    startActivity(intent);
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }).start();
    }


    @Override
    public void onBackPressed() {
        if(Utils.getMetaData(this, "AD_CHANNEL").equals("youmi")) {
            // youmi
            // 如果有需要，可以点击后退关闭插播广告。
            if (!SpotManager.getInstance(this).disMiss()) {
                // 弹出退出窗口，可以使用自定义退屏弹出和回退动画,参照demo,若不使用动画，传入-1
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

}