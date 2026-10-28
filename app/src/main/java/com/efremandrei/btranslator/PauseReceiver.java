package com.efremandrei.btranslator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public final class PauseReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){TranslatorService service=TranslatorService.current;if(service!=null)service.pause("Paused from notification");}
}
