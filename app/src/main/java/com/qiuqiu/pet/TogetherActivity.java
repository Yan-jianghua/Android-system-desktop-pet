package com.qiuqiu.pet;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

public class TogetherActivity extends Activity {
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

 @Override public void onCreate(Bundle state){super.onCreate(state);
  ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(20),dp(16),dp(28));body.setBackgroundColor(0xfffff8f3);scroll.addView(body);
  TextView title=text("💞 双人共养乐园",28);title.setTypeface(Typeface.DEFAULT_BOLD);body.addView(title);
  body.addView(text("点图案就能互动。照片、投票、装扮和小游戏都支持离线保存，恢复网络后自动同步。",14));
  addSection(body,"💗 今天一起",0,8);
  addSection(body,"📷 回忆收藏",8,15);
  addSection(body,"🌳 成长装扮",15,23);
  addSection(body,"✨ 默契与守护",23,FeatureCatalog.ALL.length);
  setContentView(scroll);
 }

 TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(0xff5a4545);t.setPadding(dp(4),dp(7),dp(4),dp(7));return t;}

 void addSection(LinearLayout body,String heading,int start,int end){
  TextView h=text(heading,20);h.setTypeface(Typeface.DEFAULT_BOLD);h.setPadding(dp(4),dp(18),0,dp(7));body.addView(h);
  GridLayout grid=new GridLayout(this);grid.setColumnCount(2);body.addView(grid,new LinearLayout.LayoutParams(-1,-2));
  for(int i=start;i<end;i++){FeatureCatalog.Def def=FeatureCatalog.ALL[i];Button tile=new Button(this);tile.setAllCaps(false);tile.setText(icon(def.name)+"\n"+shortName(def.name));tile.setTextSize(14);tile.setGravity(Gravity.CENTER);tile.setContentDescription(def.name+"，"+def.description);tile.setOnClickListener(v->open(def));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(96);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(tile,lp);}
 }

 void open(FeatureCatalog.Def def){Class<?> target=def.name.equals("异步双人小游戏")?MiniGameActivity.class:VisualFeatureActivity.class;startActivity(new Intent(this,target).putExtra("category",def.name).putExtra("description",def.description));}
 String shortName(String n){return n.replace("双人","").replace("共同","").replace("每日","");}
 String icon(String n){if(n.contains("游戏")||n.contains("猜"))return "🎮";if(n.contains("照片")||n.contains("日记")||n.contains("回顾")||n.contains("纪念"))return "📷";if(n.contains("旅行")||n.contains("明信片"))return "🗺️";if(n.contains("成长")||n.contains("性格"))return "🌱";if(n.contains("任务")||n.contains("接力"))return "✅";if(n.contains("衣柜")||n.contains("装修"))return "🎀";if(n.contains("抱抱")||n.contains("心情")||n.contains("情侣"))return "💗";if(n.contains("哄睡"))return "🌙";if(n.contains("数据")||n.contains("记忆"))return "🔐";return "🐾";}
}
