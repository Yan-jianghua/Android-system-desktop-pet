package com.qiuqiu.pet;
import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** 经期记录与经期 / 排卵期 / 安全期预测，全部保存在手机本地。 */
public final class PeriodData {
 public enum Type { NORMAL, PERIOD, PREDICTED_PERIOD, FERTILE, SAFE }
 public static final class Period {
  public LocalDate start, end;
  Period(LocalDate s, LocalDate e){start=s;end=e;}
  boolean contains(LocalDate d){
   if(d.isBefore(start))return false;
   if(end!=null)return !d.isAfter(end);
   // 尚未标记"走了"：只把开始日到今天（含）视为实际经期，明天起仍按预测显示
   return !d.isAfter(LocalDate.now());
  }
  int length(){return end==null?-1:(int)(end.toEpochDay()-start.toEpochDay()+1);}
 }
 final SharedPreferences p;
 final ArrayList<Period> periods=new ArrayList<>();
 static final int DEFAULT_CYCLE=28, DEFAULT_LENGTH=5, MAX_PERIOD_DAYS=10;

 PeriodData(Context c){
  p=c.getSharedPreferences("qiuqiu-period",0);
  load();
  autoCloseOldPeriods();
 }
 /** 获取尚未标记"走了"的经期，没有则返回 null。 */
 Period openPeriod(){for(Period pe:periods)if(pe.end==null)return pe;return null;}
 /** 来了满 10 天仍未标记"走了"，自动把第 10 天记为"走了"。 */
 void autoCloseOldPeriods(){
  autoCloseOldPeriods(LocalDate.now());
 }
 void autoCloseOldPeriods(LocalDate today){
  boolean changed=false;
  for(Period pe:periods){
   if(pe.end==null){
    LocalDate day10=pe.start.plusDays(MAX_PERIOD_DAYS-1);
    if(!today.isBefore(day10.plusDays(1))){pe.end=day10;changed=true;}
   }
  }
  if(changed){sort();save();}
 }
 void load(){
  periods.clear();
  boolean changed=false;
  String raw=p.getString("periods","");
  if(!raw.isEmpty())for(String pair:raw.split(";")){
   String[] parts=pair.split(",",-1);
   try{
    LocalDate s=LocalDate.ofEpochDay(Long.parseLong(parts[0]));
    LocalDate e=parts.length>1&&!parts[1].isEmpty()?LocalDate.ofEpochDay(Long.parseLong(parts[1])):null;
    if(e!=null&&e.isAfter(s.plusDays(MAX_PERIOD_DAYS-1))){e=s.plusDays(MAX_PERIOD_DAYS-1);changed=true;}
    periods.add(new Period(s,e));
   }catch(Exception ignored){}
  }
  sort();
  if(changed)save();
 }
 void sort(){periods.sort((a,b)->a.start.compareTo(b.start));}
 void save(){
  StringBuilder b=new StringBuilder();
  for(Period pe:periods){
   b.append(pe.start.toEpochDay()).append(',');
   if(pe.end!=null)b.append(pe.end.toEpochDay());
   b.append(';');
  }
  p.edit().putString("periods",b.toString()).commit();
 }
 boolean hasData(){return !periods.isEmpty();}
 /** 平均月经周期：相邻两次“来了”的间隔天数，记录不足时默认 28 天。 */
 int cycleLength(){
  if(periods.size()<2)return DEFAULT_CYCLE;
  long sum=0;int n=0;
  for(int i=1;i<periods.size();i++){
   long gap=ChronoUnit.DAYS.between(periods.get(i-1).start,periods.get(i).start);
   if(gap>=15&&gap<=60){sum+=gap;n++;}
  }
  return n==0?DEFAULT_CYCLE:Math.round((float)sum/n);
 }
 /** 平均经期天数：“来了”到“走了”含首尾两天，默认 5 天。 */
 int periodLength(){
  long sum=0;int n=0;
  for(Period pe:periods){int l=pe.length();if(l>=1&&l<=MAX_PERIOD_DAYS){sum+=l;n++;}}
  return n==0?DEFAULT_LENGTH:Math.round((float)sum/n);
 }
 Period periodOn(LocalDate d){
  for(Period pe:periods)if(pe.contains(d))return pe;
  return null;
 }
 Period periodStarting(LocalDate d){for(Period pe:periods)if(pe.start.equals(d))return pe;return null;}
 Period periodEnding(LocalDate d){for(Period pe:periods)if(pe.end!=null&&pe.end.equals(d))return pe;return null;}
 boolean isActualStart(LocalDate d){return periodStarting(d)!=null;}
 /** 标记“来了”：开启新的一段经期；若已有未“走了”的经期，则把开始日期改到这一天。 */
 void markStart(LocalDate d){
  Period open=null;for(Period pe:periods)if(pe.end==null)open=pe;
  if(open!=null)open.start=d;
  else if(periodStarting(d)==null)periods.add(new Period(d,null));
  sort();save();
 }
 /** 标记“走了”：结束最近一段尚未结束的经期。无法配对时返回 false。 */
 boolean markEnd(LocalDate d){
  Period open=null;for(Period pe:periods)if(pe.end==null)open=pe;
  if(open==null){
   Period latest=periods.isEmpty()?null:periods.get(periods.size()-1);
   if(latest!=null&&!d.isBefore(latest.start)&&!d.isAfter(latest.start.plusDays(MAX_PERIOD_DAYS-1))){latest.end=d;sort();save();return true;}
   return false;
  }
  if(d.isBefore(open.start)||d.isAfter(open.start.plusDays(MAX_PERIOD_DAYS-1)))return false;
  open.end=d;save();return true;
 }
 /** 清除某一天的“来了 / 走了”标记。 */
 void clearDay(LocalDate d){
  Period st=periodStarting(d),en=periodEnding(d);
  if(st!=null)periods.remove(st);
  else if(en!=null)en.end=null;
  save();
 }
 LocalDate lastStart(){return periods.isEmpty()?null:periods.get(periods.size()-1).start;}
 /** 生成落在 [from,to] 内的预测经期开始日（最后一次实际开始日逐次加周期）。 */
 List<LocalDate> predictedStarts(LocalDate from,LocalDate to){
  ArrayList<LocalDate> out=new ArrayList<>();
  LocalDate last=lastStart();
  if(last==null)return out;
  int cycle=cycleLength();
  LocalDate d=last;
  int guard=0;
  while(d.isBefore(from)&&guard++<500)d=d.plusDays(cycle);
  guard=0;
  while(!d.isAfter(to)&&guard++<500){
   if(!isActualStart(d))out.add(d);
   d=d.plusDays(cycle);
  }
  return out;
 }
 /** 判断某一天的类型，用于日历着色。 */
 Type classify(LocalDate d){
  if(periodOn(d)!=null)return Type.PERIOD;
  int cycle=cycleLength(),len=periodLength();
  if(hasData()){
   List<LocalDate> starts=predictedStarts(d.minusDays(cycle+2),d.plusDays(19));
   for(LocalDate s:starts){
    if(!d.isBefore(s)&&d.isBefore(s.plusDays(len)))return Type.PREDICTED_PERIOD;
    LocalDate ov=s.minusDays(14); // 排卵通常发生在下次月经前 14 天
    if(!d.isBefore(ov.minusDays(5))&&!d.isAfter(ov.plusDays(4)))return Type.FERTILE;
   }
   for(Period pe:periods){
    LocalDate ov=pe.start.minusDays(14);
    if(!d.isBefore(ov.minusDays(5))&&!d.isAfter(ov.plusDays(4)))return Type.FERTILE;
   }
   if(!d.isBefore(periods.get(0).start.minusDays(19)))return Type.SAFE;
  }
  return Type.NORMAL;
 }
 /** 日历格子上的小字：来了 / 走了 / 排卵 / 预测开始。 */
 String dayLabel(LocalDate d){
  if(periodStarting(d)!=null)return "来";
  if(periodEnding(d)!=null)return "走";
  for(Period pe:periods)if(pe.start.minusDays(14).equals(d))return "卵";
  if(hasData()){
   int cycle=cycleLength();
   for(LocalDate s:predictedStarts(d.minusDays(cycle+2),d.plusDays(19))){
    if(s.minusDays(14).equals(d))return "卵";
    if(s.equals(d))return "预";
   }
  }
  return "";
 }
 /** 离今天最近的、今天或之后的预测经期开始日。 */
 LocalDate nextPredictedStart(LocalDate today){
  LocalDate last=lastStart();
  if(last==null)return null;
  int cycle=cycleLength();
  LocalDate d=last;
  int guard=0;
  while(d.isBefore(today)&&guard++<500)d=d.plusDays(cycle);
  return d;
 }
 /** 问候时附带的经期与排卵提醒；没有记录时返回空串。 */
 String greetingReminder(LocalDate today){
  if(lastStart()==null)return "";
  int cycle=cycleLength(),len=periodLength();
  LocalDate next=nextPredictedStart(today);
  if(periodOn(today)==null){
   if(next.equals(today))return "妈妈今天可能来例假，记得带卫生巾";
   if(next.equals(today.plusDays(1)))return "妈妈明天可能来例假，记得带卫生巾";
  }
  LocalDate prev=next;
  while(prev.isAfter(today))prev=prev.minusDays(cycle);
  // 预测经期窗口已开始但还没记录“来了”
  if(periodOn(today)==null&&!isActualStart(prev)&&!today.isBefore(prev)&&today.isBefore(prev.plusDays(len)))
   return "妈妈这几天可能来例假，记得带卫生巾哦";
  // 排卵日按下次预测经期前 14 天计算，前后 5 天视为预测排卵期。
  LocalDate ovulation=next.minusDays(14);
  if(today.equals(ovulation))return "妈妈今天可能是排卵日，球球提醒你留意身体状态哦";
  if(today.equals(ovulation.minusDays(1)))return "妈妈明天可能是排卵日，球球提醒你留意身体状态哦";
  if(!today.isBefore(ovulation.minusDays(5))&&!today.isAfter(ovulation.plusDays(4)))
   return "妈妈现在可能处于排卵期，球球提醒你留意身体状态哦";
  return "";
 }
}
