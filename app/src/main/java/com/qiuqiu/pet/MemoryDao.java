package com.qiuqiu.pet;

import androidx.room.*;
import java.util.List;

@Dao
public interface MemoryDao {
 @Insert long insert(MemoryRecord memory);
 @Update void update(MemoryRecord memory);
 @Query("SELECT * FROM memories WHERE status='active' AND (expiresAt=0 OR expiresAt>:now) ORDER BY importance DESC,updatedAt DESC LIMIT 200") List<MemoryRecord> active(long now);
 @Query("SELECT * FROM memories WHERE id=:id LIMIT 1") MemoryRecord byId(long id);
 @Query("DELETE FROM memories WHERE id=:id") void delete(long id);
 @Query("DELETE FROM memories") void clear();
 @Query("UPDATE memories SET lastUsedAt=:now WHERE id IN (:ids)") void markUsed(List<Long> ids,long now);
}
