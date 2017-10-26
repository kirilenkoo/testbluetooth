package com.kirilenkoo.www.testbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.UUID;

public class ServerActivity extends AppCompatActivity {
    final String TAG = "SERVER";
    final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    UUID MY_UUID;
    String NAME = "kurt";
    private BluetoothServerSocket mmServerSocket;

    MyBluetoothService myBluetoothService;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MessageConstants.MESSAGE_READ:
                    int numBytes = msg.arg1;
                    byte[] bytes = (byte[]) msg.obj;
                    Log.d(TAG,""+numBytes+"::"+bytes.toString());
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
        setContentView(R.layout.activity_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        MY_UUID = UUID.fromString("bc187f36-ba13-11e7-abc4-cec278b6b50a");
    }

    private boolean checkBlueToothExist(){
        return mBluetoothAdapter != null;
    }

    private void confirmBlueToothOpen(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            onpenServer();
        }
    }

    private void onpenServer() {
        new AcceptThread().start();
    }


    private class AcceptThread extends Thread {


        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {
        Log.d(TAG,"server connected");
        myBluetoothService = new MyBluetoothService(mHandler);
        myBluetoothService.startServer(socket);
    }
    private void closeService(){
        if(myBluetoothService!=null){
            myBluetoothService.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
        closeService();
    }
}
