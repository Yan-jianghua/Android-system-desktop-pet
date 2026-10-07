package com.qiuqiu.pet;

final class PetContextSnapshot {
 final int affection,poop,urine;final long days,now;final String title,mood,action;
 PetContextSnapshot(int affection,long days,String title,String mood,int poop,int urine,String action,long now){this.affection=affection;this.days=days;this.title=title;this.mood=mood;this.poop=poop;this.urine=urine;this.action=action;this.now=now;}
 String describe(){return Dialogue.dateContext(now)+" 当前养成状态：亲密度 "+affection+"/100，称号“"+title+"”，相伴第 "+days+" 天，今日心情“"+mood+"”，猫砂盆便便 "+poop+" 个、尿团 "+urine+" 个，当前动作“"+action+"”。日期和这些数值是本地提供的可靠事实，只读，不得声称已修改。";}
}
