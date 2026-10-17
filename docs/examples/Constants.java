package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import org.hivevisionftc.vision.Navigator;
import org.hivevisionftc.vision.Sort;
import org.hivevisionftc.vision.VisionConfig;

/**
 * The one file you edit per robot and per season. Copy it into TeamCode next to your OpModes,
 * measure the numbers for your mount, paste the new model's labels when the model changes,
 * then build every Navigator from here:
 *
 * <pre>{@code
 * Navigator navigator = Constants.createNewNavigator(hardwareMap);
 * }</pre>
 */
public final class Constants {

    private Constants() {}

    // ─── Hardware ─────────────────────────────────────────────────────────────────────────────

    public static String LIMELIGHT_NAME = "limelight"; // Configure Robot > Devices
    public static int PIPELINE = 3; // object detector pipeline (0-9)
    public static int POLL_RATE_HZ = 150;

    // ─── Camera mount (measure on the robot) ──────────────────────────────────────────────────

    public static double CAM_PITCH_DEG = 25.0; // tilt below horizontal; protractor on a level
    public static double CAM_HEIGHT = 9.2; // floor to the center of the lens, inches
    public static double BALL_HEIGHT = 3.0; // floor to the CENTER of the game element, inches
    public static double CAM_X_OFFSET = 4.0; // lens forward of robot center, inches
    public static double CAM_Y_OFFSET = 0.0; // lens left of robot center, inches (negative = right)

    // ─── Detection ────────────────────────────────────────────────────────────────────────────

    public static double MIN_CONFIDENCE = 0.44; // 0.0 to 1.0; ~0.44 filters false positives
    public static long MAX_STALENESS_MS = 120; // reject detection data older than this

    // Label names, not enums - next season, paste the new names from labels.txt.
    public static String[] TRACKED_CLASSES = {"red_nectar", "blue_nectar"}; // empty = accept all

    // Every label in class-id order (index 0 first), straight from the model's labels.txt.
    // Fallback used to identify detections when the detector reports no class name.
    public static String[] MODEL_LABELS = {"yellow_pollen", "red_nectar", "blue_nectar"};

    // ─── Pathing defaults ─────────────────────────────────────────────────────────────────────

    public static int MAX_OBJECTS = 2; // most detections one route visits (0 = all of them)
    public static double CLEARANCE = -1; // inches from obstacles; -1 leaves Marrow's default
    public static Sort SORT = Sort.SHORTEST;

    /** Builds a Navigator wired with everything above. Call once per OpMode. */
    public static Navigator createNewNavigator(HardwareMap hardwareMap) {
        VisionConfig config = new VisionConfig()
                .hardwareName(LIMELIGHT_NAME)
                .pipeline(PIPELINE)
                .pollingRateHz(POLL_RATE_HZ)
                .camPitchDeg(CAM_PITCH_DEG)
                .camHeight(CAM_HEIGHT)
                .ballHeight(BALL_HEIGHT)
                .camXOffset(CAM_X_OFFSET)
                .camYOffset(CAM_Y_OFFSET)
                .minConfidence(MIN_CONFIDENCE)
                .maxStalenessMs(MAX_STALENESS_MS);

        return new Navigator(hardwareMap, config)
                .track(TRACKED_CLASSES)
                .modelLabels(MODEL_LABELS)
                .maxObjects(MAX_OBJECTS)
                .clearance(CLEARANCE)
                .sort(SORT);
    }
}
