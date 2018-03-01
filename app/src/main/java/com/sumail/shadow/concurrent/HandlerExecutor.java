package com.sumail.shadow.concurrent;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class HandlerExecutor {

	private static HandlerThread sThread = new HandlerThread("Dd-HandlerExecutor");
	private static Handler sHandlder;

	static{
		sThread.start();
		sHandlder = new Handler(sThread.getLooper());
	}

	public static Handler getHandler(){
		if(sHandlder == null){
			return null;
		}
		return sHandlder;
	}
	
	public static Looper getLooper(){
		if(sThread==null){
			return Looper.getMainLooper();
		}
		return sThread.getLooper();
	}

	public static long getId(){
        if(sThread != null){
            return sThread.getId();
        }

        return -1;
    }

}
