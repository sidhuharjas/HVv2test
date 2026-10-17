package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.skeletonarmy.marrow.weaver.PedroPathingConverter;
import com.skeletonarmy.marrow.weaver.PathResult;
import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import org.hivevisionftc.vision.Detection;
import org.hivevisionftc.vision.FieldPose;
import org.hivevisionftc.vision.Navigator;

/**
 * Autonomous OpMode demonstrating continuous detection loop, obstacle avoidance,
 * and path execution using HiveVision and WeaverRoute.
 */
@Autonomous(name = "HiveVision Loop with Obstacles")
public class ManualBallChaseOpMode extends LinearOpMode {

    @Override
    public void runOpMode() {
        Follower follower = SampleFollowerFactory.create(hardwareMap);
        Navigator navigator = Constants.createNewNavigator(hardwareMap);
        navigator.setPoseSource(() -> new FieldPose(
                follower.pose().x(),
                follower.pose().y(),
                follower.pose().heading()));

        // Field furniture is fixed, so add zones once here; clearZones() would start over.
        navigator.avoid(new CircleZone(new Point(72, 72), 18.0));

        telemetry.addData("Status", "Ready - press START");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            follower.update();
            navigator.updateVision();

            if (navigator.didDetect()) {
                Detection closest = navigator.closest();
                telemetry.addData("Closest Element", closest.getClassName());
                telemetry.addData("Field X", closest.getX());
                telemetry.addData("Field Y", closest.getY());

                if (!follower.isBusy()) {
                    PathResult route = navigator
                            .maxObjects(3)
                            .clearance(6.0)
                            .returnPath();

                    if (route != null) {
                        follower.follow(PedroPathingConverter.toPath(route));
                    }
                }
            } else {
                telemetry.addData("Status", "Searching for targets...");
            }

            telemetry.update();
        }
    }
}
