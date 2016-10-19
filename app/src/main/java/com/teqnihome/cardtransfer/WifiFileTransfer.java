package com.teqnihome.cardtransfer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.teqnihome.cardtransfer.Utils.UtilsHandler;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

/**
 * This is wifi transfer class that extends {@link IntentService}
 * Created by dhiren
 * @author dhiren
 * @see IntentService
 */
public class WifiFileTransfer extends IntentService {
    private static final int SOCKET_TIMEOUT = 0;
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    private static final String TAG = "WifiFileTransfer";

    public WifiFileTransfer() {
        super("WifiFileTransfer");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final Context context = getApplicationContext();
        String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
        Socket socket = new Socket();
        int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

        SharedPreferences prefs = context.getSharedPreferences("businesscard", Context.MODE_PRIVATE);
        String name = prefs.getString("name", "");
        String email = prefs.getString("email", "");
        String phone = prefs.getString("phone", "");
        String picture = prefs.getString("picture", "");
        Uri file = Uri.parse(picture);

        int bufferSize = 1024;
        byte[] buffer = new byte[8 * bufferSize];
        int bytes;

        try {
            Log.d(TAG, "Opening client socket - ");
            InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
            socket.bind(null);
            socket.connect(inetSocketAddress, SOCKET_TIMEOUT);
            Log.d(TAG, "Client socket - " + socket.isConnected());
            OutputStream stream = socket.getOutputStream();
            InputStream is = null;


            DataOutputStream dos = new DataOutputStream(stream);
            if (!UtilsHandler.isClient) {


                dos.writeUTF("getIp");
                dos.flush();

                UtilsHandler.isClient = true;

            } else if(UtilsHandler.isClient){
                try {

                    Log.d(TAG, "onHandleIntent: "+ new Date());
                    File f = new File(file.getPath());
                    long filelength = f.length();
                    //uriFile = Uri.fromFile(f);
                    Log.d(TAG, "sendFile: " + f.length());
                    dos.writeUTF(name);
                    dos.writeUTF(email);
                    dos.writeUTF(phone);
                    final String fileName = f.getName();
                    dos.writeUTF(fileName);
                    dos.writeInt((int) filelength);

                    FileInputStream fis = new FileInputStream(f);
                    int total = 0;
                    int counting = 0;

                    while (fis.read(buffer) > 0) {
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
                UtilsHandler.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Card Sent Successfully", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }




        }


    }
}
