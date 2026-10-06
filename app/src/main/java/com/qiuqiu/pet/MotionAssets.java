package com.qiuqiu.pet;
import android.content.Context;
import android.graphics.*;
import android.util.LruCache;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
final class MotionAssets {
 static final class Frame {final Rect source;final float ax,ay;Frame(JSONArray a)throws JSONException{source=new Rect(a.getInt(0),a.getInt(1),a.getInt(2),a.getInt(3));ax=(float)a.getDouble(4);ay=(float)a.getDouble(5);}}
 static final class Sheet {
  final String file;final Frame[] frames=new Frame[24];final float scale,anchorX,bottom;final long duration;final boolean loop;
  Sheet(JSONObject a)throws JSONException{file=a.getString("file");scale=(float)a.getDouble("scale");anchorX=(float)a.getDouble("anchorX");bottom=(float)a.getDouble("bottom");duration=MotionCycle.DURATION;loop=a.getBoolean("loop");JSONArray all=a.getJSONArray("frames");if(all.length()!=24)throw new JSONException("Expected24");for(int i=0;i<24;i++)frames[i]=new Frame(all.getJSONArray(i));}
 }
 private static Map<String,Sheet> sheets;
 static final class Sequence {final Bitmap[] keys=new Bitmap[24];final float[][][] flow=new float[24][2][];}
 private static final LruCache<String,Sequence> sequences=new LruCache<>(3);
 private static final Set<String> sequenceLoading=new HashSet<>();
 static Sequence sequence(Context context,String key,Runnable ready){
  synchronized(sequenceLoading){Sequence s=sequences.get(key);if(s!=null)return s;if(!sequenceLoading.add(key))return null;}
  Context app=context.getApplicationContext();decoder.execute(()->{
   try(java.io.InputStream in=app.getAssets().open("pet-sprites/motions24/"+key+"-flow.json");java.io.InputStream png=app.getAssets().open("pet-sprites/"+metadata(app).get(key).file)){
    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
    JSONArray vectors=new JSONObject(out.toString("UTF-8")).getJSONArray("vectors");Sequence seq=new Sequence();Bitmap atlas=BitmapFactory.decodeStream(png);Sheet sheet=metadata(app).get(key);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    for(int i=0;i<24;i++){Frame f=sheet.frames[i];float s=sheet.scale,x=sheet.anchorX+(f.source.left-f.ax)*s,y=241+(f.source.top-f.ay-sheet.bottom)*s;seq.keys[i]=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);new Canvas(seq.keys[i]).drawBitmap(atlas,f.source,new RectF(x,y,x+f.source.width()*s,y+f.source.height()*s),p);
     for(int j=0;j<2;j++){JSONArray values=vectors.getJSONArray(i).getJSONArray(j);if(values.length()!=578)throw new JSONException("Invalid motion grid");seq.flow[i][j]=new float[578];for(int k=0;k<578;k++)seq.flow[i][j][k]=(float)values.getDouble(k);}}
    atlas.recycle();sequences.put(key,seq);
   }catch(Exception e){android.util.Log.e("Qiuqiu","Cannot prepare motion "+key,e);}finally{synchronized(sequenceLoading){sequenceLoading.remove(key);}ready.run();}
  });return null;
 }
 private static final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(24*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
 private static final Set<String> loading=new HashSet<>();private static final ExecutorService decoder=Executors.newSingleThreadExecutor();
 static synchronized Map<String,Sheet> metadata(Context context){
  if(sheets!=null)return sheets;
  try(java.io.InputStream in=context.getAssets().open("pet-sprites/motions24.json")){
   java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
   JSONObject data=new JSONObject(out.toString("UTF-8")).getJSONObject("motions");Map<String,Sheet> parsed=new HashMap<>();Iterator<String> keys=data.keys();while(keys.hasNext()){String key=keys.next();parsed.put(key,new Sheet(data.getJSONObject(key)));}sheets=parsed;return sheets;
  }catch(Exception e){throw new IllegalStateException("Invalid24-frame atlas",e);}
 }
 static Bitmap image(Context context,String file,Runnable ready){
  synchronized(loading){Bitmap b=cache.get(file);if(b!=null)return b;if(!loading.add(file))return null;}
  Context app=context.getApplicationContext();decoder.execute(()->{try(java.io.InputStream in=app.getAssets().open("pet-sprites/"+file)){
   BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;Bitmap b=BitmapFactory.decodeStream(in,null,options);if(b==null)throw new java.io.IOException(file);cache.put(file,b);
  }catch(java.io.IOException e){android.util.Log.e("Qiuqiu","Cannot load "+file,e);}finally{synchronized(loading){loading.remove(file);}ready.run();}});return null;
 }
 static String key(String pose,String food){return pose.equals("eat")?(food.equals("猫条")?"treat":food.equals("巧克力")?"chocolate":"kibble"):pose;}
}
