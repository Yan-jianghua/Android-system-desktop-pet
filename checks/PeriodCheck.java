package com.qiuqiu.pet;
import android.content.Context;
import java.time.LocalDate;

public class PeriodCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
 static void history(Context context,String start1,String end1,String start2,String end2){
  context.getSharedPreferences("qiuqiu-period",0).edit().putString("periods",
   LocalDate.parse(start1).toEpochDay()+","+LocalDate.parse(end1).toEpochDay()+";"+
   LocalDate.parse(start2).toEpochDay()+","+LocalDate.parse(end2).toEpochDay()+";").commit();
 }
 public static void main(String[] args){
  LocalDate start=LocalDate.of(2026,10,1);
  PeriodData p=new PeriodData(new Context());
  p.markStart(start);
  check(!p.markEnd(start.plusDays(10)),"eleventh day must be rejected");
  check(p.markEnd(start.plusDays(9)),"tenth day should be allowed");
  check(p.periodLength()==10,"inclusive period length should cap at ten");

  PeriodData spacing=new PeriodData(new Context());
  LocalDate first=LocalDate.of(2026,1,1);
  check(spacing.markStart(first),"first period should be allowed");
  check(spacing.markEnd(first.plusDays(4)),"first period should close");
  check(!spacing.markStart(first.plusDays(14)),"a second start within fourteen days must be rejected");
  check(spacing.markStart(first.plusDays(15)),"a second start exactly fifteen days later should be allowed");
  check(!spacing.markStart(first.plusDays(1)),"backdated starts must also respect the fifteen-day gap");

  Context legacy=new Context();
  history(legacy,"2026-09-01","2026-09-20","2026-09-29","2026-10-01");
  PeriodData normalized=new PeriodData(legacy);
  check(normalized.periods.get(0).length()==10,"legacy period should be clamped to ten days");
  check(normalized.periods.get(0).end.equals(LocalDate.of(2026,9,10)),"legacy end date should be normalized");

  Context reminders=new Context();
  history(reminders,"2026-09-01","2026-09-05","2026-09-29","2026-10-03");
  PeriodData data=new PeriodData(reminders);
  eq("妈妈明天可能来例假，记得带卫生巾",data.greetingReminder(LocalDate.of(2026,10,26)));
  eq("妈妈今天可能来例假，记得带卫生巾",data.greetingReminder(LocalDate.of(2026,10,27)));
  eq("妈妈明天可能是排卵日，球球提醒你留意身体状态哦",data.greetingReminder(LocalDate.of(2026,10,12)));
  eq("妈妈今天可能是排卵日，球球提醒你留意身体状态哦",data.greetingReminder(LocalDate.of(2026,10,13)));
  eq("妈妈现在可能处于排卵期，球球提醒你留意身体状态哦",data.greetingReminder(LocalDate.of(2026,10,15)));
  System.out.println("PASS: ten-day maximum, fifteen-day start gap, legacy normalization, predicted period and ovulation reminders");
 }
}
