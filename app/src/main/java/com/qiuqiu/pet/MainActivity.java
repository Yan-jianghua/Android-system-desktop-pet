package com.qiuqiu.pet;
import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;

public class MainActivity extends Activity {
 TextView memory;Button permission;CatView cat;
 AlertDialog updateDialog;android.app.ProgressDialog downloadDialog;volatile boolean updateCheckRunning=false;
 final Handler idleHandler=new Handler(Looper.getMainLooper());
 final java.util.Random random=new java.util.Random();
 final IdleMotion idleClock=new IdleMotion();
 final Runnable idle=new Runnable(){public void run(){long now=SystemClock.uptimeMillis();if(idleClock.advance(now,true,cat.mode))cat.mode=IdleMotion.next(cat.mode,random);cat.idleStartedUptime=now-idleClock.elapsed;idleHandler.postDelayed(this,50);}};
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 // 配色
 static final int INK=0xff5a4a42, SUB=0xff9a8a82;
 static final int[] BTN_COLORS={0xffd4e8f0,0xfff8d4dc,0xfffbeecb,0xffd4ecd8,0xffe4d8f0,0xfffbe0cc};
 /** 房间内球球可出现的点位：gravity, bottomMargin, leftMargin, rightMargin, 尺寸dp */
 static class Spot{int gravity,bottom,left,right,size;Spot(int g,int b,int l,int r,int s){gravity=g;bottom=b;left=l;right=r;size=s;}}
 static final Spot[] SPOTS={
  new Spot(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,195,0,0,180), // 1.地毯中央（地上）
  new Spot(Gravity.BOTTOM|Gravity.LEFT,205,30,0,170), // 2.地毯左侧·落地灯旁（地上）
  new Spot(Gravity.BOTTOM|Gravity.RIGHT,205,0,30,170), // 3.地毯右侧（地上）
  new Spot(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,285,0,0,130), // 4.茶几上
  new Spot(Gravity.BOTTOM|Gravity.LEFT,365,55,0,135), // 5.沙发左座
  new Spot(Gravity.BOTTOM|Gravity.RIGHT,365,0,55,135), // 6.沙发右座
  new Spot(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,475,0,0,115), // 7.窗台上
 };
 GradientDrawable glassBg(int color,int radius){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 Button gridBtn(LinearLayout row,String label,int colorIdx,Runnable action){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(11);
  b.setBackground(glassBg(BTN_COLORS[colorIdx%BTN_COLORS.length],13));
  b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,dp(2),0,dp(2));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(38),1f);lp.setMargins(dp(2),dp(2),dp(2),dp(2));
  b.setOnClickListener(v->action.run());row.addView(b,lp);return b;
 }
 public void onCreate(Bundle saved){
  super.onCreate(saved);
  FrameLayout root=new FrameLayout(this);

  // 1. 3D房间背景（铺满全屏）
  ImageView roomBg=new ImageView(this);roomBg.setImageResource(R.drawable.room_3d);
  roomBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
  root.addView(roomBg,new FrameLayout.LayoutParams(-1,-1));

  // 随机选择一个点位
  Spot spot=SPOTS[new java.util.Random().nextInt(SPOTS.length)];

  // 2. 球球的阴影（椭圆，中心对齐猫咪爪子所在平面）
  View shadow=new View(this);
  GradientDrawable shadowBg=new GradientDrawable();
  shadowBg.setShape(GradientDrawable.OVAL);shadowBg.setColor(0x4d000000);
  shadow.setBackground(shadowBg);
  int shadowW=(int)(spot.size*0.68f),shadowH=(int)(spot.size*0.16f);
  FrameLayout.LayoutParams shadowLp=new FrameLayout.LayoutParams(dp(shadowW),dp(shadowH));
  shadowLp.gravity=spot.gravity;
  shadowLp.bottomMargin=dp(spot.bottom-shadowH/2); // 阴影中心在平面上
  shadowLp.leftMargin=dp(spot.left);shadowLp.rightMargin=dp(spot.right);
  root.addView(shadow,shadowLp);

  // 3. 球球（猫咪爪子对齐平面，修正 View 底部空白导致的悬浮）
  cat=new CatView(this);cat.mode="rest";
  FrameLayout.LayoutParams catLp=new FrameLayout.LayoutParams(dp(spot.size),dp(spot.size));
  catLp.gravity=spot.gravity;
  int pawGap=(int)(spot.size*47f/320f); // 猫咪爪子到 View 底部的距离
  catLp.bottomMargin=dp(spot.bottom-pawGap); // 让爪子落在平面上
  catLp.leftMargin=dp(spot.left);catLp.rightMargin=dp(spot.right);
  root.addView(cat,catLp);

  // 4. 功能UI层（透明，覆盖在房间之上）
  LinearLayout ui=new LinearLayout(this);ui.setOrientation(LinearLayout.VERTICAL);
  ui.setPadding(dp(14),dp(14),dp(14),dp(14));

  // 顶部：半透明标题 + 状态卡片
  LinearLayout topCard=new LinearLayout(this);topCard.setOrientation(LinearLayout.VERTICAL);
  topCard.setBackground(glassBg(0xe6ffffff,18));topCard.setPadding(dp(16),dp(12),dp(16),dp(12));
  TextView title=new TextView(this);title.setText("球球的小窝");title.setTextColor(INK);title.setTextSize(20);
  title.setGravity(Gravity.CENTER);topCard.addView(title);
  memory=new TextView(this);memory.setTextColor(SUB);memory.setTextSize(11);memory.setGravity(Gravity.CENTER);
  memory.setPadding(0,dp(4),0,0);topCard.addView(memory);
  ui.addView(topCard,new LinearLayout.LayoutParams(-1,-2));

  // 中间留空（展示房间和球球）
  Space space=new Space(this);
  ui.addView(space,new LinearLayout.LayoutParams(-1,0,1f));

  // 底部：紧凑入口行，降低遮挡球球的高度
  LinearLayout btnPanel=new LinearLayout(this);btnPanel.setOrientation(LinearLayout.VERTICAL);
  btnPanel.setBackground(glassBg(0xe6ffffff,15));btnPanel.setPadding(dp(4),dp(3),dp(4),dp(3));

  LinearLayout row1=new LinearLayout(this);row1.setOrientation(LinearLayout.HORIZONTAL);btnPanel.addView(row1);
  gridBtn(row1,"出门",1,()->{
   if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"请先允许悬浮窗",Toast.LENGTH_SHORT).show();return;}
   if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
   startForegroundService(new Intent(this,PetService.class));
   Toast.makeText(this,"球球出门啦！",Toast.LENGTH_SHORT).show();
  });
  gridBtn(row1,"解救",2,()->{
   Intent unlock=new Intent(this,PetService.class).setAction("unlock");
   if(Build.VERSION.SDK_INT>=26)startForegroundService(unlock);else startService(unlock);
   Toast.makeText(this,"球球已解救",Toast.LENGTH_SHORT).show();
   memory.postDelayed(this::refresh,250);
  });

  LinearLayout row2=new LinearLayout(this);row2.setOrientation(LinearLayout.HORIZONTAL);btnPanel.addView(row2);
  gridBtn(row2,"回家",3,()->{stopService(new Intent(this,PetService.class));memory.postDelayed(this::refresh,250);});
  gridBtn(row2,"经期日历",4,()->startActivity(new Intent(this,PeriodCalendarActivity.class)));
  gridBtn(row2,"更多功能",5,()->showMore());
  gridBtn(row2,"双人聊天",1,()->startActivity(new Intent(this,ChatActivity.class)));

  ui.addView(btnPanel,new LinearLayout.LayoutParams(-1,-2));

  root.addView(ui,new FrameLayout.LayoutParams(-1,-1));
  setContentView(root);
  SyncJobService.schedule(this);
  checkForUpdate(false);
 }
 void checkForUpdate(boolean manual){
  if(updateCheckRunning)return;updateCheckRunning=true;
  if(manual){updateDialog=new AlertDialog.Builder(this).setTitle("正在检查更新").setMessage("正在连接更新服务器…").setNegativeButton("取消",(d,w)->{d.dismiss();}).create();updateDialog.show();}
  new Thread(()->{java.net.HttpURLConnection c=null;try{
   c=(java.net.HttpURLConnection)new java.net.URL("https://raw.githubusercontent.com/Yan-jianghua/Android-system-desktop-pet/main/latest.json").openConnection();c.setRequestProperty("Cache-Control","no-cache");c.setConnectTimeout(5000);c.setReadTimeout(5000);int status=c.getResponseCode();if(status<200||status>=300)throw new java.io.IOException("更新服务器返回 HTTP "+status);
   byte[] data;try(java.io.InputStream in=c.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>256*1024)throw new java.io.IOException("更新信息文件过大");out.write(buf,0,n);}data=out.toByteArray();}
   org.json.JSONObject release=new org.json.JSONObject(new String(data,java.nio.charset.StandardCharsets.UTF_8));int code=release.optInt("versionCode",0);String tag=release.optString("versionName","");if(code<=0)throw new java.io.IOException("更新信息缺少有效版本号");
   runOnUiThread(()->{updateCheckRunning=false;dismissUpdateCheck();try{if(code>getPackageManager().getPackageInfo(getPackageName(),0).versionCode)showUpdate(tag,release.optString("notes",""),release.optString("apkUrl",""));else if(manual)Toast.makeText(this,"当前已是最新版本",Toast.LENGTH_SHORT).show();}catch(Exception e){if(manual)Toast.makeText(this,"读取当前版本失败",Toast.LENGTH_SHORT).show();}});
  }catch(Exception e){String detail=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();runOnUiThread(()->{updateCheckRunning=false;dismissUpdateCheck();if(manual)Toast.makeText(this,"检查更新失败："+detail,Toast.LENGTH_LONG).show();});}finally{if(c!=null)c.disconnect();}}).start();
 }
 void dismissUpdateCheck(){if(updateDialog!=null&&updateDialog.isShowing())updateDialog.dismiss();updateDialog=null;}
 void showUpdate(String version,String notes,String url){if(url==null||url.isEmpty()){Toast.makeText(this,"发现新版本，但更新地址为空",Toast.LENGTH_LONG).show();return;}new AlertDialog.Builder(this).setTitle("发现新版本 "+version).setMessage(notes.isEmpty()?"现在下载并安装更新吗？":notes+"\n\n现在下载并安装吗？").setNegativeButton("稍后",null).setPositiveButton("下载更新",(d,w)->downloadUpdate(url)).show();}
 void downloadUpdate(String url){
  downloadDialog=new android.app.ProgressDialog(this);downloadDialog.setTitle("正在下载更新");downloadDialog.setMessage("正在连接下载服务器…");downloadDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);downloadDialog.setIndeterminate(true);downloadDialog.setCancelable(true);downloadDialog.setButton(android.app.ProgressDialog.BUTTON_NEGATIVE,"取消",(d,w)->d.dismiss());downloadDialog.show();
  new Thread(()->{java.io.File apk=new java.io.File(getExternalFilesDir(null),"qiuqiu-update.apk"),part=new java.io.File(getExternalFilesDir(null),"qiuqiu-update.apk.part");java.net.HttpURLConnection c=null;try{c=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();c.setRequestProperty("Cache-Control","no-cache");c.setConnectTimeout(10000);c.setReadTimeout(30000);int status=c.getResponseCode();if(status<200||status>=300)throw new java.io.IOException("下载服务器返回 HTTP "+status);int total=c.getContentLength();runOnUiThread(()->{if(downloadDialog!=null&&downloadDialog.isShowing()){downloadDialog.setIndeterminate(total<=0);downloadDialog.setMax(total>0?100:0);downloadDialog.setProgress(0);downloadDialog.setMessage("已下载 0 MB");}});long received=0;try(java.io.InputStream in=c.getInputStream();java.io.FileOutputStream out=new java.io.FileOutputStream(part)){byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1){if(downloadDialog!=null&&!downloadDialog.isShowing())throw new java.io.InterruptedIOException("用户取消了下载");out.write(buf,0,n);received+=n;if(total>0){int progress=(int)Math.min(100,received*100/total);String msg=String.format(java.util.Locale.CHINA,"已下载 %.1f / %.1f MB",received/1048576.0,total/1048576.0);runOnUiThread(()->{if(downloadDialog!=null&&downloadDialog.isShowing()){downloadDialog.setProgress(progress);downloadDialog.setMessage(msg);}});}}}if(received<1024)throw new java.io.IOException("下载文件不完整（"+received+" 字节）");try(java.io.InputStream in=new java.io.FileInputStream(part)){if(in.read()!='P'||in.read()!='K')throw new java.io.IOException("下载地址返回的不是 APK 文件，检查 GitHub 更新地址");}if(apk.exists()&&!apk.delete())throw new java.io.IOException("无法替换旧更新文件");if(!part.renameTo(apk))throw new java.io.IOException("无法保存下载文件");
  }catch(Exception e){part.delete();String detail=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();runOnUiThread(()->{if(downloadDialog!=null&&downloadDialog.isShowing())downloadDialog.dismiss();downloadDialog=null;if(!detail.contains("用户取消"))new AlertDialog.Builder(this).setTitle("下载更新失败").setMessage(detail).setPositiveButton("确定",null).show();});return;}finally{if(c!=null)c.disconnect();}
  runOnUiThread(()->{if(downloadDialog!=null&&downloadDialog.isShowing())downloadDialog.dismiss();downloadDialog=null;try{android.net.Uri uri=new android.net.Uri.Builder().scheme("content").authority(getPackageName()+".provider").appendPath(apk.getName()).build();Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(install);}catch(Exception e){Toast.makeText(this,"下载完成，但无法打开安装器："+e.getMessage(),Toast.LENGTH_LONG).show();}});
  }).start();
 }
 void showMore(){
  String[] actions={"悬浮窗权限","经期日历","AI设置","账户","共享相册","成长与同步","检查更新"};
  new AlertDialog.Builder(this).setTitle("更多功能").setItems(actions,(d,which)->{
   switch(which){case 0:startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));break;
    case 1:startActivity(new Intent(this,PeriodCalendarActivity.class));break;
    case 2:startActivity(new Intent(this,AiSettingsActivity.class));break;
    case 3:startActivity(new Intent(this,AccountActivity.class));break;
    case 4:startActivity(new Intent(this,AlbumListActivity.class));break;
    case 5:startActivity(new Intent(this,InsightsActivity.class));break;
    case 6:checkForUpdate(true);break;}
  }).show();
 }
 void refresh(){
  PetState s=new PetState(this);
  Companion friend=new Companion(this);
  memory.setText("猫砂盆 便便"+s.poop+"·尿团"+s.urine+"  |  "+friend.title()+" 亲密度"+friend.affection+"/100  |  "+(s.p.getBoolean("locked",false)?"已锁定":"可拖动"));
 }
 @Override public void onResume(){super.onResume();refresh();ServerConfig server=new ServerConfig(this);if(server.configured()){RealtimeSyncService.start(this);new SyncRepository(this).syncAsync((ok,msg)->{if(ok)refresh();});}idleHandler.removeCallbacks(idle);idleClock.advance(SystemClock.uptimeMillis(),false);idleHandler.post(idle);}
 @Override public void onPause(){idleHandler.removeCallbacks(idle);super.onPause();}
}
