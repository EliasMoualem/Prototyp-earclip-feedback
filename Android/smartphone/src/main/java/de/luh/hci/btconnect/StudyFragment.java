package de.luh.hci.btconnect;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Random;

public class StudyFragment extends Fragment {

    private Button feedbackSensedButton;
    private CountDownTimer countDownTimer;
    private CountDownTimer extraCountDownTimer;
    private boolean extraCountDownTimerIsRunning = false;
    private long periodInMilliSeconds = 0;

    private Activity parentActivity;
    public BottomNavigationView bnv;
    boolean feedbackSensedButtonClicked = false;
    public int trialsDone = 0;
    public long timeOfCommand = 0;
    public long timeOfFeedbackSensedButtonClicked = 0;
    public long responseTime = 0;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_study, container, false);

        parentActivity = getActivity();
        bnv = ((MainActivity) parentActivity).getBottomNav();

        feedbackSensedButton = (Button) view.findViewById(R.id.feedback_sensed_button);
        feedbackSensedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedbackSensedButtonClicked = true;
                timeOfFeedbackSensedButtonClicked =  System.currentTimeMillis();
                if (extraCountDownTimerIsRunning) {
                    extraCountDownTimer.cancel();
                    extraCountDownTimer.onFinish();
                }
            }
        });
        return view;
    }

    public void startStudy(String time, String modality, int trials, int pid, String activity, int vibrationFunction) {

        StudyLog studyLog = new StudyLog(pid, modality, activity);
        String[] splittedTime = time.split(":");
        final int minutes = Integer.parseInt(splittedTime[0]);
        final int seconds = Integer.parseInt(splittedTime[1]);
        periodInMilliSeconds = minutes * 60000 + seconds * 1000;

        startTimer(modality, trials -1, studyLog, pid, activity);
    }

    //starts initial timer of length = user input, when timeleft = Random, a command = user input is sent to the micro-controller then the timer is canceld and the extra timer is started.
    public void startTimer(final String chosenModality, final int trials, final StudyLog studyLog, final int pid, final String activity){

        timeOfCommand = 0;
        timeOfFeedbackSensedButtonClicked = 0;
        final int min = 2000;
        final int max = (int) periodInMilliSeconds;

        //1000ms< random < 9000ms.
        final int random = (new Random().nextInt((max/1000)-(min/1000))) * 1000 + min;


        countDownTimer = new CountDownTimer(periodInMilliSeconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                //periodInMilliSeconds = millisUntilFinished;
                System.out.println("timer works");
                System.out.println(millisUntilFinished);
                System.out.println(random);
                if(millisUntilFinished <= random){
                    BluetoothSearchFragment.btdService.write(chosenModality);
                    countDownTimer.cancel();
                    countDownTimer.onFinish();
                    startExtraTimer(random, chosenModality , trials, studyLog, pid, activity);
                    timeOfCommand = System.currentTimeMillis();
                }
            }
            @Override
            public void onFinish() {
                //do nothing
            }
        }.start();
    }
    //extra timer in which the user has 15sec time to respond to feedback, when respond button is clicked, responce time is calculated,
    // extra timer is canceld. if number of trials done is != trials should be done then initial timer is started otherwise log calculated
    // and retrieved data to csv and make navbar visible, and end study and go back to home fragment.
    public void startExtraTimer(final int random, final String chosenModality, final int trials, final StudyLog studyLog, final int pid, final String activity){

        extraCountDownTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                extraCountDownTimerIsRunning = true;
                System.out.println("extra timer works");
            }
            @Override
            public void onFinish() {
                extraCountDownTimerIsRunning = false;
                responseTime = timeOfFeedbackSensedButtonClicked - timeOfCommand;
                System.out.println("time to reaction is: " + responseTime);
                if (responseTime < 0) responseTime = 0;
                studyLog.logData(pid, activity, trialsDone + 1, periodInMilliSeconds / 1000, chosenModality, responseTime, feedbackSensedButtonClicked);
                feedbackSensedButtonClicked = false;
                if(trialsDone == trials) {
                    studyLog.close();
                    bnv.setVisibility(View.VISIBLE);
                    HomeFragment homeFragment = new HomeFragment();
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container, homeFragment, homeFragment.getTag()).commit();
                }else {
                    trialsDone++;
                    startTimer(chosenModality, trials, studyLog, pid, activity);
                }
            }
        }.start();
    }
}