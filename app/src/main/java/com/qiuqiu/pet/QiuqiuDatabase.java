package com.qiuqiu.pet;

import android.content.Context;
import androidx.room.*;

@Database(entities={ChatMessage.class,MemoryRecord.class,TeachingRule.class},version=1,exportSchema=false)
public abstract class QiuqiuDatabase extends RoomDatabase {
 private static volatile QiuqiuDatabase instance;
 public abstract ChatDao chat();
 public abstract MemoryDao memory();
 public abstract TeachingDao teaching();
 public static QiuqiuDatabase get(Context context){
  if(instance==null)synchronized(QiuqiuDatabase.class){if(instance==null)instance=Room.databaseBuilder(context.getApplicationContext(),QiuqiuDatabase.class,"qiuqiu-ai.db").build();}
  return instance;
 }
}
