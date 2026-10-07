package com.qiuqiu.pet;

import androidx.room.*;
import java.util.List;

@Dao
public interface ChatDao {
 @Insert long insert(ChatMessage message);
 @Query("SELECT * FROM chat_messages WHERE status='complete' ORDER BY createdAt DESC LIMIT :limit") List<ChatMessage> recentNewest(int limit);
 @Query("UPDATE chat_messages SET status=:status,errorType=:error WHERE id=:id") void updateStatus(long id,String status,String error);
 @Query("DELETE FROM chat_messages") void clear();
}
