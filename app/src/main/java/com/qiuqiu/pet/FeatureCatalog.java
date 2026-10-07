package com.qiuqiu.pet;

import java.util.*;

/** Product-backed definitions for every co-care feature shipped in 3.1. */
public final class FeatureCatalog {
 public static final class Def {
  public final String name,description,titleHint,bodyHint,doneLabel;
  public final boolean supportsDate,sensitive;
  Def(String n,String d,String t,String b,String a,boolean date,boolean privateByDefault){name=n;description=d;titleHint=t;bodyHint=b;doneLabel=a;supportsDate=date;sensitive=privateByDefault;}
 }
 private static Def d(String n,String desc,String title,String body,String done,boolean date,boolean sensitive){return new Def(n,desc,title,body,done,date,sensitive);}
 public static final Def[] ALL={
  d("双人实时动作回声","查看双方刚刚完成的喂食与互动","动作摘要","记录或补充说明","已看过",false,false),
  d("共同照顾任务","双方各完成一部分共同领奖","今天一起完成什么","写清双方各自的步骤","完成我的部分",true,false),
  d("照顾接力单","把一项照顾交给另一位主人","接力事项","完成条件与给对方的话","接力完成",true,false),
  d("每日心情签到","记录情绪、精力和希望的陪伴","今天的心情","情绪、精力和希望被如何陪伴","完成签到",false,true),
  d("每日情侣问题","双方回答后解锁彼此答案","今日问题","写下自己的答案；对方作答后再一起查看","提交答案",false,true),
  d("默契猜猜看","预测对方的选择","猜测题目","我的预测和理由","揭晓答案",false,false),
  d("共享日记","保存文字、故事和球球贴纸","日记标题","记录今天发生的故事","收进回忆",false,true),
  d("纪念日时间轴","记录纪念日和倒计时","纪念日名称","日期、地点和故事","已纪念",true,false),
  d("时光胶囊","约定未来日期共同开启","胶囊名称","写给未来的两个人和球球","开启胶囊",true,true),
  d("约会愿望单","共同投票、排期与完成","想一起做的事","地点、预算或准备事项","约会完成",true,false),
  d("异步双人小游戏","保存双方轮流完成的回合","本回合动作","给对方留下下一回合提示","提交回合",false,false),
  d("双人哄睡仪式","双方晚安后完成今日仪式","今晚的晚安","留一句晚安或明早祝福","说晚安",false,false),
  d("远程抱抱","让球球替你传递轻互动","抱抱留言","轻轻说一句想传达的话","抱抱送达",false,false),
  d("和好模式","记录冷静计时与和好任务","和好任务","只记录自愿完成的小步骤","结束冷静",true,true),
  d("贡献回顾","回顾双方完成的照顾","回顾主题","记录彼此做过的照顾，不比较输赢","完成回顾",false,false),
  d("成长树","积累照顾、陪伴、探索和默契","成长目标","选择照顾、陪伴、探索或默契分支","点亮节点",false,false),
  d("性格养成","保存可解释、可重置的偏好","偏好标签","说明由哪些长期互动形成","确认标签",false,false),
  d("房间共同装修","共同保存和恢复布置方案","布置方案","家具、主题和修改理由","采用方案",false,false),
  d("衣柜搭配投票","提交搭配并记录投票","搭配名称","服装组合与我的投票","投票完成",false,false),
  d("季节通行日历","记录季节任务和奖励","季节任务","任务条件和离线奖励","领取奖励",true,false),
  d("旅行明信片","保存旅行故事与收藏","明信片地点","旅途故事和球球带回的纪念物","收藏明信片",true,false),
  d("发现图鉴","收集动作、食物与家具发现","发现名称","来源、触发方法和球球反应","加入图鉴",false,false),
  d("成就墙","记录共同、个人和季节成就","成就名称","达成条件和纪念说明","展示成就",true,false),
  d("月度共同回顾","汇总当月数据与里程碑","回顾月份","照顾、心情、照片和里程碑摘要","生成回顾",true,false),
  d("共享 AI 记忆","双方确认后保存共同记忆","共同记忆","只写双方都同意让球球记住的内容","双方确认",false,true),
  d("记忆来源解释","记录记忆来源与授权者","记忆主题","来源、授权人和授权时间","确认来源",false,true),
  d("关怀提醒卡","只分享需要关心的摘要","关怀提示","只写抽象需要，不填写健康原始数据","已关怀",true,true),
  d("离线旅行包","管理已下载的离线内容","内容包名称","包含的故事、问题与有效期","标记已下载",true,false),
  d("家庭数据导出册","记录导出任务和归档","导出册名称","选择日记、照片、里程碑等范围","完成导出",true,true),
  d("关系暂停与告别","管理冻结、导出和清理步骤","关系处理计划","冻结、导出、撤权与本机副本选择","完成处理",true,true)
 };
 private static final Map<String,Def> MAP=new HashMap<>();
 static{for(Def x:ALL)MAP.put(x.name,x);}
 public static Def get(String name){Def x=MAP.get(name);return x==null?d(name,"离线保存并在连接恢复后同步","标题","内容","完成",false,false):x;}
 private FeatureCatalog(){}
}
