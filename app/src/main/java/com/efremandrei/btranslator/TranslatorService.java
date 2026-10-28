package com.efremandrei.btranslator;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.quicksettings.TileService;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;

public final class TranslatorService extends AccessibilityService implements InputManager.InputDeviceListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static volatile TranslatorService current;
    public static volatile String status="Enable the accessibility service to begin";
    public static volatile String lastInput="No remote movement received yet";
    public static volatile String lastGesture="No swipes sent this session";
    public static volatile long samples=0, completed=0, cancelled=0;
    public volatile boolean active=false;
    private Config config;
    private StrokeRecognizer recognizer;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean baseline=false, busy=false;
    private float lastX,lastY;
    private Context visual;
    private static final int NOTIFICATION_ID=43;
    private final Runnable finishStroke=()->{
        if(!active)return;
        StrokeRecognizer.Direction direction=recognizer.finish(SystemClock.uptimeMillis());
        if(direction!=StrokeRecognizer.Direction.NONE){
            if(config.reverse())direction=StrokeRecognizer.reverse(direction);
            if(sendSwipe(direction,screenArea(),false))recognizer.dispatched(SystemClock.uptimeMillis());
        }
    };
    private final BroadcastReceiver screenOff=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){pause("Screen off · translation paused");}};

    @Override protected void onServiceConnected(){
        config=new Config(this);visual=createDisplayContext(getSystemService(android.hardware.display.DisplayManager.class).getDisplay(android.view.Display.DEFAULT_DISPLAY)).createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,null);
        active=false;current=this;rebuild();
        AccessibilityServiceInfo info=getServiceInfo();info.eventTypes=0;info.setMotionEventSources(0);setServiceInfo(info);
        config.prefs.registerOnSharedPreferenceChangeListener(this);
        getSystemService(InputManager.class).registerInputDeviceListener(this,handler);
        registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED);
        status="Service ready · translation is paused";refreshTile();
    }
    public void start(){
        if(Devices.find(config.device())==null){status="Connect and select a mouse or touchpad first";return;}
        rebuild();baseline=false;active=true;
        AccessibilityServiceInfo info=getServiceInfo();info.setMotionEventSources(Devices.SOURCES);setServiceInfo(info);
        status="Translating your selected remote";showNotification();refreshTile();
    }
    public void pause(String reason){
        active=false;handler.removeCallbacks(finishStroke);baseline=false;if(recognizer!=null)recognizer.reset();
        AccessibilityServiceInfo info=getServiceInfo();if(info!=null){info.setMotionEventSources(0);setServiceInfo(info);}
        status=reason;getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);refreshTile();
    }
    private void rebuild(){
        handler.removeCallbacks(finishStroke);
        recognizer=new StrokeRecognizer(config.threshold()*getResources().getDisplayMetrics().density,config.idle(),config.cooldown(),config.vertical());
        baseline=false;
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences prefs,String key){
        if("device".equals(key)&&active)pause("Remote changed · translation paused");
        if(!"light".equals(key)&&!"disclosure".equals(key))rebuild();
    }
    @Override public void onMotionEvent(MotionEvent event){
        if(!active)return;
        InputDevice device=event.getDevice();
        if(!Devices.supports(device)||!device.getDescriptor().equals(config.device()))return;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_CANCEL){recognizer.reset();baseline=false;return;}
        if(action==MotionEvent.ACTION_HOVER_ENTER||action==MotionEvent.ACTION_DOWN){baseline=true;lastX=event.getX();lastY=event.getY();return;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_HOVER_EXIT){baseline=false;return;}
        float dx=0,dy=0;
        if(action==MotionEvent.ACTION_SCROLL){
            if(!config.scroll())return;
            float unit=config.threshold()*getResources().getDisplayMetrics().density*1.2f;
            dx=event.getAxisValue(MotionEvent.AXIS_HSCROLL)*unit;
            dy=config.vertical()?event.getAxisValue(MotionEvent.AXIS_VSCROLL)*unit:0;
        }else if(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_HOVER_MOVE){
            dx=event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);dy=event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);
            for(int h=0;h<event.getHistorySize();h++){dx+=event.getHistoricalAxisValue(MotionEvent.AXIS_RELATIVE_X,h);dy+=event.getHistoricalAxisValue(MotionEvent.AXIS_RELATIVE_Y,h);}
            if(dx==0&&dy==0){
                if(event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)){
                    dx=event.getX();dy=event.getY();for(int h=0;h<event.getHistorySize();h++){dx+=event.getHistoricalX(h);dy+=event.getHistoricalY(h);}
                }else if(baseline){dx=event.getX()-lastX;dy=event.getY()-lastY;}
            }
            baseline=true;lastX=event.getX();lastY=event.getY();
        }else return;
        samples++;
        lastInput=device.getName()+" · "+Devices.sourceName(event.getSource())+" · Δ "+Math.round(dx)+", "+Math.round(dy)+" px";
        if(Math.abs(dx)<.1f&&Math.abs(dy)<.1f)return;
        recognizer.add(dx,dy,SystemClock.uptimeMillis());
        handler.removeCallbacks(finishStroke);handler.postDelayed(finishStroke,config.idle()+4L);
    }
    private Rect screenArea(){
        WindowMetrics metrics=visual.getSystemService(WindowManager.class).getCurrentWindowMetrics();Rect area=new Rect(metrics.getBounds());
        android.graphics.Insets bars=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
        area.inset(bars.left,bars.top,bars.right,bars.bottom);return area;
    }
    public boolean sendSwipe(StrokeRecognizer.Direction direction,Rect area,boolean test){
        if(busy){lastGesture="Previous swipe still running · movement skipped";return false;}
        if(area.width()<40||area.height()<40)return false;
        SwipeGeometry.Stroke stroke=SwipeGeometry.path(direction,area.left,area.top,area.width(),area.height(),config.span(),test?50:config.lane());
        Path path=new Path();path.moveTo(stroke.x1(),stroke.y1());path.lineTo(stroke.x2(),stroke.y2());
        GestureDescription gesture=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,config.duration())).build();
        busy=true;String label=(test?"Test ":"")+direction.name().toLowerCase(java.util.Locale.ROOT);
        boolean accepted=dispatchGesture(gesture,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription description){busy=false;completed++;lastGesture=label+" · touch gesture delivered";}
            @Override public void onCancelled(GestureDescription description){busy=false;cancelled++;lastGesture=label+" · cancelled by Android or another touch";}
        },handler);
        if(!accepted){busy=false;lastGesture="Android did not accept the swipe";}
        else lastGesture=label+" · sending touchscreen swipe";
        return accepted;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){ /* No screen content is requested or read. */ }
    @Override public void onInterrupt(){pause("Service interrupted · translation paused");}
    @Override public void onInputDeviceAdded(int id){checkDevice();}
    @Override public void onInputDeviceChanged(int id){baseline=false;recognizer.reset();checkDevice();}
    @Override public void onInputDeviceRemoved(int id){checkDevice();}
    private void checkDevice(){if(active&&Devices.find(config.device())==null)pause("Remote disconnected · translation paused");}
    private void refreshTile(){TileService.requestListeningState(this,new ComponentName(this,TranslatorTile.class));}
    public void showNotification(){
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel("translation","Active translation",NotificationManager.IMPORTANCE_LOW);channel.setShowBadge(false);manager.createNotificationChannel(channel);
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getBroadcast(this,0,new Intent(this,PauseReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,"translation").setSmallIcon(R.drawable.ic_notification).setContentTitle("BTranslator is translating").setContentText("Tap Pause to return normal mouse input").setOngoing(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"Pause",stop).build()).build();
        manager.notify(NOTIFICATION_ID,notification);
    }
    @Override public void onDestroy(){
        if(current==this){pause("Service stopped · translation paused");current=null;}
        handler.removeCallbacksAndMessages(null);
        if(config!=null)config.prefs.unregisterOnSharedPreferenceChangeListener(this);
        getSystemService(InputManager.class).unregisterInputDeviceListener(this);
        try{unregisterReceiver(screenOff);}catch(IllegalArgumentException ignored){}
        super.onDestroy();
    }
}
