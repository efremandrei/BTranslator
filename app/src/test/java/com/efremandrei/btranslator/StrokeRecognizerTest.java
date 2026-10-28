package com.efremandrei.btranslator;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.efremandrei.btranslator.StrokeRecognizer.Direction.*;

public class StrokeRecognizerTest {
    @Test public void motionWaitsForFingerPauseAndEmitsOnce(){
        StrokeRecognizer r=new StrokeRecognizer(35,150,450,false);
        r.add(20,2,10);r.add(20,1,30);
        assertEquals(NONE,r.finish(100));assertEquals(RIGHT,r.finish(180));assertEquals(NONE,r.finish(400));
    }
    @Test public void noiseAndDiagonalAndVerticalDoNotTurnPagesInHorizontalMode(){
        StrokeRecognizer r=new StrokeRecognizer(35,150,450,false);
        r.add(20,0,0);assertEquals(NONE,r.finish(150));
        r.add(60,55,200);assertEquals(NONE,r.finish(350));
        r.add(2,200,400);assertEquals(NONE,r.finish(550));
    }
    @Test public void reversedStrokeDoesNotLeaveResidualMotion(){
        StrokeRecognizer r=new StrokeRecognizer(35,150,450,false);
        r.add(60,0,0);r.add(-50,0,30);assertEquals(NONE,r.finish(180));
        r.add(-40,0,200);assertEquals(LEFT,r.finish(350));
    }
    @Test public void cooldownDropsDuplicateBurstsButAllowsNextGesture(){
        StrokeRecognizer r=new StrokeRecognizer(35,150,450,false);
        r.add(50,0,0);assertEquals(RIGHT,r.finish(150));r.dispatched(150);
        r.add(50,0,200);assertEquals(NONE,r.finish(350));
        r.add(-50,0,600);assertEquals(LEFT,r.finish(750));
    }
    @Test public void optionalVerticalModeHonorsDominance(){
        StrokeRecognizer r=new StrokeRecognizer(35,150,450,true);
        r.add(5,-70,0);assertEquals(UP,r.finish(150));
        r.add(-4,80,200);assertEquals(DOWN,r.finish(350));
        assertEquals(RIGHT,StrokeRecognizer.reverse(LEFT));assertEquals(UP,StrokeRecognizer.reverse(DOWN));
    }
    @Test public void invalidSamplesAreIgnored(){
        StrokeRecognizer r=new StrokeRecognizer(35,150,450,false);
        r.add(Double.NaN,0,0);r.add(Double.POSITIVE_INFINITY,1,0);assertEquals(NONE,r.finish(150));
    }
    @Test public void swipeUsesSafeInteriorAndRespectsDirection(){
        SwipeGeometry.Stroke left=SwipeGeometry.path(LEFT,10,20,1000,2000,60,50);
        assertEquals(810,left.x1(),.01);assertEquals(210,left.x2(),.01);assertEquals(1020,left.y1(),.01);
        SwipeGeometry.Stroke right=SwipeGeometry.path(RIGHT,0,0,1000,2000,999,-10);
        assertEquals(100,right.x1(),.01);assertEquals(900,right.x2(),.01);assertEquals(400,right.y1(),.01);
        SwipeGeometry.Stroke up=SwipeGeometry.path(UP,0,0,1000,2000,60,50);
        assertTrue(up.y1()>up.y2());assertEquals(up.x1(),up.x2(),.01);
    }
}
