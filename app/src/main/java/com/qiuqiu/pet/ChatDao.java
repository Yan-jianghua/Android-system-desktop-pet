package com.qiuqiu.pet;

import androidx.room.*;
import java.util.List;

@Dao
public interface ChatDao {
 @Insert long insert(ChatMessage message);
 @Query("SELECT * FROM chat_messages WHERE status='complete' ORDER BY createdAt DESC LIMIT :limit") List<ChatMessage> recentNewest(int limit);
 @Query("SELECT * FROM chat_messages ORDER BY createdAt DESC LIMIT :limit") List<ChatMessage> historyNewest(int limit);
 @Query("UPDATE chat_messages SET status='interrupted',errorType='process_interrupted' WHERE role='user' AND status='complete' AND requestId<>'' AND NOT EXISTS (SELECT 1 FROM chat_messages AS reply WHERE reply.role='assistant' AND reply.status='complete' AND reply.requestId=chat_messages.requestId)") void markInterruptedRequests();
 @Query("UPDATE chat_messages SET status=:status,errorType=:error WHERE id=:id") void updateStatus(long id,String status,String error);
 @Query("DELETE FROM chat_messages") void clear();
}
