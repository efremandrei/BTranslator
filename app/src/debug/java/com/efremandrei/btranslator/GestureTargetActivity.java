package com.efremandrei.btranslator;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import java.util.concurrent.atomic.AtomicInteger;

/** Debug-only native target: verifies real SOURCE_TOUCHSCREEN events, not simulated UI state. */
public final class GestureTargetActivity extends Activity {
    public static final AtomicInteger left=new AtomicInteger(),right=new AtomicInteger(),other=new AtomicInteger();
    @Override public void onCreate(Bundle state){super.onCreate(state);left.set(0);right.set(0);other.set(0);setContentView(new View(this){
        float start;
        @Override public boolean onTouchEvent(MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN)start=event.getX();
            if(event.getActionMasked()==MotionEvent.ACTION_UP){if(event.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)){float delta=event.getX()-start;if(delta<-50)left.incrementAndGet();else if(delta>50)right.incrementAndGet();}else other.incrementAndGet();invalidate();}return true;
        }
        @Override protected void onDraw(Canvas c){c.drawColor(Color.rgb(17,18,27));Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.rgb(184,174,255));p.setTextSize(40);c.drawText("Real touchscreen events",55,190,p);p.setTextSize(32);c.drawText("Left: "+left.get()+" / Right: "+right.get(),55,260,p);}
    });}
}
