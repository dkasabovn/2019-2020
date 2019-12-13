package org.firstinspires.ftc.teamcode.OpMode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.HerculesLibraries.Vision.NewBitMap;
import org.firstinspires.ftc.teamcode.OpMode.AutoStates;
import org.firstinspires.ftc.teamcode.OpMode.Robot;
import org.firstinspires.ftc.teamcode.Robot.Bhorn;
import org.firstinspires.ftc.teamcode.Robot.DriveTrain;
import org.firstinspires.ftc.teamcode.Robot.FoundationHook;
import org.firstinspires.ftc.teamcode.Robot.Grabber;
import org.firstinspires.ftc.teamcode.Robot.Intake;
import org.firstinspires.ftc.teamcode.lib.Constraints.PIDFCoefficients;
import org.firstinspires.ftc.teamcode.lib.Constraints.TankKinematics;
import org.firstinspires.ftc.teamcode.lib.Coords.Position;
import org.firstinspires.ftc.teamcode.lib.Coords.Waypoint;
import org.firstinspires.ftc.teamcode.lib.Path.PathFollower;
import org.firstinspires.ftc.teamcode.lib.Path.Paths;

import java.util.List;

@Autonomous(name = "BlueAutoRed", group = "Auto")

public class BlueAuto extends LinearOpMode {
    private AutoStates state;
    private PathFollower pp;
    private ElapsedTime t;
    double lastT = 0.0;

    private String skyStonePosition = "C";


//    Handler dispatcher;


    double kP = 0.001;
    double kI = 0.0;
    double kD = 0.0;

    @Override
    public void runOpMode() throws InterruptedException {

        Robot.init(this);
//        state = AutoStates.VISION;
//        Robot.init(this);
        t = new ElapsedTime();
//        pp = new PathFollower(Paths.getStraightLineLeft(), 10.0, new TankKinematics(DriveTrain.width + 10.0), new PIDFCoefficients(.007,0.0,.0003,1/120.0,.0003),
//                5.0, this);
        t.reset();
//        dispatcher = new Handler(Looper.getMainLooper());
        Robot.reset();
        DriveTrain.initAuto(this);

        //vision = new TensorFlowDetection(this);

        while(!isStarted()){
            skyStonePosition = NewBitMap.blueVision();
            telemetry.addData("Skystone Position: ", skyStonePosition);
            telemetry.update();
        }



        waitForStart();
        Intake.power(-1.0);
        Thread.sleep(500);
        Intake.power(0.0);
        Thread.sleep(500);
        Intake.power(1.0);
        Thread.sleep(500);
        Intake.power(0.0);
        t.reset();


        //set path to collect stone
        if (skyStonePosition.equals("L")){
            pp = new PathFollower(Paths.getLeftBlue(), 10.0, new TankKinematics(DriveTrain.width + 5.0), new PIDFCoefficients(.007,0.0,.0003,1/120.0,.0003),
                    5.0, this);
        } else if (skyStonePosition.equals("C")){
            pp = new PathFollower(Paths.getStraightBlue(), 10.0, new TankKinematics(DriveTrain.width + 5.0), new PIDFCoefficients(.007,0.0,.0003,1/120.0,.0003),
                    5.0, this);
        } else if (skyStonePosition.equals("R")){
            pp = new PathFollower(Paths.getRightBlue(), 10.0, new TankKinematics(DriveTrain.width + 5.0), new PIDFCoefficients(.007,0.0,.0003,1/120.0,.0003),
                    5.0, this);
        }

        //run the path
        while (!pp.isDone() && opModeIsActive()) {
            lastT = t.seconds() - lastT;
            Waypoint rloc = Robot.PurePursuit(lastT);
            List<Double> powers = pp.followPath(new Position(rloc.getX(), rloc.getY(), rloc.getDdx()), rloc.getDx(), rloc.getDy(), lastT);
            DriveTrain.setPower(powers.get(0), powers.get(1));
        }

        Bhorn.down();
        Thread.sleep(600);
        DriveTrain.setPower(-.25,-.25);
        if(skyStonePosition.equals("R")) {
            Thread.sleep(850);
        } else {
            Thread.sleep(750);
        }
        DriveTrain.setPower(.2, .2);
        Intake.powerAsync(1.0, 1000);
        Thread.sleep(500);
        Bhorn.up();
        DriveTrain.stopMotors();
        Thread.sleep(1000);
        Intake.stopIntake();
        //turn to face towards the build plate
        DriveTrain.turnPID(.27, false, (( Math.PI)/2.0));
        Intake.powerAsync(-0.75, 300);


        //get path to get to foundation
        if (skyStonePosition.equals("R")) {
            pp.reset(Paths.getBlueStraight(), false);
        } else if (skyStonePosition.equals("C")) {
            pp.reset(Paths.getBlueStraight(), false);
        } else {
            pp.reset(Paths.getBlueStraight(), false);
        }

        //travel to foundation

        while (!pp.isDone() && opModeIsActive()) {
            lastT = t.seconds() - lastT;
            Waypoint rloc = Robot.PurePursuit(lastT);
            List<Double> powers = pp.followPath(new Position(rloc.getX(), rloc.getY(), rloc.getDdx()), rloc.getDx(), rloc.getDy(), lastT);
            DriveTrain.setPower(powers.get(0), powers.get(1));
        }


        //prep manip machine to almost be pushing the block out
        Robot.manipMachine();

        //turn orthogonal to the build plate
        Grabber.setPosition(Grabber.POSITIONS.PUSHTHROUGH);
        DriveTrain.turnPID(.27,false,((Math.PI)/2.0));
        Grabber.setPosition(Grabber.POSITIONS.CLAMPDOWN);
        //back up to build plate
        DriveTrain.setPower(-.3,-.3);
        Thread.sleep(400);
        DriveTrain.stopMotors();
        Robot.manipMachine();
        Thread.sleep(500);

        //deposit skystone
        FoundationHook.toggle();

        Thread.sleep(500);
        Robot.manipMachine();
        Thread.sleep(1000);
        //drop the skystone
        Grabber.setPosition(Grabber.POSITIONS.DROP);
        Thread.sleep(800);

        //bring arm back in
        //toggle foundation hooks

        Robot.reset();
        DriveTrain.turnPID(.5, false, (Math.PI)/4.0, 500);
        DriveTrain.setPower(.4,.4);
        Thread.sleep(300);
        DriveTrain.stopMotors();
        DriveTrain.turnPID(.5, false, (Math.PI)/2.0, 2000);
        FoundationHook.toggle();
        Thread.sleep(300);
        DriveTrain.setPower(-.3,-.3);
        Thread.sleep(450);
        DriveTrain.stopMotors();



        pp.reset(Paths.getParkBlue(), false);

        while (!pp.isDone() && opModeIsActive()) {
            lastT = t.seconds() - lastT;
            Waypoint rloc = Robot.PurePursuit(lastT);
            List<Double> powers = pp.followPath(new Position(rloc.getX(), rloc.getY(), rloc.getDdx()), rloc.getDx(), rloc.getDy(), lastT);
            DriveTrain.setPower(powers.get(0), powers.get(1));
        }
    }
}