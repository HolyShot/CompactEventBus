package com.sumail.shadow;

import android.app.Application;

/**
 * @author Shadow
 * @date 2018.03.01.
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(){
            @Override
            public void run() {
                ApplicationContext.getInstance(MyApp.this).init();
            }
        }.start();
    }
}
