package com.qiuqiu.pet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="chat_messages")
public class ChatMessage {
 @PrimaryKey(autoGenerate=true) public long id;
 public String role="";
 public String content="";
 public long createdAt;
 public String status="complete";
 public String errorType="";
 public String requestId="";
 public ChatMessage(){}
 ChatMessage(String role,String content,long at,String status,String requestId){this.role=role;this.content=content;createdAt=at;this.status=status;this.requestId=requestId;}
}
