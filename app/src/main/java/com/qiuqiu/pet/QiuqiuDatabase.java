package com.qiuqiu.pet;

import android.content.Context;
import androidx.room.*;

@Database(entities={ChatMessage.class,MemoryRecord.class,TeachingRule.class},version=3,exportSchema=false)
public abstract class QiuqiuDatabase extends RoomDatabase {
 private static volatile QiuqiuDatabase instance;
 private static Context appContext;
 public abstract ChatDao chat();
 public abstract MemoryDao memory();
 public abstract TeachingDao teaching();
 public SyncDao sync(){return SyncDatabase.get(appContext).sync();}
 public SharedItemDao shared(){return SyncDatabase.get(appContext).shared();}
 static final androidx.room.migration.Migration MIGRATION_1_3=new androidx.room.migration.Migration(1,3){@Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db){}};
 static final androidx.room.migration.Migration MIGRATION_2_3=new androidx.room.migration.Migration(2,3){@Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db){}};
 public static QiuqiuDatabase get(Context context){
  appContext=context.getApplicationContext();
  if(instance==null)synchronized(QiuqiuDatabase.class){if(instance==null)instance=Room.databaseBuilder(context.getApplicationContext(),QiuqiuDatabase.class,"qiuqiu-ai.db").addMigrations(MIGRATION_1_3,MIGRATION_2_3).build();}
  return instance;
 }
}
