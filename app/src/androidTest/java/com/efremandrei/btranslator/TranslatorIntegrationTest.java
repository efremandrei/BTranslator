package com.efremandrei.btranslator;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.FileInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TranslatorIntegrationTest {
    private Instrumentation instrumentation;
    private UiAutomation automation;
    private Context context;
    private String oldServices,oldEnabled;
    private Activity activity;
    private TranslatorService service;
    private static final String COMPONENT="com.efremandrei.btranslator/com.efremandrei.btranslator.TranslatorService";
    private String shell(String cmd)throws Exception{
        try(ParcelFileDescriptor fd=automation.executeShellCommand(cmd);FileInputStream input=new FileInputStream(fd.getFileDescriptor())){return new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim();}
    }
    private void waitFor(java.util.function.BooleanSupplier condition,String failure)throws Exception{
        long end=SystemClock.uptimeMillis()+15_000;while(SystemClock.uptimeMillis()<end){if(condition.getAsBoolean())return;SystemClock.sleep(60);}fail(failure);
    }
    @Before public void enableService()throws Exception{
        instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        oldServices=shell("settings get secure enabled_accessibility_services");oldEnabled=shell("settings get secure accessibility_enabled");
        String enabled="null".equals(oldServices)||oldServices.isEmpty()?COMPONENT:oldServices.contains(COMPONENT)?oldServices:oldServices+":"+COMPONENT;
        // executeShellCommand tokenizes arguments directly; shell quote characters become
        // literal setting contents. Component lists contain no spaces and need no quotes.
        shell("settings put secure enabled_accessibility_services "+enabled);shell("settings put secure accessibility_enabled 1");
        waitFor(()->TranslatorService.current!=null,"Accessibility service did not connect");service=TranslatorService.current;
        instrumentation.runOnMainSync(()->{service.pause("Integration test");Config c=new Config(context);c.update("threshold",10);c.update("idle",100);c.update("duration",180);c.update("cooldown",350);c.update("reverse",0);c.update("vertical",0);});
    }
    private void selectQaMouse(){
        InputDevice qa=null;for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(Devices.supports(d)&&d.getName().equals("BTranslator QA Mouse"))qa=d;}
        assertNotNull("Start tools/virtual_mouse.py before the emulator tests",qa);String descriptor=qa.getDescriptor();
        instrumentation.runOnMainSync(()->new Config(context).prefs.edit().putString("device",descriptor).commit());
    }
    @After public void restoreSettings()throws Exception{
        if(service!=null&&TranslatorService.current==service)instrumentation.runOnMainSync(()->service.pause("Integration test finished"));
        if(activity!=null)instrumentation.runOnMainSync(()->activity.finish());
        if(automation!=null&&oldServices!=null){if("null".equals(oldServices)||oldServices.isEmpty())shell("settings delete secure enabled_accessibility_services");else shell("settings put secure enabled_accessibility_services "+oldServices);shell("settings put secure accessibility_enabled "+("1".equals(oldEnabled)?"1":"0"));}
    }
    @Test public void realMouseInputBecomesTouchAndPauseRestoresPointerInput()throws Exception{
        selectQaMouse();activity=instrumentation.startActivitySync(new Intent(context,GestureTargetActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        long samplesBefore=TranslatorService.samples,deliveredBefore=TranslatorService.completed;
        instrumentation.runOnMainSync(()->service.start());assertTrue(service.active);
        waitFor(()->GestureTargetActivity.left.get()>0&&GestureTargetActivity.right.get()>0,"Physical uinput movement did not become both touch directions: "+TranslatorService.lastInput+" / "+TranslatorService.lastGesture);
        assertTrue(TranslatorService.samples>samplesBefore);waitFor(()->TranslatorService.completed>=deliveredBefore+2,"Gesture completion callbacks did not arrive");assertEquals(0,GestureTargetActivity.other.get());
        instrumentation.runOnMainSync(()->service.pause("Test pause"));
        SystemClock.sleep(300);int before=GestureTargetActivity.left.get()+GestureTargetActivity.right.get();SystemClock.sleep(2600);
        assertEquals("Paused service must not generate gestures",before,GestureTargetActivity.left.get()+GestureTargetActivity.right.get());
        assertEquals(0,service.getServiceInfo().getMotionEventSources());
    }
    @Test public void productionWebTestButtonsDeliverActualTouchEvents()throws Exception{
        activity=instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitFor(()->activity.hasWindowFocus(),"Test activity did not gain focus");
        AtomicReference<WebView> ref=new AtomicReference<>();instrumentation.runOnMainSync(()->ref.set(findWebView(activity.getWindow().getDecorView())));WebView web=ref.get();assertNotNull(web);
        long end=SystemClock.uptimeMillis()+15_000;while(SystemClock.uptimeMillis()<end&&!"true".equals(js(web,"typeof window.refreshState==='function'")))SystemClock.sleep(100);
        assertEquals("true",js(web,"typeof window.refreshState==='function'"));
        js(web,"navigate('test'); true");long before=TranslatorService.completed;
        js(web,"sendTest('left'); true");
        waitFor(()->{try{return js(web,"document.querySelector('#test-count').textContent").contains("1 touch swipe received");}catch(Exception e){return false;}},"Native left test did not reach the production touch handler: "+TranslatorService.lastGesture);
        js(web,"sendTest('right'); true");
        waitFor(()->{try{return js(web,"document.querySelector('#test-count').textContent").contains("2 touch swipes received");}catch(Exception e){return false;}},"Native right test did not reach the production touch handler");
        waitFor(()->TranslatorService.completed>=before+2,"Web test gesture completion callbacks did not arrive");
    }
    @Test public void notificationPauseAndScreenOffStopInterception()throws Exception{
        selectQaMouse();instrumentation.runOnMainSync(()->service.start());assertTrue(service.active);
        context.sendBroadcast(new Intent(context,PauseReceiver.class));waitFor(()->!service.active,"Pause receiver failed");
        instrumentation.runOnMainSync(()->service.start());
        try{shell("input keyevent 223");waitFor(()->!service.active,"Screen off did not pause translation");waitFor(()->service.getServiceInfo().getMotionEventSources()==0,"Android did not finish restoring pointer input after screen off");}finally{shell("input keyevent 224");shell("wm dismiss-keyguard");}
    }
    private static WebView findWebView(View v){if(v instanceof WebView)return (WebView)v;if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){WebView found=findWebView(g.getChildAt(i));if(found!=null)return found;}return null;}
    private String js(WebView web,String script)throws Exception{
        AtomicReference<String> result=new AtomicReference<>();CountDownLatch done=new CountDownLatch(1);instrumentation.runOnMainSync(()->web.evaluateJavascript(script,value->{result.set(value);done.countDown();}));assertTrue("JavaScript callback timed out",done.await(10,TimeUnit.SECONDS));return result.get();
    }
}
