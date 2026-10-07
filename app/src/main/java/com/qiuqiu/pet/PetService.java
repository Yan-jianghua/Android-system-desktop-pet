package com.qiuqiu.pet;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class PetService extends Service {
 static volatile boolean running;
 WindowManager wm;WindowManager.LayoutParams params;FrameLayout root,panel,menuScroll;
 CatView cat;TextView bubble;PetState state;Companion companion;PeriodData periodData;
 AiChatEngine chatEngine;boolean teachingMode,correcting;String lastQuestion="",lastAnswer="";
 boolean busy,screen=true,dragging,moved,locked;float downX,downY,lastX,lastY,speed;
 long lastTime,fastSince,speechVersion,bubbleUntil;int originX,originY;
 final Handler h=new Handler(Looper.getMainLooper());final Random random=new Random();
 final ActionTimeline actions=new ActionTimeline();
 final IdleMotion idle=new IdleMotion();boolean belowPanel;int panelButtons,belowTextHeight;
 void restartIdle(){cat.mode="rest";idle.reset(SystemClock.uptimeMillis());cat.idleStartedUptime=SystemClock.uptimeMillis();}
 void resetPanel(boolean below,int height){panel.removeAllViews();belowPanel=below;panelButtons=0;belowTextHeight=0;panel.getLayoutParams().height=dp(height);panel.requestLayout();menuScroll.getLayoutParams().height=dp(height);menuScroll.requestLayout();}
 void layoutPet(){
  if(cat.getWidth()==0)return;FrameLayout.LayoutParams cp=(FrameLayout.LayoutParams)cat.getLayoutParams();int top=cp.topMargin;
  if(bubble.getVisibility()==View.VISIBLE){bubble.measure(View.MeasureSpec.makeMeasureSpec(dp(136),View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));top=Math.max(0,Math.round(bubble.getMeasuredHeight()+dp(3)-cat.visualTop()));}
  if(cp.topMargin!=top){cp.topMargin=top;cat.setLayoutParams(cp);}
  FrameLayout.LayoutParams mp=(FrameLayout.LayoutParams)menuScroll.getLayoutParams();int mt=belowPanel?Math.round(top+cat.visualBottom()+dp(3)):top-dp(14);if(mp.topMargin!=mt){mp.topMargin=Math.max(0,mt);menuScroll.setLayoutParams(mp);}
 }
 final BroadcastReceiver receiver=new BroadcastReceiver(){
  public void onReceive(Context c,Intent i){
   if(MessageNotifier.ACTION_INCOMING.equals(i.getAction())){String sender=i.getStringExtra("sender"),body=i.getStringExtra("body");sayChat((sender==null?"另一位主人":sender)+"："+(body==null?"发来一条消息":body));}
   else if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){screen=false;++speechVersion;root.setVisibility(View.GONE);dragging=false;cat.travel="";}
   else if(Intent.ACTION_SCREEN_ON.equals(i.getAction())||Intent.ACTION_USER_PRESENT.equals(i.getAction())){
    boolean shouldGreet=!screen||bubble.getVisibility()!=View.VISIBLE;
    screen=true;idle.advance(SystemClock.uptimeMillis(),false);root.setVisibility(View.VISIBLE);if(shouldGreet)greet();
   }
   else if(Intent.ACTION_CONFIGURATION_CHANGED.equals(i.getAction())){root.post(()->reposition());}
  }
 };
 public IBinder onBind(Intent i){return null;}
 int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
 public void onCreate(){
  super.onCreate();running=true;state=new PetState(this);companion=new Companion(this);periodData=new PeriodData(this);chatEngine=new AiChatEngine(this);wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel("pet","球球陪伴",NotificationManager.IMPORTANCE_LOW));
  PendingIntent quit=PendingIntent.getService(this,2,new Intent(this,PetService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE);
  PendingIntent open=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
  startForeground(1,new Notification.Builder(this,"pet").setSmallIcon(com.qiuqiu.pet.R.drawable.cat_icon)
   .setContentTitle("球球正在陪你").setContentText("轻点球球喂食、铲屎或聊天")
   .setContentIntent(open).addAction(new Notification.Action.Builder(null,"回家",quit).build()).build());
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  root=new FrameLayout(this);root.setClipChildren(false);
  bubble=new TextView(this);bubble.setTextColor(0xff453e39);bubble.setTextSize(14);
  bubble.setGravity(Gravity.CENTER);bubble.setPadding(dp(10),dp(6),dp(10),dp(17));bubble.setMaxWidth(dp(136));bubble.setMinWidth(0);
  bubble.setBackground(new SpeechBubbleDrawable(getResources().getDisplayMetrics().density));
  FrameLayout.LayoutParams bubbleParams=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
  bubbleParams.setMargins(dp(8),0,dp(8),dp(4));
  bubble.setVisibility(View.GONE);root.addView(bubble,bubbleParams);
  cat=new CatView(this);FrameLayout.LayoutParams catParams=new FrameLayout.LayoutParams(dp(156),dp(156),Gravity.TOP|Gravity.CENTER_HORIZONTAL);catParams.topMargin=dp(48);root.addView(cat,catParams);
  menuScroll=new FrameLayout(this);menuScroll.setVisibility(View.GONE);menuScroll.setClipChildren(false);
  panel=new FrameLayout(this);panel.setClipChildren(false);menuScroll.addView(panel,new FrameLayout.LayoutParams(-1,dp(210),Gravity.TOP));
  FrameLayout.LayoutParams menuParams=new FrameLayout.LayoutParams(-1,dp(210),Gravity.TOP);menuParams.topMargin=dp(34);root.addView(menuScroll,menuParams);
  params=new WindowManager.LayoutParams(dp(156),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
  params.gravity=Gravity.TOP|Gravity.START;params.x=state.p.getInt("x",20);params.y=state.p.getInt("y",300);
  wm.addView(root,params);root.post(this::reposition);
  screen=getSystemService(PowerManager.class).isInteractive();
  root.setVisibility(screen?View.VISIBLE:View.GONE);
  locked=state.p.getBoolean("locked",false);
  if(locked)setLocked(true);
  cat.setOnClickListener(v->menu(true));
  cat.setOnTouchListener(this::touch);
  IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_USER_PRESENT);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_CONFIGURATION_CHANGED);f.addAction(MessageNotifier.ACTION_INCOMING);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);restartIdle();h.postDelayed(()->{if(screen&&speechVersion==0)greet();},1200);h.post(tick);
 }
 boolean touch(View v,MotionEvent e){
  switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:
    dragging=true;downX=lastX=e.getRawX();downY=lastY=e.getRawY();originX=params.x;originY=params.y;
    lastTime=e.getEventTime();moved=false;speed=0;fastSince=0;break;
   case MotionEvent.ACTION_MOVE:
    float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
    if(Math.hypot(dx,dy)>dp(6))moved=true;
    if(moved){
     float current=(float)Math.hypot(e.getRawX()-lastX,e.getRawY()-lastY)/Math.max(1,e.getEventTime()-lastTime)*1000/getResources().getDisplayMetrics().density;
     speed=.55f*current+.45f*speed;
     if(speed>2200){if(fastSince==0)fastSince=e.getEventTime();}else fastSince=0;
     cat.travel=fastSince!=0&&e.getEventTime()-fastSince>=60?"run":"walk";
     if(Math.abs(e.getRawX()-lastX)>1)cat.facing=e.getRawX()<lastX?-1:1;
     params.x=originX+(int)dx;params.y=originY+(int)dy;reposition();
    }
    lastX=e.getRawX();lastY=e.getRawY();lastTime=e.getEventTime();break;
   case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
    dragging=false;cat.travel="";if(moved&&!busy)restartIdle();
    if(!moved&&e.getActionMasked()==MotionEvent.ACTION_UP)v.performClick();
    state.p.edit().putInt("x",params.x).putInt("y",params.y).apply();break;
  }return true;
 }
 void reposition(){
  if(root==null||params==null)return;
  layoutPet();
  android.util.DisplayMetrics d=getResources().getDisplayMetrics();
  int sideInset=dp(16);int minX=-sideInset,maxX=Math.max(minX,d.widthPixels-params.width+sideInset);
  params.x=Math.max(minX,Math.min(params.x,maxX));
  boolean controlsVisible=bubble.getVisibility()==View.VISIBLE||menuScroll.getVisibility()==View.VISIBLE;
  int catTop=cat.getTop(),minY=controlsVisible?0:-Math.max(0,Math.round(catTop+cat.visualTop()-dp(4)));
  int visibleBottom=Math.round(catTop+cat.visualBottom()),maxY=Math.max(minY,d.heightPixels-visibleBottom-dp(4));
  params.y=Math.max(minY,Math.min(params.y,maxY));
  wm.updateViewLayout(root,params);
 }
 void say(String text){say(text,null);}
 void say(String text,Runnable after){
  if(root==null)return;
  long version=++speechVersion;
  Runnable continuation=after==null?null:()->{if(speechVersion==version)after.run();};
  bubble.setText(text);bubble.setVisibility(View.VISIBLE);root.post(this::reposition);
  if(continuation!=null)h.postDelayed(continuation,2200);
  bubbleUntil=SystemClock.uptimeMillis()+5000;h.removeCallbacks(hide);h.postDelayed(hide,5000);
 }
 void sayChat(String text){
  if(root==null)return;++speechVersion;bubbleUntil=SystemClock.uptimeMillis()+10000;
  bubble.setText(text);bubble.setVisibility(View.VISIBLE);root.post(this::reposition);h.removeCallbacks(hide);h.postDelayed(hide,10000);
 }
 final Runnable hide=new Runnable(){public void run(){
  long remaining=bubbleUntil-SystemClock.uptimeMillis();
  if(bubbleUntil>0&&remaining>0){h.postDelayed(this,remaining);return;}
  bubbleUntil=0;if(bubble!=null){bubble.setVisibility(View.GONE);reposition();}
 }};
 void greet(){
  int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
  String hello=(hour>=5&&hour<12?"早上好":hour>=12&&hour<18?"下午好":"晚上好")+"，主人";
  String reminder=periodData.greetingReminder(java.time.LocalDate.now());
  if(reminder.isEmpty())say(hello,()->say("请尽情吩咐球球，主人"));
  else{
   String detail=reminder.startsWith("妈妈")?reminder.substring(2):reminder;
   say(hello.replace("主人","妈妈")+"，"+detail);
  }
 }
 String glyph(String name){if(name.contains("喂食")||name.equals("猫粮"))return "🍚";if(name.equals("猫条"))return "";if(name.equals("巧克力"))return "🍫";if(name.contains("铲屎"))return "🧹";if(name.equals("对话"))return "💬";if(name.contains("陪")||name.contains("摸摸"))return "♡";if(name.contains("悄悄话"))return "☀";if(name.equals("返回"))return "↩";if(name.contains("回家"))return "⌂";if(name.equals("锁定球球"))return "🔒";if(name.equals("收起")||name.equals("取消"))return "×";if(name.equals("知道啦")||name.equals("确认")||name.equals("答对了"))return "✓";if(name.equals("保留旧记忆"))return "↶";if(name.equals("替换为新内容"))return "↻";if(name.equals("教它改正"))return "✎";if(name.equals("发送"))return "➤";if(name.equals("记忆"))return "▦";if(name.equals("记录"))return "≡";if(name.equals("教球球"))return "✎";return "•";}
 static final class TreatIcon extends View {final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);TreatIcon(Context c){super(c);setContentDescription("猫条");setClickable(true);}protected void onDraw(Canvas c){super.onDraw(c);float s=Math.min(getWidth(),getHeight())/40f;c.save();c.scale(s,s);p.setColor(0xffff9eb8);c.drawRoundRect(13,7,27,33,5,5,p);p.setColor(0xffffedf2);c.drawRect(13,7,27,12,p);p.setColor(0xffe87599);c.drawCircle(20,7,3,p);p.setColor(0xffffd6e1);c.drawRoundRect(16,15,24,26,3,3,p);c.restore();}}
 void button(String name,Runnable action){
  View b=name.equals("猫条")?new TreatIcon(this):new TextView(this);if(b instanceof TextView){TextView t=(TextView)b;t.setText(glyph(name));t.setGravity(Gravity.CENTER);t.setTextSize(20);t.setTextColor(0xff4b5147);}b.setContentDescription(name);b.setTooltipText(name);b.setBackground(circle(0xfffffdf8));
  int i=panelButtons++;int[][] xy={{90,2},{18,72},{90,160},{158,72},{18,160},{158,160},{174,10}};int n=Math.min(i,xy.length-1);
  FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(belowPanel?32:40),dp(belowPanel?32:40));lp.leftMargin=dp(belowPanel?4+i*76:xy[n][0]);lp.topMargin=dp(belowPanel?belowTextHeight:xy[n][1]);b.setOnClickListener(v->action.run());panel.addView(b,lp);
 }
 android.graphics.drawable.GradientDrawable circle(int color){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setShape(android.graphics.drawable.GradientDrawable.OVAL);d.setStroke(dp(1),0xffd7deca);return d;}
 android.graphics.drawable.GradientDrawable card(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xffddd9cd);return d;}
 void focus(boolean yes){
  params.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|(yes?0:WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
  params.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;reposition();
 }
 void resizeOverlay(boolean menu){if(params==null)return;params.width=dp(menu?220:156);wm.updateViewLayout(root,params);}
 void showPanel(){resizeOverlay(true);menuScroll.setVisibility(View.VISIBLE);root.post(this::reposition);}
 void menu(boolean clicked){
  focus(false);resetPanel(false,210);showPanel();
  if(clicked){
   String fest=Dialogue.celebration(System.currentTimeMillis());
   if(fest.isEmpty())say("请尽情吩咐球球，主人");else say(fest,()->say("请尽情吩咐球球，主人"));
  }
  button("喂食",()->{
   resetPanel(false,210);for(String food:new String[]{"猫粮","猫条","巧克力"})button(food,()->feed(food));
   button("返回",()->menu(false));
  });
  button("铲屎 · 便便 "+state.poop+" / 尿团 "+state.urine,this::clean);
  button("对话",this::chat);button("陪球球玩",this::companionMenu);button("让球球回家",()->{state.save();stopSelf();});button("收起",this::close);button("锁定球球",()->setLocked(true));
 }
 void companionMenu(){
  resetPanel(true,120);long now=System.currentTimeMillis();belowTextHeight=76;showPanel();
  TextView info=new TextView(this);info.setTextColor(0xff67705e);info.setTextSize(14);info.setPadding(dp(8),dp(6),dp(8),dp(10));
  info.setText(companion.title()+" · 亲密度 "+companion.affection+" / 100\n相伴第 "+companion.days(now)+" 天 · "+companion.mood(now));FrameLayout.LayoutParams infoLp=new FrameLayout.LayoutParams(-1,-2);infoLp.topMargin=dp(2);info.setGravity(Gravity.CENTER);panel.addView(info,infoLp);
  button("摸摸球球",()->{
   if(busy){say("等球球忙完，再来贴贴主人");return;}
   boolean earned=companion.pet(System.currentTimeMillis());close();
   actions.start("happy","",5000,null,SystemClock.elapsedRealtime());syncAction();cat.heartsUntil=SystemClock.uptimeMillis()+5000;
   say(earned?"呼噜呼噜，最喜欢主人摸摸了！亲密度 +2":"再摸摸，球球还想贴着主人。 ");
  });
  button("今日悄悄话",()->{boolean earned=companion.visit(System.currentTimeMillis());say(companion.note(System.currentTimeMillis())+(earned?" 亲密度 +3":""));companionMenu();});
  button("返回",()->menu(false));root.post(this::reposition);
 }
 void close(){
  if(menuScroll.getVisibility()==View.VISIBLE){
   android.view.inputmethod.InputMethodManager imm=getSystemService(android.view.inputmethod.InputMethodManager.class);
   imm.hideSoftInputFromWindow(root.getWindowToken(),0);
  }
  menuScroll.setVisibility(View.GONE);resizeOverlay(false);focus(false);
 }
 void setLocked(boolean value){
  locked=value;state.p.edit().putBoolean("locked",locked).apply();
  if(menuScroll!=null)menuScroll.setVisibility(View.GONE);
  if(bubble!=null)bubble.setVisibility(View.GONE);
  if(params==null||root==null)return;
  params.width=dp(156);
  params.alpha=1.0f;root.setAlpha(1.0f);cat.setAlpha(1.0f);
  params.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|(locked?WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE:0);
  wm.updateViewLayout(root,params);
  if(!locked&&screen)root.setVisibility(View.VISIBLE);
  if(!busy&&!locked)restartIdle();
 }
 void chat(){
  teachingMode=false;resetPanel(true,76);showPanel();focus(true);
  EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(12);input.setPadding(dp(6),0,dp(6),0);input.setBackground(card(0xfffffdf8,10));input.setHint("和球球说话…");input.setContentDescription("对球球说的话");FrameLayout.LayoutParams inputLp=new FrameLayout.LayoutParams(dp(136),dp(32));inputLp.leftMargin=dp(40);inputLp.topMargin=dp(0);panel.addView(input,inputLp);
  Runnable[] sendAction=new Runnable[1];sendAction[0]=()->{
   String value=input.getText().toString().trim();if(value.isEmpty())return;
   if(correcting){correcting=false;String question=lastQuestion;input.setText("");say("球球根据纠正重新回答……");chatEngine.correct(question,value,result->{lastAnswer=result.text;sayChat(result.text);if(!result.error)showAnswerFeedback(input,sendAction[0]);else chat();});showThinkingPanel();return;}
   if(teachingMode){String[] parts=value.split("(?:=>|→)",2);if(parts.length!=2){sayChat("请用“问题 => 希望的回答”来教球球。 ");return;}say("球球正在学习……");chatEngine.teach(parts[0],parts[1],result->{sayChat(result.text);teachingMode=false;input.setHint("和球球说话…");});input.setText("");return;}
   say("球球正在想……");input.setText("");chatEngine.send(value,petSnapshot(),result->{sayChat(result.text);if(result.awaitingConfirmation)showMemoryConfirmation(result.conflictChoice);else if(result.error)chat();else{lastQuestion=value;lastAnswer=result.text;showAnswerFeedback(input,sendAction[0]);}});showThinkingPanel();
  };
  input.setOnEditorActionListener((v,id,event)->{sendAction[0].run();return true;});
  chatAction("返回",4,38,()->menu(false));chatAction("教球球",46,38,()->{teachingMode=true;input.setHint("问题 => 希望的回答");input.requestFocus();});chatAction("记忆",88,38,()->openActivity(MemoryActivity.class));chatAction("记录",130,38,()->openActivity(ChatHistoryActivity.class));chatAction("发送",172,38,sendAction[0]);root.post(this::reposition);
 }
 void chatAction(String name,int left,int top,Runnable action){
  int before=panel.getChildCount();button(name,action);View view=panel.getChildAt(before);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)view.getLayoutParams();lp.leftMargin=dp(left);lp.topMargin=dp(top);view.setLayoutParams(lp);
 }
 void showMemoryConfirmation(){showMemoryConfirmation(false);}
 void showMemoryConfirmation(boolean conflict){
  focus(false);resetPanel(true,40);showPanel();
  if(conflict){chatAction("保留旧记忆",24,2,()->chatEngine.keepOld(result->{sayChat(result.text);chat();}));chatAction("替换为新内容",88,2,()->chatEngine.confirm(result->{sayChat(result.text);chat();}));chatAction("取消",152,2,()->chatEngine.reject(result->{sayChat(result.text);chat();}));}
  else{chatAction("确认",45,2,()->chatEngine.confirm(result->{sayChat(result.text);chat();}));chatAction("取消",95,2,()->chatEngine.reject(result->{sayChat(result.text);chat();}));chatAction("返回",145,2,()->menu(false));}
 }
 void showAnswerFeedback(EditText input,Runnable send){
  focus(false);resetPanel(true,40);showPanel();
  chatAction("答对了",38,2,()->{sayChat("谢谢主人确认，球球会继续按这个方式陪你聊天。 ");chat();});
  chatAction("教它改正",100,2,()->{correcting=true;input.setText("");input.setHint("输入正确内容后发送");resetPanel(true,76);showPanel();FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(136),dp(32));lp.leftMargin=dp(40);panel.addView(input,lp);chatAction("返回",4,38,()->{correcting=false;chat();});chatAction("发送",172,38,send);focus(true);input.requestFocus();});
  chatAction("返回",162,2,()->chat());
 }
 void showThinkingPanel(){focus(false);resetPanel(true,40);showPanel();chatAction("取消",70,2,()->chatEngine.cancel());chatAction("返回",120,2,()->menu(false));}
 void openActivity(Class<? extends Activity> type){startActivity(new Intent(this,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
 PetContextSnapshot petSnapshot(){long now=System.currentTimeMillis();return new PetContextSnapshot(companion.affection,companion.days(now),companion.title(),companion.mood(now),state.poop,state.urine,busy?cat.mode:"待机",now);}
 void feed(String food){
  long now=System.currentTimeMillis();
  if(busy){say(now<state.feedUntil?"十秒内不能再次喂食，还剩 "+Math.max(1,(state.feedUntil-now+999)/1000)+" 秒":"球球正在忙，稍等一下哦");return;}
  if(!state.feed(now)){say("十秒内不能再次喂食，还剩 "+Math.max(1,(state.feedUntil-now+999)/1000)+" 秒");return;}
  close();actions.start("eat",food,5000,()->{
   if(food.equals("猫条")||food.equals("巧克力")){
    actions.start(food.equals("猫条")?"happy":"disgust","",5000,null,SystemClock.elapsedRealtime());
    say(food.equals("猫条")?"主人你真好":"狗屎东西");
   }
  },SystemClock.elapsedRealtime());syncAction();
 }
 void syncAction(){
  boolean wasBusy=busy;busy=actions.busy();cat.food=actions.food();
  cat.actionStartedUptime=busy?SystemClock.uptimeMillis()-actions.elapsed(SystemClock.elapsedRealtime()):-1;
  if(busy)cat.mode=actions.mode();else if(wasBusy){restartIdle();}
 }
 void clean(){
  if(busy){say("球球正在忙，稍等一下哦");return;}close();
  actions.start("clean","",7000,()->{
   int poop=state.poop,urine=state.urine;state.clear();cat.poop=cat.urine=0;
   say("铲完啦！一共有 "+poop+" 个便便、"+urine+" 个尿团");
   resetPanel(true,144);belowTextHeight=108;showPanel();TextView result=new TextView(this);result.setTextSize(14);
   result.setText("本次清理\n便便："+poop+" 个\n尿团："+urine+" 个\n猫砂盆已清空");
   panel.addView(result);button("知道啦",this::close);
  },SystemClock.elapsedRealtime());syncAction();
 }
 final Runnable tick=new Runnable(){
  public void run(){
   if(root==null)return;
   long now=System.currentTimeMillis(),motionNow=SystemClock.elapsedRealtime();actions.update(motionNow);syncAction();
   cat.urine=state.urine;cat.poop=state.poop;
   if(screen&&!actions.toilet()){
    PetState.Event event=state.nextDue(now);
    if(event!=null){
     actions.interrupt(event.kind==1?"pee":"poop",5000,()->state.finish(event),motionNow);syncAction();
    }
   }
   long uptime=SystemClock.uptimeMillis();if(idle.advance(uptime,screen&&!busy&&!dragging,cat.mode))cat.mode=IdleMotion.next(cat.mode,random);
   cat.idleStartedUptime=uptime-idle.elapsed;layoutPet();h.postDelayed(this,50);
  }
 };
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"stop".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  if(i!=null&&"unlock".equals(i.getAction())){setLocked(false);return START_STICKY;}
  return root==null?START_NOT_STICKY:START_STICKY;
 }
 public void onDestroy(){
  running=false;
  h.removeCallbacksAndMessages(null);
  if(chatEngine!=null)chatEngine.close();
  if(root!=null){unregisterReceiver(receiver);wm.removeView(root);}
  if(state!=null)state.save();super.onDestroy();
 }
}






