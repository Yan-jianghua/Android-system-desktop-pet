package com.qiuqiu.pet;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

@SuppressLint("SetTextI18n")
public class AiSettingsActivity extends Activity {
 AiSettings settings;Switch enabled;EditText endpoint,model,key,maxOutput;CheckBox wifiOnly;TextView status;
 final ExecutorService worker=Executors.newSingleThreadExecutor();AiClient testing;
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 TextView label(LinearLayout parent,String text,int size){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(0xff443f39);v.setPadding(0,dp(7),0,dp(4));parent.addView(v);return v;}
 EditText input(LinearLayout parent,String hint){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setTextSize(14);parent.addView(e,new LinearLayout.LayoutParams(-1,dp(52)));return e;}
 Button button(LinearLayout parent,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));return b;}
 public void onCreate(Bundle state){
  super.onCreate(state);settings=new AiSettings(this);ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(24),dp(22),dp(30));body.setBackgroundColor(0xfff8f9f2);scroll.addView(body);
  label(body,"球球 AI 设置",28);label(body,"接入 OpenAI 兼容接口。普通聊天会发送到你配置的服务，并可能产生第三方费用。生日、节日和桌宠养成功能仍可离线使用。",13);
  enabled=new Switch(this);enabled.setText("启用 AI 聊天");enabled.setChecked(settings.enabled());body.addView(enabled);
  label(body,"接口地址（必须使用 HTTPS）",13);endpoint=input(body,"https://api.openai.com/v1");endpoint.setText(settings.endpoint());
  label(body,"模型名称",13);model=input(body,"模型名称");model.setText(settings.model());
  label(body,"API Key",13);key=input(body,settings.hasKey()?"已安全保存；留空表示不修改":"输入 API Key（可留空）");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  label(body,"回答最大字符数（80～600）",13);maxOutput=input(body,"240");maxOutput.setInputType(InputType.TYPE_CLASS_NUMBER);maxOutput.setText(String.valueOf(settings.maxOutput()));
  wifiOnly=new CheckBox(this);wifiOnly.setText("仅在 Wi-Fi 下使用");wifiOnly.setChecked(settings.wifiOnly());body.addView(wifiOnly);
  status=label(body,"",13);button(body,"保存设置",this::save);button(body,"保存并测试连接",this::test);button(body,"管理球球的记忆与教学",()->startActivity(new Intent(this,MemoryActivity.class)));button(body,"清空近期聊天",this::clearChats);button(body,"清除已保存的 API Key",this::clearKey);button(body,"清空全部 AI 数据",this::confirmClearAll);
  label(body,"隐私说明",18);label(body,"聊天内容与相关记忆只会发送到你配置的 AI 接口。本应用不把聊天用于训练公共模型。API Key 由 Android Keystore 加密保存在本机，日志不会记录 Key、完整聊天或完整接口响应。卸载或清除应用数据会删除本地聊天与记忆。",13);
  setContentView(scroll);
 }
 boolean save(){try{int max=Integer.parseInt(maxOutput.getText().toString().trim());settings.save(enabled.isChecked(),endpoint.getText().toString(),model.getText().toString(),max,wifiOnly.isChecked(),key.getText().toString());key.setText("");key.setHint(settings.hasKey()?"已安全保存；留空表示不修改":"输入 API Key（可留空）");status.setText("设置已保存");return true;}catch(Exception e){status.setText("无法保存："+e.getMessage());return false;}}
 void test(){if(!save())return;status.setText("正在测试连接……");worker.execute(()->{testing=new AiClient(this,settings);try{String reply=testing.complete(Arrays.asList(new AiClient.Message("system","只进行连接测试。"),new AiClient.Message("user","只回复：连接成功")),UUID.randomUUID().toString());runOnUiThread(()->status.setText("连接成功："+reply));}catch(AiClient.Failure e){runOnUiThread(()->status.setText(e.userMessage));}});}
 void clearChats(){AiChatEngine engine=new AiChatEngine(this);engine.clearChats(()->{status.setText("近期聊天已清空，长期记忆仍保留。");engine.close();});}
 void clearKey(){settings.clearKey();key.setText("");key.setHint("输入 API Key（可留空）");status.setText("API Key 已清除");}
 void confirmClearAll(){new AlertDialog.Builder(this).setTitle("清空全部 AI 数据？").setMessage("将删除聊天、长期记忆、教学规则和 AI 设置，原有养成记录不会受影响。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{AiChatEngine engine=new AiChatEngine(this);engine.clearAll(()->{settings.clearAll();enabled.setChecked(false);status.setText("全部 AI 数据已清空");engine.close();});}).show();}
 @Override protected void onDestroy(){if(testing!=null)testing.cancel();worker.shutdownNow();super.onDestroy();}
}
