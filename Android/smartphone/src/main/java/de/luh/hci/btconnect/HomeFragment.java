package de.luh.hci.btconnect;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class HomeFragment extends Fragment {

    private EditText timeField;
    private Spinner modalitySpinner;
    private Spinner activitySpinner;
    private Button startStudyButton;
    private EditText trialsNumberField;
    private EditText participantIdField;
    private EditText vibrationFuncNumberField;

    private Activity parentActivity;
    public BottomNavigationView bnv;

    public String time = "";
    public String modality = "";
    public String activity = "";
    public int trials = 0;
    public int pid = 0;
    public int vibrationFunction  = 1;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        parentActivity = getActivity();

        timeField = (EditText) view.findViewById(R.id.time_field);
        trialsNumberField = (EditText) view.findViewById(R.id.trials_field);
        modalitySpinner = (Spinner) view.findViewById(R.id.modality_spinner);
        activitySpinner = (Spinner) view.findViewById(R.id.activity_spinner);
        startStudyButton = (Button) view.findViewById(R.id.start_study_button);
        participantIdField = (EditText) view.findViewById(R.id.participant_id_field);
        vibrationFuncNumberField = (EditText) view.findViewById(R.id.vibfunc_number_field);

        bnv = ((MainActivity) parentActivity).getBottomNav();

        startStudyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startStudyFragment();
            }
        });

        activitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        activity= "reading book";
                        break;
                    case 1:
                        activity = "mamory game";
                        break;
                    case 2:
                        activity = "watching lecture";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        modalitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        modality = "OLED";
                        vibrationFuncNumberField.setVisibility(View.INVISIBLE);
                        break;
                    case 1:
                        modality = "LED";
                        vibrationFuncNumberField.setVisibility(View.INVISIBLE);
                        break;
                    case 2:
                        modality = "Poke";
                        vibrationFuncNumberField.setVisibility(View.INVISIBLE);
                        break;
                    case 3:
                        modality = "Vibration";
                        vibrationFuncNumberField.setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        modality = "Buzzer";
                        vibrationFuncNumberField.setVisibility(View.INVISIBLE);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        return view;
    }

    private void startStudyFragment(){
        //make sure the fields are not empty then start the study with the retrieved data and make the navbar invisible.
        if(!timeField.getText().toString().isEmpty() && !trialsNumberField.getText().toString().isEmpty() && !participantIdField.getText().toString().isEmpty()) {
            time = timeField.getText().toString();
            trials = Integer.parseInt(trialsNumberField.getText().toString());
            pid = Integer.parseInt(participantIdField.getText().toString());
            vibrationFunction = Integer.parseInt(vibrationFuncNumberField.getText().toString());
            if (modality.equals("Vibration")){
                modality = modality + String.valueOf(vibrationFunction);
            }
            StudyFragment studyFragment = new StudyFragment();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, studyFragment, studyFragment.getTag()).commit();
            studyFragment.startStudy(time, modality, trials, pid, activity, vibrationFunction);
            bnv.setVisibility(View.INVISIBLE);
        }else {
            Toast.makeText(getContext(),"obligatory field is empty",Toast.LENGTH_LONG).show();
        }
    }
}