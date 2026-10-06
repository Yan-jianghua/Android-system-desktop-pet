package com.qiuqiu.pet;
final class MotionCycle {
 static final int FRAMES=60;static final long DURATION=5000,IDLE_DURATION=3*DURATION;
 /**
  * 连续播放帧相位（浮点）。
  * 原先返回整数 (elapsed*60/duration)，在 60fps 重绘下每 ~83ms 才 +1，
  * 中间多帧姿势完全相同，动画被锁成 12fps 的“停顿-跳变”阶梯。
  * 改为浮点后每次重绘都平滑推进。
  */
 static float frame(long elapsed,long duration,boolean loop){
  long t=Math.max(0,elapsed);
  if(!loop&&t>=duration)return FRAMES-1;
  return (t%duration)*(float)FRAMES/duration;
 }
 /**
  * 连续源关键帧相位：loop 为 0..24 循环，非 loop 为 0..23。
  * 直接由真实时间计算，供网格补间在相邻关键帧之间连续插值。
  */
 static float phase(long elapsed,long duration,boolean loop){
  long t=Math.max(0,elapsed);
  if(loop)return (t%duration)*24f/duration;
  if(t>=duration)return 23f;
  return t*23f/duration;
 }
}
