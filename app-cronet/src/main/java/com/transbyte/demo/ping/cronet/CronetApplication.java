package com.transbyte.demo.ping.cronet;

import android.app.Application;

import com.transbyte.demo.ping.AliYunDnsFactory;

public class CronetApplication extends Application {

    public static Application application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        new AliYunDnsFactory().init(this);
        CronetManager.getInstance().create(this);
    }
}
