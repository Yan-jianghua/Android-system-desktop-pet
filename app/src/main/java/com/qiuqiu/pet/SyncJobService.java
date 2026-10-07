package com.qiuqiu.pet;

import android.app.job.*;
import android.content.*;

/** Persistent, network-aware background retry for the offline operation queue. */
public final class SyncJobService extends JobService {
 private static final int JOB_ID=0x514955;
 public static void schedule(Context c){
  JobScheduler js=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
  if(js==null)return;
  JobInfo info=new JobInfo.Builder(JOB_ID,new ComponentName(c,SyncJobService.class))
   .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setPeriodic(15*60*1000L).build();
  js.schedule(info);
 }
 @Override public boolean onStartJob(JobParameters p){
  ServerConfig cfg=new ServerConfig(this);if(!cfg.configured()){jobFinished(p,false);return false;}
  new SyncRepository(this).syncAsync((ok,msg)->jobFinished(p,!ok));return true;
 }
 @Override public boolean onStopJob(JobParameters p){return true;}
}
