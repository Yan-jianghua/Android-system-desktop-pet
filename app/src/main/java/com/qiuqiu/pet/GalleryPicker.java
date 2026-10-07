package com.qiuqiu.pet;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
final class GalleryPicker {
 static void open(Activity activity,String type,boolean multiple,int request){
  Intent intent=Build.VERSION.SDK_INT>=33?new Intent(MediaStore.ACTION_PICK_IMAGES).setType(type):new Intent(Intent.ACTION_PICK,type.startsWith("video/")?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:MediaStore.Images.Media.EXTERNAL_CONTENT_URI).setType(type);
  if(Build.VERSION.SDK_INT>=33&&multiple)intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,Math.min(20,MediaStore.getPickImagesMaxLimit()));
  try{activity.startActivityForResult(intent,request);}catch(android.content.ActivityNotFoundException e){activity.startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType(type).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple),request);}
 }
}
