package org.firstinspires.ftc.teamcode.OpMode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.PIDCoefficients;

import org.firstinspires.ftc.teamcode.Robot.NRobot;
import org.firstinspires.ftc.teamcode.lib.Coords.State;

@Autonomous(name = "tuner", group = "Auto")
public class PurePursuitPIDTuner extends LinearOpMode {
    public static double P = .01;
    public static double I = 0.0;
    public static double D = 0.0;
    public static double kV = 1/120.0;
    public static double kA = .003;
    @Override
    public void runOpMode() throws InterruptedException {
        NRobot r = new NRobot(this);
        waitForStart();
        r.followPath(
                r.builder()
                        .addStraight(new State(0.0,0.0))
                        .addStraight(new State(80.0,0.0))
                        .build()
        , new PIDCoefficients(P,I,D), kV, kA);
    }
}