package de.luh.hci.btconnect.shared;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.UUID;


public class BluetoothDataService extends Service {

    private static final String TAG = BluetoothDataService.class.getSimpleName();
    private static final String ACTION_DATA_AVAILABLE = "de.luh.hci.btconnect.ACTION_DATA_AVAILABLE";
    private final static String ACTION_GATT_CONNECTED = "de.luh.hci.btconnect.ACTION_GATT_CONNECTED";
    private final static String ACTION_GATT_DISCONNECTED = "de.luh.hci.btconnect.ACTION_GATT_DISCONNECTED";
    private final static String ACTION_GATT_SERVICES_DISCOVERED = "de.luh.hci.btconnect.ACTION_GATT_SERVICES_DISCOVERED";
    private final static String EXTRA_DATA = "de.luh.hci.btconnect.EXTRA_DATA";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    final int handlerState = 0;                        //used to identify handler message
    private Handler bluetoothIn;
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter = null;
    private BluetoothLeScanner btScanner;
    private Boolean btScanning = false;
    private ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic receiverCharacteristic;
    private BluetoothGattCharacteristic transmitterCharacteristic;
    private String sendToBTDevice = "";

    private static final long SCAN_PERIOD = 5000;

    //private ConnectingThread mConnectingThread;
    //private ConnectedThread mConnectedThread;

    private boolean stopThread;
    // UART UUID Service
    private static final UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    // UART UUID Transmitter
    private static final UUID UART_TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    // UART UUID Receiver
    private static final UUID UART_RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // Default UUID for message receiving via UART
    private static final UUID DEFAULT_CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    // String for MAC address of last known device
    private String MAC_ADDRESS = "YOUR:MAC:ADDRESS:HERE";

    private StringBuilder recDataString = new StringBuilder();

    private IBinder binder = new MyBinder();

    //Needed for runOnUiThread
    private Handler handler;

    private Context context = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SERVICE CREATED");
        stopThread = false;
        handler = new Handler();
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        devicesDiscovered = new ArrayList<>();
        deviceNames = new ArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SERVICE STARTED");
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Log.d("DEBUG", "handleMessage");
                if (msg.what == handlerState) { //if message is what we want
                    String readMessage = (String) msg.obj; // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);//`enter code here`
                    Log.d("RECORDED", recDataString.toString());
                    // Do stuff here with your data, like adding it to the database
                }
                recDataString.delete(0, recDataString.length()); //clear all string data
            }
        };
        btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth adapter
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(bluetoothIn != null) bluetoothIn.removeCallbacksAndMessages(null);
        stopThread = true;
        /*
        if (mConnectedThread != null) {
            //mConnectedThread.closeStreams();
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
        }
         */
        Log.d("SERVICE", "onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onUnbind");
        context = getApplicationContext();
        return binder;
    }

    public class MyBinder extends Binder {
        public BluetoothDataService getService(){
            return BluetoothDataService.this;
        }
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        private final String TAG = BluetoothGattCallback.class.getSimpleName();

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            switch (newState) {
                case BluetoothProfile.STATE_DISCONNECTED:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context,"Disconnected from "+gatt.getDevice().getName(),Toast.LENGTH_LONG).show();
                        }
                    });
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    gatt.close();
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context,"Connected to "+gatt.getDevice().getName(),Toast.LENGTH_LONG).show();
                        }
                    });
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    // discover services and characteristics for this device
                    gatt.discoverServices();

                    break;
                default:
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "Encountered unknown state: "+newState);
                        }
                    });
                    gatt.close();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a BluetoothGatt.discoverServices() call
            runOnUiThread(new Runnable() {
                public void run() {
                    //bleList.append("device services have been discovered\n");
                }
            });
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            if (gatt.getServices() == null) return;

            // Loops through available GATT Services.
            for (BluetoothGattService gattService : gatt.getServices()) {
                Log.d(TAG,"UUID: " + gattService.getUuid());
                if(gattService.getUuid().equals(UART_UUID)){
                    Log.i(TAG, "Found matching UUID : " + UART_UUID);
                    try {
                        transmitterCharacteristic = gattService.getCharacteristic(UART_TX_UUID);
                        transmitterCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                        receiverCharacteristic = gattService.getCharacteristic(UART_RX_UUID);
                        if (!gatt.setCharacteristicNotification(receiverCharacteristic, true)) {
                            Log.d(TAG, "Couldn't set notifications for RX characteristic!");
                        }
                        // Next update the RX characteristic's client descriptor to enable notifications.
                        if (receiverCharacteristic.getDescriptor(DEFAULT_CLIENT_UUID) != null) {
                            BluetoothGattDescriptor desc = receiverCharacteristic.getDescriptor(DEFAULT_CLIENT_UUID);
                            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            if (!gatt.writeDescriptor(desc)) {
                                Log.d(TAG, "Couldn't write RX client descriptor value!");
                            }
                        }
                        else {
                            Log.d(TAG, "Couldn't get RX client descriptor!");
                            for (BluetoothGattDescriptor descriptor:receiverCharacteristic.getDescriptors()){
                                Log.i(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                            }
                        }

                    }catch(Exception e){
                        Log.w(TAG, "Can not get characteristic: "+e.toString());
                    }
                }
            }
        }

        @Override
        @Deprecated
        // Result of a characteristic read operation
        // This method won't be triggered when the BLE device sends a message over UART.
        // Use onCharacteristicChanged instead.
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            Log.d(TAG,"Read data : " + characteristic.getStringValue(0));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG,"Wrote data : " + characteristic.getStringValue(0));
            }
        }

        @Override
        // Use this for incoming messages via UART from BLE device.
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            runOnUiThread(new Runnable() {
                public void run() {
                    //bleList.append("device read or wrote to\n");
                    //TODO: Handle received message
                    Log.d(TAG, "onCharacteristicChanged : "+characteristic.getStringValue(0));
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            });
        }

        // Currently unsure whether I will use this
        private void broadcastUpdate(final String action) {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }

        private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }
    };

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice() != null) {
                if(result.getDevice().getName() != null && !deviceNames.contains(result.getDevice().getName()+" ; "+result.getDevice().getAddress())) {
                    try{deviceAdapter.add(result.getDevice().getName()+" ; "+result.getDevice().getAddress());} catch (Exception e){}
                    devicesDiscovered.add(result.getDevice());
                }
            }
        }
    };

    public void startScanning() {
        btScanning = true;
        deviceAdapter.clear();
        deviceNames.clear();
        devicesDiscovered.clear();

        //bleList.append("Started Scanning\n");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, SCAN_PERIOD);
    }

    public void stopScanning() {
        //bleList.append("Stopped Scanning\n");
        btScanning = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void write(String input){
        sendToBTDevice = input;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (transmitterCharacteristic != null) {
                        transmitterCharacteristic.setValue(sendToBTDevice);
                        bluetoothGatt.writeCharacteristic(transmitterCharacteristic);
                    } else {
                        Log.e(TAG, "Missing characteristic on BT Device.");
                    }
                }
                catch (Exception e){
                    Log.d(TAG, e.toString());
                }
            }
        });
    }

    public boolean isScanning(){
        return  btScanning;
    }

    public BluetoothGatt getBluetoothGatt(){
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt btGattInput){
        if(btGattInput != null) MAC_ADDRESS = btGattInput.getDevice().getAddress();
        bluetoothGatt = btGattInput;
    }

    public BluetoothGattCallback getBluetoothGattCallback(){
        return btleGattCallback;
    }

    public ArrayList<BluetoothDevice> getDevicesDiscovered() {
        return devicesDiscovered;
    }

    public ArrayList<String> getDeviceNames() {
        return deviceNames;
    }

    /**
     * Necessary if you want to show the devices in the application
     * @param da set the ArrayAdapter to display the devices in the application
     */
    public void setDeviceAdapter(ArrayAdapter<String> da){
        deviceAdapter = da;
    }

    public ArrayAdapter<String> getDeviceAdapter(){
        return deviceAdapter;
    }
}