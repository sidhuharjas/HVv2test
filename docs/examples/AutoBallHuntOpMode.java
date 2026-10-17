package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.skeletonarmy.marrow.weaver.PedroPathingConverter;
import com.skeletonarmy.marrow.weaver.PathResult;
import org.hivevisionftc.vision.Detection;
import org.hivevisionftc.vision.FieldPose;
import org.hivevisionftc.vision.Navigator;

/**
 * Autonomous OpMode demonstrating target detection and Weaver route generation using HiveVision.
 */
@Autonomous(name = "HiveVision Navigator Auto")
public class AutoBallHuntOpMode extends LinearOpMode {

    @Override
    public void runOpMode() {
        Follower follower = SampleFollowerFactory.create(hardwareMap);
        Navigator navigator = Constants.createNewNavigator(hardwareMap);
        navigator.setPoseSource(() -> new FieldPose(
                follower.pose().x(),
                follower.pose().y(),
                follower.pose().heading()));

        telemetry.addData("Status", "Ready - press START");
        telemetry.update();
        waitForStart();

        if (opModeIsActive()) {
            navigator.updateVision();

            if (navigator.didDetect()) {
                Detection closest = navigator.closest();
                telemetry.addData("Closest Element", closest.getClassName());
                telemetry.addData("Field X", closest.getX());
                telemetry.addData("Field Y", closest.getY());

                PathResult route = navigator.returnPath();
                if (route != null) {
                    follower.follow(PedroPathingConverter.toPath(route));
                    while (opModeIsActive() && follower.isBusy()) {
                        follower.update();
                        telemetry.addData("Status", "Following generated route");
                        telemetry.update();
                    }
                }
            } else {
                telemetry.addData("Status", "No targets detected");
                telemetry.update();
            }
        }
    }
}
