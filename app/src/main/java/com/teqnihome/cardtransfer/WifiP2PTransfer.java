package com.teqnihome.cardtransfer;

import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.teqnihome.cardtransfer.Thread.SendBusinessCardThread;

import java.util.ArrayList;
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


    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.business_layout_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mContext = this;
        ImageCache.setContext(mContext);


        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        bluetoothEnabled();


    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        this.info = info;
        Log.d(TAG, "onConnectionInfoAvailable:  " + info.isGroupOwner);
        if (!info.isGroupOwner) {

            deviceAdapter.setInfo(info);
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
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder.getItemViewType() == 0) {
                holder.nameTV.setText(names.get(position));
                holder.itemView.setTag(devices.get(position));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        device = (WifiP2pDevice) v.getTag();
                        final WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;
                        config.wps.setup = WpsInfo.PBC;
                        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "onSuccess: in print");
                                Intent serviceIntent = new Intent(mContext, WifiFileTransfer.class);
                                serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_ADDRESS,
                                        info.groupOwnerAddress.getHostAddress());
                                serviceIntent.putExtra(WifiFileTransfer.EXTRAS_GROUP_OWNER_PORT, 8988);
                                mContext.startService(serviceIntent);

                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "onFailure: Failure With connection");

                            }
                        });
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


            public ViewHolder(View itemView, Context context, int type) {
                super(itemView);
                if (type == 0) {
                    nameTV = (TextView) itemView.findViewById(R.id.business_user_name);
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
