package org.hivevisionftc.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.skeletonarmy.marrow.weaver.PathResult;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Desktop self-test for the Navigator API: built from a scripted {@link DetectionSource}. */
class NavigatorTest {
    private static final FieldPose START = new FieldPose(0, 0, 0);

    private static VisionConfig config() {
        return new VisionConfig()
                .camPitchDeg(25.0)
                .camHeight(9.2)
                .ballHeight(3.0)
                .camXOffset(4.0);
    }

    private static Navigator navigatorSeeing(Detection... detections) {
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
        return new Navigator(new HiveVision(source, config()), config());
    }

    /** A raw detection 20" ahead of the camera (see {@link ProjectionTest#tyFor}). */
    private static Detection ahead(double groundInches) {
        double depression = Math.toDegrees(Math.atan2(9.2 - 3.0, groundInches));
        return new Detection(1, "blue_nectar", 0, 25.0 - depression, 0.9, 0.1);
    }

    @Test
    void updateVisionClosestAndObjectsWorkTogether() {
        Navigator navigator = navigatorSeeing(ahead(40), ahead(20)).setPoseSource(() -> START);

        navigator.updateVision();

        assertTrue(navigator.didDetect());
        Detection closest = navigator.closest();
        assertNotNull(closest);
        assertEquals(24.0, closest.getX(), 1e-6, "closest() is the 20\" target plus mount offset");
        assertEquals(2, navigator.objects().size());
    }

    @Test
    void closestIsNullWhenNothingWasSeen() {
        Navigator navigator = navigatorSeeing().setPoseSource(() -> START);
        navigator.updateVision();

        assertNull(navigator.closest());
        assertTrue(navigator.objects().isEmpty());
    }

    @Test
    void returnPathPlansThroughTheDetections() {
        Navigator navigator = navigatorSeeing(ahead(20)).setPoseSource(() -> START);

        PathResult route = navigator.updateVision().returnPath();

        assertNotNull(route);
        assertEquals(1, route.getVisitedTargets().size());
    }

    @Test
    void returnPathBeforeUpdateVisionSaysSo() {
        Navigator navigator = navigatorSeeing(ahead(20));

        assertThrows(IllegalStateException.class, navigator::returnPath);
        assertThrows(IllegalStateException.class, navigator::detections);
    }

    @Test
    void updateVisionWithoutAPoseSourceSaysSo() {
        Navigator navigator = navigatorSeeing(ahead(20));

        assertThrows(IllegalStateException.class, navigator::updateVision);
    }

    @Test
    void anExplicitPoseBeatsTheMissingPoseSource() {
        Navigator navigator = navigatorSeeing(ahead(20));

        navigator.updateVision(new FieldPose(10, 10, Math.PI / 2));

        assertEquals(10.0, navigator.closest().getX(), 1e-6);
        assertEquals(34.0, navigator.closest().getY(), 1e-6);
    }

    @Test
    void fluentCallsStackAndRefineThePlan() {
        Navigator navigator = navigatorSeeing(ahead(20), ahead(40), ahead(60))
                .setPoseSource(() -> START)
                .track("blue_nectar")
                .modelLabels("yellow_pollen", "red_nectar", "blue_nectar")
                .maxObjects(2)
                .sort(Sort.SIZE);

        PathResult route = navigator.updateVision().maxObjects(2).returnPath();

        assertNotNull(route);
        assertEquals(2, route.getVisitedTargets().size(), "maxObjects caps the route");
        assertEquals(2, navigator.objects().size());
    }
}
