package org.hivevisionftc.vision;

public class Detection {
    private final BiobuzzClasses ballClass;
    private final int classId;
    private final String className;
    private final double x, y, confidence, area;

    public Detection(int classId, double txDeg, double tyDeg, double confidence, double area) {
        this(classId, null, txDeg, tyDeg, confidence, area);
    }

    public Detection(
            int classId, String className, double txDeg, double tyDeg, double confidence, double area) {
        this.ballClass = BiobuzzClasses.fromId(classId);
        this.classId = classId;
        this.className = className;
        this.x = txDeg;
        this.y = tyDeg;
        this.confidence = confidence;
        this.area = area;
    }

    public Detection(Detection detection) {
        this.ballClass = detection.ballClass;
        this.classId = detection.classId;
        this.className = detection.className;
        this.x = detection.x;
        this.y = detection.y;
        this.confidence = detection.confidence;
        this.area = detection.area;
    }

    public Detection(Detection detection, FieldPose newCords) {
        this.ballClass = detection.ballClass;
        this.classId = detection.classId;
        this.className = detection.className;
        this.x = newCords.getX();
        this.y = newCords.getY();
        this.confidence = detection.confidence;
        this.area = detection.area;
    }

    public BiobuzzClasses getBallClass() {
        return ballClass;
    }

    public int getClassId() {
        return classId;
    }

    /** The model's label name (e.g. "red_nectar"), or null when the detector does not report one. */
    public String getClassName() {
        return className;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getArea() {
        return area;
    }
}
