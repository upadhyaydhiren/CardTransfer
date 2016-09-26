package com.teqnihome.cardtransfer;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This is Pairing receiver that extends {@link BroadcastReceiver}
 * Created by dhiren
 * @author dhiren
 * @see BroadcastReceiver
 */
public class PairingReceiver extends BroadcastReceiver {
    private static final String TAG = "PairingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + "   pairing request");

        if (intent.getAction().equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            device.setPairingConfirmation(true);
        }

    }

}
