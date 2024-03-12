package com.zamzar.mock.pagination;

public class Anchor {

    protected final String ref;
    protected final Orientation orientation;

    public static Anchor after(String ref) {
        return new Anchor(ref, Orientation.AFTER);
    }

    public static Anchor before(String ref) {
        return new Anchor(ref, Orientation.BEFORE);
    }

    protected Anchor(String ref, Orientation orientation) {
        this.ref = ref;
        this.orientation = orientation;
    }

    @Override
    public String toString() {
        if (orientation == Orientation.AFTER) {
            return "after " + ref;
        } else {
            return "before " + ref;
        }
    }

    public enum Orientation {
        AFTER,
        BEFORE;
    }

}