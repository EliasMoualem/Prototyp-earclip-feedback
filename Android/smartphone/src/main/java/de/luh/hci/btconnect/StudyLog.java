package de.luh.hci.btconnect;

import android.os.Environment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StudyLog {

    private File logFile;
    private FileOutputStream output;

    public StudyLog(int pid, String modality, String activity) {

        logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), buildFilename(pid, modality,activity));

        try {
            output = new FileOutputStream(logFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            output.write("participantID;activity;trials;timeInSec;modality;responseTimeInMs;responseBtnClicked\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String buildFilename(int pid, String modality, String activity) {
        return pid + "_" + modality + "_" + activity + ".csv";
    }

    public void logData (int pid, String activity, int trials, long time, String modality, long responseTime, boolean buttonClicked){

        String logString = String.format(
                "%d;%S;%d;%d;%s;%d;%b\n",
                pid,
                activity,
                trials,
                time,
                modality,
                responseTime,
                buttonClicked);
        try {
            output.write(logString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close () {
        try {
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        output = null;
        logFile = null;
    }
}