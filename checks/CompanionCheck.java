package com.qiuqiu.pet;
import android.content.Context;
public class CompanionCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  long n=1800000000000L;Context c=new Context();Companion a=new Companion(c);
  check(a.pet(n)&&a.affection==2,"pet earns affection");
  a=new Companion(c);check(!a.pet(n+29999)&&a.affection==2,"cooldown survives reopening");
  check(a.pet(n+30000)&&a.affection==4,"30 second boundary");
  check(a.visit(n)&&a.affection==7,"daily note reward");
  a=new Companion(c);check(!a.visit(n)&&a.affection==7,"daily reward persists");
  check(a.visit(n+86400000)&&a.affection==10,"next day reward");
  String note=a.note(n);check(note.equals(new Companion(c).note(n)),"same day's note is stable");
  for(int i=2;i<80;i++)a.pet(n+i*30000);
  check(new Companion(c).affection==100,"affection caps and persists");
  PetState care=new PetState(c);care.urine=8;care.poop=3;care.save();a.visit(n+2*86400000);
  care=new PetState(c);check(care.urine==8&&care.poop==3,"friendship cannot clear litter");
  System.out.println("PASS: companionship restart, cooldown boundary, daily notes/rewards, cap and separate care history");
 }
}
