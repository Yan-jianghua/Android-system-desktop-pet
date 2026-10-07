package org.json;
import java.util.*;
public class JSONObject {
 private final Map<String,Object> values=new HashMap<>();
 public JSONObject put(String key,Object value){values.put(key,value);return this;}
 public int optInt(String key,int fallback){Object v=values.get(key);return v instanceof Number?((Number)v).intValue():fallback;}
 public long optLong(String key,long fallback){Object v=values.get(key);return v instanceof Number?((Number)v).longValue():fallback;}
 public String optString(String key,String fallback){Object v=values.get(key);return v==null?fallback:String.valueOf(v);}
}
