package com.qiuqiu.pet;

import android.content.*;
import android.net.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class AiClient {
 static final class Message {final String role,content;Message(String role,String content){this.role=role;this.content=content;}}
 static final class Failure extends Exception {
  final String type,userMessage;final boolean retryable;
  Failure(String type,String userMessage,boolean retryable){super(userMessage);this.type=type;this.userMessage=userMessage;this.retryable=retryable;}
 }
 final Context context;final AiSettings settings;volatile HttpURLConnection active;
 AiClient(Context context,AiSettings settings){this.context=context.getApplicationContext();this.settings=settings;}
 void cancel(){HttpURLConnection connection=active;if(connection!=null)connection.disconnect();active=null;}
 String complete(List<Message> messages,String requestId) throws Failure {
  if(settings.wifiOnly()&&!onWifi())throw new Failure("wifi_required","球球设置为仅在 Wi-Fi 下聊天。",false);
  String endpoint=settings.endpoint();String url=endpoint.endsWith("/chat/completions")?endpoint:endpoint+"/chat/completions";
  try{
   HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();active=c;c.setRequestMethod("POST");c.setConnectTimeout(10000);c.setReadTimeout(30000);c.setDoOutput(true);
   c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Accept","application/json");c.setRequestProperty("X-Client-Request-Id",requestId);
   String key=settings.apiKey();if(!key.isEmpty())c.setRequestProperty("Authorization","Bearer "+key);
   JSONObject body=new JSONObject().put("model",settings.model()).put("temperature",0.7).put("max_tokens",Math.max(64,settings.maxOutput()));JSONArray array=new JSONArray();
   for(Message m:messages)array.put(new JSONObject().put("role",m.role).put("content",m.content));body.put("messages",array);
   byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);
   try(OutputStream output=c.getOutputStream()){output.write(bytes);}
   int status=c.getResponseCode();String response=read(status>=200&&status<300?c.getInputStream():c.getErrorStream());
   if(status<200||status>=300)throw httpFailure(status,response);
   JSONObject json=new JSONObject(response);JSONArray choices=json.optJSONArray("choices");
   if(choices==null||choices.length()==0)throw new Failure("empty_response","球球没有收到有效回答。",true);
   JSONObject message=choices.getJSONObject(0).optJSONObject("message");String text=message==null?"":message.optString("content","").trim();
   if(text.isEmpty())throw new Failure("empty_response","球球没有收到有效回答。",true);
   return limit(text,settings.maxOutput());
  }catch(Failure e){throw e;}catch(SocketTimeoutException e){throw new Failure("timeout","球球想了太久，稍后再聊喵。",false);}
  catch(UnknownHostException|ConnectException e){throw new Failure("network","球球现在连不上网络，等会儿再聊喵。",true);}
  catch(JSONException e){throw new Failure("bad_response","AI 返回格式不正确。",true);}
  catch(IOException e){throw new Failure("network","球球现在连不上网络，等会儿再聊喵。",false);}
  finally{HttpURLConnection c=active;if(c!=null)c.disconnect();active=null;}
 }
 boolean onWifi(){ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);return caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);}
 static Failure httpFailure(int status,String body){
  String detail="";try{JSONObject error=new JSONObject(body).optJSONObject("error");if(error!=null)detail=error.optString("message","");}catch(Exception ignored){}
  if(status==401||status==403)return new Failure("auth","AI 鉴权失败，请检查 API Key。",false);
  if(status==404)return new Failure("not_found","没有找到接口或模型，请检查地址和模型名称。",false);
  if(status==429)return new Failure("quota","AI 请求过多或额度不足，请稍后再试并检查账户。",false);
  if(status>=500)return new Failure("server","AI 服务暂时不可用，稍后再聊喵。",false);
  return new Failure("http_"+status,detail.isEmpty()?"AI 请求失败（"+status+"）。":"AI 请求失败："+limit(detail,100),false);
 }
 static String read(InputStream input) throws IOException {if(input==null)return "";try(input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n,total=0;while((n=input.read(buffer))!=-1){total+=n;if(total>1048576)throw new IOException("response too large");out.write(buffer,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
 static String limit(String text,int max){String clean=text.replaceAll("(?is)<script.*?</script>","").trim();return clean.length()<=max?clean:clean.substring(0,max).trim()+"…";}
}
