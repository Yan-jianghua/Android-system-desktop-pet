package android.content;
import java.util.*;
public class SharedPreferences {
 private final Map<String,Object> values=new HashMap<>();
 public int getInt(String k,int d){return (int)values.getOrDefault(k,d);}
 public long getLong(String k,long d){return (long)values.getOrDefault(k,d);}
 public String getString(String k,String d){return (String)values.getOrDefault(k,d);}
 public Editor edit(){return new Editor();}
 public class Editor {
  public Editor putInt(String k,int v){values.put(k,v);return this;}
  public Editor putLong(String k,long v){values.put(k,v);return this;}
  public Editor putString(String k,String v){values.put(k,v);return this;}
  public void apply(){}
  public boolean commit(){return true;}
 }
}
