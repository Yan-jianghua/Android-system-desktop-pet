package com.qiuqiu.pet;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;

/** Durable care history. Interrupted toilet animations can safely be replayed. */
public final class PetState {
 final SharedPreferences p;
 public int urine,poop;
 public long nextUrine,nextPoop,feedUntil;
 final ArrayList<Long> feeds=new ArrayList<>();
 static final long H=3600000L;
 static final class Event {
  final int kind,pieces; final long at;
  Event(int kind,long at,int pieces){this.kind=kind;this.at=at;this.pieces=pieces;}
 }
 PetState(Context c){
  p=c.getSharedPreferences("qiuqiu",0);long n=System.currentTimeMillis();
  urine=p.getInt("urine",0);poop=p.getInt("poop",0);
  nextUrine=p.getLong("nextUrine",n+2*H);nextPoop=p.getLong("nextPoop",n+6*H);feedUntil=p.getLong("feedUntil",0);
  for(String s:p.getString("feeds","").split(",")){try{feeds.add(Long.parseLong(s));}catch(NumberFormatException ignored){}}
  save();
 }
 boolean feed(long n){
  if(n<feedUntil)return false;
  feeds.add(n);feedUntil=n+10000;save();return true;
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
  save();
 }
 void clear(){urine=poop=0;save();}
 void save(){
  StringBuilder b=new StringBuilder();for(long f:feeds)b.append(f).append(',');
  p.edit().putInt("urine",urine).putInt("poop",poop).putLong("nextUrine",nextUrine)
   .putLong("nextPoop",nextPoop).putLong("feedUntil",feedUntil).putString("feeds",b.toString()).commit();
 }
}
