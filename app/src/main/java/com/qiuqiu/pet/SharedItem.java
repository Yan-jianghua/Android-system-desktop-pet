package com.qiuqiu.pet;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="shared_items")
public class SharedItem {
 @PrimaryKey @NonNull public String id="";
 public String category="";
 public String title="";
 public String body="";
 public String privacyScope="shared";
 public String status="active";
 public String createdBy="";
 public long createdAt;
 public long updatedAt;
 public long serverSeq;
 public boolean deleted;
}
