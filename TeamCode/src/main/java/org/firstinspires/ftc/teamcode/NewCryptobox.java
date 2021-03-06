package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Sage Creek Level Up on 11/21/2017.
 */

@Autonomous(name = "New Cryptobox", group = "autonomous")
public class NewCryptobox extends LinearOpMode implements CameraBridgeViewBase.CvCameraViewListener2 {

    enum CryptoboxDetectionMode {

        HSV_RED, HSV_BLUE

    }


    enum CryptoboxSpeed {

        VERY_FAST, FAST, BALANCED, SLOW, VERY_SLOW

    }

    public CryptoboxDetectionMode detectionMode = CryptoboxDetectionMode.HSV_RED;
    public double downScaleFactor = 0.6;
    public boolean rotateMat = false;
    public CryptoboxSpeed speed = CryptoboxSpeed.BALANCED;
    public boolean debugShowMask = true;
    private boolean CryptoBoxDetected = false;
    private boolean ColumnDetected = false;
    private int[] CryptoBoxPositions = new int[3];

    Scalar lower = new Scalar(90, 135, 25);
    Scalar upper = new Scalar(130, 250, 150);

    private Mat workingMat = new Mat();
    private Mat mask1 = new Mat();
    private Mat mask2 = new Mat();
    private Mat mask = new Mat();
    private Mat hsv = new Mat();
    private Mat structure = new Mat();
    private Mat hierarchy = new Mat();

    Mat kernel = Mat.ones(5, 5, CvType.CV_32F);

    private double center = 640;
    private double targetcenter = 640;

    String key = "left";

    private DcMotor motor1;
    private DcMotor motor2;
    private DcMotor motor3;
    private DcMotor motor4;

    @Override
    public void runOpMode() {

        motor1 = hardwareMap.dcMotor.get("motor1");
        motor2 = hardwareMap.dcMotor.get("motor2");
        motor3 = hardwareMap.dcMotor.get("motor3");
        motor4 = hardwareMap.dcMotor.get("motor4");

        motor1.setDirection(DcMotor.Direction.REVERSE);
        motor2.setDirection(DcMotor.Direction.FORWARD);
        motor3.setDirection(DcMotor.Direction.REVERSE);
        motor4.setDirection(DcMotor.Direction.FORWARD);

        waitForStart();

        startOpenCV(this);

        while (opModeIsActive()) {
            if(targetcenter < center) {
                strafe(0.7);
            }
            else if(targetcenter > center) {
                strafe(-0.7);
            }
            else {
                stopDrive();
            }
        }

        stopOpenCV();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat gray = inputFrame.gray();
        Mat rgba = inputFrame.rgba();

        try {
            Size initSize = rgba.size();
            Size newSize = new Size(initSize.width * downScaleFactor, initSize.height * downScaleFactor);
            rgba.copyTo(workingMat);
            Imgproc.resize(workingMat, workingMat, newSize);
            Imgproc.putText(workingMat, newSize.toString() + speed.toString(), new Point(5, 15), 0, 0.6, new Scalar(0, 255, 255), 2);
            if (rotateMat) {
                Mat tempBefore = workingMat.t();
                Core.flip(tempBefore, workingMat, 1); //mRgba.t() is the transpose
                tempBefore.release();
            }
            List<MatOfPoint> contours = new ArrayList<>();
            List<Rect> boxes = new ArrayList<>();
            Imgproc.erode(workingMat, workingMat, kernel);
            Imgproc.dilate(workingMat, workingMat, kernel);
            Imgproc.cvtColor(workingMat, hsv, Imgproc.COLOR_RGB2HSV);
            switch (detectionMode) { //I'm sad about this.... I'll fix it later
                case HSV_RED:
                    getRedMask(hsv);
                    break;
                case HSV_BLUE:
                    getBlueMask(hsv);
            }
            switch (speed) {
                case VERY_FAST:
                    Imgproc.blur(hsv, hsv, new Size(3, 3));
                    structure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 30));
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, structure);
                    break;
                case FAST:
                    Imgproc.blur(hsv, hsv, new Size(4, 4));
                    structure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 30));
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, structure);
                    break;
                case BALANCED:
                    Imgproc.blur(hsv, hsv, new Size(5, 5));
                    structure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 40));
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, structure);
                    break;
                case SLOW:
                    Imgproc.blur(hsv, hsv, new Size(7, 7));
                    structure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 55));
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, structure);
                    break;
                case VERY_SLOW:
                    Imgproc.blur(hsv, hsv, new Size(8, 8));
                    structure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 60));
                    Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, structure);
                    break;
            }
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            for (MatOfPoint c : contours) {
                if (Imgproc.contourArea(c) >= newSize.height / 4 * 30) { //Filter by area
                    Rect column = Imgproc.boundingRect(c);
                    int ratio = Math.abs(column.height / column.width);
                    if (ratio > 1.5) { //Check to see if the box is tall
                        boxes.add(column); //If all true add the box to array
                    }
                }
            }
            for (Rect box : boxes) {
                Imgproc.rectangle(workingMat, new Point(box.x, box.y), new Point(box.x + box.width, box.y + box.height), new Scalar(255, 0, 0), 2);
            }
            Collections.sort(boxes, new Comparator<Rect>() {
                @Override
                public int compare(Rect rect, Rect t1) {
                    if (rect.x > t1.x) {
                        return 1;
                    } else if (rect.x < t1.x) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
            CryptoBoxDetected = boxes.size() >= 4;
            if (CryptoBoxDetected) {
                Point left = drawSlot(0, boxes);
                Point center = drawSlot(1, boxes);
                Point right = drawSlot(2, boxes);
                CryptoBoxPositions[0] = (int) left.x;
                CryptoBoxPositions[1] = (int) center.x;
                CryptoBoxPositions[2] = (int) right.x;
                Imgproc.putText(workingMat, "Left", new Point(left.x - 10, left.y - 20), 0, 0.8, new Scalar(0, 255, 255), 2);
                Imgproc.circle(workingMat, left, 5, new Scalar(0, 255, 255), 3);
                Imgproc.putText(workingMat, "Center", new Point(center.x - 10, center.y - 20), 0, 0.8, new Scalar(0, 255, 255), 2);
                Imgproc.circle(workingMat, center, 5, new Scalar(0, 255, 255), 3);
                Imgproc.putText(workingMat, "Right", new Point(right.x - 10, right.y - 20), 0, 0.8, new Scalar(0, 255, 255), 2);
                Imgproc.circle(workingMat, right, 5, new Scalar(0, 255, 255), 3);

                if(key == "left") {
                    targetcenter = left.x;
                    telemetry.addLine("I ran");
                }
                else if(key == "right") {
                    targetcenter = right.x;
                }
                else {
                    targetcenter = center.x;
                }
            } else {
                ArrayList<Double> pts = new ArrayList<>();
                for (int i = 0; i < boxes.size() - 1; i++) {
                    Point column = drawSlot(i, boxes);
                    pts.add(column.x);
                    Imgproc.circle(workingMat, column, 5, new Scalar(0, 255, 255), 3);
                    if (i < 3) {
                        CryptoBoxPositions[i] = (int) column.x;
                    }
                }
                //targetcenter = Collections.min(pts);
                /*
                if (key == "left") {
                    targetcenter = Collections.min(pts);
                }
                else if(key == "right") {
                    targetcenter = Collections.max(pts);
                }
                else {
                    Collections.sort(pts);

                }
                */
                ColumnDetected = boxes.size() > 1;
            }
            if (rotateMat) {
                Mat tempAfter = workingMat.t();
                Core.flip(tempAfter, workingMat, 0); //mRgba.t() is the transpose
                tempAfter.release();
            }
            Imgproc.resize(workingMat, workingMat, initSize);
        }
        catch (Exception e) {
            telemetry.addData("error", e.getMessage());
        }
        telemetry.update();
        return workingMat;
        // This is where the magic will happen. inputFrame has all the data for each camera frame.
    }

    public void startOpenCV(CameraBridgeViewBase.CvCameraViewListener2 cameraViewListener) {
        FtcRobotControllerActivity.turnOnCameraView.obtainMessage(1, cameraViewListener).sendToTarget();
    }

    public void stopOpenCV() {
        try {
            FtcRobotControllerActivity.turnOffCameraView.obtainMessage().sendToTarget();

        } catch (Exception e) {
            telemetry.addData("error:", e.getMessage());
            telemetry.addData("cause:", e.getCause());
            telemetry.addData("stack trace:", e.getStackTrace());
            telemetry.update();
        }
    }
    public Mat getRedMask(Mat input){
        Scalar lower1 = new Scalar(0,150,100);
        Scalar upper1 = new Scalar(20,255,255);
        Scalar lower2 = new Scalar(140,100,100);
        Scalar upper2 = new Scalar(179,255,255);
        Core.inRange(input,lower1,upper1,mask1);
        Core.inRange(input,lower2,upper2,mask2);
        Core.addWeighted(mask1,1.0, mask2,1.0, 0.0, mask);
        return mask;
    }
    public Mat getBlueMask(Mat input){
        Scalar lower = new Scalar(90, 135, 25);
        Scalar upper = new Scalar(130, 250, 150);
        Core.inRange(input,lower,upper,mask);
        return mask;
    }
    public Point drawSlot(int slot, List<Rect> boxes){
        Rect leftColumn = boxes.get(slot); //Get the pillar to the left
        Rect rightColumn = boxes.get(slot + 1); //Get the pillar to the right
        int leftX = leftColumn.x + leftColumn.width; //Get the X Coord
        int rightX = rightColumn.x; //Get the X Coord
        int drawX = ((rightX - leftX) / 2) + leftX; //Calculate the point between the two
        int drawY = leftColumn.height + leftColumn.y; //Calculate Y Coord. We wont use this in our bot's opetation, buts its nice for drawing
        return new Point(drawX, drawY);
    }
    public ArrayList ones(int width, int height) {
        ArrayList output = new ArrayList();
        for(int i = 1; i <= height; i++) {
            ArrayList row = new ArrayList();
            for(int j = 1; i <= width; i++) {
                row.add(1);
            }
            output.add(row);
        }
        return output;
    }
    public int[] getCryptoBoxPositions() {
        return CryptoBoxPositions;
    }
    public int getCryptoBoxLeftPosition() {
        return CryptoBoxPositions[0];
    }
    public int getCryptoBoxCenterPosition() {
        return CryptoBoxPositions[1];
    }
    public int getCryptoBoxRightPosition() {
        return CryptoBoxPositions[2];
    }
    public boolean isCryptoBoxDetected() {
        return CryptoBoxDetected;
    }
    public boolean isColumnDetected() {
        return ColumnDetected;
    }

    private void strafe(double power) {
        motor1.setPower(power);
        motor2.setPower(-power);
        motor3.setPower(-power);
        motor4.setPower(power);
    }
    private void stopDrive(){
        motor1.setPower(0);
        motor2.setPower(0);
        motor3.setPower(0);
        motor4.setPower(0);
    }
}