package com.qiuqiu.pet;

import java.util.*;

final class ChatRepository {
 final ChatDao dao;
 ChatRepository(QiuqiuDatabase db){dao=db.chat();}
 long pending(String role,String content,String requestId){return dao.insert(new ChatMessage(role,content,System.currentTimeMillis(),"pending",requestId));}
 long complete(String role,String content,String requestId){return dao.insert(new ChatMessage(role,content,System.currentTimeMillis(),"complete",requestId));}
 void complete(long id){dao.updateStatus(id,"complete","");}
 void fail(long id,String error){dao.updateStatus(id,"failed",error);}
 List<ChatMessage> recent(int limit){List<ChatMessage> list=dao.recentNewest(limit);Collections.reverse(list);return list;}
 List<ChatMessage> history(int limit){List<ChatMessage> list=dao.historyNewest(limit);Collections.reverse(list);return list;}
 void recoverInterrupted(){dao.markInterruptedRequests();}
 void clear(){dao.clear();}
}
