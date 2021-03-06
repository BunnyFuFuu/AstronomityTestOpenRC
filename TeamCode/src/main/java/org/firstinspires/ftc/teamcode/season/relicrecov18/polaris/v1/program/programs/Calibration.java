package org.firstinspires.ftc.teamcode.season.relicrecov18.polaris.v1.program.programs;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sage Creek Level Up on 11/21/2017.
 */
@Autonomous(name = "Test", group = "meme")
public class Calibration extends LinearOpMode implements CameraBridgeViewBase.CvCameraViewListener2 {
    String direction;
    int times = 0;
    private ArrayList<Double> a;
    private int count;
    @Override
    public void runOpMode() {

        waitForStart();

        startOpenCV(this);

        while (opModeIsActive()) {

        }

        stopOpenCV();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        a = new ArrayList<>();
        count = 0;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
    public void startOpenCV(CameraBridgeViewBase.CvCameraViewListener2 cameraViewListener) {
        FtcRobotControllerActivity.turnOnCameraView.obtainMessage(1,cameraViewListener).sendToTarget();
    }

    public void stopOpenCV() {
        try {
            FtcRobotControllerActivity.turnOffCameraView.obtainMessage().sendToTarget();
        }
        catch(Exception e) {
            Log.d("Stack Trace", e.getStackTrace().toString());
            telemetry.addData("error:", e.getMessage());
            telemetry.addData("cause:", e.getCause());
            telemetry.addData("stack trace:", e.getStackTrace());
            telemetry.update();
        }
    }

    public double mean(List<Double> vals) {
        double sum = 0;
        for(double val:vals) {
            sum+= val;
        }
        return sum/vals.size();
    }
}




/*
detection ideas:
weighted standard deviation detection
double color detection (red and blue)

fps improvement ideas:
resize image then scale it back up
close all mats that I stop using

noise reduction ideas:
bilateral filter somehow?
median filter?
fastNlMeansDenoisingColored?
 */