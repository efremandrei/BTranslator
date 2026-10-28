package com.efremandrei.btranslator;

import android.view.InputDevice;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class Devices {
    public static final int SOURCES=InputDevice.SOURCE_MOUSE|InputDevice.SOURCE_MOUSE_RELATIVE|InputDevice.SOURCE_TOUCHPAD;
    private Devices(){}
    public static boolean supports(InputDevice d){return d!=null&&!d.isVirtual()&&(d.supportsSource(InputDevice.SOURCE_MOUSE)||d.supportsSource(InputDevice.SOURCE_MOUSE_RELATIVE)||d.supportsSource(InputDevice.SOURCE_TOUCHPAD));}
    public static InputDevice find(String descriptor){
        if(descriptor.isEmpty())return null;
        for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(supports(d)&&d.getDescriptor().equals(descriptor))return d;}
        return null;
    }
    public static JSONArray json() throws JSONException {
        JSONArray devices=new JSONArray();
        for(int id:InputDevice.getDeviceIds()){
            InputDevice d=InputDevice.getDevice(id);
            if(supports(d))devices.put(new JSONObject().put("id",id).put("name",d.getName()).put("descriptor",d.getDescriptor()).put("source",sourceName(d.getSources())).put("external",d.isExternal()));
        }
        return devices;
    }
    public static String sourceName(int source){
        if((source&InputDevice.SOURCE_MOUSE_RELATIVE)==InputDevice.SOURCE_MOUSE_RELATIVE)return "Relative mouse";
        if((source&InputDevice.SOURCE_TOUCHPAD)==InputDevice.SOURCE_TOUCHPAD)return "Touchpad";
        if((source&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE)return "Mouse";
        return "Other";
    }
}
