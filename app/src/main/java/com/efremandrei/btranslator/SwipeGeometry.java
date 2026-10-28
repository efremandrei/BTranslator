package com.efremandrei.btranslator;

public final class SwipeGeometry {
    private SwipeGeometry() {}
    public record Stroke(float x1,float y1,float x2,float y2) {}
    public static Stroke path(StrokeRecognizer.Direction direction, float left,float top,float width,float height,int spanPercent,int lanePercent) {
        if(width<=0||height<=0||direction==StrokeRecognizer.Direction.NONE)throw new IllegalArgumentException("Invalid swipe area");
        float half=Math.max(20,Math.min(80,spanPercent))/200f;
        float lane=Math.max(20,Math.min(80,lanePercent))/100f;
        float x1=.5f,x2=.5f,y1=.5f,y2=.5f;
        switch(direction) {
            case LEFT -> {x1+=half; x2-=half; y1=lane;y2=lane;}
            case RIGHT -> {x1-=half;x2+=half;y1=lane;y2=lane;}
            case UP -> {y1+=half;y2-=half;x1=lane;x2=lane;}
            case DOWN -> {y1-=half;y2+=half;x1=lane;x2=lane;}
            default -> throw new IllegalArgumentException("No direction");
        }
        return new Stroke(left+x1*width,top+y1*height,left+x2*width,top+y2*height);
    }
}
