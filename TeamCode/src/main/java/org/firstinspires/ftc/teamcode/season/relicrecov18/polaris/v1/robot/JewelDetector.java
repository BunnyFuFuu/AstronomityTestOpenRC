package org.firstinspires.ftc.teamcode.season.relicrecov18.polaris.v1.robot;

import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.teamcode.robot.Robot;
import org.firstinspires.ftc.teamcode.robot.SubSystem;
import org.firstinspires.ftc.teamcode.season.relicrecov18.polaris.v1.program.programs.BallDetector;
import org.firstinspires.ftc.teamcode.season.relicrecov18.polaris.v1.program.programs.Circle;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by andre_000 on 12/31/2017.
 */

public class JewelDetector extends SubSystem implements CameraBridgeViewBase.CvCameraViewListener2{
    public JewelDetector(Robot robot){super(robot);}
    public final ExecutorService service = Executors.newFixedThreadPool(2); //The executor service that will run the two processing threads simultaneously

    public Circle prevRedCircle;
    public Circle prevBlueCircle;
    public String direction;
    @Override
    public void init() throws InterruptedException {

    }

    @Override
    public void handle() {

    }

    @Override
    public void stop() {

    }
    public void programStart(){
        startOpenCV(this);
    }
    public void programStop(){
        stopOpenCV();
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        //Give the backup data an initial value, in case it can't find anything on the first frame
        prevRedCircle = new Circle(new Point(0,0),0);
        prevBlueCircle = new Circle(new Point(0,0),0);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        ArrayList<Mat> rgbaChannels = new ArrayList<>();
        ArrayList<Mat> YUVChannels = new ArrayList<>();

        Mat raw = inputFrame.rgba();
        Mat hsv = new Mat();
        Mat lowerRedRange = new Mat();
        Mat upperRedRange = new Mat();
        Mat red = new Mat();
        Mat blue = new Mat();
        Mat b= new Mat();
        Mat z= new Mat();

        Mat redChannel;
        Mat blueChannel;
        Mat greenChannel;

        //Future<Circle> means that this task will someday return a circle, but not right now
        final Future<Circle> task1;
        final Future<Circle> task2;

        Circle blueCircle;
        Circle redCircle;

        try {
            //Reduce the image size by half to increase speed of the algorithm
            Imgproc.resize(raw, raw, new Size(320, 180));

            //Blur the raw image to reduce noise
            Imgproc.medianBlur(raw, raw, 3);

            //Split the input image into its 4 component channels, red, green, blue, and alpha
            Core.split(raw,rgbaChannels);
            redChannel = rgbaChannels.get(0);
            blueChannel = rgbaChannels.get(2);
            greenChannel = rgbaChannels.get(1);

            //Convert image from RGBA to HSV format
            //This is done to account for variations in lighting, which do not affect the HSV format as much as the RGBA format
            Imgproc.cvtColor(raw, hsv, Imgproc.COLOR_RGBA2RGB);
            Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV);

            //Filters for only red pixels
            //There are two ranges for red because HSV is a cylindrical coordinate system and red circles back on itself
            Core.inRange(hsv, new Scalar(0, 100, 100), new Scalar(8, 255, 255), lowerRedRange);
            Core.inRange(hsv, new Scalar(140,100,100), new Scalar(179,255,255), upperRedRange);

            //Filters for only blue pixels
            //Core.inRange(hsv, new Scalar(85,255,32), new Scalar(135,255,255), blue);
            Imgproc.cvtColor(raw, b, Imgproc.COLOR_RGB2YUV);
            Imgproc.GaussianBlur(b,b,new Size(3,3),0);
            Core.split(b, YUVChannels);
            Imgproc.threshold(YUVChannels.get(1), blue, 145, 255, Imgproc.THRESH_BINARY);

            Core.inRange(hsv, new Scalar(40,100,55), new Scalar(120,255,255), z);

            //Binarize and filter masks
            Imgproc.threshold(blueChannel, blueChannel,140,255, Imgproc.THRESH_BINARY);
            Imgproc.threshold(redChannel, redChannel,145,255, Imgproc.THRESH_BINARY);
            Imgproc.threshold(greenChannel, greenChannel,145,255, Imgproc.THRESH_BINARY);

            hsv.release(); //Empty the hsv Mat

            //Combine the two red ranges into one Mat, then empty the Mats we used
            Core.addWeighted(lowerRedRange, 1, upperRedRange, 1, 0, red);
            lowerRedRange.release();
            upperRedRange.release();

            /*
            Core.bitwise_not(greenChannel,greenChannel);
            Core.bitwise_and(blue,greenChannel,blue);
*/
            //Create the two new processing threads
            BallDetector processRed = new BallDetector(red, redChannel, 0.8, prevRedCircle);
            BallDetector processBlue = new BallDetector(blue, z, 0.7, prevBlueCircle);

            //Register the threads with the executor service
            task1 = service.submit(processRed);
            task2 = service.submit(processBlue);

            //Get the results of the threads
            redCircle = task1.get();
            blueCircle = task2.get();

            //Empty all the mats that were used
            redChannel.release();
            blueChannel.release();
            red.release();
            blue.release();

            //Store the returned circles as backup data for the next frame
            prevBlueCircle = blueCircle;
            prevRedCircle = redCircle;

            //Draw the returned circles
            redCircle.draw(raw, new Scalar(255,0,0));
            blueCircle.draw(raw, new Scalar(0,0,255));

            //Determine the location of each ball
            /*
            direction = redCircle.center.x < blueCircle.center.x ? "left" : redCircle.center.x > blueCircle.center.x ? "right" : "???????? blame crowforce";
            telemetry.addData("red location", direction);
            */
            direction = blueCircle.center.x < redCircle.center.x ? "left" : blueCircle.center.x > redCircle.center.x ? "right" : "???????? blame crowforce";
            robot.telemetry.addData("blue location", direction);
        }
        //If Opencv throws an exception, it does not print what it is, so you need a try-catch statement to recognize errors
        catch (Exception e) {
            robot.telemetry.addData("error", e.getMessage());
            e.printStackTrace();
        }

        robot.telemetry.update();

        //Resize the image so the camera can display it and return the result
        Mat returnImage = raw;
        Imgproc.resize(returnImage, returnImage, new Size(1280, 720));
        return returnImage;
    }

    public void startOpenCV(CameraBridgeViewBase.CvCameraViewListener2 cameraViewListener) {
        FtcRobotControllerActivity.turnOnCameraView.obtainMessage(1,cameraViewListener).sendToTarget();
    }

    public void stopOpenCV() {
        try {
            FtcRobotControllerActivity.turnOffCameraView.obtainMessage().sendToTarget();
        }
        catch(Exception e) {
            robot.telemetry.addData("error:", e.getMessage());
            robot.telemetry.addData("cause:", e.getCause());
            robot.telemetry.addData("stack trace:",e.getStackTrace());
            robot.telemetry.update();
        }
    }
}
