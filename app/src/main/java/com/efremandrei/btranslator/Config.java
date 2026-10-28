package com.efremandrei.btranslator;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

public final class Config {
    public final SharedPreferences prefs;
    public Config(Context context) { prefs=context.getSharedPreferences("translator",Context.MODE_PRIVATE); }
    public String device() { return prefs.getString("device",""); }
    public boolean light() { return prefs.getBoolean("light",false); }
    public int threshold(){return prefs.getInt("threshold",35);}
    public int idle(){return prefs.getInt("idle",150);}
    public int duration(){return prefs.getInt("duration",220);}
    public int span(){return prefs.getInt("span",60);}
    public int lane(){return prefs.getInt("lane",50);}
    public int cooldown(){return prefs.getInt("cooldown",450);}
    public boolean reverse(){return prefs.getBoolean("reverse",false);}
    public boolean vertical(){return prefs.getBoolean("vertical",false);}
    public boolean scroll(){return prefs.getBoolean("scroll",true);}
    public void update(String key,int value){
        int min,max;
        switch(key){
            case "threshold"->{min=10;max=180;}
            case "idle"->{min=80;max=350;}
            case "duration"->{min=100;max=700;}
            case "span","lane"->{min=20;max=80;}
            case "cooldown"->{min=250;max=1200;}
            case "reverse","vertical","scroll"->{prefs.edit().putBoolean(key,value!=0).apply();return;}
            default->{return;}
        }
        prefs.edit().putInt(key,Math.max(min,Math.min(max,value))).apply();
    }
    public JSONObject json() throws JSONException {
        return new JSONObject().put("threshold",threshold()).put("idle",idle()).put("duration",duration()).put("span",span()).put("lane",lane()).put("cooldown",cooldown()).put("reverse",reverse()).put("vertical",vertical()).put("scroll",scroll()).put("device",device()).put("light",light());
    }
}
