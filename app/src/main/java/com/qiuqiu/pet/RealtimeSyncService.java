package com.qiuqiu.pet;

import android.app.*;import android.content.*;import android.os.*;

public class RealtimeSyncService extends Service {
 final Handler timer=new Handler(Looper.getMainLooper());boolean running;
 final Runnable tick=new Runnable(){public void run(){if(!running)return;ServerConfig cfg=new ServerConfig(RealtimeSyncService.this);if(!cfg.configured()){stopSelf();return;}new SyncRepository(RealtimeSyncService.this).syncAsync((ok,msg)->{if(running)timer.postDelayed(this,ok?2500:8000);});}};
 @Override public void onCreate(){super.onCreate();NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("qiuqiu-sync","球球实时同步",NotificationManager.IMPORTANCE_LOW));Notification n=new Notification.Builder(this,"qiuqiu-sync").setSmallIcon(R.drawable.cat_icon).setContentTitle("球球正在实时同步").setContentText("聊天、相册和照顾数据会自动同步").setOngoing(true).build();startForeground(8,n);running=true;timer.post(tick);}
 @Override public int onStartCommand(Intent i,int f,int id){running=true;timer.removeCallbacks(tick);timer.post(tick);return START_STICKY;}
 @Override public void onDestroy(){running=false;timer.removeCallbacksAndMessages(null);super.onDestroy();}
 @Override public android.os.IBinder onBind(Intent i){return null;}
 public static void start(Context c){if(!new ServerConfig(c).configured())return;Intent i=new Intent(c,RealtimeSyncService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
}
