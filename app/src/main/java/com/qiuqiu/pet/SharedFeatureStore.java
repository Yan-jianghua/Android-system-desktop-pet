package com.qiuqiu.pet;
import android.content.Context;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.*;

final class SharedFeatureStore {
 static void saveNow(Context c,String id,String category,String title,String body){SharedItem item=new SharedItem();item.id=id;item.category=category;item.title=title;item.body=body;item.privacyScope="shared";item.status="active";item.createdBy=new ServerConfig(c).actorId();item.createdAt=item.updatedAt=System.currentTimeMillis();SyncDatabase.get(c).shared().save(item);queue(c,item,false);}
 private static final ExecutorService IO=Executors.newSingleThreadExecutor();
 static void save(Context c,String category,String title,String body,String scope){Context app=c.getApplicationContext();long now=System.currentTimeMillis();SharedItem item=new SharedItem();item.id=UUID.randomUUID().toString();item.category=category;item.title=title;item.body=body;item.privacyScope=scope;item.status="active";item.createdBy=new ServerConfig(app).actorId();item.createdAt=item.updatedAt=now;IO.execute(()->{SyncDatabase.get(app).shared().save(item);if(scope.equals("shared"))queue(app,item,false);});}
 static void update(Context c,SharedItem item,boolean deleted){Context app=c.getApplicationContext();item.updatedAt=System.currentTimeMillis();item.deleted=deleted;IO.execute(()->{SyncDatabase.get(app).shared().save(item);if(item.privacyScope.equals("shared"))queue(app,item,deleted);});}
 static void queue(Context c,SharedItem item,boolean deleted){try{JSONObject p=new JSONObject().put("category",item.category).put("title",item.title).put("body",item.body).put("privacyScope",item.privacyScope).put("status",item.status).put("createdAt",item.createdAt);SyncRepository.enqueue(c,"feature",item.id,deleted?"delete":"upsert",p,deleted);}catch(Exception ignored){}}
 private SharedFeatureStore(){}
}
