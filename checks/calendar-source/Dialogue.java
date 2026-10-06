package com.qiuqiu.pet;
import com.ibm.icu.util.ChineseCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import java.util.Date;

public final class Dialogue {
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
 static String answer(String text,long now){
  String s=text.replaceAll("[\\s，。！？,.!?]","");
  switch(s){
   case "你最爱谁呀":case "你最爱谁":return "最爱爸爸妈妈";
   case "爸爸妈妈是谁":return "爸爸是闫江桦，妈妈是张春华";
   case "叫爸爸":return "爸爸";
   case "叫妈妈":return "妈妈";
   default:String who=birthday(now);return who.isEmpty()?"球球不知道":"球球祝"+who+"生日快乐";
  }
 }
}

