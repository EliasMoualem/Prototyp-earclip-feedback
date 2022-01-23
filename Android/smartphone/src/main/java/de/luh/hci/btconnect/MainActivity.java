package de.luh.hci.btconnect;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import de.luh.hci.btconnect.shared.BluetoothDataService;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BluetoothDataService btdService;
    private ArrayAdapter<String> deviceAdapter;

    private Context context;

    public File logFile;
    public FileOutputStream output;

    public BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle("Earclip feedback study app");

        Intent i = new Intent(this, BluetoothDataService.class);
        bindService(i, this, Context.BIND_AUTO_CREATE);

        context = getApplicationContext();

        bottomNav = findViewById(R.id.bottom_navigationbar);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        final BluetoothSearchFragment bsf = new BluetoothSearchFragment();
        fragmentTransaction.add(R.id.fragment_container, bsf, BluetoothSearchFragment.class.getSimpleName());
        fragmentTransaction.commit();

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                Fragment selectedFragment = null;

                switch (menuItem.getItemId()){
                    case R.id.nav_bluetooth:
                        selectedFragment = bsf;
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment, selectedFragment.getTag()).commit();
                        break;
                    case R.id.nav_study:
                        selectedFragment = new HomeFragment();
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment, selectedFragment.getTag()).commit();
                        break;
                    case R.id.nav_control_center:
                        selectedFragment = new ControlCentterFragment();
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment, selectedFragment.getTag()).commit();
                        break;
                }
                return true;
            }
        });

        //getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new HomeFragment()).commit();

    }

    public void onServiceConnected(ComponentName name, IBinder binder) {
        BluetoothDataService.MyBinder b = (BluetoothDataService.MyBinder) binder;
        btdService = b.getService();
        Toast.makeText(MainActivity.this, "Service startet and connected", Toast.LENGTH_SHORT).show();

        deviceAdapter = new ArrayAdapter<>(context, R.layout.device_name, btdService.getDeviceNames());
        btdService.setDeviceAdapter(deviceAdapter);

        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag(BluetoothSearchFragment.class.getSimpleName());
        if(f instanceof BluetoothSearchFragment){
            BluetoothSearchFragment bsf = (BluetoothSearchFragment) f;
            bsf.getBleList().setAdapter(btdService.getDeviceAdapter());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        btdService = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
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

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BluetoothDataService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, BluetoothDataService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    public BluetoothDataService getBoundBluetoothService(){
        return btdService;
    }

    public BottomNavigationView getBottomNav() {return bottomNav;}
}