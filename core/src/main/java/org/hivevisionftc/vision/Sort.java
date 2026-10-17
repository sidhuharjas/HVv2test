package org.hivevisionftc.vision;

/** Route visit order for {@link Navigator}. */
public enum Sort {
    SHORTEST, // nearest first
    LONGEST, // farthest first
    SIZE, // largest in frame first
    CONFIDENCE,
    COLOR, // by class id, ascending
    NONE
}
