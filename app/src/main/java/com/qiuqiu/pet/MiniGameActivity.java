package com.qiuqiu.pet;

import android.app.*;
import android.os.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MiniGameActivity extends Activity {
 final Handler handler=new Handler(Looper.getMainLooper());
 final Random random=new Random();
 LinearLayout root;FrameLayout arena;TextView status;int score;

 int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(0xff5a4545);t.setGravity(Gravity.CENTER);t.setPadding(dp(5),dp(7),dp(5),dp(7));return t;}

 @Override public void onCreate(Bundle state){super.onCreate(state);showMenu();}

 void base(String title,String subtitle){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(16),dp(20),dp(16),dp(28));root.setBackgroundColor(0xfffff8f3);scroll.addView(root);
  TextView heading=text(title,27);heading.setTypeface(Typeface.DEFAULT_BOLD);root.addView(heading,new LinearLayout.LayoutParams(-1,-2));root.addView(text(subtitle,14),new LinearLayout.LayoutParams(-1,-2));setContentView(scroll);
 }

 void showMenu(){handler.removeCallbacksAndMessages(null);base("🎮 球球游戏屋","不是填写表单——直接选择游戏开始玩，成绩会离线保存并同步给另一位主人。");addGameButton("🧶","毛线球追逐","点到 12 次，看看谁的手速更快",v->startYarn());addGameButton("🧠","记忆翻牌","翻开相同图案，找齐 6 对",v->startMemory());addGameButton("💞","默契选择","从图卡中选出今晚想一起做的事",v->startChoice());}

 void addGameButton(String icon,String name,String desc,View.OnClickListener action){Button b=new Button(this);b.setAllCaps(false);b.setText(icon+"\n"+name+"\n"+desc);b.setTextSize(16);b.setGravity(Gravity.CENTER);b.setOnClickListener(action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(112));lp.setMargins(0,dp(7),0,dp(7));root.addView(b,lp);}

 void addBack(){Button back=new Button(this);back.setText("⌂ 返回游戏屋");back.setOnClickListener(v->showMenu());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(12),0,0);root.addView(back,lp);}

 void startYarn(){base("🧶 毛线球追逐","毛线球会到处跑，点中 12 次就胜利！");status=text("已抓到 0 / 12",18);root.addView(status,new LinearLayout.LayoutParams(-1,-2));arena=new FrameLayout(this);arena.setBackgroundColor(0xffffeee8);LinearLayout.LayoutParams areaLp=new LinearLayout.LayoutParams(-1,dp(430));areaLp.setMargins(0,dp(8),0,0);root.addView(arena,areaLp);score=0;Button yarn=new Button(this);yarn.setText("🧶");yarn.setTextSize(29);yarn.setContentDescription("会移动的毛线球");FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(76),dp(76));arena.addView(yarn,lp);yarn.setOnClickListener(v->{score++;status.setText("已抓到 "+score+" / 12");if(score>=12){finishGame("🧶 毛线球追逐","抓住毛线球 12 次，挑战完成！");}else moveYarn(yarn);});arena.post(()->moveYarn(yarn));addBack();}

 void moveYarn(View yarn){int maxX=Math.max(1,arena.getWidth()-yarn.getWidth());int maxY=Math.max(1,arena.getHeight()-yarn.getHeight());yarn.animate().x(random.nextInt(maxX)).y(random.nextInt(maxY)).setDuration(130).start();}

 void startMemory(){base("🧠 记忆翻牌","每次翻两张，找出全部相同的球球宝物。");status=text("已找到 0 / 6 对",18);root.addView(status,new LinearLayout.LayoutParams(-1,-2));GridLayout board=new GridLayout(this);board.setColumnCount(3);root.addView(board,new LinearLayout.LayoutParams(-1,-2));List<String> cards=new ArrayList<>(Arrays.asList("🐟","🧶","🎀","🌙","🍓","🐾","🐟","🧶","🎀","🌙","🍓","🐾"));Collections.shuffle(cards);Button[] buttons=new Button[12];boolean[] matched=new boolean[12];int[] first={-1};boolean[] locked={false};int[] pairs={0};
  for(int i=0;i<12;i++){final int index=i;Button card=new Button(this);card.setText("❓");card.setTextSize(27);card.setContentDescription("未翻开的卡片 "+(i+1));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(82);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));board.addView(card,lp);buttons[i]=card;card.setOnClickListener(v->{if(locked[0]||matched[index]||first[0]==index)return;card.setText(cards.get(index));if(first[0]<0){first[0]=index;return;}int previous=first[0];if(cards.get(previous).equals(cards.get(index))){matched[previous]=matched[index]=true;pairs[0]++;first[0]=-1;status.setText("已找到 "+pairs[0]+" / 6 对");if(pairs[0]==6)finishGame("🧠 记忆翻牌","成功找齐 6 对宝物！");}else{locked[0]=true;handler.postDelayed(()->{buttons[previous].setText("❓");buttons[index].setText("❓");first[0]=-1;locked[0]=false;},650);}});}
  addBack();
 }

 void startChoice(){base("💞 默契选择","点一张今晚最想和对方一起完成的图卡。");status=text("等待你的选择…",17);root.addView(status,new LinearLayout.LayoutParams(-1,-2));GridLayout choices=new GridLayout(this);choices.setColumnCount(2);root.addView(choices,new LinearLayout.LayoutParams(-1,-2));String[] values={"🍜 一起吃点好吃的","🎬 一起看电影","🌳 一起散步","🎮 一起玩游戏","📞 认真聊聊天","🌙 早点休息"};for(String value:values){Button tile=new Button(this);tile.setAllCaps(false);tile.setText(value);tile.setTextSize(17);tile.setOnClickListener(v->{for(int i=0;i<choices.getChildCount();i++)choices.getChildAt(i).setAlpha(choices.getChildAt(i)==v?1f:.45f);status.setText("已选："+value);SharedFeatureStore.save(this,"异步双人小游戏","💞 默契选择",value,"shared");Toast.makeText(this,"选择已送给另一位主人 💗",Toast.LENGTH_SHORT).show();});GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(92);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));choices.addView(tile,lp);}addBack();}

 void finishGame(String title,String result){SharedFeatureStore.save(this,"异步双人小游戏",title,result,"shared");new AlertDialog.Builder(this).setTitle("🎉 挑战完成").setMessage(result+"\n成绩已保存，联网后会同步给对方。").setCancelable(false).setPositiveButton("再玩一次",(d,w)->showMenu()).show();}

 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
