package com.qiuqiu.pet;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** AI configuration. The API key is encrypted by a non-exportable Android Keystore key. */
public final class AiSettings {
 static final String PREFS="qiuqiu-ai-settings",ALIAS="qiuqiu.ai.api-key";
 final SharedPreferences p;
 public AiSettings(Context c){p=c.getSharedPreferences(PREFS,0);}
 public boolean enabled(){return p.getBoolean("enabled",false);}
 public String endpoint(){return p.getString("endpoint","https://api.openai.com/v1").trim();}
 public String model(){return p.getString("model","gpt-4.1-mini").trim();}
 public int maxOutput(){return Math.max(80,Math.min(600,p.getInt("maxOutput",240)));}
 public boolean wifiOnly(){return p.getBoolean("wifiOnly",false);}
 public boolean hasKey(){return !p.getString("key","").isEmpty();}
 public void save(boolean enabled,String endpoint,String model,int maxOutput,boolean wifiOnly,String newKey) throws Exception {
  if(endpoint==null||endpoint.isBlank())throw new IllegalArgumentException("请填写接口地址");
  if(!endpoint.startsWith("https://"))throw new IllegalArgumentException("接口地址必须使用 HTTPS");
  if(model==null||model.isBlank())throw new IllegalArgumentException("请填写模型名称");
  SharedPreferences.Editor e=p.edit().putBoolean("enabled",enabled).putString("endpoint",stripSlash(endpoint.trim()))
   .putString("model",model.trim()).putInt("maxOutput",Math.max(80,Math.min(600,maxOutput))).putBoolean("wifiOnly",wifiOnly);
  if(newKey!=null&&!newKey.isBlank())e.putString("key",encrypt(newKey.trim()));
  e.apply();
 }
 public void clearKey(){p.edit().remove("key").apply();}
 public void clearAll(){p.edit().clear().apply();}
 public String apiKey(){
  String saved=p.getString("key","");if(saved.isEmpty())return "";
  try{return decrypt(saved);}catch(Exception ignored){return "";}
 }
 static String stripSlash(String s){while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
 private SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(ks.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
  return generator.generateKey();
 }
 String encrypt(String plain) throws Exception {
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  return Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
 }
 String decrypt(String stored) throws Exception {
  String[] parts=stored.split(":",2);if(parts.length!=2)throw new IllegalArgumentException("bad key");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
  return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
 }
}
