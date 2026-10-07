package com.qiuqiu.pet;
import android.content.*;
public final class SyncBootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){SyncJobService.schedule(c);}
}
