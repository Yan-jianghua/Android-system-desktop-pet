package com.qiuqiu.pet;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SyncRepository {
 public interface Callback{void done(boolean ok,String message);}
 private static final ExecutorService IO=Executors.newSingleThreadExecutor();
 private final Context context;private final SyncDatabase db;private final ServerConfig config;
 public SyncRepository(Context c){context=c.getApplicationContext();db=SyncDatabase.get(context);config=new ServerConfig(context);}
 static synchronized String hlc(Context c,long now){android.content.SharedPreferences p=c.getSharedPreferences("qiuqiu-hlc",0);long last=p.getLong("millis",0);int counter=p.getInt("counter",0);long millis=Math.max(now,last);counter=millis==last?counter+1:0;p.edit().putLong("millis",millis).putInt("counter",counter).commit();ServerConfig s=new ServerConfig(c);return String.format(Locale.ROOT,"%013d-%06d-%s",millis,counter,s.deviceId());}
 public static void enqueue(Context c,String entityType,String entityId,String actionType,JSONObject payload){enqueue(c,entityType,entityId,actionType,payload,false);}
 public static void enqueue(Context c,String entityType,String entityId,String actionType,JSONObject payload,boolean tombstone){
  Context app=c.getApplicationContext();long now=System.currentTimeMillis();ServerConfig cfg=new ServerConfig(app);
  SyncOperation op=new SyncOperation();op.opId=UUID.randomUUID().toString();op.entityType=entityType;op.entityId=entityId;op.actionType=actionType;op.payloadJson=payload.toString();op.hlc=hlc(app,now);op.createdAtLocal=now;op.tombstone=tombstone;op.actorId=cfg.actorId();op.deviceId=cfg.deviceId();
  IO.execute(()->{try{SyncDatabase.get(app).sync().insert(op);SyncJobService.schedule(app);if(cfg.configured())new SyncRepository(app).syncAsync(null);}catch(Throwable failure){cfg.error("同步队列暂时不可用；养成操作仍已保存在本机");}});
 }
 public int pendingCount(){return db.sync().pendingCount();}
 public List<SyncOperation> timeline(){return db.sync().timeline(100);}
 public void syncAsync(Callback callback){IO.execute(()->{try{syncNow();callback(callback,true,"同步完成");}catch(Exception e){config.error(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());callback(callback,false,"同步失败："+config.lastError());}});}
 private void callback(Callback c,boolean ok,String m){if(c!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(()->c.done(ok,m));}
 private JSONObject request(String method,String path,JSONObject body,String token)throws Exception{
  String endpoint=config.endpoint();if(endpoint.startsWith("http://")&&!isPrivateHttp(endpoint))throw new IOException("公网服务器必须使用 HTTPS");
  URL url=new URL(endpoint+path);HttpURLConnection h=(HttpURLConnection)url.openConnection();h.setRequestMethod(method);h.setConnectTimeout(10000);h.setReadTimeout(30000);h.setRequestProperty("Accept","application/json");if(token!=null&&!token.isEmpty())h.setRequestProperty("Authorization","Bearer "+token);
  if(body!=null){h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json; charset=utf-8");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream out=h.getOutputStream()){out.write(bytes);}}
  int status=h.getResponseCode();InputStream in=status>=400?h.getErrorStream():h.getInputStream();String text=read(in);JSONObject result=text.isEmpty()?new JSONObject():new JSONObject(text);if(status>=400)throw new IOException(result.optString("error","HTTP "+status));return result;
 }
 private boolean isPrivateHttp(String endpoint){try{String h=new URL(endpoint).getHost();return h.equals("localhost")||h.equals("127.0.0.1")||h.startsWith("10.")||h.startsWith("192.168.")||h.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*");}catch(Exception e){return false;}}
 private static String read(InputStream in)throws IOException{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>=0)b.write(buf,0,n);return b.toString("UTF-8");}
 public JSONObject health()throws Exception{return request("GET","/v1/health",null,null);}
 public JSONObject registerAccount(String username,String password,String name,String device)throws Exception{JSONObject r=request("POST","/v1/accounts/register",new JSONObject().put("username",username).put("password",password).put("displayName",name).put("deviceName",device).put("deviceToken",config.token()),null);applyAccount(r,username,name);return r;}
 public JSONObject loginAccount(String username,String password,String device)throws Exception{JSONObject r=request("POST","/v1/accounts/login",new JSONObject().put("username",username).put("password",password).put("deviceName",device),null);applyAccount(r,username,r.optString("displayName",username));return r;}
 void applyAccount(JSONObject r,String username,String name)throws Exception{config.accountIdentity(username,r.getString("accountToken"),name);if(r.has("deviceToken"))config.identity(r.getString("spaceId"),r.getString("actorId"),r.getString("deviceId"),r.getString("deviceToken"),name);}
 public JSONObject createSpace(String admin,String name,String device)throws Exception{
  String auth=config.accountLoggedIn()?config.accountToken():admin;JSONObject b=new JSONObject().put("petName","球球").put("displayName",name).put("deviceName",device);JSONObject r=request("POST","/v1/spaces",b,auth);config.identity(r.getString("spaceId"),r.getString("actorId"),r.getString("deviceId"),r.getString("deviceToken"),name);return r;
 }
 public JSONObject createInvite()throws Exception{return request("POST","/v1/invites",new JSONObject().put("publicBaseUrl",config.endpoint()),config.token());}
 public JSONObject acceptInvite(String invite,String name,String device)throws Exception{JSONObject r=request("POST","/v1/invites/accept",new JSONObject().put("inviteToken",invite).put("accountToken",config.accountToken()).put("displayName",name).put("deviceName",device),null);config.identity(r.getString("spaceId"),r.getString("actorId"),r.getString("deviceId"),r.getString("deviceToken"),name);return r;}
 public JSONObject devices()throws Exception{return request("GET","/v1/devices",null,config.token());}
 public JSONObject conflicts()throws Exception{return request("GET","/v1/conflicts",null,config.token());}
 public JSONObject revokeDevice(String id)throws Exception{return request("DELETE","/v1/devices/"+URLEncoder.encode(id,"UTF-8"),null,config.token());}
 public JSONObject freezeRelation()throws Exception{return request("POST","/v1/relation/freeze",new JSONObject(),config.token());}
 public JSONObject resumeRelation()throws Exception{return request("POST","/v1/relation/resume",new JSONObject(),config.token());}
 public void syncNow()throws Exception{
  if(!config.configured())throw new IllegalStateException("尚未连接球球服务器");
  List<SyncOperation> pending=db.sync().pending(500);JSONArray arr=new JSONArray();for(SyncOperation op:pending)arr.put(toJson(op));
  if(arr.length()>0){JSONObject pushed=request("POST","/v1/sync/push",new JSONObject().put("events",arr),config.token());JSONArray results=pushed.getJSONArray("results");for(int i=0;i<results.length();i++){JSONObject r=results.getJSONObject(i);String status=r.optString("status");String state=(status.equals("accepted")||status.equals("duplicate"))?"synced":status;db.sync().finish(r.getString("opId"),state,r.optLong("serverSeq"),r.optString("reason",status));}}
  boolean more=true;long seq=config.lastServerSeq();while(more){JSONObject pulled=request("GET","/v1/sync/pull?after="+seq+"&limit=200",null,config.token());JSONArray events=pulled.getJSONArray("events");for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);applyRemote(e);seq=Math.max(seq,e.getLong("serverSeq"));}more=pulled.optBoolean("hasMore",false);if(events.length()==0)break;}
  config.checkpoint(seq);
 }
 JSONObject toJson(SyncOperation o)throws JSONException{return new JSONObject().put("opId",o.opId).put("entityType",o.entityType).put("entityId",o.entityId).put("actionType",o.actionType).put("payload",new JSONObject(o.payloadJson)).put("hlc",o.hlc).put("baseVersion",o.baseVersion).put("schemaVersion",o.schemaVersion).put("createdAtLocal",o.createdAtLocal).put("tombstone",o.tombstone);}
 void applyRemote(JSONObject e)throws JSONException{
  SyncOperation op=new SyncOperation();op.opId=e.getString("opId");op.entityType=e.getString("entityType");op.entityId=e.getString("entityId");op.actionType=e.getString("actionType");op.payloadJson=e.getJSONObject("payload").toString();op.hlc=e.getString("hlc");op.createdAtLocal=e.getLong("createdAtLocal");op.tombstone=e.optBoolean("tombstone");op.syncState="synced";op.serverSeq=e.getLong("serverSeq");op.result=e.optString("result","accepted");op.actorId=e.optString("actorId");op.deviceId=e.optString("deviceId");long inserted=db.sync().insert(op);
  if(inserted==-1)return;
  if(op.entityType.equals("feature")){JSONObject p=e.getJSONObject("payload");SharedItem item=new SharedItem();item.id=op.entityId;item.category=p.optString("category","共同回忆");item.title=p.optString("title","");item.body=p.optString("body","");item.privacyScope=p.optString("privacyScope","shared");item.status=p.optString("status","active");item.createdBy=op.actorId;item.createdAt=p.optLong("createdAt",op.createdAtLocal);item.updatedAt=op.createdAtLocal;item.serverSeq=op.serverSeq;item.deleted=op.tombstone;db.shared().save(item);if(!item.deleted&&"_chat".equals(item.category)&&!item.createdBy.equals(config.actorId()))MessageNotifier.received(context,item);}
  if(op.entityType.equals("care")){JSONObject p=e.getJSONObject("payload");if(op.actionType.equals("pet")||op.actionType.equals("visit"))Companion.applyRemote(context,op.actionType,p,op.createdAtLocal);else PetState.applyRemote(context,op.actionType,p,op.createdAtLocal);}
  if(op.entityType.equals("period"))PeriodData.applyRemote(context,op.actionType,e.getJSONObject("payload"));
 }
}
