package com.qiuqiu.pet;

import org.json.JSONObject;
import android.util.Base64;

final class ChatMedia {
 static final String MARK="[[qiuqiu-media:";
 static final class Item {String id,mime,name;long size;}
 static String body(String id,String mime,String name,long size){try{JSONObject j=new JSONObject().put("id",id).put("mime",mime).put("name",name).put("size",size);return MARK+Base64.encodeToString(j.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),Base64.NO_WRAP)+"]]";}catch(Exception e){return "";}}
 static Item parse(String body){try{int a=body.indexOf(MARK),b=body.indexOf("]]",a);if(a<0||b<0)return null;JSONObject j=new JSONObject(new String(Base64.decode(body.substring(a+MARK.length(),b),Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8));Item item=new Item();item.id=j.getString("id");item.mime=j.getString("mime");item.name=j.optString("name","附件");item.size=j.optLong("size");return item;}catch(Exception e){return null;}}
 private ChatMedia(){}
}
