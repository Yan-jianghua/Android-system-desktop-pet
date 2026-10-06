package com.qiuqiu.pet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

final class IdleMotion {
 long elapsed;private long last=-1;
 void reset(long now){elapsed=0;last=now;}
 boolean advance(long now,boolean playing,String mode){long delta=last<0?0:Math.max(0,now-last);last=now;if(!playing)return false;elapsed+=delta;long span=mode.equals("rest")?10000:15000;if(elapsed<span)return false;elapsed-=span;return true;}
 boolean advance(long now,boolean playing){return advance(now,playing,"sit");}
 static String next(String current,Random random){
  if(!current.equals("rest"))return "rest";
  ArrayList<String> choices=new ArrayList<>(Arrays.asList("groom","lie","sit"));return choices.get(random.nextInt(choices.size()));
 }
}
