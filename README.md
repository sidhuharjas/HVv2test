# HiveVision

Camera-based detection, target filtering, field positioning, and Weaver path routing library for FTC robots.

`HiveVision` turns Limelight 3A neural network detections into absolute field coordinates and generates path routes for autonomous navigation. It is built for FTC game elements using Limelight 3A SSD detectors and integrates with [Pedro Pathing](https://github.com/pedropathing/core) and [Marrow Weaver](https://marrow.skeletonarmyftc.com/).

**Docs & Maven repository:** [sidhuharjas.github.io/hvtest](https://sidhuharjas.github.io/hvtest)

---

## Installation

A single artifact: vision, field projection, and route planning all ship in `core`.
Artifacts are hosted on GitHub Pages — no Maven Central account or extra mirrors needed.

Add the repository and dependency to your `TeamCode/build.gradle`:

```gradle
repositories {
    maven { url = uri("https://sidhuharjas.github.io/hvtest") }
}

dependencies {
    implementation 'org.hivevisionftc:core:2.0.0'
}
```

> **Upgrading from 1.x:** delete the `org.hivevisionftc:weaver` line — route planning now
> lives inside `core`. Keeping both on the classpath causes duplicate classes.

Step-by-step setup, calibration, and usage: [sidhuharjas.github.io/hvtest](https://sidhuharjas.github.io/hvtest)

---

## Quickstart

All the per-robot numbers live in one editable file: [`Constants.java`](docs/examples/Constants.java).
Copy it into TeamCode, measure your camera mount, paste your model's `labels.txt` names, and
build every Navigator from it:

```java
Navigator navigator = Constants.createNewNavigator(hardwareMap);
navigator.setPoseSource(() -> new FieldPose(
        follower.pose().x(), follower.pose().y(), follower.pose().heading()));
```

### 1. The basic loop

```java
while (opModeIsActive()) {
    follower.update();
    navigator.updateVision();                  // read camera, project onto the field

    if (navigator.didDetect()) {
        Detection ball = navigator.closest();  // nearest element, or null
        telemetry.addData("Element", ball.getClassName());

        PathResult route = navigator.returnPath();   // obstacle-avoiding Weaver route
        if (route != null && !follower.isBusy()) {
            follower.follow(PedroPathingConverter.toPath(route));
        }
    }
    telemetry.update();
}
```

### 2. Advanced: refine the plan (all optional, all fluent)

```java
navigator.updateVision()
        .track("red_nectar", "blue_nectar")    // model labels, not enums
        .maxObjects(2)                         // visit at most 2 elements
        .clearance(6.0)                        // inches kept from obstacles
        .sort(Sort.SIZE)                       // SHORTEST, LONGEST, SIZE, CONFIDENCE, COLOR, NONE
        .avoid(new CircleZone(new Point(72, 72), 18.0));  // zones persist; clearZones() resets

PathResult route = navigator.returnPath();
```

### 3. The 1.x API, still available

Everything from 1.x is still there: `navigator.vision()` returns the
`HiveVision` session (`setAllowedClasses`, `track`, raw results) and `navigator.detections()`
returns the `DetectionList` (`clampYDetections`, `getBiggest`, `with(WeaverRoute...)`).

---

## Running the Examples

Three ready-to-run files live in [`docs/examples/`](docs/examples/):

| File | What it does |
|------|--------------|
| `Constants.java` | The one file you edit: camera mount, labels, pathing defaults. |
| `AutoBallHuntOpMode` | One-shot autonomous: detect, build a Weaver route through the closest 2 targets, drive it. |
| `ManualBallChaseOpMode` | Detection loop for the whole match; re-plans around an obstacle when the robot is idle. |

To run one:

1. Copy that OpMode plus `Constants.java` and `SampleFollowerFactory.java` into your
   TeamCode module (all already declare `package org.firstinspires.ftc.teamcode`).
2. Edit `Constants.java` for your camera mount, labels, and hardware name.
3. Build and deploy TeamCode to the robot as usual.
4. On the Driver Station, select the OpMode, power on the Limelight, and press start.

Telemetry reports the closest element label, field X/Y, and route status. If it says
"No targets detected", check the Limelight pipeline index and that the game elements are in view.

---

## Calibration

Accurate distance estimation and field projection require proper physical measurement of your camera mounting geometry. Refer to [docs/calibration.md](docs/calibration.md) for step-by-step measurement procedures and coordinate system conventions.

| Constant (in `Constants.java`) | Description |
|--------------------------------|-------------|
| `CAM_PITCH_DEG` | Downward tilt angle of the camera optic axis in degrees below horizontal. |
| `CAM_HEIGHT` | Distance from floor to center of camera lens (inches). |
| `BALL_HEIGHT` | Distance from floor to center of target game element (inches) — half its diameter if it sits on the floor. |
| `CAM_X_OFFSET` | Forward (+X) offset of camera lens relative to robot center (inches). |
| `CAM_Y_OFFSET` | Left (+Y) offset of camera lens relative to robot center (inches, negative for right). |
| `MIN_CONFIDENCE` | Confidence threshold for accepting detections (0.0 to 1.0). |
| `MAX_STALENESS_MS` | Maximum time in milliseconds before detection data is discarded. |
| `TRACKED_CLASSES` / `MODEL_LABELS` | Model label names to chase, and the full labels.txt list in class-id order. |

---

## Documentation & Examples

- **[sidhuharjas.github.io/hvtest](https://sidhuharjas.github.io/hvtest)** — Setup guide, quickstart, and calibration summary.
- **[MODULES.md](docs/MODULES.md)** — Complete API and class reference.
- **[calibration.md](docs/calibration.md)** — Procedure for measuring camera offsets, pitch angles, and understanding coordinate frames.
- **[docs/examples/](docs/examples/)** — `Constants.java`, sample OpModes, and `SampleFollowerFactory` — see [Running the Examples](#running-the-examples).

---

## Building and Testing

```bash
# Run unit test suites (JVM-based, no camera required)
./gradlew test

# Format code according to Spotless Java rules
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Build release library archives (AARs, sources, javadocs)
./gradlew assembleRelease
```

---

## Publishing

Releases are published to the GitHub Pages Maven repository at
[sidhuharjas.github.io/hvtest](https://sidhuharjas.github.io/hvtest) by the
[`Publish to Pages Maven repo`](.github/workflows/pages-maven.yml) workflow.
It builds the AARs, pushes them to the `gh-pages` branch, and the docs site
along with them.

To cut a release:

1. Bump `version` in `build.gradle.kts`.
2. Push a `v*` tag (`git tag v2.0.0 && git push --tags`), or run the workflow
   manually from the Actions tab.

Local testing uses the Deployer plugin configured in `build.gradle.kts`:

| Gradle Task | Description |
|-------------|-------------|
| `./gradlew deployLocal` | Publishes artifacts to Maven Local (`~/.m2`) for testing. |
| `./gradlew deployNexusSnapshot` | Publishes snapshot builds to Sonatype Nexus. |
| `./gradlew deployCentralPortal` | Publishes release builds to Maven Central. |

---

## License

MIT License. See [LICENSE](LICENSE) for details.
