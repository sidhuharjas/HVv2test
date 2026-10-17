# Module Reference — HiveVision

This document provides an API and architecture reference for the `HiveVision` library.

---

## Overview

Since 2.0.0 `HiveVision` ships as a single artifact with two layers:

- **`org.hivevisionftc.vision`**: The `Navigator` (the entry point most OpModes use), plus the perception pipeline beneath it — Limelight 3A abstraction, configuration, detection filtering, sorting, and camera-to-field projection.
- **`org.hivevisionftc.vision.weaver`**: [Marrow Weaver](https://marrow.skeletonarmyftc.com/) route planning (`WeaverRoute`), folded into `core` — `Navigator.returnPath()` wraps it.

---

## Navigator (`org.hivevisionftc.vision.Navigator`)

The one object an OpMode talks to.

#### Constructors
```java
public Navigator(HardwareMap hardwareMap, VisionConfig config);  // opens the camera itself
public Navigator(Limelight3A limelight, VisionConfig config);
public Navigator(HiveVision vision, VisionConfig config);        // custom source / shared session
```

The `HardwareMap` overload looks the camera up under `config.getHardwareName()` (default `"limelight"`).

#### Pose wiring (once, at init)
| Method | Description |
|--------|-------------|
| `Navigator setPoseSource(Supplier<FieldPose> source)` | Supplies the robot's field pose for no-arg `updateVision()`. |

#### Basic tier (call every loop)
| Method | Description |
|--------|-------------|
| `Navigator updateVision()` | Reads the camera and projects detections using the pose source. |
| `Navigator updateVision(FieldPose pose)` | Same, with a pose you already hold (overloads for x/y/heading). |
| `Detection closest()` | Nearest detection to the robot, or `null`. Always distance-ordered. |
| `List<Detection> objects()` | All accepted detections in the configured `Sort` order (empty if none). |
| `boolean didDetect()` | Whether the last update saw anything. |
| `PathResult returnPath()` | Plans a Marrow route from the update pose; `null` when there is nothing to route to. |

#### Advanced tier (fluent, applied by the next `returnPath()`)
| Method | Description |
|--------|-------------|
| `Navigator track(String... labels)` | Chase only these model labels (case-insensitive); no args = everything. |
| `Navigator modelLabels(String... labels)` | Full labels.txt in class-id order; fallback when the detector reports no name. |
| `Navigator maxObjects(int n)` | Visit at most `n` detections (`0` or less = all). |
| `Navigator clearance(double inches)` | Marrow clearance from obstacles (omit to keep Marrow's value). |
| `Navigator sort(Sort sort)` | Ordering for routing/listing — see `Sort`. |
| `Navigator avoid(Zone... zones)` | Add obstacle zones (persist across loops). |
| `Navigator clearZones()` | Remove all zones. |

#### Underlying API
| Method | Description |
|--------|-------------|
| `HiveVision vision()` | The underlying session (`setAllowedClasses`, `track`, raw results). |
| `HiveVision.DetectionList detections()` | The list from the last update (`clampYDetections`, `getBiggest`, `with(...)`). |
| `VisionConfig getConfig()` | The configuration this navigator was built with. |

#### Basic loop
```java
Navigator navigator = Constants.createNewNavigator(hardwareMap);
navigator.setPoseSource(() -> new FieldPose(follower.pose().x(), follower.pose().y(), follower.pose().heading()));

while (opModeIsActive()) {
    follower.update();
    navigator.updateVision();
    if (navigator.didDetect()) {
        PathResult route = navigator.returnPath();
        if (route != null && !follower.isBusy()) follower.follow(PedroPathingConverter.toPath(route));
    }
}
```

---

### `Sort`

Order used by `Navigator.sort(...)` (class ids, not season-specific names):

- **`SHORTEST`** — nearest first (default).
- **`LONGEST`** — farthest first.
- **`SIZE`** — largest in the image first.
- **`CONFIDENCE`** — highest detector confidence first.
- **`COLOR`** — grouped by class id ascending.
- **`NONE`** — camera order.

---

### `HiveVision`

The session beneath `Navigator`, reachable through `navigator.vision()`.

#### Constructors
```java
public HiveVision(DetectionSource source, VisionConfig config);
public HiveVision(Limelight3A limelight, VisionConfig config);
```

#### Methods

| Method                                                    | Description                                                                                 |
|-----------------------------------------------------------|---------------------------------------------------------------------------------------------|
| `HiveVision track(String... labels)`                      | Chase only these model labels (case-insensitive); no args = everything.                     |
| `HiveVision modelLabels(String... labels)`                | Full labels.txt in class-id order; fallback when detections carry no class name.            |
| `HiveVision setAllowedClasses(BiobuzzClasses... classes)` | Legacy enum filter (overwrites current allowed classes). ANDs with `track(...)`.             |
| `HiveVision addAllowedClass(BiobuzzClasses clazz)`        | Adds a specific class to the allowed class set.                                             |
| `boolean isAllowed(BiobuzzClasses clazz)`                 | Checks if a class is currently allowed.                                                     |
| `Set<BiobuzzClasses> getAllowed()`                        | Returns the active set of allowed classes.                                                  |
| `List<Detection> getRawDetections()`                      | Retrieves the raw detection list directly from the underlying detection source.             |
| `long getStalenessMs()`                                   | Returns data staleness in milliseconds from the detection source.                           |
| `DetectionList detections()`                              | Creates a new `DetectionList` bound to this vision instance for fetching and processing.    |

---

### `HiveVision.DetectionList`

A fluent builder and query container for fetching, filtering, sorting, and transforming detection results into absolute field coordinates.

#### Fetching & Projection
```java
public DetectionList fetch(double robotX, double robotY);
public DetectionList fetch(FieldPose robotPose);
```
Queries the `DetectionSource` for the latest detections, filters them by allowed classes, tracked labels (`track(...)`), minimum confidence threshold (`minConfidence`), and data staleness (`maxStalenessMs`), then projects camera-relative angles (`tx`, `ty`) into field coordinates (`x`, `y` in inches) based on camera mount geometry and the given robot pose. Detections whose ray never meets the element's plane (at or above the horizon) are dropped.

Coordinate conventions: `tx` positive right, `ty` positive up, field `y` positive left, heading counterclockwise.

#### Sorting Methods
- **`sortByDistance()` / `sortByDistance(boolean closestFirst)`**: Sorts by straight-line distance from the robot pose (closest first, or farthest first with `false`).
- **`sortByColor()`**: Sorts detections by class ID ordinal ascending.
- **`sortBySize()`**: Sorts detections by target area descending (largest first).
- **`sortByConfidence()`**: Sorts detections by confidence score descending.

#### Filtering Methods
- **`clampYDetections(double minY, double maxY)`**: Removes detections whose field Y-coordinate falls outside `[minY, maxY]`.

#### Terminal & Query Methods
- **`Detection getClosest()`**: Returns the closest detection relative to robot pose, or `null` if empty.
- **`Detection getBiggest()`**: Returns the detection with the largest bounding area, or `null` if empty.
- **`List<Detection> getDetections()`**: Returns a copy of the list of projected field detections.
- **`int count()`**: Returns the number of valid detections in the list.
- **`boolean didDetect()`**: Returns `true` if one or more valid detections exist.
- **`FieldPose getRobotPose()`**: Returns the robot pose used during the last `fetch(...)` call.

#### Extension Point
```java
public <R> R with(Function<? super DetectionList, ? extends R> fn);
```
Passes this `DetectionList` instance into the provided function and returns its result — the hook `WeaverRoute` plugs into.

Example:
```java
PathResult path = vision.detections()
        .fetch(robotPose)
        .sortByDistance()
        .with(WeaverRoute.fromFetchPose());
```

---

### `VisionConfig`

A fluent builder class that holds the hardware device name, camera mounting geometry parameters, detection threshold filters, and hardware polling options.

```java
VisionConfig config = new VisionConfig()
        .hardwareName("limelight")   // Device name in the robot configuration
        .camPitchDeg(25.0)      // Downward tilt angle below horizontal (degrees)
        .camHeight(9.2)         // Lens height above floor (inches)
        .ballHeight(3.0)        // Game element center height above floor (inches)
        .camXOffset(4.0)        // Forward offset from robot center (inches)
        .camYOffset(0.0)        // Left offset from robot center (inches, negative for right)
        .maxStalenessMs(120)    // Max allowed detection staleness (milliseconds)
        .minConfidence(0.44)    // Minimum confidence score threshold (0.0 - 1.0)
        .ignoreConfidence(false)// If true, skips confidence filtering
        .pipeline(3)            // Camera pipeline index
        .pollingRateHz(150);    // Limelight polling rate in Hz
```

---

### `Detection`

Represents a single detected target. Before `fetch(...)`, `x` and `y` represent raw camera angles (`tx`, `ty` in degrees). After `fetch(...)`, `x` and `y` represent projected field coordinates in inches.

#### Getters
- `int getClassId()`: Model class id (index into labels.txt).
- `String getClassName()`: Model label (e.g. `"red_nectar"`), or `null` if the detector reports none.
- `BiobuzzClasses getBallClass()`: Legacy enum view of the class id.
- `double getX()`: Target X position (`tx` degrees or field X inches).
- `double getY()`: Target Y position (`ty` degrees or field Y inches).
- `double getConfidence()`: Detection confidence score (0.0 to 1.0).
- `double getArea()`: Detection bounding box area (share of the image, 0.0 to 1.0).

---

### `FieldPose`

An immutable 2D pose representing location and heading on the field.

```java
public FieldPose(double x, double y, double heading);
public FieldPose(double x, double y); // heading defaults to 0.0
```

- `double getX()`: X position in field inches.
- `double getY()`: Y position in field inches.
- `double getHeading()`: Robot orientation heading in radians.

---

### `BiobuzzClasses`

Legacy enum view of model class ids (new code should use `track(...)` label names instead):
- `POLLEN(0)` — Neutral yellow pollen element
- `NECTAR_RED(1)` — Alliance red nectar element
- `NECTAR_BLUE(2)` — Alliance blue nectar element
- `UNKNOWN(Integer.MAX_VALUE)` — Fallback safety classification

Includes `getId()` and static `BiobuzzClasses.fromId(int id)`.

---

### `DetectionSource` & `LimelightSource`

`DetectionSource` is the interface for vision backends:
```java
public interface DetectionSource {
    List<Detection> latest();
    long stalenessMs();
}
```

`LimelightSource` implements `DetectionSource` for the FTC `Limelight3A` camera, managing pipeline switches, polling rate, and mapping `LLResult` detector outputs (including class names) into `Detection` instances.

---

## Route Planning (`org.hivevisionftc.vision.weaver`)

### `WeaverRoute`

Implements `Function<HiveVision.DetectionList, PathResult>`. It uses [Marrow Weaver](https://marrow.skeletonarmyftc.com/) to build cubic bezier routes passing through field-frame detections. `Navigator.returnPath()` wraps it.

#### Factory Methods
- **`WeaverRoute.from(FieldPose start)`**: Specifies an explicit starting pose for the path route.
- **`WeaverRoute.fromFetchPose()`**: Uses the robot pose passed during the previous `DetectionList.fetch(...)` call.

#### Configuration Methods
- **`avoid(Zone... zones)` / `addObstacle(Zone zone)` / `avoid(Collection<? extends Zone> zones)`**: Adds obstacle zones (such as `CircleZone`) that the generated path must route around.
- **`maxTargets(int n)`**: Sets the maximum number of targets to include in a single path route (`0` or negative means no limit).
- **`ordered()`**: Configures Weaver to visit targets in the order they appear in the `DetectionList` (bypassing cost-ordering).
- **`end(FieldPose end)`**: Sets an explicit destination pose for the end of the route.

#### Execution
```java
PathResult path = WeaverRoute.from(startPose)
        .avoid(new CircleZone(new Point(72, 72), 18.0))
        .maxTargets(3)
        .plan(fieldDetections, startPose);
```
Or via the fluent `DetectionList.with(...)` chain:
```java
PathResult path = vision.detections()
        .fetch(robotPose)
        .sortByDistance()
        .with(WeaverRoute.fromFetchPose().maxTargets(3));
```
Returns a `PathResult` object (or `null` if no valid detections exist).
