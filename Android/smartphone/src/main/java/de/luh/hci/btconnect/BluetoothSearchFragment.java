package de.luh.hci.btconnect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.luh.hci.btconnect.shared.BluetoothDataService;

public class BluetoothSearchFragment extends Fragment {

    private static final String TAG = BluetoothSearchFragment.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Activity parentActivity;
    public static BluetoothDataService btdService;

    private Button startScanningButton;
    private Button stopScanningButton;
    private Button disconnectButton;

    private Button sendButton;
    private EditText bleInput;

    private ListView bleList;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth_search, container, false);

        parentActivity = getActivity();
        btdService = ((MainActivity) parentActivity).getBoundBluetoothService();

        bleInput = view.findViewById(R.id.BLEcmd);

        bleList = view.findViewById(R.id.BLEList);

        bleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemClick");
                String clicked = (String) parent.getItemAtPosition(position);
                String[] splitted = clicked.split(" ; ");
                BluetoothDevice device = null;
                for(BluetoothDevice btd : btdService.getDevicesDiscovered()){
                    if(btd.getAddress().equals(splitted[1])){
                        device = btd;
                        break;
                    }
                }

                if(device != null) {
                    if (btdService.getBluetoothGatt() != null) {
                        btdService.getBluetoothGatt().disconnect();
                        btdService.setBluetoothGatt(null);
                    }
                    btdService.setBluetoothGatt(device.connectGatt(getContext(), true, btdService.getBluetoothGattCallback(), BluetoothDevice.TRANSPORT_LE));
                } else {
                    Toast.makeText(getContext(),"Could not connect...",Toast.LENGTH_LONG).show();
                }
            }
        });

        startScanningButton = (Button) view.findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) view.findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        //stopScanningButton.setVisibility(View.INVISIBLE);

        disconnectButton = (Button) view.findViewById(R.id.DisconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (btdService.getBluetoothGatt() != null) {
                    btdService.getBluetoothGatt().disconnect();
                }
            }
        });

        sendButton = (Button) view.findViewById(R.id.BLEsend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(btdService != null) btdService.write(bleInput.getText().toString());
            }
        });

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (parentActivity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        return view;
    }

    public void startScanning() {
        if(btdService == null){
            btdService = ((MainActivity) parentActivity).getBoundBluetoothService();
        }
        //bleList.append("Started Scanning\n");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        btdService.startScanning();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                while(btdService.isScanning()){}
                parentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startScanningButton.setVisibility(View.VISIBLE);
                        stopScanningButton.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }

    public void stopScanning() {
        //bleList.append("Stopped Scanning\n");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        btdService.stopScanning();
    }

    public ListView getBleList(){
        return bleList;
    }
}
