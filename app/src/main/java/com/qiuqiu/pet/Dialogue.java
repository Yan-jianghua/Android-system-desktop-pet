package com.qiuqiu.pet;
import android.icu.util.ChineseCalendar;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import java.util.*;
import java.util.regex.*;

public final class Dialogue {
 static final String[] LUNAR_MONTHS={"正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","冬月","腊月"};
 static final String[] LUNAR_DAYS={"","初一","初二","初三","初四","初五","初六","初七","初八","初九","初十","十一","十二","十三","十四","十五","十六","十七","十八","十九","二十","廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十"};
 static final String[] WEEKDAYS={"星期日","星期一","星期二","星期三","星期四","星期五","星期六"};
 static ChineseCalendar lunar(long now){
  ChineseCalendar c=new ChineseCalendar(TimeZone.getTimeZone("Asia/Shanghai"));c.setTime(new Date(now));return c;
 }
 static String birthday(long now){
  ChineseCalendar c=lunar(now);if(c.get(ChineseCalendar.IS_LEAP_MONTH)!=0)return "";
  int m=c.get(Calendar.MONTH)+1,d=c.get(Calendar.DAY_OF_MONTH);
  if(m==9&&d==18)return "爸爸";if(m==12&&d==18)return "妈妈";return "";
 }
 static String celebration(long now){
  String who=birthday(now);if(!who.isEmpty())return who+"生日快乐";
  ChineseCalendar c=lunar(now);String f="";
  int m=c.get(Calendar.MONTH)+1,d=c.get(Calendar.DAY_OF_MONTH);
  if(c.get(ChineseCalendar.IS_LEAP_MONTH)==0){
   if(m==1&&d<=3)f="新年";
   else if(m==1&&d==15)f="元宵节";
   else if(m==2&&d==2)f="龙抬头";
   else if(m==5&&d==5)f="端午节";
   else if(m==7&&d==7)f="七夕节";
   else if(m==8&&d==15)f="中秋节";
   else if(m==9&&d==9)f="重阳节";
   else if(m==12&&d==8)f="腊八节";
   else if(m==12&&(d==23||d==24))f="小年";
   else {
    ChineseCalendar next=lunar(now);next.add(Calendar.DAY_OF_MONTH,1);
    if(next.get(Calendar.MONTH)==0&&next.get(Calendar.DAY_OF_MONTH)==1&&next.get(ChineseCalendar.IS_LEAP_MONTH)==0)f="新年";
   }
  }
  if(f.isEmpty())f=SolarTerms.festival(now);
  return f.isEmpty()?"":"祝爸爸妈妈"+f+"快乐";
 }
 static String dateContext(long now){
  java.util.Calendar solar=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Shanghai"));solar.setTimeInMillis(now);
  ChineseCalendar c=lunar(now);int month=c.get(Calendar.MONTH),day=c.get(Calendar.DAY_OF_MONTH);String leap=c.get(ChineseCalendar.IS_LEAP_MONTH)!=0?"闰":"";
  String lunarMonth=month>=0&&month<LUNAR_MONTHS.length?LUNAR_MONTHS[month]:(month+1)+"月";
  String lunarDay=day>0&&day<LUNAR_DAYS.length?LUNAR_DAYS[day]:String.valueOf(day);
  return "今天是公历"+solar.get(java.util.Calendar.YEAR)+"年"+(solar.get(java.util.Calendar.MONTH)+1)+"月"+solar.get(java.util.Calendar.DAY_OF_MONTH)+"日，"+WEEKDAYS[solar.get(java.util.Calendar.DAY_OF_WEEK)-1]+"；农历"+leap+lunarMonth+lunarDay+"。";
 }
 static boolean asksToday(String normalized){return normalized.contains("今天几月几日")||normalized.contains("今天几号")||normalized.contains("今天日期")||normalized.contains("今天是几月")||normalized.contains("今天农历")||normalized.contains("今天阴历")||normalized.contains("今天阳历")||normalized.contains("今天公历")||normalized.contains("今天星期")||normalized.equals("几月几日")||normalized.equals("几号")||normalized.equals("日期")||normalized.equals("农历")||normalized.equals("阴历")||normalized.equals("阳历")||normalized.equals("公历")||normalized.equals("星期几");}
 static boolean asksBirthdayCountdown(String normalized){return normalized.contains("生日")&&(normalized.contains("几天")||normalized.contains("多少天")||normalized.contains("多久")||normalized.contains("什么时候")||normalized.contains("哪天")||normalized.contains("日期"));}
 static String birthdayCountdown(String text,long now,List<String> memoryTexts){
  String query=normalize(text);if(!asksBirthdayCountdown(query))return null;
  if(query.contains("爸爸"))return countdownReply("爸爸",9,18,true,now);
  if(query.contains("妈妈"))return countdownReply("妈妈",12,18,true,now);
  BirthdayFact chosen=null;
  if(memoryTexts!=null)for(String memory:memoryTexts){BirthdayFact fact=parseBirthday(memory);if(fact==null)continue;
   boolean requestedQiuqiu=query.contains("球球"),factQiuqiu=normalize(fact.owner).contains("球球");
   if(requestedQiuqiu!=factQiuqiu)continue;
   if(chosen==null||fact.owner.equals("主人"))chosen=fact;
  }
  if(chosen!=null)return countdownReply(chosen.owner,chosen.month,chosen.day,chosen.lunar,now);
  String who=query.contains("球球")?"球球":query.contains("我的")||query.startsWith("我")?"主人":"谁";
  return who.equals("谁")?"主人是想问谁的生日呢？告诉球球姓名和生日日期，球球就能帮你计算。":"球球还不知道"+who+"的生日。请先告诉球球“记住"+who+"的生日是几月几日”。";
 }
 static BirthdayFact parseBirthday(String text){
  if(text==null||!text.contains("生日"))return null;Matcher matcher=Pattern.compile("(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*(?:日|号)?").matcher(text);if(!matcher.find())return null;
  int month=Integer.parseInt(matcher.group(1)),day=Integer.parseInt(matcher.group(2));String n=normalize(text);boolean lunar=n.contains("农历")||n.contains("阴历");if(month<1||month>12||day<1||day>(lunar?30:31))return null;
  String owner=n.contains("球球")?"球球":n.contains("爸爸")?"爸爸":n.contains("妈妈")?"妈妈":"主人";
  return new BirthdayFact(owner,month,day,lunar);
 }
 static String countdownReply(String owner,int month,int day,boolean lunarDate,long now){
  java.util.Calendar today=solarDay(now);java.util.Calendar target=lunarDate?nextLunar(month,day,today):nextSolar(month,day,today);if(target==null)return "球球暂时算不出这个生日日期，请检查日期是否正确。";
  int days=(int)((target.getTimeInMillis()-today.getTimeInMillis())/86400000L);String kind=lunarDate?"农历":"公历";String date=(target.get(java.util.Calendar.MONTH)+1)+"月"+target.get(java.util.Calendar.DAY_OF_MONTH)+"日";
  if(days==0)return "今天就是"+owner+"的生日，生日快乐！";
  return "距离"+owner+"的"+kind+(lunarDate?LUNAR_MONTHS[month-1]+LUNAR_DAYS[day]:month+"月"+day+"日")+"生日还有"+days+"天，对应公历"+date+"。";
 }
 static java.util.Calendar solarDay(long now){java.util.Calendar c=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Shanghai"));c.setTimeInMillis(now);c.set(java.util.Calendar.HOUR_OF_DAY,0);c.set(java.util.Calendar.MINUTE,0);c.set(java.util.Calendar.SECOND,0);c.set(java.util.Calendar.MILLISECOND,0);return c;}
 static java.util.Calendar nextSolar(int month,int day,java.util.Calendar today){for(int year=today.get(java.util.Calendar.YEAR);year<=today.get(java.util.Calendar.YEAR)+4;year++){java.util.Calendar target=(java.util.Calendar)today.clone();target.setLenient(false);try{target.set(java.util.Calendar.YEAR,year);target.set(java.util.Calendar.MONTH,month-1);target.set(java.util.Calendar.DAY_OF_MONTH,day);target.getTimeInMillis();if(!target.before(today))return target;}catch(IllegalArgumentException ignored){}}return null;}
 static java.util.Calendar nextLunar(int month,int day,java.util.Calendar today){for(int i=0;i<=400;i++){java.util.Calendar candidate=(java.util.Calendar)today.clone();candidate.add(java.util.Calendar.DAY_OF_MONTH,i);ChineseCalendar lc=lunar(candidate.getTimeInMillis());if(lc.get(ChineseCalendar.IS_LEAP_MONTH)==0&&lc.get(Calendar.MONTH)+1==month&&lc.get(Calendar.DAY_OF_MONTH)==day)return candidate;}return null;}
 static final class BirthdayFact {final String owner;final int month,day;final boolean lunar;BirthdayFact(String owner,int month,int day,boolean lunar){this.owner=owner;this.month=month;this.day=day;this.lunar=lunar;}}
 static String normalize(String text){return text==null?"":text.replaceAll("[\\s，。！？,.!?：:；;‘’'\"“”]","").toLowerCase(java.util.Locale.ROOT);}
 /** Returns null when the AI router may handle the question. */
 static String deterministicAnswer(String text,long now){
  String s=normalize(text);
  if(asksToday(s))return dateContext(now);
  switch(s){
   case "你最爱谁呀":case "你最爱谁":return "最爱爸爸妈妈";
   case "爸爸妈妈是谁":return "爸爸是闫江桦，妈妈是张春华";
   case "叫爸爸":return "爸爸";
   case "叫妈妈":return "妈妈";
   default:String who=birthday(now);return who.isEmpty()?null:"球球祝"+who+"生日快乐";
  }
 }
 static String answer(String text,long now){String result=deterministicAnswer(text,now);return result==null?"球球不知道":result;}
}
