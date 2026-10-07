package com.qiuqiu.pet;

public final class AiRulesCheck {
 static void eq(Object actual,Object expected){if(!java.util.Objects.equals(actual,expected))throw new AssertionError("expected "+expected+" but got "+actual);}
 static void ok(boolean value){if(!value)throw new AssertionError("expected true");}
 public static void main(String[] args){
  eq(AiRules.afterPrefix("  记住，我喜欢拿铁","记住"),"，我喜欢拿铁");
  eq(AiRules.trimLead("，我喜欢拿铁"),"我喜欢拿铁");
  eq(AiRules.candidateMemory("我不喜欢香菜"),"我不喜欢香菜");
  eq(AiRules.candidateMemory("今天天气不错"),null);
  ok(AiRules.isSensitive("我的住址是这里"));ok(!AiRules.isSensitive("我喜欢拿铁"));
  ok(AiRules.shouldRetryOnce(true,0,false));ok(!AiRules.shouldRetryOnce(true,1,false));ok(!AiRules.shouldRetryOnce(true,0,true));ok(!AiRules.shouldRetryOnce(false,0,false));
  eq(AiRules.memoryConflictTopic("我喜欢拿铁"),AiRules.memoryConflictTopic("我最喜欢美式"));
  eq(AiRules.memoryConflictTopic("我的生日是10月1日"),AiRules.memoryConflictTopic("我的生日是12月1日"));
  ok(!AiRules.memoryConflictTopic("我不喜欢香菜").equals(AiRules.memoryConflictTopic("我喜欢香菜")));
  System.out.println("PASS: explicit memory commands, candidate suggestions and sensitive-data guard");
 }
}
