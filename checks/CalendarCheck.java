package com.qiuqiu.pet;
import java.time.*;
import java.util.*;
public class CalendarCheck {
 static long date(String s){return LocalDate.parse(s).atTime(12,0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();}
 static void eq(String a,String b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(Dialogue.dateContext(date("2026-10-07")),"今天是公历2026年10月7日，星期三；农历八月廿七。");
  eq(Dialogue.answer("今天几号？",date("2026-10-07")),"今天是公历2026年10月7日，星期三；农历八月廿七。");
  eq(Dialogue.birthdayCountdown("爸爸还有几天生日？",date("2026-10-07"),Collections.emptyList()),"距离爸爸的农历九月十八生日还有20天，对应公历10月27日。");
  eq(Dialogue.birthdayCountdown("我的生日还有几天？",date("2026-10-07"),Arrays.asList("我的生日是10月10日")),"距离主人的公历10月10日生日还有3天，对应公历10月10日。");
  eq(Dialogue.birthdayCountdown("我的生日还有多少天？",date("2026-10-07"),Arrays.asList("我的生日是10月10日")),"距离主人的公历10月10日生日还有3天，对应公历10月10日。");
  eq(Dialogue.birthdayCountdown("我的农历生日什么时候？",date("2026-10-07"),Arrays.asList("我的生日是农历9月18日")),"距离主人的农历九月十八生日还有20天，对应公历10月27日。");
  eq(Dialogue.birthdayCountdown("还有几天生日？",date("2026-10-07"),Collections.emptyList()),"主人是想问谁的生日呢？告诉球球姓名和生日日期，球球就能帮你计算。");
  eq(Dialogue.celebration(date("2026-10-27")),"爸爸生日快乐");
  eq(Dialogue.answer("你好吗",date("2026-10-27")),"球球祝爸爸生日快乐");
  eq(Dialogue.celebration(date("2026-02-05")),"妈妈生日快乐");
  eq(Dialogue.answer("喵",date("2026-02-05")),"球球祝妈妈生日快乐");
  eq(Dialogue.celebration(date("2027-01-25")),"妈妈生日快乐");
  eq(Dialogue.celebration(date("2026-02-16")),"祝爸爸妈妈新年快乐");
  eq(Dialogue.celebration(date("2026-02-17")),"祝爸爸妈妈新年快乐");
  eq(Dialogue.celebration(date("2026-03-03")),"祝爸爸妈妈元宵节快乐");
  eq(Dialogue.celebration(date("2026-06-19")),"祝爸爸妈妈端午节快乐");
  eq(Dialogue.celebration(date("2026-09-25")),"祝爸爸妈妈中秋节快乐");
  eq(Dialogue.celebration(date("2026-10-18")),"祝爸爸妈妈重阳节快乐");
  eq(Dialogue.celebration(date("2026-04-05")),"祝爸爸妈妈清明节快乐");
  eq(Dialogue.celebration(date("2026-12-22")),"祝爸爸妈妈冬至快乐");
  eq(Dialogue.celebration(date("2021-12-21")),"祝爸爸妈妈冬至快乐");
  eq(Dialogue.answer("你最爱谁呀？",date("2026-10-27")),"最爱爸爸妈妈");
  eq(Dialogue.answer("爸爸妈妈是谁",date("2026-10-06")),"爸爸是闫江桦，妈妈是张春华");
  eq(Dialogue.answer("叫爸爸",date("2026-10-06")),"爸爸");
  eq(Dialogue.answer("叫妈妈",date("2026-10-06")),"妈妈");
  eq(Dialogue.answer("球球你好！",date("2026-10-06")),"球球不知道");
  eq(Dialogue.birthday(date("2026-10-26")),"");
  eq(Dialogue.birthday(date("2026-10-28")),"");
  System.out.println("PASS: annual lunar birthdays, birthday fallback, NY eve, festivals and all family replies");
 }
}
