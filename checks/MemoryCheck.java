package com.qiuqiu.pet;
import android.content.Context;
public class MemoryCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static final long BASE=1800000000000L;
 static PetState setup(Context c){PetState s=new PetState(c);s.nextUrine=BASE+2*PetState.H;s.nextPoop=BASE+6*PetState.H;s.feeds.clear();s.feedUntil=0;s.save();return s;}
 static void drain(PetState s,long until){PetState.Event e;while((e=s.nextDue(until))!=null)s.finish(e);}
 public static void main(String[] args){
  for(int count=0;count<=4;count++){
   Context c=new Context();PetState s=setup(c);
   for(int j=0;j<count;j++)check(s.feed(BASE+j*10000),"10 second boundary accepts food");
   check(s.nextDue(BASE+2*PetState.H-1)==null,"no early event");
   PetState.Event interrupted=s.nextDue(BASE+2*PetState.H);
   PetState reopened=new PetState(c);check(reopened.urine==0,"interrupt before finish leaves no deposit");
   check(reopened.nextDue(BASE+2*PetState.H).at==interrupted.at,"interrupted event resumes");
   reopened.finish(interrupted);reopened.finish(interrupted);
   check(new PetState(c).urine==1,"committed urine persists exactly once");
   drain(reopened,BASE+6*PetState.H);
   PetState again=new PetState(c);check(again.urine==3,"three two-hour events");
   check(again.poop==(count<=1?1:count<=3?2:3),"0,1,2,3,4 feeding piece counts");
   check(again.feeds.isEmpty(),"old window trimmed");
   long next=again.nextPoop;again.clear();
   PetState cleaned=new PetState(c);check(cleaned.poop==0&&cleaned.urine==0,"clean persists");
   check(cleaned.nextPoop==next,"clean never resets interval");
  }
  Context c=new Context();PetState s=setup(c);s.feed(BASE);
  check(!new PetState(c).feed(BASE+9999),"cooldown survives restart");
  check(new PetState(c).feed(BASE+10000),"cooldown expires exactly at 10 seconds");
  s=setup(new Context());s.feeds.add(BASE-1);s.feeds.add(BASE);s.feeds.add(BASE+6*PetState.H);s.feeds.add(BASE+6*PetState.H);
  drain(s,BASE+6*PetState.H);check(s.poop==1,"window excludes previous food and endpoint");
  drain(s,BASE+12*PetState.H);check(s.poop==3,"endpoint belongs to next six-hour window");
  c=new Context();s=setup(c);drain(s,BASE+32*PetState.H);
  check(new PetState(c).urine==16&&new PetState(c).poop==5,"offline catch-up and reopen preserve every due event");
  System.out.println("PASS: interrupted/resumed events, no duplicates, all feeding boundaries, cooldown, windows, persisted counts and cleaning");
 }
}
