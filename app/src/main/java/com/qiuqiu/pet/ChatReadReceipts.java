package com.qiuqiu.pet;

import android.content.Context;
import java.util.List;

/** Shared per-member read cursor for the two-person chat. */
final class ChatReadReceipts {
 static final String CATEGORY="_chat_read";
 static String id(String actorId){return CATEGORY+":"+actorId;}
 static long readAt(List<SharedItem> receipts,String exceptActor){long latest=0;for(SharedItem r:receipts)if(!r.deleted&&!r.createdBy.equals(exceptActor))try{latest=Math.max(latest,Long.parseLong(r.body));}catch(Exception ignored){}return latest;}
 static void markLatestIncoming(Context context,List<SharedItem> messages,String actorId,String displayName){
  long latest=0;for(SharedItem m:messages)if(!m.deleted&&!m.createdBy.equals(actorId))latest=Math.max(latest,m.createdAt);
  if(latest==0)return;SyncDatabase db=SyncDatabase.get(context);String id=id(actorId);SharedItem old=db.shared().byId(id);long oldAt=0;if(old!=null)try{oldAt=Long.parseLong(old.body);}catch(Exception ignored){}
  if(latest<=oldAt)return;long now=System.currentTimeMillis();SharedItem receipt=new SharedItem();receipt.id=id;receipt.category=CATEGORY;receipt.title=displayName;receipt.body=Long.toString(latest);receipt.privacyScope="shared";receipt.status="active";receipt.createdBy=actorId;receipt.createdAt=old==null?now:old.createdAt;receipt.updatedAt=now;db.shared().save(receipt);SharedFeatureStore.queue(context,receipt,false);
 }
 private ChatReadReceipts(){}
}
