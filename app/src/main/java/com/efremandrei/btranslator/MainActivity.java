package com.efremandrei.btranslator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private Config config;
    private WebView web;
    private FrameLayout root;
    private boolean pageReady=false;
    private android.window.SplashScreenView launchSplash;
    private volatile long detectUntil=0;
    private volatile String detection="";
    private final Handler handler=new Handler(Looper.getMainLooper());
    @Override public void onCreate(Bundle state){
        config=new Config(this);if(config.light())setTheme(R.style.AppTheme_Light);super.onCreate(state);
        getSplashScreen().setOnExitAnimationListener(splash->{launchSplash=splash;finishStartup();});
        root=new FrameLayout(this);web=new WebView(this);root.addView(web);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsets.CONSUMED;});root.requestApplyInsets();theme(config.light());
        web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
        web.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){pageReady=true;finishStartup();}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !isLocal(request.getUrl());}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();String path=uri.getPath();
                if(!isLocal(uri)||path==null||path.contains(".."))return empty();
                try{String name=path.substring(1);String mime=name.endsWith(".html")?"text/html":name.endsWith(".css")?"text/css":name.endsWith(".js")?"application/javascript":"image/svg+xml";return new WebResourceResponse(mime,"UTF-8",getAssets().open(name));}catch(java.io.IOException e){return empty();}
            }
            private WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));}
        });
        web.addJavascriptInterface(new Bridge(),"AndroidTranslator");web.loadUrl("https://appassets.androidplatform.net/index.html");
    }
    private boolean isLocal(Uri uri){return "https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost());}
    private void finishStartup(){
        if(!pageReady||launchSplash==null||web==null)return;
        web.postVisualStateCallback(0,new WebView.VisualStateCallback(){@Override public void onComplete(long requestId){
            web.getViewTreeObserver().registerFrameCommitCallback(()->handler.post(()->{if(launchSplash!=null){launchSplash.remove();launchSplash=null;}}));web.invalidate();
        }});
    }
    private void theme(boolean light){int bg=Color.parseColor(light?"#F5F4FA":"#11121B");root.setBackgroundColor(bg);web.setBackgroundColor(bg);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);getSplashScreen().setSplashScreenTheme(light?R.style.AppTheme_Light:R.style.AppTheme);}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){
        if(detectUntil>SystemClock.uptimeMillis()&&(event.getActionMasked()==MotionEvent.ACTION_HOVER_MOVE||event.getActionMasked()==MotionEvent.ACTION_MOVE||event.getActionMasked()==MotionEvent.ACTION_SCROLL)){
            InputDevice d=event.getDevice();if(Devices.supports(d)){
                config.prefs.edit().putString("device",d.getDescriptor()).apply();detectUntil=0;detection="Detected "+d.getName();
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }
    public final class Bridge {
        @JavascriptInterface public String state(){
            try{TranslatorService s=TranslatorService.current;InputDevice selected=Devices.find(config.device());return new JSONObject().put("android",true).put("version",BuildConfig.VERSION_NAME).put("build",BuildConfig.VERSION_CODE).put("connected",s!=null).put("active",s!=null&&s.active).put("status",TranslatorService.status).put("lastInput",TranslatorService.lastInput).put("lastGesture",TranslatorService.lastGesture).put("samples",TranslatorService.samples).put("completed",TranslatorService.completed).put("cancelled",TranslatorService.cancelled).put("devices",Devices.json()).put("selectedName",selected==null?"":selected.getName()).put("detecting",detectUntil>SystemClock.uptimeMillis()).put("detection",detection).put("notifications",checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED).put("config",config.json()).toString();}catch(JSONException e){return "{}";}
        }
        @JavascriptInterface public void selectDevice(String descriptor){runOnUiThread(()->{if(Devices.find(descriptor)!=null){config.prefs.edit().putString("device",descriptor).apply();detection="";}});}
        @JavascriptInterface public void detect(){runOnUiThread(()->{TranslatorService s=TranslatorService.current;if(s!=null&&s.active)s.pause("Paused to detect a remote");detectUntil=SystemClock.uptimeMillis()+12_000;detection="Move a finger across your remote’s touchpad now";});}
        @JavascriptInterface public void configure(String key,int value){runOnUiThread(()->config.update(key,value));}
        @JavascriptInterface public void setTheme(boolean light){runOnUiThread(()->{config.prefs.edit().putBoolean("light",light).apply();theme(light);});}
        @JavascriptInterface public void toggle(){runOnUiThread(()->{TranslatorService s=TranslatorService.current;if(s==null){Toast.makeText(MainActivity.this,"Enable BTranslator in Accessibility first",Toast.LENGTH_LONG).show();return;}if(s.active)s.pause("Paused · normal pointer input restored");else s.start();});}
        @JavascriptInterface public void accessibility(){runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Enable remote swipe control").setMessage(R.string.service_description).setPositiveButton("Open settings",(d,w)->{
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }).setNegativeButton("Cancel",null).show());}
        @JavascriptInterface public void bluetooth(){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));}
        @JavascriptInterface public void appInfo(){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));}
        @JavascriptInterface public void notifications(){runOnUiThread(()->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8));}
        @JavascriptInterface public void repository(){runOnUiThread(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/efremandrei/BTranslator")));}catch(android.content.ActivityNotFoundException ignored){}});}
        @JavascriptInterface public void testSwipe(String direction,double left,double top,double width,double height,double viewportWidth){
            runOnUiThread(()->{
                TranslatorService s=TranslatorService.current;if(s==null){Toast.makeText(MainActivity.this,"Enable the accessibility service to send a test swipe",Toast.LENGTH_LONG).show();return;}
                if(!"left".equals(direction)&&!"right".equals(direction))return;
                if(!Double.isFinite(left+top+width+height+viewportWidth)||viewportWidth<100||width<40||height<40)return;
                float scale=web.getWidth()/(float)viewportWidth;int[] position=new int[2];web.getLocationOnScreen(position);
                Rect rect=new Rect(position[0]+Math.round((float)left*scale),position[1]+Math.round((float)top*scale),position[0]+Math.round((float)(left+width)*scale),position[1]+Math.round((float)(top+height)*scale));
                if(!rect.intersect(new Rect(position[0],position[1],position[0]+web.getWidth(),position[1]+web.getHeight())))return;
                handler.postDelayed(()->{if(!isFinishing()&&hasWindowFocus()&&TranslatorService.current==s)s.sendSwipe("left".equals(direction)?StrokeRecognizer.Direction.LEFT:StrokeRecognizer.Direction.RIGHT,rect,true);},300);
            });
        }
    }
    @Override protected void onResume(){super.onResume();if(web!=null){web.onResume();web.evaluateJavascript("window.refreshState&&window.refreshState()",null);}}
    @Override protected void onPause(){if(web!=null)web.onPause();super.onPause();}
    @Override public void onBackPressed(){web.evaluateJavascript("window.appBack&&window.appBack()",result->{if(!"true".equals(result))super.onBackPressed();});}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);web.removeJavascriptInterface("AndroidTranslator");web.destroy();super.onDestroy();}
}
