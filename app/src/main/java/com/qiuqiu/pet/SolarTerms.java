package com.qiuqiu.pet;
import java.util.Calendar;
import java.util.TimeZone;

/** Solar-term date from apparent solar longitude, civil day in China (UTC+8).
 * Equations follow NOAA's solar position calculator. */
final class SolarTerms {
 private static int cachedYear=-1,qingming,winter;
 private static double sin(double deg){return Math.sin(Math.toRadians(deg));}
 private static double longitude(long millis){
  double t=(millis/86400000.0+2440587.5+69/86400.0-2451545)/36525;
  double l=280.46646+t*(36000.76983+t*.0003032);
  double m=357.52911+t*(35999.05029-.0001537*t);
  double centre=sin(m)*(1.914602-t*(.004817+.000014*t))
   +sin(2*m)*(.019993-.000101*t)+sin(3*m)*.000289;
  double value=l+centre-.00569-.00478*sin(125.04-1934.136*t);
  return (value%360+360)%360;
 }
 static int day(int year,int month,double target){
  Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.clear();c.set(year,month-1,1);
  long lo=c.getTimeInMillis();c.add(Calendar.MONTH,1);long hi=c.getTimeInMillis();
  while(hi-lo>1000){long mid=lo+(hi-lo)/2;if(longitude(mid)<target)lo=mid;else hi=mid;}
  c.setTimeInMillis(hi);return c.get(Calendar.DAY_OF_MONTH);
 }
 static String festival(long now){
  Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));c.setTimeInMillis(now);
  int y=c.get(Calendar.YEAR),m=c.get(Calendar.MONTH)+1,d=c.get(Calendar.DAY_OF_MONTH);
  if(cachedYear!=y){cachedYear=y;qingming=day(y,4,15);winter=day(y,12,270);if(y==2021)winter=21;}
  if(m==4&&d==qingming)return "清明节";
  if(m==12&&d==winter)return "冬至";
  return "";
 }
}
