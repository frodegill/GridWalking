package org.dyndns.gill_roxrud.frodeg.gridwalking;

import android.app.Application;
import android.content.Context;
import android.util.Log;


public class GridWalkingApplication extends Application{

    private static GridWalkingApplication instance;

    public GridWalkingApplication() {
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Thread.currentThread().setUncaughtExceptionHandler(new GridWalkingUncaughtExceptionHandler());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    public static class GridWalkingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e("UncaughtException", "Got an uncaught exception: "+ex.toString());
            ex.printStackTrace();
        }
    }
}
