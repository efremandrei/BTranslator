package com.efremandrei.btranslator;
import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
public final class TranslatorTile extends TileService {
    @Override public void onStartListening(){update();}
    private void update(){Tile tile=getQsTile();if(tile==null)return;TranslatorService s=TranslatorService.current;boolean active=s!=null&&s.active;tile.setState(active?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);tile.setLabel("BTranslator");tile.setSubtitle(active?"Tap to pause":"Paused · tap to open");tile.updateTile();}
    @Override public void onClick(){TranslatorService s=TranslatorService.current;if(s!=null&&s.active)s.pause("Paused from Quick Settings");else startActivityAndCollapse(PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));update();}
}
