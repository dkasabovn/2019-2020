package org.firstinspires.ftc.teamcode.Robot

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.teamcode.lib.Coords.Position
import org.firstinspires.ftc.teamcode.Robot.Meta.Constants
import org.firstinspires.ftc.teamcode.Robot.Sensors.IMU
import org.firstinspires.ftc.teamcode.lib.Constraints.TankKinematics
import org.firstinspires.ftc.teamcode.lib.Coords.Point
import org.firstinspires.ftc.teamcode.lib.Coords.State
import org.firstinspires.ftc.teamcode.lib.Extensions.plus
import org.firstinspires.ftc.teamcode.lib.Extensions.toVector
import org.firstinspires.ftc.teamcode.lib.Path.NPathFollower
import org.firstinspires.ftc.teamcode.lib.Path.PathFollower
import org.firstinspires.ftc.teamcode.lib.Path.Paths
import org.firstinspires.ftc.teamcode.lib.Path.TankController
import org.openftc.revextensions2.ExpansionHubEx
import org.openftc.revextensions2.ExpansionHubMotor
import org.openftc.revextensions2.RevBulkData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureTimeMillis

class NDriveTrain constructor(val Op : OpMode) {
    var position : Position = Position(0.0,0.0,0.0)
    var lastWheelPositions = emptyList<Double>()
    var time = 0.0

    val motorType : MotorConfigurationType = Constants.MOTOR_CONFIG

    val left : List<ExpansionHubMotor>
    val right : List<ExpansionHubMotor>
    val all : List<ExpansionHubMotor>

    val hubL : ExpansionHubEx = Op.hardwareMap.get(ExpansionHubEx::class.java, "hubLeft")
    val hubR : ExpansionHubEx = Op.hardwareMap.get(ExpansionHubEx::class.java, "hubRight")

    init {
        val fl : ExpansionHubMotor = Op.hardwareMap.get(ExpansionHubMotor::class.java, "fl")
        val ml : ExpansionHubMotor = Op.hardwareMap.get(ExpansionHubMotor::class.java, "ml")
        val bl : ExpansionHubMotor = Op.hardwareMap.get(ExpansionHubMotor::class.java, "bl")

        val fr : ExpansionHubMotor = Op.hardwareMap.get(ExpansionHubMotor::class.java, "fr")
        val mr : ExpansionHubMotor = Op.hardwareMap.get(ExpansionHubMotor::class.java, "mr")
        val br : ExpansionHubMotor = Op.hardwareMap.get(ExpansionHubMotor::class.java, "br")

        fr.direction = DcMotorSimple.Direction.REVERSE
        br.direction = DcMotorSimple.Direction.REVERSE
        mr.direction = DcMotorSimple.Direction.REVERSE

        left = listOf(fl,ml,bl)
        right = listOf(fr,mr,br)
        all = listOf(fl,fr,ml,mr,bl,br)

        all.forEach {
            it.mode = DcMotor.RunMode.RUN_USING_ENCODER
            it.setPIDFCoefficients(it.mode, PIDFCoefficients(35.0,0.0,30.0,0.0))
        }

        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE)
        resetEncoders()
    }

    fun update() {
        val encoderPose = getEncoderAverage()
        val heading = IMU.heading()
        if (lastWheelPositions.isNotEmpty()) {
            val delta = encoderPose.zip(lastWheelPositions).map { it.first - it.second }
            val (l, r) = delta
            val dist = (l + r) / 2.0
            val x = dist * cos(heading)
            val y = dist * sin(heading)
            position = Position(position.x + x, position.y + y, heading)
        }
        lastWheelPositions = encoderPose
    }

    fun Double.e2i() : Double = (Constants.WHEEL_RAD * 2.0 * PI * Constants.GEAR_RATIO * this) / 383.6

    fun Double.rpmToVel() : Double = (this * Constants.GEAR_RATIO * 2.0 * PI * Constants.WHEEL_RAD) / 60.0

    fun getMaxRpm() : Double {
        return motorType.maxRPM * motorType.achieveableMaxRPMFraction
    }

    fun getTps() : Double {
        return (motorType.maxRPM * motorType.ticksPerRev) / 60.0
    }

    fun getF() : Double {
        return 32767 / getTps()
    }

    fun getEncoderAverage() : List<Double> {
        var leftSum : Double = 0.0
        var rightSum : Double = 0.0

        val bulkL : RevBulkData = hubL.bulkInputData
        val bulkR : RevBulkData = hubR.bulkInputData

        left.forEach {
            leftSum += bulkL.getMotorCurrentPosition(it).toDouble().e2i()
        }
        right.forEach {
            rightSum += bulkR.getMotorCurrentPosition(it).toDouble().e2i()
        }

        return listOf(leftSum / 3.0, rightSum / 3.0)
    }

    fun getVelocityAverage() : List<Double> {
        var leftSum : Double = 0.0
        var rightSum : Double = 0.0

        val bulkL : RevBulkData = hubL.bulkInputData
        val bulkR : RevBulkData = hubR.bulkInputData

        left.forEach {
                leftSum += bulkL.getMotorVelocity(it).toDouble().e2i()
        }
        right.forEach {
                rightSum += bulkR.getMotorVelocity(it).toDouble().e2i()
        }

        return listOf(leftSum / 3.0, rightSum / 3.0)
    }

    fun getAllReadings() : List<Double> {
        val readings : MutableList<Double> = mutableListOf()
        val bulkL : RevBulkData = hubL.bulkInputData
        val bulkR : RevBulkData = hubR.bulkInputData
        left.forEach {
            readings.add(bulkL.getMotorCurrentPosition(it).toDouble().e2i())
        }
        right.forEach {
            readings.add(bulkR.getMotorCurrentPosition(it).toDouble().e2i())
        }
        return readings
    }

    fun resetEncoders() {
        all.forEach {
            it.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            it.mode = DcMotor.RunMode.RUN_USING_ENCODER
        }
    }

    fun reverse() {
        all.forEach {
            it.direction = if (it.direction == DcMotorSimple.Direction.REVERSE) {
                DcMotorSimple.Direction.FORWARD
            } else {
                DcMotorSimple.Direction.REVERSE
            }
        }
    }

    fun setZeroPowerBehavior(type : DcMotor.ZeroPowerBehavior) {
        all.forEach {
            it.zeroPowerBehavior = type
        }
    }

    fun setPower(p0 : Double, p1 : Double) {
        left.forEach {
            it.power = p0
        }
        right.forEach {
            it.power = p1
        }
    }

    fun turnPrimitive(power : Double, right : Boolean) {
        if (right) setPower(power, -power) else setPower(-power, power)
    }

    fun followPath(path : MutableList<State>) {
        val pid = PIDCoefficients(.01,0.0,.003)
        val nano = ElapsedTime()
        val follower = NPathFollower(path = path, op = Op, clock = nano)
        val leftC = TankController(pid, 1.0/120.0,.002, nano)
        val rightC = TankController(pid, 1.0/120.0, .002, nano)
        while (!follower.isDone && !Thread.interrupted()) {
                update()
                val (l,r) = getVelocityAverage()
                val (vld, vrd)= follower.getTargets(position)
                leftC.target = vld
                rightC.target = vrd
                val lp = leftC.calc(l)
                val rp = rightC.calc(r)
                setPower(lp,rp)
                Op.telemetry.addData("powers: ", lp)
                Op.telemetry.addData("powers: ", rp)
                Op.telemetry.addData("rloc: ", position)
                Op.telemetry.update()
        }
    }

    fun followPath(path : MutableList<State>, pidCoefficients: PIDCoefficients, kv: Double, ka: Double) {
        val pid = pidCoefficients
        val nano = ElapsedTime()
        val follower = NPathFollower(path = path, op = Op, clock = nano)
        val leftC = TankController(pid, kv,ka, nano)
        val rightC = TankController(pid, kv, ka, nano)
        val dash = FtcDashboard.getInstance()
        Op.telemetry = dash.telemetry
        while (!follower.isDone && !Thread.interrupted()) {
            update()
            val (l,r) = getVelocityAverage()
            val (vld, vrd)= follower.getTargets(position)
            leftC.target = vld
            rightC.target = vrd
            val lp = leftC.calc(l)
            val rp = rightC.calc(r)
            setPower(lp,rp)
            Op.telemetry.addData("Left Vel", vld)
            Op.telemetry.addData("Right Vel", vrd)
            Op.telemetry.addData("L Actual", l)
            Op.telemetry.addData("R Actual", r)
            Op.telemetry.addData("E Left", vld - l)
            Op.telemetry.addData("E Right", vrd - r)
            Op.telemetry.addData("Power Left", lp)
            Op.telemetry.addData("Power Right", rp)
            Op.telemetry.update()
        }
    }


}