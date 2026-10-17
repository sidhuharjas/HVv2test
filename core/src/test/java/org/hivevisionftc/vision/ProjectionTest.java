package org.hivevisionftc.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Desktop self-test for camera-to-field projection: no robot, no camera, no Android.
 *
 * <p>The scenarios are built from the geometry backwards (choose a ground distance, derive the
 * ty that would see it) so they pin down the sign conventions: Limelight ty is positive up,
 * camPitchDeg is tilt below horizontal, field y is positive left, tx positive right.
 */
class ProjectionTest {
    private static final double PITCH = 25.0;
    private static final double CAM_HEIGHT = 9.2;
    private static final double BALL_HEIGHT = 3.0;
    private static final double CAM_X_OFFSET = 4.0;

    /** The ty a camera pitched {@link #PITCH} degrees down reports for a target {@code groundInches} ahead. */
    private static double tyFor(double groundInches) {
        double depression = Math.toDegrees(Math.atan2(CAM_HEIGHT - BALL_HEIGHT, groundInches));
        return PITCH - depression;
    }

    private static VisionConfig config() {
        return new VisionConfig()
                .camPitchDeg(PITCH)
                .camHeight(CAM_HEIGHT)
                .ballHeight(BALL_HEIGHT)
                .camXOffset(CAM_X_OFFSET)
                .camYOffset(0.0);
    }

    private static Detection seeing(double tx, double ty) {
        return new Detection(1, "blue_nectar", tx, ty, 0.9, 0.1);
    }

    private static HiveVision visionSeeing(Detection... detections) {
        final List<Detection> list = Arrays.asList(detections);
        DetectionSource source = new DetectionSource() {
            @Override
            public List<Detection> latest() {
                return list;
            }

            @Override
            public long stalenessMs() {
                return 0;
            }
        };
        return new HiveVision(source, config());
    }

    @Test
    void aTargetStraightAheadLandsOnTheFieldAxis() {
        HiveVision vision = visionSeeing(seeing(0, tyFor(20)));

        List<Detection> found =
                vision.detections().fetch(new FieldPose(0, 0, 0)).getDetections();

        assertEquals(1, found.size());
        // 20" of ground from the camera plus the 4" camera mount offset ahead of the robot center.
        assertEquals(24.0, found.get(0).getX(), 1e-6);
        assertEquals(0.0, found.get(0).getY(), 1e-6);
    }

    @Test
    void aTargetToTheRightLandsAtNegativeFieldY() {
        // Field y is positive left, so a target right of the camera (tx positive) must come out negative.
        HiveVision vision = visionSeeing(seeing(30, tyFor(20)));

        List<Detection> found =
                vision.detections().fetch(new FieldPose(0, 0, 0)).getDetections();

        assertEquals(1, found.size());
        assertEquals(
                20 * Math.cos(Math.toRadians(30)) + CAM_X_OFFSET, found.get(0).getX(), 1e-6);
        assertEquals(-20 * Math.sin(Math.toRadians(30)), found.get(0).getY(), 1e-6);
    }

    @Test
    void theRobotHeadingRotatesTheResultIntoFieldFrame() {
        HiveVision vision = visionSeeing(seeing(0, tyFor(20)));

        List<Detection> found =
                vision.detections().fetch(new FieldPose(10, 10, Math.PI / 2)).getDetections();

        assertEquals(1, found.size());
        // Robot faces +y at heading 90 degrees: the target sits 24" along +y from (10, 10).
        assertEquals(10.0, found.get(0).getX(), 1e-6);
        assertEquals(34.0, found.get(0).getY(), 1e-6);
    }

    @Test
    void aTargetOnOrAboveTheHorizonIsDropped() {
        // ty at or above the pitch means the ray never meets the ball's plane.
        HiveVision above = visionSeeing(seeing(0, PITCH + 15));
        assertTrue(
                above.detections().fetch(new FieldPose(0, 0, 0)).getDetections().isEmpty());

        HiveVision atHorizon = visionSeeing(seeing(0, PITCH));
        assertTrue(atHorizon
                .detections()
                .fetch(new FieldPose(0, 0, 0))
                .getDetections()
                .isEmpty());
    }

    @Test
    void trackKeepsOnlyTheNamedLabels() {
        HiveVision vision = visionSeeing(
                new Detection(1, "red_nectar", 0, tyFor(20), 0.9, 0.1),
                new Detection(2, "blue_nectar", 0, tyFor(30), 0.9, 0.1));
        vision.track("blue_nectar");

        List<Detection> found =
                vision.detections().fetch(new FieldPose(0, 0, 0)).getDetections();

        assertEquals(1, found.size());
        assertEquals("blue_nectar", found.get(0).getClassName());
    }

    @Test
    void trackFallsBackToModelLabelsWhenTheDetectorReportsNoName() {
        HiveVision vision = visionSeeing(
                new Detection(0, null, 0, tyFor(20), 0.9, 0.1), new Detection(1, null, 0, tyFor(30), 0.9, 0.1));
        vision.modelLabels("yellow_pollen", "red_nectar", "blue_nectar").track("red_nectar");

        List<Detection> found =
                vision.detections().fetch(new FieldPose(0, 0, 0)).getDetections();

        assertEquals(1, found.size());
        assertEquals(1, found.get(0).getClassId());
    }

    @Test
    void trackingIsCaseInsensitiveAndClearable() {
        HiveVision vision = visionSeeing(new Detection(1, "Red_Nectar", 0, tyFor(20), 0.9, 0.1));
        vision.track("red_nectar");
        assertEquals(1, vision.detections().fetch(new FieldPose(0, 0, 0)).count());

        vision.track(); // back to everything
        assertEquals(1, vision.detections().fetch(new FieldPose(0, 0, 0)).count());
    }

    @Test
    void sortByDistanceCanOrderFarthestFirst() {
        HiveVision vision = visionSeeing(seeing(0, tyFor(20)), seeing(0, tyFor(40)));
        HiveVision.DetectionList list = vision.detections().fetch(new FieldPose(0, 0, 0));

        List<Detection> farthestFirst = list.sortByDistance(false).getDetections();
        assertEquals(44.0, farthestFirst.get(0).getX(), 1e-6, "farthest first");
        assertEquals(24.0, farthestFirst.get(1).getX(), 1e-6);

        List<Detection> closestFirst = list.sortByDistance(true).getDetections();
        assertEquals(24.0, closestFirst.get(0).getX(), 1e-6, "closest first");
    }

    @Test
    void lowConfidenceAndStaleDetectionsAreFiltered() {
        List<Detection> raw = new ArrayList<>();
        raw.add(new Detection(1, "blue_nectar", 0, tyFor(20), 0.2, 0.1)); // below min confidence
        DetectionSource source = new DetectionSource() {
            @Override
            public List<Detection> latest() {
                return raw;
            }

            @Override
            public long stalenessMs() {
                return 0;
            }
        };
        HiveVision vision = new HiveVision(source, config());
        assertTrue(vision.detections()
                .fetch(new FieldPose(0, 0, 0))
                .getDetections()
                .isEmpty());

        raw.clear();
        raw.add(new Detection(1, "blue_nectar", 0, tyFor(20), 0.9, 0.1));
        assertEquals(1, vision.detections().fetch(new FieldPose(0, 0, 0)).count());
    }
}
