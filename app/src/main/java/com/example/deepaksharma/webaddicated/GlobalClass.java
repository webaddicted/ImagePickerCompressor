package com.example.deepaksharma.webaddicated;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

/**
 * Created by deepaksharma on 21/8/18.
 */

public class GlobalClass extends Application {
    public static Context context;
    public static String setCompressImagePath;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        setCompressImagePath = Environment.getExternalStorageDirectory().toString();
    }

    public static Context getInstance() {
        return context;
    }
}
