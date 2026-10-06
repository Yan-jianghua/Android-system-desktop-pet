package com.qiuqiu.pet;
import android.app.*;
import android.os.*;
import android.provider.Settings;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {
 TextView memory;Button permission;CatView cat;
 final Handler idleHandler=new Handler(Looper.getMainLooper());
 final java.util.Random random=new java.util.Random();
 final IdleMotion idleClock=new IdleMotion();
 final Runnable idle=new Runnable(){public void run(){long now=SystemClock.uptimeMillis();if(idleClock.advance(now,true,cat.mode))cat.mode=IdleMotion.next(cat.mode,random);cat.idleStartedUptime=now-idleClock.elapsed;idleHandler.postDelayed(this,50);}};
 int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout parent,String value,int size){
  TextView t=new TextView(this);t.setText(value);t.setTextColor(0xff443f39);t.setTextSize(size);
  t.setPadding(0,dp(7),0,dp(7));parent.addView(t);return t;
 }
 Button button(LinearLayout parent,String label,Runnable action){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(0xff465343);b.setTextSize(15);
  android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(0xfff0f3e9);bg.setCornerRadius(dp(16));bg.setStroke(dp(1),0xffd7deca);b.setBackground(bg);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(5),0,dp(5));b.setOnClickListener(v->action.run());parent.addView(b,lp);return b;
 }
 public void onCreate(Bundle saved){
  super.onCreate(saved);ScrollView scroll=new ScrollView(this);
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
  body.setPadding(dp(24),dp(28),dp(24),dp(28));body.setBackgroundColor(Color.rgb(248,249,242));scroll.addView(body);
  text(body,"球球",34);text(body,"把软乎乎的陪伴，放在身边",17);
  cat=new CatView(this);LinearLayout.LayoutParams previewCat=new LinearLayout.LayoutParams(dp(156),dp(156));previewCat.gravity=android.view.Gravity.CENTER_HORIZONTAL;body.addView(cat,previewCat);cat.mode="rest";
  text(body,"球球会自动随机舔毛、趴着摇尾巴、坐着左右看。拖动时走路，特别快才会跑。轻点球球，可以喂食、铲屎或对话。亮屏或解锁时，球球会用文字气泡向你问好。",15);
  button(body,"女生经期日历（记录经期 / 看排卵期安全期）",()->startActivity(new Intent(this,PeriodCalendarActivity.class)));
  memory=text(body,"",15);
  permission=button(body,"允许悬浮窗",()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
  button(body,"让球球出门",()->{
   if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"请先允许悬浮窗",Toast.LENGTH_SHORT).show();return;}
   if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
   startForegroundService(new Intent(this,PetService.class));
   Toast.makeText(this,"球球出门啦！点击小猫打开菜单",Toast.LENGTH_SHORT).show();
  });
  button(body,"解救球球",()->{
   Intent unlock=new Intent(this,PetService.class).setAction("unlock");
   if(Build.VERSION.SDK_INT>=26)startForegroundService(unlock);else startService(unlock);
   Toast.makeText(this,"球球已解救，可以点击和拖动了",Toast.LENGTH_SHORT).show();
   memory.postDelayed(this::refresh,250);
  });
  button(body,"让球球回家",()->{stopService(new Intent(this,PetService.class));memory.postDelayed(this::refresh,250);});
  text(body,"每两小时一个尿团；每六小时按喂食次数产生便便。喂食立即开始，进食动作五秒，十秒内不能再次喂。铲屎动作七秒，完成后报告数量并清空猫砂盆。到点排泄会优先进行，然后继续未完成的进食或铲屎动作。\n\n记录会保存在手机本地，关闭或重开应用都还在。所有回复使用文字气泡，对话与生日节日判断离线完成。巧克力是虚拟道具。",13);
  setContentView(scroll);
 }
 void refresh(){
  PetState s=new PetState(this);memory.setText("猫砂盆记忆\n便便："+s.poop+" 个 · 尿团："+s.urine+" 个\n未铲除的记录会一直保留");
  memory.append("\n球球状态："+(s.p.getBoolean("locked",false)?"已锁定（点击会穿透）":"可以点击和拖动"));
  Companion friend=new Companion(this);memory.append("\n\n"+friend.title()+" · 亲密度 "+friend.affection+" / 100\n今天的心情："+friend.mood(System.currentTimeMillis())+"\n点击悬浮球球 → 陪球球玩，可以摸摸和看悄悄话。");
  permission.setText(Settings.canDrawOverlays(this)?"悬浮窗权限已允许":"允许悬浮窗");
 }
 @Override public void onResume(){super.onResume();refresh();idleHandler.removeCallbacks(idle);idleClock.advance(SystemClock.uptimeMillis(),false);idleHandler.post(idle);}
 @Override public void onPause(){idleHandler.removeCallbacks(idle);super.onPause();}
}


