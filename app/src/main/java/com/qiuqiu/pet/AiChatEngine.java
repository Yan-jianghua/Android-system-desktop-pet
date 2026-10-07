package com.qiuqiu.pet;

import android.content.Context;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

/** Serial conversation coordinator. Database and network work never run on the UI thread. */
final class AiChatEngine {
 interface Callback {void onResult(Result result);}
 static final class Result {
  final String text;final int usedMemories;final boolean awaitingConfirmation,error,conflictChoice;
  Result(String text,int usedMemories,boolean awaitingConfirmation,boolean error){this(text,usedMemories,awaitingConfirmation,error,false);}
  Result(String text,int usedMemories,boolean awaitingConfirmation,boolean error,boolean conflictChoice){this.text=text;this.usedMemories=usedMemories;this.awaitingConfirmation=awaitingConfirmation;this.error=error;this.conflictChoice=conflictChoice;}
  static Result text(String text){return new Result(text,0,false,false);}
 }
 static final class Pending {
  static final int ADD=1,DELETE=2,CONFLICT=3;final int kind;final String content,type;final long id,conflictId;
  Pending(int kind,String content,String type,long id,long conflictId){this.kind=kind;this.content=content;this.type=type;this.id=id;this.conflictId=conflictId;}
 }
 final ExecutorService worker=Executors.newSingleThreadExecutor();final Handler main=new Handler(Looper.getMainLooper());
 final AiSettings settings;final AiClient client;final QiuqiuDatabase db;final ChatRepository chats;final MemoryRepository memories;
 volatile boolean busy,canceled;volatile Pending pending;
 AiChatEngine(Context context){settings=new AiSettings(context);client=new AiClient(context,settings);db=QiuqiuDatabase.get(context);chats=new ChatRepository(db);memories=new MemoryRepository(db);}
 boolean busy(){return busy;}
 void cancel(){canceled=true;client.cancel();}
 void close(){client.cancel();worker.shutdownNow();}
 void send(String raw,PetContextSnapshot pet,Callback callback){
  String input=raw==null?"":raw.trim();if(input.isEmpty()){deliver(callback,Result.text("主人还没有说话呢。"));return;}
  if(input.length()>2000){deliver(callback,new Result("一次最多输入 2000 个字，分几次告诉球球吧。",0,false,true));return;}
  if(busy){deliver(callback,Result.text("球球还在想上一句话，稍等一下哦。"));return;}
  canceled=false;busy=true;worker.execute(()->{try{chats.recoverInterrupted();handle(input,pet,callback);}catch(Exception e){deliver(callback,new Result("球球的记忆暂时打不开，稍后再试喵。",0,false,true));}finally{busy=false;}});
 }
 void handle(String input,PetContextSnapshot pet,Callback callback){
  String normalized=Dialogue.normalize(input);
  if((normalized.equals("确认")||normalized.equals("是")||normalized.equals("要"))&&pending!=null){confirmPending(callback);return;}
  if((normalized.equals("取消")||normalized.equals("不要")||normalized.equals("算了"))&&pending!=null){pending=null;deliver(callback,Result.text("好，球球没有改动记忆。"));return;}
  String remember=AiRules.afterPrefix(input,"记住");
  if(remember!=null){prepareAdd(remember,"偏好习惯",callback);return;}
  String forget=AiRules.afterPrefix(input,"忘掉");if(forget==null)forget=AiRules.afterPrefix(input,"忘记");
  if(forget!=null){prepareDelete(forget,callback);return;}
  if(input.startsWith("不是")||input.startsWith("纠正")){prepareAdd(input,"纠正规则",callback);return;}
  long now=System.currentTimeMillis();String deterministic=Dialogue.deterministicAnswer(input,now);
  if(deterministic!=null){savePair(input,deterministic);deliver(callback,Result.text(deterministic));return;}
  List<MemoryRecord> allMemories=memories.all();List<String> memoryTexts=new ArrayList<>();for(MemoryRecord memory:allMemories)memoryTexts.add(memory.content);
  String birthdayAnswer=Dialogue.birthdayCountdown(input,now,memoryTexts);
  if(birthdayAnswer!=null){savePair(input,birthdayAnswer);deliver(callback,Result.text(birthdayAnswer));return;}
  TeachingRule taught=db.teaching().exact(normalized);
  if(taught!=null){String reply=taught.answer;savePair(input,reply);deliver(callback,Result.text(reply));return;}
  if(!settings.enabled()){String reply="AI 还没有设置好，主人可以到应用首页开启；球球现在只能回答已经学会的内容。";savePair(input,reply);deliver(callback,new Result(reply,0,false,true));return;}
  List<ChatMessage> history=chats.recent(12);List<MemoryRecord> related=memories.relevant(input,5);String requestId=UUID.randomUUID().toString();long userId=chats.pending("user",input,requestId);chats.complete(userId);
  try{
   if(canceled){chats.fail(userId,"canceled");deliver(callback,new Result("已经取消这次回答。",related.size(),false,true));return;}
   String answer=completeWithOneSafeRetry(PromptBuilder.build(history,related,pet,input),requestId);if(canceled){chats.fail(userId,"canceled");deliver(callback,new Result("已经取消这次回答。",related.size(),false,true));return;}chats.complete("assistant",answer,requestId);
   String candidate=AiRules.candidateMemory(input);if(candidate!=null&&!AiRules.isSensitive(candidate)){pending=new Pending(Pending.ADD,candidate,"偏好习惯",0,0);deliver(callback,new Result(answer+"\n这件事要球球记住吗？",related.size(),true,false));}
   else deliver(callback,new Result(answer,related.size(),false,false));
  }catch(AiClient.Failure e){chats.fail(userId,canceled?"canceled":e.type);deliver(callback,new Result(canceled?"已经取消这次回答。":e.userMessage,related.size(),false,true));}
 }
 void prepareAdd(String content,String type,Callback callback){
  String value=AiRules.trimLead(content);if(value.isEmpty()){deliver(callback,Result.text("主人想让球球记住什么呢？"));return;}
  MemoryRecord conflict=memories.findConflict(value);
  if(conflict!=null){pending=new Pending(Pending.CONFLICT,value,type,0,conflict.id);String warning=AiRules.isSensitive(value)?"\n这可能包含敏感信息，只会保存在本机。":"";deliver(callback,new Result("发现相近的旧记忆：“"+conflict.content+"”。请选择保留旧记忆，或用新内容替换。"+warning,0,true,false,true));return;}
  pending=new Pending(Pending.ADD,value,type,0,0);
  String warning=AiRules.isSensitive(value)?"这可能包含敏感信息，只会保存在本机。":"";
  deliver(callback,new Result("球球准备记住：“"+value+"”。"+warning+"确定吗？",0,true,false));
 }
 void prepareDelete(String phrase,Callback callback){
  String value=AiRules.trimLead(phrase);MemoryRecord found=memories.find(value);
  if(found==null){deliver(callback,Result.text("球球没有找到和“"+value+"”相关的记忆。"));return;}
  pending=new Pending(Pending.DELETE,found.content,found.type,found.id,0);
  deliver(callback,new Result("要忘掉“"+found.content+"”吗？",0,true,false));
 }
 void confirmPending(Callback callback){
  Pending action=pending;pending=null;if(action==null){deliver(callback,Result.text("现在没有等待确认的记忆。"));return;}
  if(action.kind==Pending.ADD){memories.add(action.content,action.type);deliver(callback,Result.text("球球记住啦。"));}
  else if(action.kind==Pending.CONFLICT){memories.delete(action.conflictId);memories.add(action.content,action.type);deliver(callback,Result.text("已用新内容替换旧记忆，球球记住啦。"));}
  else {memories.delete(action.id);deliver(callback,Result.text("球球已经忘掉这条记忆了。"));}
 }
 void confirm(Callback callback){if(busy)return;busy=true;worker.execute(()->{try{confirmPending(callback);}finally{busy=false;}});}
 void reject(Callback callback){pending=null;deliver(callback,Result.text("好，球球没有改动记忆。"));}
 void keepOld(Callback callback){Pending action=pending;pending=null;if(action==null||action.kind!=Pending.CONFLICT){deliver(callback,Result.text("当前没有待处理的记忆冲突。"));return;}deliver(callback,Result.text("已保留原记忆，没有加入新内容。"));}
 void teach(String question,String answer,Callback callback){
  if(question==null||question.isBlank()||answer==null||answer.isBlank()){deliver(callback,new Result("请用“问题 => 希望的回答”来教球球。",0,false,true));return;}
  worker.execute(()->{long now=System.currentTimeMillis();db.teaching().insert(new TeachingRule(question.trim(),answer.trim(),now));deliver(callback,Result.text("球球学会啦，下次会按主人教的回答。"));});
 }
 void correct(String question,String correction,Callback callback){
  String q=question==null?"":question.trim(),fixed=correction==null?"":correction.trim();
  if(q.isEmpty()||fixed.isEmpty()){deliver(callback,new Result("请告诉球球正确内容。",0,false,true));return;}
  if(busy){deliver(callback,new Result("球球还在处理上一句话，稍等一下哦。",0,false,true));return;}
  busy=true;worker.execute(()->{try{
   long now=System.currentTimeMillis();TeachingRule existing=db.teaching().exact(Dialogue.normalize(q));
   if(existing==null)db.teaching().insert(new TeachingRule(q,fixed,now));else{existing.question=q;existing.answer=fixed;existing.updatedAt=now;db.teaching().update(existing);}
   String answer=fixed;
   if(settings.enabled()&&!canceled){List<AiClient.Message> messages=Arrays.asList(
    new AiClient.Message("system",PromptBuilder.PERSONA+"\n主人刚刚明确纠正了回答。纠正内容是权威事实，必须优先使用，不得争辩或改变其事实含义。请基于纠正内容重新回答原问题，保持简短。"),
    new AiClient.Message("user","原问题："+q+"\n主人提供的正确内容："+fixed+"\n请重新回答原问题。"));
    try{answer=completeWithOneSafeRetry(messages,UUID.randomUUID().toString());}catch(AiClient.Failure ignored){answer=fixed+"（已保存为教学内容；当前接口不可用，先按你给出的正确内容回答。）";}
   }
   savePair("纠正上一条回答："+fixed,answer);deliver(callback,Result.text(answer));
  }catch(Exception e){deliver(callback,new Result("纠正已收到，但暂时保存失败，请稍后再试。",0,false,true));}finally{busy=false;}});
 }
 String completeWithOneSafeRetry(List<AiClient.Message> messages,String requestId) throws AiClient.Failure {
  try{return client.complete(messages,requestId);}catch(AiClient.Failure first){if(!AiRules.shouldRetryOnce(first.retryable,0,canceled))throw first;return client.complete(messages,requestId);}
 }
 void clearChats(Runnable done){worker.execute(()->{chats.clear();main.post(done);});}
 void clearAll(Runnable done){worker.execute(()->{chats.clear();memories.clear();db.teaching().clear();main.post(done);});}
 void savePair(String input,String answer){String id=UUID.randomUUID().toString();chats.complete("user",input,id);chats.complete("assistant",answer,id);}
 void deliver(Callback callback,Result result){main.post(()->callback.onResult(result));}
}
