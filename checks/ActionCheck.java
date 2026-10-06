package com.qiuqiu.pet;
import java.util.*;

public class ActionCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  ActionTimeline a=new ActionTimeline();int[] count={0};
  a.start("eat","猫条",5000,()->{count[0]++;a.start("happy","",2500,()->count[0]++,10000);},0);
  a.interrupt("pee",5000,()->count[0]++,2000);
  check(a.toilet()&&a.food().isEmpty(),"toilet interrupts feeding immediately");
  a.update(6999);check(count[0]==0,"no early toilet commit");
  a.update(7000);check(count[0]==1&&a.mode().equals("eat")&&a.food().equals("猫条"),"food and unfinished eating resume");
  check(a.elapsed(7000)==2000,"animation resumes at saved progress rather than restarting");
  a.update(9999);check(count[0]==1,"remaining eating duration preserved");
  a.update(10000);check(count[0]==2&&a.mode().equals("happy"),"treat reaction once after actual eating completion");
  a.update(12500);a.update(12501);check(count[0]==3&&!a.busy(),"completion happens once");

  ActionTimeline clean=new ActionTimeline();int[] waste={2},cleared={-1};
  clean.start("clean","",7000,()->{cleared[0]=waste[0];waste[0]=0;},0);
  clean.interrupt("poop",5000,()->waste[0]+=3,4000);
  clean.update(9000);check(waste[0]==5&&clean.mode().equals("clean"),"new deposits included in resumed cleaning");
  clean.update(11999);check(cleared[0]==-1,"no early clear after interruption");
  clean.update(12000);check(cleared[0]==5&&waste[0]==0&&!clean.busy(),"seven seconds of cleaning then correct result");

  ActionTimeline same=new ActionTimeline();int[] events={0};
  same.start("eat","猫粮",5000,()->events[0]++,0);
  same.interrupt("pee",5000,()->events[0]++,5000);
  same.update(10000);check(events[0]==2&&!same.busy(),"simultaneous finish and due event lose neither completion");
  Set<String> observed=new HashSet<>();String mode="sit";Random random=new Random(17);
  for(int i=0;i<100;i++){String next=IdleMotion.next(mode,random);check(!next.equals(mode),"idle does not repeat");observed.add(next);mode=next;}
  check(observed.equals(new HashSet<>(Arrays.asList("rest","sit","groom","lie"))),"all three idle poses available");
  System.out.println("PASS: priority toilet, resumed feeding/reaction, interrupted cleaning counts, exact boundaries and automatic idle");
 }
}

