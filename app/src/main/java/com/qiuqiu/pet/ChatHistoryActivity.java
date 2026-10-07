package com.qiuqiu.pet;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatHistoryActivity extends Activity {
 final ExecutorService worker=Executors.newSingleThreadExecutor();LinearLayout body;
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 public void onCreate(Bundle state){super.onCreate(state);ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(24),dp(22),dp(30));body.setBackgroundColor(Color.rgb(248,249,242));scroll.addView(body);TextView title=new TextView(this);title.setText("球球的聊天记录");title.setTextSize(28);title.setTextColor(0xff443f39);body.addView(title);setContentView(scroll);load();}
 void load(){worker.execute(()->{List<ChatMessage> list=new ChatRepository(QiuqiuDatabase.get(this)).recent(100);runOnUiThread(()->render(list));});}
 void render(List<ChatMessage> list){if(list.isEmpty()){add("还没有聊天记录。",13);return;}SimpleDateFormat format=new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault());for(ChatMessage message:list)add((message.role.equals("user")?"主人":"球球")+" · "+format.format(new Date(message.createdAt))+"\n"+message.content,14);}
 void add(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(0xff443f39);text.setPadding(0,dp(9),0,dp(9));body.addView(text);}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
