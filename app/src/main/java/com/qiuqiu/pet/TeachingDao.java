package com.qiuqiu.pet;

import androidx.room.*;
import java.util.List;

@Dao
public interface TeachingDao {
 @Insert long insert(TeachingRule rule);
 @Update void update(TeachingRule rule);
 @Query("SELECT * FROM teaching_rules WHERE normalizedQuestion=:question ORDER BY updatedAt DESC LIMIT 1") TeachingRule exact(String question);
 @Query("SELECT * FROM teaching_rules ORDER BY updatedAt DESC") List<TeachingRule> all();
 @Query("DELETE FROM teaching_rules WHERE id=:id") void delete(long id);
 @Query("DELETE FROM teaching_rules") void clear();
}
