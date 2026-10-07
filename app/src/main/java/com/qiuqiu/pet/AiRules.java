package com.qiuqiu.pet;

/** Pure, locally testable rules for explicit memory commands and suggestions. */
final class AiRules {
 static String afterPrefix(String text,String prefix){String compact=text==null?"":text.trim();if(!compact.startsWith(prefix))return null;return compact.substring(prefix.length());}
 static String trimLead(String value){return value==null?"":value.replaceFirst("^[\\s，,：:]+","").trim();}
 static boolean isSensitive(String value){return value!=null&&value.matches(".*(住址|地址|身份证|密码|银行卡|账号|病历|疾病|收入|电话|手机号).* ".trim());}
 static boolean shouldRetryOnce(boolean retryable,int attempts,boolean canceled){return retryable&&attempts==0&&!canceled;}
 static String memoryConflictTopic(String content){
  String value=Dialogue.normalize(content).replaceFirst("^(不是|纠正)","");
  if(value.startsWith("我最喜欢")||value.startsWith("我喜欢"))return "preference";
  if(value.startsWith("我不喜欢"))return "dislike";
  if(value.startsWith("我叫"))return "name";
  if(value.startsWith("我的生日")||value.startsWith("我生日"))return "birthday";
  if(value.startsWith("我妈妈喜欢"))return "mother_preference";
  if(value.startsWith("我爸爸喜欢"))return "father_preference";
  return "";
 }
 static String candidateMemory(String input){if(input==null)return null;String value=input.trim();for(String prefix:new String[]{"我喜欢","我最喜欢","我不喜欢","我叫","我的生日是","我妈妈喜欢","我爸爸喜欢"})if(value.startsWith(prefix)&&value.length()>prefix.length())return value;return null;}
 private AiRules(){}
}
