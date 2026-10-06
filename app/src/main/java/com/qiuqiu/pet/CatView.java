package com.qiuqiu.pet;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Independently bounded full poses, with extra space around the character. */
public class CatView extends View {
 public String mode="sit",food="",travel="";public int facing=1,urine,poop;public long actionStartedUptime=-1,heartsUntil;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);private final Rect source=new Rect();private final RectF destination=new RectF();
 private final Path hand=new Path(),heart=new Path();private final java.util.Map<String,MotionAssets.Sheet> sheets;
 private Bitmap props;private MotionAssets.Sequence current;private MotionAssets.Sheet shown;private String previous="";private long since;
 public long idleStartedUptime=-1;private final float[] vertices=new float[578];private final PorterDuffXfermode add=new PorterDuffXfermode(PorterDuff.Mode.ADD);
 public float visualTop(){MotionAssets.Sheet s=sheets.get(MotionAssets.key(travel.isEmpty()?mode:travel,food));if(s==null)s=sheets.get("sit");float top=241;for(MotionAssets.Frame f:s.frames)top=Math.min(top,241+(f.source.top-f.ay-s.bottom)*s.scale);float factor=mode.equals("clean")?.66f:(mode.equals("pee")||mode.equals("poop"))?.86f:1,ground=mode.equals("clean")?157:factor<1?185:241;return (32+ground+(top-241)*factor)*Math.min(getWidth(),getHeight())/320f;}
 public float visualBottom(){return (mode.equals("pee")||mode.equals("poop")||mode.equals("clean")?300:273)*Math.min(getWidth(),getHeight())/320f;}
 public CatView(Context context){
  super(context);sheets=MotionAssets.metadata(context);setContentDescription("球球，点击喂食、铲屎、对话或陪球球玩");
  float[][] points={{460,0},{627,0},{558,92},{570,113},{553,204},{497,280},{458,315},{466,337},{425,438},{356,454},{260,452},{207,425},{294,339},{347,294},{382,246},{381,189},{422,115}};
  for(int i=0;i<points.length;i++){if(i==0)hand.moveTo(points[i][0],points[i][1]);else hand.lineTo(points[i][0],points[i][1]);}hand.close();
  heart.moveTo(-7,-1);heart.lineTo(0,8);heart.lineTo(7,-1);heart.close();MotionAssets.image(context,"care-props.png",this::postInvalidate);MotionAssets.sequence(context,"sit",this::postInvalidate);
 }
 @Override public boolean performClick(){super.performClick();return true;}
 private void prop(Canvas c,int index,float x,float y,float width,float height){if(props==null)return;int cell=props.getWidth()/2;source.set(index%2*cell,index/2*cell,(index%2+1)*cell,(index/2+1)*cell);destination.set(x,y,x+width,y+height);c.drawBitmap(props,source,destination,paint);}
 private void basin(Canvas c,boolean front){c.save();if(front)c.clipRect(0,197,256,268);prop(c,0,0,12,256,256);c.restore();}
 private void deposits(Canvas c){for(int i=0;i<Math.min(urine,18);i++)prop(c,2,13+(i*47)%179,130+(i*17)%40,26,30);for(int i=0;i<Math.min(poop,18);i++)prop(c,3,17+(i*61)%175,136+(i*11)%31,30,30);}
 private void scoop(Canvas c,long elapsed){float phase=MotionCycle.index(elapsed,5000,true)*2*(float)Math.PI/144;c.save();c.translate((float)Math.sin(phase)*20,(float)Math.cos(phase)*5);c.rotate((float)Math.sin(phase)*5,159,180);c.translate(41,31);c.scale(.34f,.34f);c.clipPath(hand);prop(c,1,0,0,627,627);c.restore();}
 private void mesh(Canvas c,Bitmap bitmap,float[] flow,float amount){for(int j=0;j<=16;j++)for(int i=0;i<=16;i++){int p=(j*17+i)*2;vertices[p]=i*16+flow[p]*amount*.30f;vertices[p+1]=j*16+flow[p+1]*amount*.30f;}c.drawBitmapMesh(bitmap,16,16,vertices,0,null,0,paint);}
 private void cat(Canvas c,MotionAssets.Sheet sheet,MotionAssets.Sequence seq,int index,float ground,float factor,float centre){
  float phase=MotionCycle.sourcePhase(index,sheet.loop);int first=(int)phase,next=(first+1)%24;float raw=phase-first,blend=raw*raw*(3-2*raw);
  c.save();c.translate(centre-128*factor,ground-241*factor);c.scale(factor,factor);
  if(blend==0)c.drawBitmap(seq.keys[first],0,0,paint);else{int layer=c.saveLayer(0,0,256,256,null);paint.setAlpha(Math.round((1-blend)*255));mesh(c,seq.keys[first],seq.flow[first][0],blend);paint.setXfermode(add);paint.setAlpha(Math.round(blend*255));mesh(c,seq.keys[next],seq.flow[first][1],1-blend);paint.setXfermode(null);paint.setAlpha(255);c.restoreToCount(layer);}c.restore();
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);long now=android.os.SystemClock.uptimeMillis();String pose=travel.isEmpty()?mode:travel;if(!pose.equals(previous)){previous=pose;since=now;}long elapsed=Math.max(0,now-(!travel.isEmpty()?since:actionStartedUptime>=0?actionStartedUptime:idleStartedUptime>=0?idleStartedUptime:since));
  String key=MotionAssets.key(pose.equals("rest")?"sit":pose,food);MotionAssets.Sheet wanted=sheets.get(key);if(wanted==null){key="sit";wanted=sheets.get(key);}MotionAssets.Sequence loaded=MotionAssets.sequence(getContext(),key,this::postInvalidate);if(loaded!=null){shown=wanted;current=loaded;}props=MotionAssets.image(getContext(),"care-props.png",this::postInvalidate);
  c.save();float size=Math.min(getWidth(),getHeight()),scale=size/320;c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(scale,scale);c.translate(0,32);
  boolean inTray=mode.equals("pee")||mode.equals("poop")||mode.equals("clean");if(inTray){basin(c,false);deposits(c);}
  if(current!=null&&shown!=null){float ground=inTray?185:241,factor=inTray?.86f:1,centre=128;if(pose.equals("clean")){ground=157;factor=.66f;centre=69;}if(pose.equals("rest"))factor*=1f+(float)Math.sin(now*.002)*.008f;c.save();if((pose.equals("walk")||pose.equals("run"))&&facing<0){c.translate(256,0);c.scale(-1,1);}cat(c,shown,current,MotionCycle.index(elapsed,shown.duration,shown.loop),ground,factor,centre);c.restore();}
  if(inTray)basin(c,true);if(mode.equals("clean"))scoop(c,elapsed);if(now<heartsUntil&&pose.equals("happy"))hearts(c,now);c.restore();if(isShown())postInvalidateOnAnimation();
 }
 private void hearts(Canvas c,long now){float progress=1-(heartsUntil-now)/5000f;for(int i=0;i<3;i++){float t=(progress+i*.22f)%1;paint.setColor(0xffd58f9c);paint.setAlpha(Math.round(210*(1-t)));c.save();c.translate(72+i*55+(float)Math.sin(t*5+i)*7,105-t*62);c.drawCircle(-3,-2,4,paint);c.drawCircle(3,-2,4,paint);c.drawPath(heart,paint);c.restore();}paint.setAlpha(255);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();invalidate();}
}


