package com.qiuqiu.pet;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

public final class ServerConfig {
 public static final String DEFAULT_ENDPOINT="https://yanjianghua-vmware-virtual-platform.tailaffa3b.ts.net";
 private final SharedPreferences p;
 ServerConfig(Context context){p=context.getSharedPreferences("qiuqiu-server",0);ensureLocalDevice();}
 void ensureLocalDevice(){if(p.getString("localDeviceId","").isEmpty())p.edit().putString("localDeviceId",UUID.randomUUID().toString()).apply();}
 public String endpoint(){String v=p.getString("endpoint",DEFAULT_ENDPOINT).trim();while(v.endsWith("/"))v=v.substring(0,v.length()-1);return v;}
 public String token(){return p.getString("deviceToken","");}
 public String spaceId(){return p.getString("spaceId","");}
 public String actorId(){return p.getString("actorId","");}
 public String deviceId(){String remote=p.getString("deviceId","");return remote.isEmpty()?p.getString("localDeviceId",""):remote;}
 public String displayName(){return p.getString("displayName","主人");}
 public String accountToken(){return p.getString("accountToken","");}
 public String accountName(){return p.getString("accountName","");}
 public boolean accountLoggedIn(){return !accountToken().isEmpty();}
 public long lastServerSeq(){return p.getLong("lastServerSeq",0);}
 public long lastSync(){return p.getLong("lastSync",0);}
 public String lastError(){return p.getString("lastError","");}
 public boolean configured(){return !endpoint().isEmpty()&&!token().isEmpty();}
 public void endpoint(String value){p.edit().putString("endpoint",value.trim()).apply();}
 public void identity(String space,String actor,String device,String token,String name){p.edit().putString("spaceId",space).putString("actorId",actor).putString("deviceId",device).putString("deviceToken",token).putString("displayName",name).apply();}
 public void accountIdentity(String username,String token,String name){p.edit().putString("accountName",username).putString("accountToken",token).putString("displayName",name).apply();}
 public void checkpoint(long seq){p.edit().putLong("lastServerSeq",seq).putLong("lastSync",System.currentTimeMillis()).putString("lastError","").apply();}
 public void error(String value){p.edit().putString("lastError",value).apply();}
 public void clearIdentity(){p.edit().remove("spaceId").remove("actorId").remove("deviceId").remove("deviceToken").remove("lastServerSeq").apply();}
 public void logout(){clearIdentity();p.edit().remove("accountName").remove("accountToken").apply();}
}
