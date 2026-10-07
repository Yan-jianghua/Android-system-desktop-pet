package com.qiuqiu.pet;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.concurrent.*;

public class ServerSetupActivity extends Activity {
 final ExecutorService io=Executors.newSingleThreadExecutor();EditText endpoint,name,secret;TextView status,message;ServerConfig config;SyncRepository sync;
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout p,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(0xff443f39);t.setPadding(0,dp(6),0,dp(4));p.addView(t);return t;}
 EditText input(LinearLayout p,String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setSingleLine();p.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
 Button button(LinearLayout p,String label,Runnable run){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->run.run());p.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));return b;}
 @Override public void onCreate(Bundle state){super.onCreate(state);config=new ServerConfig(this);sync=new SyncRepository(this);ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(24),dp(20),dp(30));body.setBackgroundColor(0xfff8f5f2);scroll.addView(body);
  text(body,"账户与共养连接",26);text(body,"双方分别登录自己的账户。每个账户只能创建或加入一只球球；断网时聊天与相册仍保存在本机。",13);
  button(body,"打开账户登录",()->startActivity(new Intent(this,AccountActivity.class)));
  endpoint=input(body,"服务器地址，例如 https://你的服务器",config.endpoint());name=input(body,"我的昵称",config.displayName());secret=input(body,"邀请令牌（创建者不需要填写）","");
  Uri link=getIntent().getData();if(link!=null){String server=link.getQueryParameter("server"),token=link.getQueryParameter("token");if(server!=null)endpoint.setText(server);if(token!=null)secret.setText(token);}
  button(body,"测试连接",()->run(()->{saveEndpoint();JSONObject h=sync.health();return "服务器在线 · 版本 "+h.optString("version")+" · 时间 "+h.optLong("serverTime");}));
  button(body,"创建新的共享球球",()->run(()->{requireAccount();saveEndpoint();sync.createSpace("",safeName(),Build.MODEL);return "共享球球已创建，当前账户已绑定";}));
  button(body,"使用邀请加入",()->run(()->{requireAccount();saveEndpoint();sync.acceptInvite(secret.getText().toString().trim(),safeName(),Build.MODEL);return "加入成功，当前账户已绑定";}));
  button(body,"生成并分享邀请链接",()->run(()->{saveEndpoint();JSONObject r=sync.createInvite();String token=r.getString("inviteToken"),web=r.optString("inviteUrl",config.endpoint()+"/join?token="+Uri.encode(token));String deep="qiuqiu://join?server="+Uri.encode(config.endpoint())+"&token="+Uri.encode(token);runOnUiThread(()->shareInvite(web,deep,token));return "邀请已生成，15 分钟内有效且只能使用一次";}));
  button(body,"手动刷新（通常不需要）",()->{saveEndpoint();sync.syncAsync((ok,msg)->{showMessage(msg,ok);if(ok)refresh();});});
  button(body,"进入双人聊天",()->startActivity(new Intent(this,ChatActivity.class)));
  button(body,"打开共享相册",()->startActivity(new Intent(this,AlbumListActivity.class)));
  button(body,"设备、权限与冲突",()->startActivity(new Intent(this,SyncAdminActivity.class)));
  message=text(body,"尚未测试服务器连接",14);status=text(body,"",14);setContentView(scroll);refresh();
 }
 String safeName(){String v=name.getText().toString().trim();return v.isEmpty()?"主人":v;}
 void requireAccount(){if(!config.accountLoggedIn())throw new IllegalStateException("请先注册或登录球球账户");}
 void shareInvite(String web,String deep,String token){String value="邀请你和我一起养球球：\n"+web+"\n\n已安装球球 App 可打开：\n"+deep;((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("球球双人共养邀请",value));secret.setText(token);Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,value);startActivity(Intent.createChooser(send,"分享球球邀请"));}
 void saveEndpoint(){config.endpoint(endpoint.getText().toString());}
 interface Job{String run()throws Exception;}
 void run(Job job){showMessage("处理中……",true);io.execute(()->{try{String m=job.run();runOnUiThread(()->{showMessage(m,true);refresh();RealtimeSyncService.start(this);});}catch(Exception e){runOnUiThread(()->showMessage("失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()),false));}});}
 void showMessage(String value,boolean ok){message.setText((ok?"● ":"⚠ ")+value);message.setTextColor(ok?0xff2e7d62:0xffb13d4a);}
 void refresh(){io.execute(()->{int pending=sync.pendingCount();runOnUiThread(()->status.setText((config.configured()?"已绑定 · "+config.displayName():"尚未绑定")+"\n待同步 "+pending+" 条 · 最后同步 "+(config.lastSync()==0?"从未":new java.text.SimpleDateFormat("MM-dd HH:mm",java.util.Locale.CHINA).format(new java.util.Date(config.lastSync())))+(config.lastError().isEmpty()?"":"\n"+config.lastError())));});}
 @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
}
