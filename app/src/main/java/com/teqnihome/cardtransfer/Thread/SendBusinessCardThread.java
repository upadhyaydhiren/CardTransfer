package com.teqnihome.cardtransfer.Thread;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.teqnihome.cardtransfer.ImageCache;
import com.teqnihome.cardtransfer.Utils.UtilsHandler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * This class is used for connect with bluetooth and Send {@link com.teqnihome.cardtransfer.Database.BusinessCard}
 * Created by dhiren
 * @author dhiren
 * @see BluetoothDevice
 * @see Thread
 * @see com.teqnihome.cardtransfer.Database.BusinessCard
 */
public class SendBusinessCardThread extends Thread {

    private final BluetoothDevice device;
    private final BluetoothSocket mmSocket;
    private static final String TAG = "ConnectedThread";
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a77");
    public static InputStream inputStream;
    private static final long serialVersionUID = 1L;

    public transient Context context;

    public SendBusinessCardThread(BluetoothDevice device) {
        this.context = ImageCache.getContext();
        this.device = device;

        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);

        } catch (IOException e) {

            Log.d(TAG, "ConnectThread: " + e.getMessage());
        }
        mmSocket = tmp;
    }


    @Override
    public void run() {
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager.getAdapter().isEnabled()) {
            manager.getAdapter().cancelDiscovery();
        }
        try {
            Log.d(TAG, "run: connected  " + mmSocket.isConnected());
            mmSocket.connect();
            sendBusinessCard(mmSocket);
        } catch (IOException connectException) {
            try {
                Method m = device.getClass()
                        .getMethod("removeBond", (Class[]) null);
                m.invoke(device, (Object[]) null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            UtilsHandler.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "device can't coonnected, try again ... ", Toast.LENGTH_LONG).show();
                }
            });
            connectException.printStackTrace();
            Log.d(TAG, "run: " + connectException.getMessage());
            return;
        }
        Log.d(TAG, "run: is- connected " + mmSocket.isConnected());
    }

    public void cancel() {
        try {
            mmSocket.close();
            Log.d(TAG, "cancel: Socket Close ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendBusinessCard(BluetoothSocket socket) {
        SharedPreferences prefs = context.getSharedPreferences("businesscard", Context.MODE_PRIVATE);
        String name = prefs.getString("name", "");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "");
        String picture = prefs.getString("picture", "");
        InputStream in;
        Uri file = Uri.parse(picture);

        int bufferSize = 1024;
        byte[] buffer = new byte[8 * bufferSize];
        int bytes;


        try {

            DataOutputStream dos = new DataOutputStream(mmSocket.getOutputStream());
            try {


                File f = new File(file.getPath());
                long filelength = f.length();
                //uriFile = Uri.fromFile(f);
                Log.d(TAG, "sendFile: " + f.length());
                dos.writeUTF(name);
                dos.writeUTF(email);
                dos.writeUTF(phone);
                final String fileName = f.getName();
                dos.writeUTF(fileName);
                dos.writeInt((int)filelength);

                FileInputStream fis = new FileInputStream(f);
                int total = 0;
                int counting = 0;

                while (fis.read(buffer) > 0 ) {
                    dos.write(buffer);
                    Log.d(TAG, "doInBackground: " + filelength + "   " + total + "  counting " + counting);
                }
                dos.flush();
                fis.close();
                //dos.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
               // socket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "disconnected", e);
        }


    }

}
