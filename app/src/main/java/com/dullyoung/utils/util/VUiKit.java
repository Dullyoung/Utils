package com.dullyoung.utils.util;

import android.os.Handler;
import android.os.Looper;

/*
 *  Created by Dullyoung in  2021/4/23
 */
public class VUiKit {
    private static final Handler gUiHandler = new Handler(Looper.getMainLooper());

    public static void post(Runnable r) {
        gUiHandler.post(r);
    }

    public static void postDelayed(long delay, Runnable r) {
        gUiHandler.postDelayed(r, delay);
    }
}
