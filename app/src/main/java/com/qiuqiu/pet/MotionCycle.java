package com.qiuqiu.pet;
final class MotionCycle {
 static final int FRAMES=60;static final long DURATION=5000,IDLE_DURATION=3*DURATION;
 static int index(long elapsed,long duration,boolean loop){long t=Math.max(0,elapsed);if(!loop&&t>=duration)return FRAMES-1;return (int)((t%duration)*FRAMES/duration);}
 static float sourcePhase(int index,boolean loop){return loop?index/2.5f:index*23f/(FRAMES-1);}
}
