package com.efremandrei.btranslator;

/** Recognizes one direction per motion burst, independent of the Android input plumbing. */
public final class StrokeRecognizer {
    public enum Direction { NONE, LEFT, RIGHT, UP, DOWN }
    private final double threshold;
    private final long idleMillis;
    private final long cooldownMillis;
    private final boolean vertical;
    private double x, y;
    private long lastSample = -1;
    private long lastDispatch = -100_000;

    public StrokeRecognizer(double threshold, long idleMillis, long cooldownMillis, boolean vertical) {
        if (threshold <= 0 || !Double.isFinite(threshold) || idleMillis < 1 || cooldownMillis < 0) throw new IllegalArgumentException("Invalid recognition settings");
        this.threshold=threshold; this.idleMillis=idleMillis; this.cooldownMillis=cooldownMillis; this.vertical=vertical;
    }
    public void add(double dx, double dy, long now) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || (Math.abs(dx)<.1 && Math.abs(dy)<.1)) return;
        x+=dx; y+=dy; lastSample=now;
    }
    public Direction finish(long now) {
        if (lastSample<0 || now-lastSample<idleMillis) return Direction.NONE;
        double dx=x, dy=y; reset();
        if (now-lastDispatch<cooldownMillis) return Direction.NONE;
        if (Math.abs(dx)>=threshold && Math.abs(dx)>=Math.abs(dy)*1.5) return dx>0?Direction.RIGHT:Direction.LEFT;
        if (vertical && Math.abs(dy)>=threshold && Math.abs(dy)>=Math.abs(dx)*1.5) return dy>0?Direction.DOWN:Direction.UP;
        return Direction.NONE;
    }
    public void dispatched(long now) { lastDispatch=now; }
    public void reset() { x=0; y=0; lastSample=-1; }
    public static Direction reverse(Direction d) {
        return switch(d) { case LEFT->Direction.RIGHT; case RIGHT->Direction.LEFT; case UP->Direction.DOWN; case DOWN->Direction.UP; default->Direction.NONE; };
    }
}
