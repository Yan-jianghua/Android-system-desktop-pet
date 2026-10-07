package com.qiuqiu.pet;

import androidx.room.*;
import java.util.List;

@Dao
public interface SyncDao {
 @Insert(onConflict=OnConflictStrategy.IGNORE) long insert(SyncOperation operation);
 @Query("SELECT * FROM sync_operations WHERE syncState='pending' ORDER BY createdAtLocal LIMIT :limit") List<SyncOperation> pending(int limit);
 @Query("SELECT * FROM sync_operations ORDER BY CASE WHEN serverSeq=0 THEN 9223372036854775807 ELSE serverSeq END DESC, createdAtLocal DESC LIMIT :limit") List<SyncOperation> timeline(int limit);
 @Query("SELECT COUNT(*) FROM sync_operations WHERE syncState='pending'") int pendingCount();
 @Query("UPDATE sync_operations SET syncState=:state,serverSeq=:seq,result=:result WHERE opId=:opId") void finish(String opId,String state,long seq,String result);
 @Query("SELECT COUNT(*) FROM sync_operations WHERE actorId=:actorId") int contribution(String actorId);
 @Query("DELETE FROM sync_operations WHERE syncState='synced' AND serverSeq>0 AND serverSeq<:beforeSeq") void prune(long beforeSeq);
}
