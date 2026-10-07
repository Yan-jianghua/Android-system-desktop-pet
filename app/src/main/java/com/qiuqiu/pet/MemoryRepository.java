package com.qiuqiu.pet;

import java.util.*;

/** Stores explicit memories and performs a small local relevance search. */
final class MemoryRepository {
 final MemoryDao dao;
 MemoryRepository(QiuqiuDatabase db){dao=db.memory();}
 long add(String content,String type){return dao.insert(new MemoryRecord(content.trim(),type,System.currentTimeMillis()));}
 void update(MemoryRecord memory,String content){memory.content=content.trim();memory.updatedAt=System.currentTimeMillis();dao.update(memory);}
 void delete(long id){dao.delete(id);}
 void clear(){dao.clear();}
 List<MemoryRecord> all(){return dao.active(System.currentTimeMillis());}
 List<MemoryRecord> relevant(String query,int limit){
  String normalized=Dialogue.normalize(query);List<Scored> scored=new ArrayList<>();
  for(MemoryRecord memory:all()){
   int score=score(normalized,Dialogue.normalize(memory.content));
   if(score>0)scored.add(new Scored(memory,score));
  }
  scored.sort((a,b)->Integer.compare(b.score,a.score));List<MemoryRecord> result=new ArrayList<>();List<Long> used=new ArrayList<>();
  for(Scored item:scored){if(result.size()>=limit)break;result.add(item.memory);used.add(item.memory.id);}
  if(!used.isEmpty())dao.markUsed(used,System.currentTimeMillis());return result;
 }
 MemoryRecord find(String phrase){
  String q=Dialogue.normalize(phrase);MemoryRecord best=null;int bestScore=0;
  for(MemoryRecord m:all()){int score=score(q,Dialogue.normalize(m.content));if(score>bestScore){bestScore=score;best=m;}}
  return bestScore>0?best:null;
 }
 static int score(String q,String value){
  if(q.isEmpty()||value.isEmpty())return 0;if(value.contains(q)||q.contains(value))return 100+Math.min(q.length(),value.length());
  Set<Character> chars=new HashSet<>();for(char c:q.toCharArray())chars.add(c);int hit=0;for(char c:value.toCharArray())if(chars.contains(c))hit++;
  return hit>=2?hit:0;
 }
 static final class Scored {final MemoryRecord memory;final int score;Scored(MemoryRecord m,int s){memory=m;score=s;}}
}
