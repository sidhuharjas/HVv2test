package org.hivevisionftc.vision;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.skeletonarmy.marrow.weaver.PathResult;
import com.skeletonarmy.marrow.weaver.Weaver;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.hivevisionftc.vision.weaver.WeaverRoute;

/**
 * Tracks game elements with the Limelight and turns them into field positions and Marrow
 * routes.
 *
 * <pre>{@code
 * Navigator navigator = Constants.createNewNavigator(hardwareMap);
 * navigator.setPoseSource(() -> new FieldPose(
 *         follower.pose().x(), follower.pose().y(), follower.pose().heading()));
 *
 * while (opModeIsActive()) {
 *     follower.update();
 *     navigator.updateVision();
 *     if (navigator.didDetect()) {
 *         PathResult route = navigator.returnPath();
 *         if (route != null) follower.follow(PedroPathingConverter.toPath(route));
 *     }
 * }
 * }</pre>
 */
public class Navigator {
    private final HiveVision vision;
    private final VisionConfig config;

    private Supplier<FieldPose> poseSource;
    private int maxObjects;
    private double clearance = -1;
    private Sort sort = Sort.SHORTEST;
    private final List<Zone> zones = new ArrayList<>();
    private HiveVision.DetectionList latest;

    public Navigator(HardwareMap hardwareMap, VisionConfig config) {
        this(hardwareMap.get(Limelight3A.class, config.getHardwareName()), config);
    }

    public Navigator(Limelight3A limelight, VisionConfig config) {
        this(new HiveVision(limelight, config), config);
    }

    public Navigator(HiveVision vision, VisionConfig config) {
        this.vision = vision;
        this.config = config;
    }

    /**
     * Supplies the robot's field pose for {@link #updateVision()}. Wire once at init; the
     * supplier is re-read every call so it tracks the localizer.
     */
    public Navigator setPoseSource(Supplier<FieldPose> poseSource) {
        this.poseSource = poseSource;
        return this;
    }

    /** Reads the camera once and projects every accepted detection onto the field. */
    public Navigator updateVision() {
        if (poseSource == null) {
            throw new IllegalStateException(
                    "No pose source: call setPoseSource(...) once at init, "
                            + "or updateVision(pose) with a pose you already have");
        }
        return updateVision(poseSource.get());
    }

    /** Reads the camera once and projects detections using a pose you already hold. */
    public Navigator updateVision(FieldPose robotPose) {
        latest = vision.detections().fetch(robotPose);
        applySort(latest);
        return this;
    }

    /** Convenience overload of {@link #updateVision(FieldPose)}. */
    public Navigator updateVision(double robotX, double robotY, double robotHeading) {
        return updateVision(new FieldPose(robotX, robotY, robotHeading));
    }

    /** The accepted detection closest to the robot, or {@code null}. Distance-ordered regardless of {@link #sort}. */
    public Detection closest() {
        if (latest == null || !latest.didDetect()) return null;
        FieldPose robot = latest.getRobotPose();
        Detection best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Detection detection : latest.getDetections()) {
            double distance = Math.hypot(
                    detection.getX() - robot.getX(), detection.getY() - robot.getY());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = detection;
            }
        }
        return best;
    }

    /** Every accepted detection from the last {@code updateVision(...)}, in the configured sort order. */
    public List<Detection> objects() {
        if (latest == null) return Collections.emptyList();
        return Collections.unmodifiableList(latest.getDetections());
    }

    /** Whether the last {@code updateVision(...)} saw anything. */
    public boolean didDetect() {
        return latest != null && latest.didDetect();
    }

    /**
     * Plans a Marrow route through the current detections, starting at the pose of the last
     * {@code updateVision(...)}. Returns {@code null} when there is nothing to route to.
     */
    public PathResult returnPath() {
        if (latest == null) {
            throw new IllegalStateException("Call updateVision() before returnPath()");
        }
        applySort(latest);
        if (clearance >= 0) {
            Weaver.getConfig().clearance(clearance);
        }
        WeaverRoute route = WeaverRoute.fromFetchPose().maxTargets(maxObjects);
        if (!zones.isEmpty()) {
            route.avoid(new ArrayList<>(zones));
        }
        return route.apply(latest);
    }

    /**
     * Tracks only these model labels (case-insensitive), e.g. {@code track("red_nectar")}.
     * Label names, not enums, so next season you just paste the new model's labels. Call with
     * no arguments to track everything again.
     */
    public Navigator track(String... labels) {
        vision.track(labels);
        return this;
    }

    /**
     * Label names in class-id order (from the model's labels.txt). Fallback for {@link #track}
     * when the detector reports no class name on a detection.
     */
    public Navigator modelLabels(String... labels) {
        vision.modelLabels(labels);
        return this;
    }

    /** Visits at most this many detections when planning. {@code 0} or less means all of them. */
    public Navigator maxObjects(int maxObjects) {
        this.maxObjects = maxObjects;
        return this;
    }

    /** Inches the route keeps from obstacles. Omit to leave Marrow's current value alone. */
    public Navigator clearance(double inches) {
        this.clearance = inches;
        return this;
    }

    /** Sets the order detections are routed and listed in (see {@link Sort}). */
    public Navigator sort(Sort sort) {
        this.sort = sort;
        return this;
    }

    /** Adds obstacle zones the route must bend around. Add once at init; see {@link #clearZones()}. */
    public Navigator avoid(Zone... obstacleZones) {
        Collections.addAll(zones, obstacleZones);
        return this;
    }

    /** Removes every zone added by {@link #avoid(Zone...)}. */
    public Navigator clearZones() {
        zones.clear();
        return this;
    }

    /** The underlying vision session, for {@link HiveVision#track} and raw results. */
    public HiveVision vision() {
        return vision;
    }

    /** The detection list from the last {@code updateVision(...)}: raw {@link HiveVision.DetectionList} ops. */
    public HiveVision.DetectionList detections() {
        if (latest == null) {
            throw new IllegalStateException("Call updateVision() before detections()");
        }
        return latest;
    }

    public VisionConfig getConfig() {
        return config;
    }

    private void applySort(HiveVision.DetectionList list) {
        switch (sort) {
            case SHORTEST:
                list.sortByDistance(true);
                break;
            case LONGEST:
                list.sortByDistance(false);
                break;
            case SIZE:
                list.sortBySize();
                break;
            case CONFIDENCE:
                list.sortByConfidence();
                break;
            case COLOR:
                list.sortByColor();
                break;
            case NONE:
                break;
        }
    }
}
