package com.qiuqiu.pet;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** 经期日历：记录“来了 / 走了”，查看算法标出的经期、排卵期、安全期与下次经期预测。 */
public class PeriodCalendarActivity extends Activity {
 static final String[] WEEK_CN={"周日","周一","周二","周三","周四","周五","周六"};
 static final int C_PERIOD=0xfff07a9b, C_PREDICT=0xfffce4ea, C_FERTILE=0xffe4dbf8,
  C_SAFE=0xffe5f2e0, C_NORMAL=0xffffffff, C_TODAY_RING=0xff8fa678;
 PeriodData data;
 LocalDate cursor;
 TextView monthTitle, stats;
 final Cell[][] cells=new Cell[6][7];
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

 static final class Cell extends LinearLayout {
  final TextView num, tag;
  Cell(Activity a){
   super(a);
   setOrientation(VERTICAL);setGravity(Gravity.CENTER);
   setClipToOutline(false);
   num=new TextView(a);num.setTextSize(14);num.setGravity(Gravity.CENTER);
   tag=new TextView(a);tag.setTextSize(8);tag.setGravity(Gravity.CENTER);
   addView(num,new LayoutParams(-2,-2));addView(tag,new LayoutParams(-2,-2));
  }
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  data=new PeriodData(this);
  cursor=LocalDate.now();
  ScrollView scroll=new ScrollView(this);
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
  body.setPadding(dp(16),dp(22),dp(16),dp(24));body.setBackgroundColor(Color.rgb(248,249,242));
  scroll.addView(body);

  TextView title=text(body,"女生经期日历",26,0xff443f39);title.setPadding(0,0,0,dp(2));
  text(body,"记录来了 / 走了，自动标出排卵期和安全期，预测下次经期",13,0xff8a857c);

  stats=text(body,"",14);
  GradientDrawable statsBg=rounded(0xffffffff,16,0xffe3ded2,1);
  stats.setBackground(statsBg);stats.setPadding(dp(14),dp(12),dp(14),dp(12));
  LinearLayout.LayoutParams statsLp=new LinearLayout.LayoutParams(-1,-2);
  statsLp.setMargins(0,dp(12),0,dp(10));stats.setLayoutParams(statsLp);

  LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
  body.addView(nav);
  Button prev=navBtn("‹");Button next=navBtn("›");Button today=navBtn("今天");
  monthTitle=text(nav,"",17);
  LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(0,-2,1f);
  titleLp.gravity=Gravity.CENTER;
  nav.addView(prev);nav.addView(monthTitle,titleLp);nav.addView(today);nav.addView(next);
  prev.setOnClickListener(v->{cursor=cursor.minusMonths(1);render();});
  next.setOnClickListener(v->{cursor=cursor.plusMonths(1);render();});
  today.setOnClickListener(v->{cursor=LocalDate.now();render();});

  LinearLayout weekHead=new LinearLayout(this);
  for(int i=0;i<7;i++){
   TextView w=text(weekHead,new String[]{"日","一","二","三","四","五","六"}[i],13,0xff8a857c);
   w.setGravity(Gravity.CENTER);
   weekHead.addView(w,new LinearLayout.LayoutParams(0,-2,1f));
  }
  LinearLayout.LayoutParams whLp=new LinearLayout.LayoutParams(-1,-2);whLp.setMargins(0,dp(10),0,dp(2));
  body.addView(weekHead,whLp);

  LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
  GradientDrawable gridBg=rounded(0xffffffff,14,0xffe7e2d7,1);
  grid.setBackground(gridBg);grid.setPadding(dp(3),dp(3),dp(3),dp(3));
  for(int r=0;r<6;r++){
   LinearLayout row=new LinearLayout(this);
   for(int c=0;c<7;c++){
    Cell cell=new Cell(this);cells[r][c]=cell;
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(48),1f);
    cp.setMargins(dp(1),dp(1),dp(1),dp(1));
    row.addView(cell,cp);
   }
   grid.addView(row);
  }
  body.addView(grid);
  body.addView(legend());
  TextView tip=text(body,"用法：例假来的那天点一下日期，选“来了”；走的那天选“走了”。球球会据此学习你的周期，亮屏问候时在经期前一天和当天提醒带卫生巾。",12,0xff8a857c);
  tip.setPadding(dp(4),dp(10),dp(4),0);

  setContentView(scroll);
  render();
 }
 TextView text(LinearLayout parent,String value,int size){return text(parent,value,size,0xff443f39);}
 TextView text(LinearLayout parent,String value,int size,int color){
  TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);
  t.setLineSpacing(dp(2),1f);parent.addView(t);return t;
 }
 Button navBtn(String label){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);
  b.setTextSize(label.length()>1?13:20);b.setTextColor(0xff5b6653);
  b.setBackground(rounded(0xfff0f3e9,14,0xffd7deca,1));
  b.setPadding(dp(12),dp(4),dp(12),dp(4));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(3),0,dp(3),0);
  b.setLayoutParams(lp);return b;
 }
 GradientDrawable rounded(int color,int radius,int strokeColor,int strokeDp){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
  if(strokeDp>0)d.setStroke(dp(strokeDp),strokeColor);
  return d;
 }
 LinearLayout legend(){
  LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
  wrap.setPadding(dp(2),dp(14),dp(2),0);
  Object[][] items={{C_PERIOD,"经期"},{C_PREDICT,"预测经期"},
   {C_FERTILE,"排卵期"},{C_SAFE,"安全期"}};
  LinearLayout row=new LinearLayout(this);
  for(int i=0;i<items.length;i++){
   LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);
   View dot=new View(this);
   int dotColor=(int)items[i][0];
   GradientDrawable dotBg=rounded(dotColor,6,0xffd8d3c8,1);
   if("预测经期".equals(items[i][1]))dotBg.setStroke(dp(1),0xffe88baa,dp(4),dp(2));
   dot.setBackground(dotBg);
   LinearLayout.LayoutParams dotLp=new LinearLayout.LayoutParams(dp(14),dp(14));
   dotLp.setMargins(0,0,dp(5),0);item.addView(dot,dotLp);
   TextView t=text(item,(String)items[i][1],12,0xff6b665c);item.addView(t);
   row.addView(item,new LinearLayout.LayoutParams(0,-2,1f));
  }
  wrap.addView(row);return wrap;
 }
 void render(){
  monthTitle.setText(cursor.getYear()+"年 "+cursor.getMonthValue()+"月");
  stats.setText(buildStats());
  LocalDate first=cursor.withDayOfMonth(1);
  int offset=first.getDayOfWeek().getValue()%7; // 周日为第一列
  LocalDate d0=first.minusDays(offset);
  LocalDate today=LocalDate.now();
  for(int r=0;r<6;r++)for(int c=0;c<7;c++){
   LocalDate d=d0.plusDays((long)r*7+c);
   bindCell(cells[r][c],d,d.getMonthValue()==cursor.getMonthValue(),d.equals(today));
  }
 }
 String buildStats(){
  if(!data.hasData())
   return "还没有记录\n在例假来的那天点日历选“来了”，走的那天选“走了”，球球就能算出排卵期、安全期并预测下次经期。";
  LocalDate today=LocalDate.now();
  LocalDate next=data.nextPredictedStart(today);
  LocalDate ov=next.minusDays(14);
  long days=java.time.temporal.ChronoUnit.DAYS.between(today,next);
  return "平均周期 "+data.cycleLength()+" 天 · 平均经期 "+data.periodLength()+" 天\n"
   +"下次经期预测："+md(next)+"（"+WEEK_CN[next.getDayOfWeek().getValue()%7]+"）"
   +(days==0?"，就是今天":days>0?"，还有 "+days+" 天":"")+"\n"
   +"预测排卵日："+md(ov)+"（"+WEEK_CN[ov.getDayOfWeek().getValue()%7]+"）";
 }
 static String md(LocalDate d){return d.getMonthValue()+"月"+d.getDayOfMonth()+"日";}
 void bindCell(Cell cell,LocalDate d,boolean inMonth,boolean today){
  PeriodData.Type type=data.classify(d);
  int bg,fg;
  switch(type){
   case PERIOD:bg=C_PERIOD;fg=0xffffffff;break;
   case PREDICTED_PERIOD:bg=C_PREDICT;fg=0xffc2567c;break;
   case FERTILE:bg=C_FERTILE;fg=0xff6a53a6;break;
   case SAFE:bg=C_SAFE;fg=0xff5e7c55;break;
   default:bg=C_NORMAL;fg=0xff55504a;
  }
  if(!inMonth)fg=0xffc3bfb6;
  GradientDrawable bgd=rounded(bg,10,0,0);
  if(type==PeriodData.Type.PREDICTED_PERIOD)bgd.setStroke(dp(1),0xffe88baa,dp(4),dp(2));
  if(today)bgd.setStroke(dp(2),C_TODAY_RING);
  cell.setBackground(bgd);
  cell.num.setText(String.valueOf(d.getDayOfMonth()));
  cell.num.setTextColor(fg);
  cell.num.setTypeface(today?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);
  String label=data.dayLabel(d);
  cell.tag.setText(label);
  int tagColor=type==PeriodData.Type.PERIOD?0xffffe3ec:fg;
  if(label.equals("卵"))tagColor=type==PeriodData.Type.PERIOD?0xffffffff:0xff8a4fb8;
  cell.tag.setTextColor(tagColor);
  cell.setOnClickListener(v->openDay(d));
 }
 void openDay(LocalDate d){
  String dateText=d.getYear()+"年"+d.getMonthValue()+"月"+d.getDayOfMonth()+"日 "
   +WEEK_CN[d.getDayOfWeek().getValue()%7];
  PeriodData.Period on=data.periodOn(d);
  String stateText;
  if(data.periodStarting(d)!=null)stateText="这一天标记了“来了”（例假开始）";
  else if(data.periodEnding(d)!=null)stateText="这一天标记了“走了”（例假结束）";
  else if(on!=null)stateText="这一天处于经期中";
  else stateText="这一天还没有标记";
  boolean marked=data.periodStarting(d)!=null||data.periodEnding(d)!=null;
  AlertDialog.Builder b=new AlertDialog.Builder(this);
  b.setTitle(dateText).setMessage(stateText+"\n\n要把这一天记成哪一天？");
  b.setItems(marked?new String[]{"来了（例假开始）","走了（例假结束）","清除这一天的标记"}
                      :new String[]{"来了（例假开始）","走了（例假结束）"},(dialog,which)->{
   if(which==0){data.markStart(d);toast("已记录："+md(d)+" 来了");render();}
   else if(which==1){
    if(data.markEnd(d)){toast("已记录："+md(d)+" 走了");render();}
    else toast("请先在例假来的那天标记“来了”");
   } else if(which==2){data.clearDay(d);toast("已清除这一天的标记");render();}
  });
  b.setNegativeButton("取消",null).show();
 }
 void toast(String t){android.widget.Toast.makeText(this,t,android.widget.Toast.LENGTH_SHORT).show();}
}
