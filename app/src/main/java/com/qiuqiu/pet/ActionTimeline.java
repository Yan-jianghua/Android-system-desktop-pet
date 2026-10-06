package com.qiuqiu.pet;

import java.util.ArrayDeque;

/** A toilet visit can pause care and then resume its remaining animation time. */
final class ActionTimeline {
 static final class Action {
  final String mode,food;final Runnable done;final long duration;long remaining;
  Action(String mode,String food,long duration,Runnable done){this.mode=mode;this.food=food;this.duration=duration;remaining=duration;this.done=done;}
 }
 private final ArrayDeque<Action> paused=new ArrayDeque<>();
 private Action current;
 private long deadline;
 boolean busy(){return current!=null;}
 boolean toilet(){return current!=null&&(current.mode.equals("pee")||current.mode.equals("poop"));}
 String mode(){return current==null?"sit":current.mode;}
 String food(){return current==null?"":current.food;}
 long elapsed(long now){return current==null?0:Math.max(0,Math.min(current.duration,current.duration-(deadline-now)));}
 void start(String mode,String food,long duration,Runnable done,long now){
  current=new Action(mode,food,duration,done);deadline=now+duration;
 }
 void interrupt(String mode,long duration,Runnable done,long now){
  update(now);
  if(current!=null){current.remaining=Math.max(0,deadline-now);paused.push(current);}
  start(mode,"",duration,done,now);
 }
 void update(long now){
  if(current==null||now<deadline)return;
  Action finished=current;current=null;
  if(finished.done!=null)finished.done.run();
  if(current==null&&!paused.isEmpty()){current=paused.pop();deadline=now+current.remaining;}
 }
}
