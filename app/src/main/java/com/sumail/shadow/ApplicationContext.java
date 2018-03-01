package com.sumail.shadow;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.sumail.shadow.event.User;
import com.sumail.shadow.eventbus.EventBus;

/**
 * @author Shadow
 * @date 2018.03.01.
 */

public class ApplicationContext {
    public static Context sContext;
    public static  ApplicationContext mApp;
    private final String TAG = "ApplicationContext";

    public static ApplicationContext getInstance(Context context){
        if (mApp == null){
            mApp = new ApplicationContext();
        }
        sContext = context;
        return mApp;
    }
    public synchronized void init() {
        EventBus.getDefault().register(ApplicationContext.this);
    }
    public void onEvent(User user){
        Log.d(TAG,"name: " + user.name + ",age:" + user.age );
    }
}
