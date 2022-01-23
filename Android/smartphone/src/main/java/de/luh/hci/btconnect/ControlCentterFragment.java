package de.luh.hci.btconnect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ControlCentterFragment extends Fragment {

    private EditText ccVibrationFuncNumberField;
    String vibFunc = "Vibration1";

    private Button oledButton;
    private Button ledButton;
    private Button pokeButton;
    private Button vibrationButton;
    private Button buzzerButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controlcenter, container, false);

        ccVibrationFuncNumberField = (EditText) view.findViewById(R.id.CC_vibfunc_field);

        oledButton = (Button) view.findViewById(R.id.oled_button);
        ledButton = (Button) view.findViewById(R.id.led_button);
        pokeButton = (Button) view.findViewById(R.id.poke_button);
        vibrationButton = (Button) view.findViewById(R.id.vibration_button);
        buzzerButton = (Button) view.findViewById(R.id.buzzer_button);

        oledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BluetoothSearchFragment.btdService != null) BluetoothSearchFragment.btdService.write("OLED");
            }
        });

        ledButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BluetoothSearchFragment.btdService != null) BluetoothSearchFragment.btdService.write("LED");
            }
        });

        pokeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BluetoothSearchFragment.btdService != null) BluetoothSearchFragment.btdService.write("Poke");
            }
        });

        vibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vibFunc = "Vibration" + ccVibrationFuncNumberField.getText().toString();
                if(BluetoothSearchFragment.btdService != null) BluetoothSearchFragment.btdService.write(vibFunc);
            }
        });

        buzzerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BluetoothSearchFragment.btdService != null) BluetoothSearchFragment.btdService.write("Buzzer");
            }
        });
        return view;
    }
}