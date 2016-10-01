package com.teqnihome.cardtransfer;

import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.teqnihome.cardtransfer.Database.DataBaseHelper;
import com.teqnihome.cardtransfer.Thread.AcceptBusinessThread;
import com.teqnihome.cardtransfer.Utils.UtilsHandler;
import com.teqnihome.imagecrop.CropImageIntentBuilder;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This is BusinessCardActivity class that extends {@link AppCompatActivity}
 * Created by dhiren
 * @author dhiren
 * @see AppCompatActivity
 * @see View
 */
public class BusinessCard extends AppCompatActivity implements View.OnClickListener {


    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;



    public class ReceivingError extends Exception {
    }

    public class ResizeError extends Exception {
    }

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    public static final int AVATAR_REQUEST_CODE = 21;
    public static final int PIC_CROP = 31;
    public static final int IMAGE_QUALITY = 50;
    public static final int AVATAR_SIZE = 512;
    private static final String TAG = "MainActivity";
    String file_Path = "";
    private File tempFile;
    Uri picUri;
    File tempFileForGalleryPicture;
    ImageView card_avatar;
    ImageView card_avatar_edit_overlay;
    EditText card_name;
    EditText card_email;
    EditText card_phone;
    Button button_done;
    Button button_send;
    Button button_edit;
    Button button_send_wifi;
    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.businesscardlayout);
        preferences = getSharedPreferences("businesscard", MODE_PRIVATE);
        editor = preferences.edit();
        ImageCache.setContext(this);

        new FileServerAsyncTask(this).execute();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Intent enableBlueTooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBlueTooth, 1);
        } else {
            bluetoothEnabled();
        }

        card_avatar = (ImageView) findViewById(R.id.card_avatar);
        card_avatar_edit_overlay = (ImageView) findViewById(R.id.card_avatar_edit_overlay);
        card_name = (EditText) findViewById(R.id.card_name);
        card_email = (EditText) findViewById(R.id.card_email);
        card_phone = (EditText) findViewById(R.id.card_phone);
        button_done = (Button) findViewById(R.id.button_done);
        button_send = (Button) findViewById(R.id.button_send);
        button_edit = (Button) findViewById(R.id.button_edit);
        button_send_wifi = (Button) findViewById(R.id.button_send_wifi);
        button_edit.setOnClickListener(this);
        button_send.setOnClickListener(this);
        button_done.setOnClickListener(this);
        button_send_wifi.setOnClickListener(this);
        card_name.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        card_email.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        card_phone.getBackground().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);




        if (!new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth").exists()) {
            new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth").mkdir();
            new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth/BusinessCard").mkdir();
        }
        card_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!card_name.getText().toString().trim().equals("")) {
                    button_done.setVisibility(View.VISIBLE);
                } else {
                    button_done.setVisibility(View.GONE);
                }
            }
        });

        if (isBusinessCardCreated()) {

            card_avatar.setImageURI(Uri.parse(preferences.getString("picture", "")));
            card_avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card_name.setText(preferences.getString("name", ""));
            card_email.setText(preferences.getString("email", ""));
            card_phone.setText(preferences.getString("phone", ""));
            tempFileForGalleryPicture = new File(Uri.parse(preferences.getString("picture", "")).getPath());
            setDisableMode(preferences.getString("picture", ""));


        } else {
            imageClickEvent();
        }





    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }



    public void bluetoothEnabled() {
        AcceptBusinessThread thread = new AcceptBusinessThread(BluetoothAdapter.getDefaultAdapter(), this);
        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_list_card:
                Intent intent = new Intent(this, BusinessCardReceivedList.class);
                this.startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_done:
                Log.d(TAG, "onClick: done clicked");
                setDisableMode(tempFileForGalleryPicture != null ? tempFileForGalleryPicture.getPath() : "");
                break;
            case R.id.button_send:
                Log.d(TAG, "onClick: send clicked");
                Intent intent = new Intent(BusinessCard.this, BusinessCardListActivityUser.class);
                this.startActivity(intent);
                break;
            case R.id.button_edit:
                Log.d(TAG, "onClick: edit clickedd");
                setEnableMode();
                break;
            case R.id.button_send_wifi:
                Log.d(TAG, "onClick: send clicked");
                Intent intentWifi = new Intent(BusinessCard.this, WifiP2PTransfer.class);
                this.startActivity(intentWifi);
                break;


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == 1) {
            if (resultCode == RESULT_OK)
                bluetoothEnabled();

            else {
                Toast.makeText(getApplicationContext(), "Please Turn On Bluetooth ", Toast.LENGTH_LONG).show();
                finish();
            }

        }

        if (resultCode == RESULT_OK) {
            if (requestCode == AVATAR_REQUEST_CODE) {
                Log.d(TAG, "onActivityResult: " + tempFile.toString());
                if (data != null && data.getData() != null) {
                    tempFile = new File(data.getData().getPath());

                }

                tempFileForGalleryPicture = new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth/BusinessCard", tempFile.getName());
                Log.d(TAG, "onActivityResult: " + tempFile.toString()
                );
                performCrop();
            } else if (requestCode == PIC_CROP) {
                ImageView picView = (ImageView) findViewById(R.id.card_avatar);
                picView.setImageBitmap(BitmapFactory.decodeFile(tempFileForGalleryPicture.getAbsolutePath()));

            }

        }
    }


    private void performCrop() {
        try {


            Uri croppedImage = Uri.fromFile(tempFile);

            com.teqnihome.imagecrop.CropImageIntentBuilder cropImage = new CropImageIntentBuilder(200, 200, Uri.fromFile(tempFileForGalleryPicture));
            cropImage.setOutlineColor(0xFF03A9F4);
            cropImage.setSourceImage(croppedImage);
            startActivityForResult(cropImage.getIntent(this), PIC_CROP);
        } catch (ActivityNotFoundException anfe) {
            //display an error message
            anfe.printStackTrace();
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void setDisableMode(String path) {
        card_avatar_edit_overlay.setVisibility(View.GONE);
        card_name.setEnabled(false);
        card_phone.setEnabled(false);
        card_email.setEnabled(false);
        button_done.setVisibility(View.GONE);
        if (!path.equals("")) {
            button_send.setVisibility(View.VISIBLE);
            button_send_wifi.setVisibility(View.VISIBLE);
        }
        button_edit.setVisibility(View.VISIBLE);
        card_name.setTextColor(Color.BLACK);
        card_email.setTextColor(Color.BLACK);
        card_phone.setTextColor(Color.BLACK);
        card_avatar.setOnClickListener(null);

        if (card_phone.getText().toString().trim().equals(""))
            card_phone.setVisibility(View.GONE);
        if (card_email.getText().toString().trim().equals(""))
            card_email.setVisibility(View.GONE);

        Log.d(TAG, "setDisableMode:  path " + path);
        editor.putString("picture", path);
        editor.putString("name", card_name.getText().toString());
        editor.putString("email", card_email.getText().toString());
        editor.putString("phone", card_phone.getText().toString());
        editor.putBoolean("isdataAvailable", true);
        editor.commit();


    }

    public void setEnableMode() {

        card_avatar_edit_overlay.setVisibility(View.VISIBLE);
        card_name.setEnabled(true);
        card_phone.setEnabled(true);
        card_email.setEnabled(true);
        button_done.setVisibility(View.VISIBLE);
        button_send.setVisibility(View.GONE);
        button_edit.setVisibility(View.GONE);
        button_send_wifi.setVisibility(View.GONE);
        card_email.setVisibility(View.VISIBLE);
        card_phone.setVisibility(View.VISIBLE);
        imageClickEvent();

    }


    public void imageClickEvent() {

        card_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent();
                pickIntent.setType("image/*");
                pickIntent.setAction(Intent.ACTION_GET_CONTENT);
                // pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
                pickIntent.putExtra(MediaStore.EXTRA_OUTPUT, Environment.getExternalStorageDirectory());
                tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), System.nanoTime() + ".jpg");
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePhotoIntent.putExtra("android.intent.extras.CAMERA_FACING",
                        Camera.CameraInfo.CAMERA_FACING_FRONT);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                takePhotoIntent.addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                String pickTitle = "where from ?";
                Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
                chooserIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                startActivityForResult(chooserIntent, AVATAR_REQUEST_CODE);

            }
        });

    }


    public boolean isBusinessCardCreated() {

        return preferences.getBoolean("isdataAvailable", false);

    }

    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private final Context context;
        InputStream in = null;
        OutputStream out = null;
        int bufferSize = 1024;
        byte[] buffer = new byte[8 * bufferSize];
        File files;
        DataBaseHelper db;


        /**
         * @param context
         */
        public FileServerAsyncTask(Context context) {
            this.context = context;
            db = new DataBaseHelper(context);

        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();


                Log.d(TAG, "Server: connection done");
                BufferedInputStream bufferedInputStream = new BufferedInputStream(client.getInputStream(), buffer.length);
                DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
                String name = dataInputStream.readUTF();
                String email = dataInputStream.readUTF();
                String phone = dataInputStream.readUTF();
                String filename = dataInputStream.readUTF();


                int fileLength = dataInputStream.readInt();
                int counting = 0;
                if (!new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth").exists()) {
                    new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth").mkdir();
                    new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth/BusinessCard").mkdir();

                }

                files = new File(Environment.getExternalStorageDirectory() + "/TransferBluetooth/BusinessCard", filename);
                com.teqnihome.cardtransfer.Database.BusinessCard businessCard = new com.teqnihome.cardtransfer.Database.BusinessCard();
                businessCard.setName(name);
                businessCard.setPhone(phone);
                businessCard.setEmail(email);
                businessCard.setPicture(files.getPath());

                long value = db.insertBusinessCard(businessCard);

                db.closeDB();
                FileOutputStream fos = new FileOutputStream(files);
                int len = 0;
                int newBuffer = 8192;
                int remaining = fileLength;

                while ((len = dataInputStream.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                    counting += len;
                    remaining -= len;
                    System.out.println("read " + counting + " bytes.");
                    fos.write(buffer, 0, len);
                }
                Log.d(TAG, "run: data inserted id  is  " + value);


                fos.close();
                dataInputStream.close();
                serverSocket.close();
                //return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
            return "";
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                UtilsHandler.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(context, BusinessCardReceivedList.class);
                        context.startActivity(intent);

                    }
                });
            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

        }

    }



}
