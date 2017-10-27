package com.kirilenkoo.www.testbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    final String TAG = "BLUE_MAIN";
    HashMap<String, String> devices = new HashMap<>();
    ArrayList<String> deviceMacs = new ArrayList<>();
    ListView listView;
    BaseAdapter mAdapter;
    UUID MY_UUID;
    TextView textView;

    MyBluetoothService myBluetoothService;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MessageConstants.MESSAGE_READ:
                    int numBytes = msg.arg1;
                    byte[] bytes = (byte[]) msg.obj;
                    Log.d(TAG,"length:"+numBytes);
                    Log.d(TAG,"received:"+bytesToHexString(bytes));
                    break;
                case MessageConstants.MESSAGE_WRITE:
                    Log.d(TAG,"data writed");
                    break;
                case MessageConstants.MESSAGE_TOAST:
                    Log.d(TAG,msg.getData().getString("toast"));
                    break;
            }
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeAndSendPackage();

//                if(myBluetoothService!=null){
//                    String str =  "kirilenko";
//                    byte[] bytes = str.getBytes();
//                    myBluetoothService.write(bytes);
//                }
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        listView = (ListView) findViewById(R.id.listView);
        mAdapter = new ListAdapter();
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                stopSearch();
                connectDevice(deviceMacs.get(position));
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!checkBlueToothExist()) {
                    Snackbar.make(view, "蓝牙不可用", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }else{
                    Log.d(TAG,"start process");
                    confirmBlueToothOpen();
                }
            }
        });

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    }

    private void connectDevice(String mac) {
        new ConnectThread(mBluetoothAdapter.getRemoteDevice(mac)).start();
    }

    class ListAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return deviceMacs.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceMacs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = new TextView(MainActivity.this);
            textView.setText(devices.get(deviceMacs.get(position)));
            textView.setPadding(100,100,100,100);
            return textView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_server) {
            Intent goServer = new Intent(MainActivity.this, ServerActivity.class);
            startActivity(goServer);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean checkBlueToothExist(){
        return mBluetoothAdapter != null;
    }

    private void confirmBlueToothOpen(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            searchBlueToothDevices();
        }
    }

    private void searchBlueToothDevices(){
        getBondedDevices();
        boolean started = mBluetoothAdapter.startDiscovery();
        Log.d(TAG,"discovery started"+started);
    }
    private void stopSearch(){
        if(mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void getBondedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG,"bonded device:"+deviceName+"||"+deviceHardwareAddress);
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                devices.put(deviceHardwareAddress, deviceName);
                if(!deviceMacs.contains(deviceHardwareAddress)) {
                    deviceMacs.add(deviceHardwareAddress);
                    mAdapter.notifyDataSetChanged();
                }

                Log.d(TAG,"searched device:"+deviceName+"||"+deviceHardwareAddress);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Log.d(TAG,"searched device:started");
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d(TAG,"searched device:finished");
            }
        }
    };

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            stopSearch();
//            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket mmSocket) {
        Log.d(TAG,"connected");
        myBluetoothService = new MyBluetoothService(mHandler);
        myBluetoothService.startServer(mmSocket);
    }

    private void closeService(){
        if(myBluetoothService!=null){
            myBluetoothService.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

            switch (requestCode){
                case REQUEST_ENABLE_BT:
                    if(resultCode == RESULT_OK) {
                        //bluetooth opened
                        searchBlueToothDevices();
                    }else{
                        //bluetooth open failed
                    }
                    break;
            }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopSearch();
        closeService();
    }

    private void makeAndSendPackage() {
        byte head = 0x7e;
        byte type = 0x01;
        byte[] placeholders = {0x55,0x55,0x55,0x55};
        byte[] unchecked = {0x01,0x55,0x55,0x55,0x55};
        byte checkByte = 0x55;
        for (byte b: unchecked){
            checkByte ^= b;
        }
        byte tail = 0x7f;

        byte[] pack = new byte[8];
        pack[0] = head;
        pack[1] = type;
        System.arraycopy(placeholders,0,pack,2,4);
        pack[6] = checkByte;
        pack[7] = tail;

        myBluetoothService.write(pack);

    }


    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}
