package com.qiuqiu.pet;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import org.json.JSONObject;

/** Durable care history. Interrupted toilet animations can safely be replayed. */
public final class PetState {
 final SharedPreferences p;final Context context;
 public int urine,poop;
 public long nextUrine,nextPoop,feedUntil;
 final ArrayList<Long> feeds=new ArrayList<>();
 static final long H=3600000L;
 static final class Event {
  final int kind,pieces; final long at;
  Event(int kind,long at,int pieces){this.kind=kind;this.at=at;this.pieces=pieces;}
 }
 PetState(Context c){
  context=c.getApplicationContext();p=context.getSharedPreferences("qiuqiu",0);long n=System.currentTimeMillis();
  urine=p.getInt("urine",0);poop=p.getInt("poop",0);
  nextUrine=p.getLong("nextUrine",n+2*H);nextPoop=p.getLong("nextPoop",n+6*H);feedUntil=p.getLong("feedUntil",0);
  for(String s:p.getString("feeds","").split(",")){try{feeds.add(Long.parseLong(s));}catch(NumberFormatException ignored){}}
  save();
 }
 boolean feed(long n){
  if(n<feedUntil)return false;
  feeds.add(n);feedUntil=n+10000;save();try{SyncRepository.enqueue(context,"care","pet","feed",new JSONObject().put("at",n));}catch(Exception ignored){}return true;
 }
 Event nextDue(long n){
  if(nextUrine<=n&&(nextUrine<=nextPoop||nextPoop>n))return new Event(1,nextUrine,1);
  if(nextPoop<=n){
   int count=0;for(long f:feeds)if(f>=nextPoop-6*H&&f<nextPoop)count++;
   return new Event(2,nextPoop,count<=1?1:count<=3?2:3);
  }
  return null;
 }
 void finish(Event e){
  if(e.kind==1&&nextUrine==e.at){urine++;nextUrine+=2*H;}
  else if(e.kind==2&&nextPoop==e.at){poop+=e.pieces;nextPoop+=6*H;feeds.removeIf(f->f<nextPoop-6*H);}
  else return; // Never count the same event twice.
  save();try{SyncRepository.enqueue(context,"care","pet",e.kind==1?"urine":"poop",new JSONObject().put("at",e.at).put("pieces",e.pieces));}catch(Exception ignored){}
 }
 void clear(){urine=poop=0;long now=System.currentTimeMillis();context.getSharedPreferences("qiuqiu-sync-applied",0).edit().putLong("lastClearAt",now).apply();save();try{SyncRepository.enqueue(context,"care","pet","clear_litter",new JSONObject().put("at",now));}catch(Exception ignored){}}
 static void applyRemote(Context c,String action,JSONObject payload,long at){
  PetState s=new PetState(c);if(action.equals("feed")){long t=payload.optLong("at",at);if(!s.feeds.contains(t)){s.feeds.add(t);s.feedUntil=Math.max(s.feedUntil,t+10000);}}
  else if(action.equals("urine")){String id="urine:"+payload.optLong("at",at);if(markApplied(c,id)&&at>lastClear(c))s.urine+=Math.max(1,payload.optInt("pieces",1));}
  else if(action.equals("poop")){String id="poop:"+payload.optLong("at",at);if(markApplied(c,id)&&at>lastClear(c))s.poop+=Math.max(1,payload.optInt("pieces",1));}
  else if(action.equals("clear_litter")){String id="clear:"+payload.optLong("at",at);if(markApplied(c,id)){s.urine=0;s.poop=0;c.getApplicationContext().getSharedPreferences("qiuqiu-sync-applied",0).edit().putLong("lastClearAt",Math.max(at,lastClear(c))).apply();}}s.save();
 }
 private static long lastClear(Context c){return c.getApplicationContext().getSharedPreferences("qiuqiu-sync-applied",0).getLong("lastClearAt",0);}
 private static boolean markApplied(Context c,String id){SharedPreferences p=c.getApplicationContext().getSharedPreferences("qiuqiu-sync-applied",0);if(p.getBoolean(id,false))return false;return p.edit().putBoolean(id,true).commit();}
 void save(){
  StringBuilder b=new StringBuilder();for(long f:feeds)b.append(f).append(',');
  p.edit().putInt("urine",urine).putInt("poop",poop).putLong("nextUrine",nextUrine)
   .putLong("nextPoop",nextPoop).putLong("feedUntil",feedUntil).putString("feeds",b.toString()).commit();
 }
}
