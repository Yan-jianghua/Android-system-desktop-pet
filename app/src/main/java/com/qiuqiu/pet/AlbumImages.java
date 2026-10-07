package com.qiuqiu.pet;
import android.content.*;import android.graphics.*;import android.net.Uri;import android.provider.MediaStore;import android.util.Base64;import java.io.*;
final class AlbumImages {
 static final String MARK="[[qiuqiu-image:xx";
 static String encode(Context c,Uri uri)throws Exception{Bitmap src=MediaStore.Images.Media.getBitmap(c.getContentResolver(),uri);int max=Math.max(src.getWidth(),src.getHeight());float s=max>1280?1280f/max:1f;Bitmap out=s<1?Bitmap.createScaledBitmap(src,Math.round(src.getWidth()*s),Math.round(src.getHeight()*s),true):src;for(int q=82;q>=42;q-=8){ByteArrayOutputStream b=new ByteArrayOutputStream();out.compress(Bitmap.CompressFormat.JPEG,q,b);if(b.size()<600000||q==42)return MARK+Base64.encodeToString(b.toByteArray(),Base64.NO_WRAP)+"]]";}throw new IOException("照片压缩失败");}
 static byte[] bytes(String body){try{int a=body.indexOf(MARK),e=body.indexOf("]]",a);if(a<0||e<0)return null;return Base64.decode(body.substring(a+MARK.length(),e),Base64.DEFAULT);}catch(Exception e){return null;}}
 static Bitmap decode(String body){byte[] b=bytes(body);return b==null?null:BitmapFactory.decodeByteArray(b,0,b.length);}
 private AlbumImages(){}
}
