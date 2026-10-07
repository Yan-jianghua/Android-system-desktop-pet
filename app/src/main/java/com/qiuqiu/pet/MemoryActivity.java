package com.qiuqiu.pet;

import android.app.*;
import android.graphics.Color;
import android.os.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public class MemoryActivity extends Activity {
 LinearLayout memoriesView,rulesView;EditText newMemory,question,answer;QiuqiuDatabase db;MemoryRepository repository;final ExecutorService worker=Executors.newSingleThreadExecutor();
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout p,String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(0xff443f39);v.setPadding(0,dp(6),0,dp(6));p.addView(v);return v;}
 EditText input(LinearLayout p,String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(false);e.setMaxLines(3);p.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
 Button button(LinearLayout p,String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->action.run());p.addView(b);return b;}
 public void onCreate(Bundle state){
  super.onCreate(state);db=QiuqiuDatabase.get(this);repository=new MemoryRepository(db);ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(24),dp(22),dp(30));body.setBackgroundColor(Color.rgb(248,249,242));scroll.addView(body);
  text(body,"球球的记忆",28);text(body,"只有主人确认过的内容才会成为长期记忆。可以在这里新增、修改和删除。",13);newMemory=input(body,"新增一条记忆");button(body,"添加记忆",()->{String value=newMemory.getText().toString().trim();if(value.isEmpty())return;worker.execute(()->{repository.add(value,"手动新增");runOnUiThread(()->{newMemory.setText("");load();});});});memoriesView=new LinearLayout(this);memoriesView.setOrientation(LinearLayout.VERTICAL);body.addView(memoriesView);
  text(body,"教球球回答",24);text(body,"明确教学会优先于 AI 自由回答。",13);question=input(body,"当我问……");answer=input(body,"希望球球回答……");button(body,"保存教学",this::addTeaching);rulesView=new LinearLayout(this);rulesView.setOrientation(LinearLayout.VERTICAL);body.addView(rulesView);setContentView(scroll);load();
 }
 void load(){worker.execute(()->{List<MemoryRecord> memories=repository.all();List<TeachingRule> rules=db.teaching().all();runOnUiThread(()->render(memories,rules));});}
 void render(List<MemoryRecord> memories,List<TeachingRule> rules){
  memoriesView.removeAllViews();if(memories.isEmpty())text(memoriesView,"球球还没有长期记忆。",13);
  for(MemoryRecord m:memories){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);TextView content=text(row,"["+m.type+"] "+m.content,15);LinearLayout actions=new LinearLayout(this);row.addView(actions);button(actions,"编辑",()->edit(m));button(actions,"删除",()->new AlertDialog.Builder(this).setMessage("删除“"+m.content+"”？").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->worker.execute(()->{repository.delete(m.id);runOnUiThread(this::load);})).show());memoriesView.addView(row);}
  rulesView.removeAllViews();if(rules.isEmpty())text(rulesView,"还没有教学规则。",13);
  for(TeachingRule rule:rules){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);TextView v=text(row,"问："+rule.question+"\n答："+rule.answer,14);row.getChildAt(0).setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));button(row,"删除",()->worker.execute(()->{db.teaching().delete(rule.id);runOnUiThread(this::load);}));rulesView.addView(row);}
 }
 void edit(MemoryRecord memory){EditText value=new EditText(this);value.setText(memory.content);new AlertDialog.Builder(this).setTitle("修改记忆").setView(value).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{String changed=value.getText().toString().trim();if(changed.isEmpty()){Toast.makeText(this,"记忆内容不能为空",Toast.LENGTH_SHORT).show();return;}worker.execute(()->{repository.update(memory,changed);runOnUiThread(this::load);});}).show();}
 void addTeaching(){String q=question.getText().toString().trim(),a=answer.getText().toString().trim();if(q.isEmpty()||a.isEmpty()){Toast.makeText(this,"问题和回答都要填写",Toast.LENGTH_SHORT).show();return;}worker.execute(()->{db.teaching().insert(new TeachingRule(q,a,System.currentTimeMillis()));runOnUiThread(()->{question.setText("");answer.setText("");load();});});}
 @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
