package com.qiuqiu.pet;
public class MotionCheck {
 static void eq(int a,int b){if(a!=b)throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(MotionCycle.index(0,5000,true),0);eq(MotionCycle.index(4999,5000,true),59);eq(MotionCycle.index(5000,5000,true),0);
  eq(MotionCycle.index(-1,5000,true),0);eq(MotionCycle.index(9999,5000,false),59);
  java.util.Set<Integer> frames=new java.util.HashSet<>();for(long t=0;t<5000;t++){int i=MotionCycle.index(t,5000,true);frames.add(i);if(i<0||i>59)throw new AssertionError(i);}eq(frames.size(),60);
  if(MotionCycle.sourcePhase(1,true)!=.4f||MotionCycle.sourcePhase(59,false)!=23f)throw new AssertionError("subframe mapping");
  IdleMotion clock=new IdleMotion();clock.reset(0);if(clock.advance(9999,true,"rest"))throw new AssertionError("early rest switch");
  if(!clock.advance(10000,true,"rest"))throw new AssertionError("ten second rest must switch");
  clock.advance(18000,true,"rest");clock.advance(50000,false);eq((int)clock.elapsed,8000);
  if(!clock.advance(58000,true,"rest"))throw new AssertionError("paused rest time");
  System.out.println("PASS: 60 interpolated phases / 5 seconds; idle switches after 3 visible cycles, with pause/resume");
 }
}




