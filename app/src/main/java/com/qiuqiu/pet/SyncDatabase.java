package com.qiuqiu.pet;
import android.content.Context;
import androidx.room.*;
@Database(entities={SyncOperation.class,SharedItem.class},version=1,exportSchema=false)
public abstract class SyncDatabase extends RoomDatabase {
 private static volatile SyncDatabase instance;
 public abstract SyncDao sync(); public abstract SharedItemDao shared();
 public static SyncDatabase get(Context c){if(instance==null)synchronized(SyncDatabase.class){if(instance==null)instance=Room.databaseBuilder(c.getApplicationContext(),SyncDatabase.class,"qiuqiu-sync.db").build();}return instance;}
}
