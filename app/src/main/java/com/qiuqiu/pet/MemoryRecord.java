package com.qiuqiu.pet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="memories")
public class MemoryRecord {
 @PrimaryKey(autoGenerate=true) public long id;
 public String content="";
 public String type="偏好习惯";
 public String person="主人";
 public long createdAt;
 public long updatedAt;
 public long sourceMessageId;
 public String status="active";
 public long lastUsedAt;
 public int importance=1;
 public long expiresAt;
 public MemoryRecord(){}
 MemoryRecord(String content,String type,long now){this.content=content;this.type=type;createdAt=updatedAt=now;}
}
