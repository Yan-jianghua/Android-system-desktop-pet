package com.qiuqiu.pet;

import androidx.room.*;
import java.util.List;

@Dao
public interface SharedItemDao {
 @Insert(onConflict=OnConflictStrategy.REPLACE) void save(SharedItem item);
 @Query("SELECT * FROM shared_items WHERE deleted=0 ORDER BY updatedAt DESC") List<SharedItem> all();
 @Query("SELECT * FROM shared_items WHERE deleted=0 AND category=:category ORDER BY updatedAt DESC") List<SharedItem> byCategory(String category);
 @Query("SELECT * FROM shared_items WHERE deleted=0 AND category LIKE :prefix ORDER BY updatedAt DESC") List<SharedItem> byPrefix(String prefix);
 @Query("SELECT * FROM shared_items WHERE id=:id LIMIT 1") SharedItem byId(String id);
 @Query("SELECT COUNT(*) FROM shared_items WHERE deleted=0 AND category=:category") int count(String category);
 @Query("UPDATE shared_items SET deleted=1,updatedAt=:time WHERE id=:id") void markDeleted(String id,long time);
}
