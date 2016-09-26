package com.teqnihome.cardtransfer.Utils;

import android.os.Handler;
import android.os.Looper;

/**
 * This is Handler class for execute Background task in ui thread
 * Created by dhiren
 * @author dhiren
 * @see Handler
 */
public class UtilsHandler {
    public static void runOnUiThread(Runnable runnable){

        Handler UIHandler = new Handler(
                Looper.getMainLooper());
        UIHandler.post(runnable);
    }

}

