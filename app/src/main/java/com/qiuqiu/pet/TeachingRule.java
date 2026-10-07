package com.qiuqiu.pet;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName="teaching_rules")
public class TeachingRule {
 @PrimaryKey(autoGenerate=true) public long id;
 public String question="";
 public String normalizedQuestion="";
 public String answer="";
 public long createdAt;
 public long updatedAt;
 public TeachingRule(){}
 TeachingRule(String question,String answer,long now){this.question=question;normalizedQuestion=Dialogue.normalize(question);this.answer=answer;createdAt=updatedAt=now;}
}
