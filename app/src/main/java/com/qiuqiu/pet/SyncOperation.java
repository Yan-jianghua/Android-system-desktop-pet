package com.qiuqiu.pet;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="sync_operations")
public class SyncOperation {
 @PrimaryKey @NonNull public String opId="";
 public String entityType="";
 public String entityId="";
 public String actionType="";
 public String payloadJson="{}";
 public String hlc="";
 public long baseVersion;
 public int schemaVersion=1;
 public long createdAtLocal;
 public boolean tombstone;
 public String syncState="pending";
 public long serverSeq;
 public String result="";
 public String actorId="";
 public String deviceId="";
}
