package com.qiuqiu.pet;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Rounded speech balloon with a continuous outline and a tail toward Qiuqiu. */
final class SpeechBubbleDrawable extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path outline=new Path();
 private final float density;
 private int alpha=255;

 SpeechBubbleDrawable(float density){this.density=density;}

 @Override public void draw(Canvas canvas){
  Rect bounds=getBounds();
  float stroke=density,left=bounds.left+stroke/2,right=bounds.right-stroke/2;
  float top=bounds.top+stroke/2,tip=bounds.bottom-stroke/2,bottom=tip-11*density;
  float radius=Math.min(16*density,Math.min((right-left)/2,(bottom-top)/2));
  float middle=(left+right)/2;
  outline.reset();outline.moveTo(left+radius,top);
  outline.lineTo(right-radius,top);outline.quadTo(right,top,right,top+radius);
  outline.lineTo(right,bottom-radius);outline.quadTo(right,bottom,right-radius,bottom);
  outline.lineTo(middle+10*density,bottom);
  outline.lineTo(middle+2*density,tip-2*density);outline.quadTo(middle,tip,middle-2*density,tip-2*density);
  outline.lineTo(middle-10*density,bottom);
  outline.lineTo(left+radius,bottom);outline.quadTo(left,bottom,left,bottom-radius);
  outline.lineTo(left,top+radius);outline.quadTo(left,top,left+radius,top);outline.close();
  paint.setStyle(Paint.Style.FILL);paint.setColor(0xfffffaf2);paint.setAlpha(alpha);
  canvas.drawPath(outline,paint);
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(0xffdfd4c5);paint.setAlpha(alpha);
  canvas.drawPath(outline,paint);
 }

 @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
 @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
 @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
