package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

/**
 * Created by Sage Creek Level Up on 10/22/2017.
 */
@TeleOp(name = "1ManDrive", group = "OpMode")
public class MecanumDrive extends OpMode{
    private DcMotor motor1, motor2, motor3, motor4; //motors 1 & 3 are left motors

    private DcMotor motor5, motor6; //glyph motors

    private DcMotor motor7, motor8;

    private CRServo leftcr, rightcr;

    private Servo servo1, servo2, servo3;       //servo1 is gate, servo2 is putin, servo3 is hammer

    //private Servo glyphservo;

    @Override
    public void init(){
        motor1 = hardwareMap.dcMotor.get("motor1");
        motor1.setDirection(DcMotor.Direction.FORWARD);
        motor2 = hardwareMap.dcMotor.get("motor2");
        motor2.setDirection(DcMotor.Direction.REVERSE);

        motor3 = hardwareMap.dcMotor.get("motor3");
        motor3.setDirection(DcMotor.Direction.FORWARD);
        motor4 = hardwareMap.dcMotor.get("motor4");
        motor4.setDirection(DcMotor.Direction.REVERSE);

        motor5 = hardwareMap.dcMotor.get("motor5");
        motor6 = hardwareMap.dcMotor.get("motor6");
        motor7 = hardwareMap.dcMotor.get("motor7");
        motor8 = hardwareMap.dcMotor.get("motor8");

        motor5.setDirection(DcMotor.Direction.FORWARD);
        motor6.setDirection(DcMotor.Direction.REVERSE);
        motor7.setDirection(DcMotor.Direction.FORWARD);
        motor8.setDirection(DcMotor.Direction.FORWARD);

        leftcr = hardwareMap.crservo.get("leftcr");     //leftcr and rightcr are flipped
        rightcr = hardwareMap.crservo.get("rightcr");

        leftcr.setDirection(CRServo.Direction.REVERSE);
        rightcr.setDirection(CRServo.Direction.FORWARD);

        servo1 = hardwareMap.servo.get("greenCard");
        servo2 = hardwareMap.servo.get("putin");
        servo3 = hardwareMap.servo.get("hammer");


    }

    @Override
    public void init_loop(){

    }

    @Override
    public void start(){
        servo1.setPosition(1);
        servo2.setPosition(0);
        servo3.setPosition(0.5);
    }

    @Override
    public void loop(){
        double x = gamepad1.left_stick_x;
        double y = gamepad1.left_stick_y;
        double c = gamepad1.left_trigger-gamepad1.right_trigger;
        motor1.setPower(Range.clip(y-x+c, -1, 1));
        motor2.setPower(Range.clip(y+x-c, -1, 1));
        motor3.setPower(Range.clip(y+x+c, -1, 1));
        motor4.setPower(Range.clip(y-x-c, -1, 1));

        if(gamepad1.b) {
            motor5.setPower(gamepad1.right_stick_y);
            motor6.setPower(gamepad1.right_stick_y);
            leftcr.setPower(gamepad1.right_stick_y+gamepad1.right_stick_x);
            rightcr.setPower(gamepad1.right_stick_y-gamepad1.right_stick_x);
        }else {
            motor5.setPower(gamepad1.right_stick_y*0.7);
            motor6.setPower(gamepad1.right_stick_y*0.7);
            leftcr.setPower(gamepad1.right_stick_y+gamepad1.right_stick_x/2);
            rightcr.setPower(gamepad1.right_stick_y-gamepad1.right_stick_x/2);
        }

        if(gamepad1.left_bumper){
            servo3.setPosition(0.1);
        }else{
            servo3.setPosition(0.4);
        }

        if(gamepad1.right_bumper){
            servo1.setPosition(0.4);
        }else{
            servo1.setPosition(1);
        }

        if(gamepad1.x){
            motor8.setPower(-1);
        }else if(gamepad1.y){
            motor8.setPower(1);
        }else{
            motor8.setPower(0);
        }

        if(gamepad1.a) {
            motor7.setPower(1);
        }else{
            motor7.setPower(0);
        }

    }

    @Override
    public void stop(){

    }
}
