package com.qiuqiu.pet;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;import java.util.concurrent.*;

public class FeatureActivity extends Activity {
 final ExecutorService io=Executors.newSingleThreadExecutor();LinearLayout list;String category,description;FeatureCatalog.Def def;QiuqiuDatabase db;ServerConfig cfg;
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout p,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(0xff443f39);t.setPadding(0,dp(6),0,dp(6));p.addView(t);return t;}
 @Override public void onCreate(Bundle s){super.onCreate(s);category=getIntent().getStringExtra("category");description=getIntent().getStringExtra("description");def=FeatureCatalog.get(category);db=QiuqiuDatabase.get(this);cfg=new ServerConfig(this);ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(20),dp(18),dp(28));body.setBackgroundColor(0xfff8f5f2);scroll.addView(body);text(body,category,27);text(body,description,13);text(body,"离线可用 · 每条记录显示共享范围 · 联网后自动合并",12);Button add=new Button(this);add.setText("新增"+category);add.setAllCaps(false);add.setOnClickListener(v->addDialog());body.addView(add,new LinearLayout.LayoutParams(-1,dp(50)));list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);body.addView(list);setContentView(scroll);load();}
 void load(){io.execute(()->{List<SharedItem> items=db.shared().byCategory(category);runOnUiThread(()->render(items));});}
 void render(List<SharedItem> items){list.removeAllViews();if(items.isEmpty()){text(list,"还没有内容。现在创建也会立即保存在本机，服务器恢复后自动同步。",14);return;}for(SharedItem item:items){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(8),dp(12),dp(8));card.setBackgroundColor(Color.WHITE);text(card,item.title+(item.status.equals("completed")?"  ✓ 已完成":"  · 进行中"),17);text(card,item.body,14);String privacy=item.privacyScope.equals("shared")?"👥 双方共享":item.privacyScope.equals("sensitive")?"🔒 敏感内容，仅本机":"👤 仅自己";text(card,privacy+" · "+new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(item.updatedAt)),11);LinearLayout actions=new LinearLayout(this);card.addView(actions);Button complete=new Button(this);complete.setText(item.status.equals("completed")?"重新打开":def.doneLabel);complete.setAllCaps(false);complete.setOnClickListener(v->update(item,item.status.equals("completed")?"active":"completed",false));actions.addView(complete,new LinearLayout.LayoutParams(0,dp(44),1));Button edit=new Button(this);edit.setText("编辑");edit.setAllCaps(false);edit.setOnClickListener(v->editDialog(item));actions.addView(edit,new LinearLayout.LayoutParams(0,dp(44),1));Button del=new Button(this);del.setText("删除");del.setAllCaps(false);del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("删除后会同步删除墓碑，旧设备不会把内容恢复。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->update(item,item.status,true)).show());actions.addView(del,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));list.addView(card,lp);}}
 void addDialog(){showEditor(null);}
 void editDialog(SharedItem item){showEditor(item);}
 void showEditor(SharedItem existing){
  LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),0);
  EditText title=new EditText(this);title.setHint(def.titleHint);if(existing!=null)title.setText(existing.title);form.addView(title);
  EditText body=new EditText(this);body.setHint(def.bodyHint);body.setMinLines(3);if(existing!=null)body.setText(existing.body);form.addView(body);
  EditText date=null;if(def.supportsDate){date=new EditText(this);date.setHint("日期或解锁时间，例如 2026-12-31 20:00");form.addView(date);}
  CheckBox personal=new CheckBox(this);personal.setText(def.sensitive?"默认仅自己保存；取消勾选后双方共享":"仅自己保存，不同步给另一位主人");personal.setChecked(existing!=null?!existing.privacyScope.equals("shared"):def.sensitive);form.addView(personal);
  EditText finalDate=date;
  new AlertDialog.Builder(this).setTitle(existing==null?"新增"+category:"编辑"+category).setView(form).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
   String rawTitle=title.getText().toString().trim();final String savedTitle=rawTitle.isEmpty()?category+"记录":rawTitle;
   String rawBody=body.getText().toString().trim(),dateText=finalDate==null?"":finalDate.getText().toString().trim();final String savedBody=dateText.isEmpty()?rawBody:rawBody+"\n日期："+dateText;
   final String scope=personal.isChecked()?(def.sensitive?"sensitive":"personal"):"shared";final boolean wasShared=existing!=null&&existing.privacyScope.equals("shared");
   Runnable persist=()->persist(existing,savedTitle,savedBody,scope,wasShared);
   if(def.sensitive&&scope.equals("shared"))new AlertDialog.Builder(this).setTitle("确认共享敏感内容").setMessage("这条内容会上传到私人服务器并同步给另一位主人。请确认正文不包含经期日期、住址、财务或账号密钥。").setNegativeButton("取消",null).setPositiveButton("确认共享",(x,y)->persist.run()).show();else persist.run();
  }).show();
 }
 void persist(SharedItem existing,String title,String body,String scope,boolean wasShared){if(existing==null){save(title,body,scope);return;}if(wasShared&&!scope.equals("shared"))queue(existing,true);existing.title=title;existing.body=body;existing.privacyScope=scope;update(existing,existing.status,false);}
 void save(String title,String body,String scope){SharedItem item=new SharedItem();item.id=UUID.randomUUID().toString();item.category=category;item.title=title;item.body=body;item.privacyScope=scope;item.createdBy=cfg.actorId();item.createdAt=item.updatedAt=System.currentTimeMillis();io.execute(()->{db.shared().save(item);if(scope.equals("shared"))queue(item,false);runOnUiThread(this::load);});}
 void update(SharedItem item,String status,boolean delete){io.execute(()->{item.status=status;item.updatedAt=System.currentTimeMillis();item.deleted=delete;db.shared().save(item);if(item.privacyScope.equals("shared"))queue(item,delete);runOnUiThread(this::load);});}
 void queue(SharedItem item,boolean delete){try{JSONObject p=new JSONObject().put("category",item.category).put("title",item.title).put("body",item.body).put("privacyScope",item.privacyScope).put("status",item.status).put("createdAt",item.createdAt);SyncRepository.enqueue(this,"feature",item.id,delete?"delete":"upsert",p,delete);}catch(Exception ignored){}}
 @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
