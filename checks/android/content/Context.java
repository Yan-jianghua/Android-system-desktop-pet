package android.content;
import java.util.*;
public class Context {
 private final Map<String,SharedPreferences> stores=new HashMap<>();
 public SharedPreferences getSharedPreferences(String name,int mode){return stores.computeIfAbsent(name,n->new SharedPreferences());}
}
