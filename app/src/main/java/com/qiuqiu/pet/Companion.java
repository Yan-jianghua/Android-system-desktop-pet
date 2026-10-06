package com.qiuqiu.pet;
import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Optional companionship progress, independent of food and litter history. */
final class Companion {
 final SharedPreferences p;int affection,pets;long adopted,lastPet;String visited;
 static final String[] MOODS={"想被摸摸","安静陪伴","有点好奇","想晒太阳","软乎乎的一天","今天很黏人"};
 static final String[] NOTES={"主人，今天也摸摸球球的头吧。","你忙你的，球球就在这里陪着你。","今天会遇到什么有趣的事情呢？","找个暖暖的角落，一起歇一会儿吧。","把今天的小烦恼交给球球，喵。","爸爸妈妈在的地方，就是球球的家。"};
 Companion(Context context){p=context.getSharedPreferences("qiuqiu-companion",0);affection=p.getInt("affection",0);pets=p.getInt("pets",0);adopted=p.getLong("adopted",System.currentTimeMillis());lastPet=p.getLong("lastPet",0);visited=p.getString("visited","");save();}
 static String day(long n){return new SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(new Date(n));}
 static int moodIndex(long n){return Integer.parseInt(day(n))%MOODS.length;}
 String mood(long n){return MOODS[moodIndex(n)];}
 String note(long n){return NOTES[moodIndex(n)];}
 String title(){return affection<20?"初见的小伙伴":affection<50?"熟悉的朋友":affection<80?"黏人的小棉袄":"最爱的家人";}
 long days(long n){return Math.max(1,(n-adopted)/86400000L+1);}
 boolean pet(long n){if(lastPet!=0&&n-lastPet<30000)return false;lastPet=n;pets++;affection=Math.min(100,affection+2);save();return true;}
 boolean visit(long n){String today=day(n);if(today.equals(visited))return false;visited=today;affection=Math.min(100,affection+3);save();return true;}
 void save(){p.edit().putInt("affection",affection).putInt("pets",pets).putLong("adopted",adopted).putLong("lastPet",lastPet).putString("visited",visited).commit();}
}
