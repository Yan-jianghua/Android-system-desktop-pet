package com.qiuqiu.pet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

/** Notifies the user when a new shared chat message arrives from the other member. */
final class MessageNotifier {
 static final String ACTION_INCOMING="com.qiuqiu.pet.INCOMING_CHAT_MESSAGE";
 private static final String CHANNEL="chat_message_v1";
 static void received(Context context,SharedItem item){
  Context app=context.getApplicationContext();NotificationManager manager=app.getSystemService(NotificationManager.class);
  if(manager==null)return;
  if(Build.VERSION.SDK_INT>=26&&manager.getNotificationChannel(CHANNEL)==null){
   NotificationChannel channel=new NotificationChannel(CHANNEL,"双人聊天新消息",NotificationManager.IMPORTANCE_HIGH);
   channel.setDescription("收到另一位主人发来的聊天消息时提醒");channel.enableVibration(true);
   channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
   manager.createNotificationChannel(channel);
  }
  Intent open=new Intent(app,ChatActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
  PendingIntent tap=PendingIntent.getActivity(app,3101,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  String who=item.title==null||item.title.isEmpty()?"另一位主人":item.title;
  String body=item.body==null||item.body.isEmpty()?"发来一条消息":item.body;
  Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(app,CHANNEL):new Notification.Builder(app).setPriority(Notification.PRIORITY_HIGH).setSound(Uri.parse("content://settings/system/notification_sound"));
  Notification notification=builder.setSmallIcon(R.drawable.cat_icon).setContentTitle(who+" 给球球留言啦").setContentText(body)
   .setStyle(new Notification.BigTextStyle().bigText(body)).setCategory(Notification.CATEGORY_MESSAGE).setAutoCancel(true).setContentIntent(tap).build();
  manager.notify(item.id.hashCode(),notification);
  Intent overlay=new Intent(ACTION_INCOMING).setPackage(app.getPackageName()).putExtra("sender",who).putExtra("body",body);
  app.sendBroadcast(overlay);
 }
 private MessageNotifier(){}
}
