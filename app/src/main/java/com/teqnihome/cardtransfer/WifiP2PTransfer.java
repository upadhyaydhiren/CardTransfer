package com.teqnihome.cardtransfer;

import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.teqnihome.cardtransfer.Database.DataBaseHelper;
import com.teqnihome.cardtransfer.Thread.SendBusinessCardThread;
import com.teqnihome.cardtransfer.Utils.UtilsHandler;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songline on 01/10/16.
 */
public class WifiP2PTransfer extends AppCompatActivity implements SearchView.OnQueryTextListener, WifiP2pManager.ConnectionInfoListener, WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener {
    private static final String TAG = "MainActivity";
    BluetoothAdapter bluetoothAdapter;
    private Toolbar toolbar;
    static SendBusinessCardThread connectedThread;
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pInfo info;
    static DeviceAdapter deviceAdapter;
    static RecyclerView deviceLayout;
    private ArrayList<WifiP2pDevice> wifiDevices = new ArrayList<>();
    private ArrayList<WifiP2pDevice> tempWifiDevices = new ArrayList<>();
    static Context mContext;
    static WifiP2pDevice device;
    private SearchView searchView;
    SharedPreferences prefs;
    static boolean clientCheck = false;
    static Button button;


    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;
    Context context;
    String deviceId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        prefs = getSharedPreferences("wifi", MODE_PRIVATE);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.business_layout_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mContext = this;
        ImageCache.setContext(mContext);
        deviceId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        bluetoothEnabled();


    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        this.info = info;
        Log.d(TAG, "onConnectionInfoAvailable:  " + (info.groupOwnerAddress.getHostAddress() == null));

        System.out.println(" Info Below    " + deviceId + "    -- - - --  - " + info);
        try {
            SharedPreferences prefs = context.getSharedPreferences("server", MODE_PRIVATE);
            if (!info.isGroupOwner && info.groupFormed) {

                new FileServerAsyncTask(this).execute();
                if (!UtilsHandler.isClient) {

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("serverIP", info.groupOwnerAddress.getHostAddress());
                    editor.commit();

                    Intent serviceIntent = new Intent(mContext, WifiFileTransfer.class);
                    serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_ADDRESS,
                            info.groupOwnerAddress.getHostAddress());
                    serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_PORT, 8988);
                    mContext.startService(serviceIntent);
                }
            } else if (prefs.getString("serverIP", "").isEmpty()) {


                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("is_server", true);
                editor.commit();
                Log.d(TAG, "onConnectionInfoAvailable: " + deviceId + "<----- device Id" + getLocalIpAddress());
                new FileServerAsyncTask(this).execute();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: here");
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.searchable, menu);
        // Associate searchable configuration with the SearchView
        searchView = (SearchView) menu.findItem(R.id.item_list_search).getActionView();
        searchView.setOnQueryTextListener(this);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(
                new ComponentName(this, BusinessCardListActivityUser.class)));
        searchView.setIconifiedByDefault(false);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + resultCode);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK)
                bluetoothEnabled();

        } else {

            Toast.makeText(getApplicationContext(), "Please Turn On Bluetooth ", Toast.LENGTH_LONG).show();
            finish();
        }
    }


    void bluetoothEnabled() {
        deviceLayout = (RecyclerView) findViewById(R.id.list_business);
        deviceAdapter = new DeviceAdapter(this, wifiDevices, channel, manager);
        setDeviceLayout(deviceLayout);
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WifiP2PTransfer.this, "Discovery Started",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WifiP2PTransfer.this, "Discovery Failed",
                        Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        deviceAdapter.getFilter().filter(newText);
        return false;
    }

    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peersList) {
        Log.d(TAG, "onPeersAvailable:  --- - - - --  - -- - - - -  -   " + peersList.getDeviceList().size());
        peers.clear();
        deviceAdapter.clear();
        peers.addAll(peersList.getDeviceList());
        Log.d(TAG, "onPeersAvailable: " + peersList.getDeviceList());
        // deviceAdapter.notifyDataSetChanged();
        if (peers.size() > 0) {
            for (WifiP2pDevice wifiP2pDevice : peers) {
                deviceAdapter.add(wifiP2pDevice.deviceName, wifiP2pDevice);
            }
            deviceAdapter.notifyDataSetChanged();
        }


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
                SharedPreferences prefs = context.getSharedPreferences("server", MODE_PRIVATE);
                boolean key = prefs.getBoolean("is_server", false);


                Log.d(TAG, "Server: connection done");
                BufferedInputStream bufferedInputStream = new BufferedInputStream(client.getInputStream(), buffer.length);
                DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

                if (!UtilsHandler.isClient && key) {
                    final String ipAddress = client.getInetAddress().getHostAddress();
                    UtilsHandler.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Ip Address" + ipAddress, Toast.LENGTH_LONG).show();
                        }
                    });
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("IP", ipAddress);
                    editor.commit();


                } else {


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
                }
                dataInputStream.close();
                client.close();
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
            if (result != null && UtilsHandler.isClient) {
                UtilsHandler.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(context, "Card Received ", Toast.LENGTH_LONG).show();
                        /*

                        Intent intent = new Intent(context, BusinessCardReceivedList.class);
                        context.startActivity(intent);
*/

                    }
                });


            }
            UtilsHandler.isClient = true;
            FileServerAsyncTask async = new FileServerAsyncTask(context);
            async.execute();

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

        }

    }


    public static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> implements Filterable {
        List<String> names = new ArrayList<>();
        List<String> status = new ArrayList<>();
        FriendFilter friendFilter;
        static Context mContext;
        List<WifiP2pDevice> devices = new ArrayList<>();
        List<String> listName = new ArrayList<>();
        List<WifiP2pDevice> listDevice = new ArrayList<>();
        private WifiP2pManager.Channel channel;
        private WifiP2pManager manager;
        WifiP2pInfo info = null;

        public void setInfo(WifiP2pInfo infoObj) {
            info = infoObj;
        }

        public DeviceAdapter(Context mContext, List<WifiP2pDevice> devices, WifiP2pManager.Channel channel, WifiP2pManager manager) {
            this.mContext = mContext;
            this.devices = devices;
            this.channel = channel;
            this.manager = manager;
            getFilter();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(mContext).inflate(viewType == 0 ? R.layout.business_layout_single_user : R.layout.searching_devices, parent, false), mContext, viewType);
        }

        public void add(String name, WifiP2pDevice device) {
            names.add(name);
            devices.add(device);
            listName.add(name);
            listDevice.add(device);
            // notifyDataSetChanged();
        }

        public void clear() {
            names.clear();
            devices.clear();
            listName.clear();
            listDevice.clear();
        }


        @Override
        public int getItemViewType(int position) {
            return position == 0 && names.isEmpty() ? 1 : 0;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                holder.nameTV.setText(names.get(position));
                holder.itemView.setTag(devices.get(position));
                holder.button.setTag(devices.get(position));
                holder.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        button = holder.button;
                        device = (WifiP2pDevice) v.getTag();
                        final WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        config.wps.setup = WpsInfo.PBC;
                        if (((Button) v).getText().toString().equalsIgnoreCase("connect")) {
                            final Button b = (Button) v;
                            b.setEnabled(false);
                            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "onSuccess: hereeeeeee");
                                    b.setText("Send Card");
                                    b.setEnabled(true);
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.d(TAG, "onFailure: Failure With connection");

                                }
                            });
                        } else if (((Button) v).getText().toString().equalsIgnoreCase("send card")) {
                            SharedPreferences prefs = mContext.getSharedPreferences("server", MODE_PRIVATE);

                            if (prefs.getBoolean("is_server", false)) {
                                String Ip = prefs.getString("IP", "");

                                Intent serviceIntent = new Intent(mContext, WifiFileTransfer.class);
                                serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_ADDRESS,
                                        Ip);
                                serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_PORT, 8988);
                                mContext.startService(serviceIntent);


                            } else {
                                String Ip = prefs.getString("serverIP", "");
                                Intent serviceIntent = new Intent(mContext, WifiFileTransfer.class);
                                serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_ADDRESS,
                                        Ip);
                                serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_PORT, 8988);
                                mContext.startService(serviceIntent);
                            }

                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return names.isEmpty() ? 1 : names.size();
        }

        @Override
        public Filter getFilter() {
            if (friendFilter == null) {
                friendFilter = new FriendFilter();
            }
            return friendFilter;
        }


        private class FriendFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                Map<WifiP2pDevice, String> map = new HashMap<>();
                if (constraint != null && constraint.length() > 0 && constraint.toString().trim().length() > 0) {
                    ArrayList<String> tempList = new ArrayList<String>();
                    int i = 0;
                    // search content in friend list
                    for (String user : listName) {
                        if (user.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            //tempList.add(user);
                            map.put(listDevice.get(i), user);
                            i++;
                        } else {
                            i++;
                        }
                    }
                    filterResults.count = map.size();
                    filterResults.values = map;
                } else {
                    int i = 0;
                    for (String user : listName) {
                        map.put(listDevice.get(i++), user);
                    }
                    filterResults.count = map.size();
                    filterResults.values = map;
                }
                return filterResults;
            }

            /**
             * Notify about filtered list to ui
             *
             * @param constraint text
             * @param results    filtered result
             */
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                Map<WifiP2pDevice, String> objDeviceMap = (Map) results.values;
                names.clear();
                names.addAll(objDeviceMap.values());
                devices.clear();
                devices.addAll(objDeviceMap.keySet());
                notifyDataSetChanged();

            }
        }


        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTV;
            ImageView imageView;
            public Context context;
            Button button;


            public ViewHolder(View itemView, Context context, int type) {
                super(itemView);
                if (type == 0) {
                    nameTV = (TextView) itemView.findViewById(R.id.business_user_name);
                    button = (Button) itemView.findViewById(R.id.button_connect_user);
                    this.context = context;
                    //itemView.setOnClickListener(this);
                }
            }


        }

    }


    public void setDeviceLayout(RecyclerView deviceLayout) {

        deviceLayout.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        deviceLayout.setAdapter(deviceAdapter);

    }

    public static SendBusinessCardThread getCurrentThread() {
        return connectedThread;
    }


    public void resetData() {

        deviceAdapter.clear();
        deviceAdapter.notifyDataSetChanged();

    }

}
