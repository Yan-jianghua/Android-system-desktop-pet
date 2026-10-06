const assert=require('node:assert/strict'),fs=require('node:fs');
const {motionIndex,motionPhase,IdleClock}=require('../app/src/main/assets/pet-sprites/care.js');
const frames=new Set;for(let t=0;t<5000;t++)frames.add(motionIndex(t,5000,true));assert.equal(frames.size,60);
assert.equal(motionIndex(4999,5000,true),59);assert.equal(motionIndex(5000,5000,true),0);assert.equal(motionIndex(5000,5000,false),59);
assert.equal(motionPhase(1,true),.4);assert.equal(motionPhase(59,false),23);
const idle=new IdleClock;idle.reset(0);assert(!idle.advance(9999,true,'rest'));assert(idle.advance(10000,true,'rest'));
idle.advance(18000,true,'rest');idle.advance(50000,false);assert.equal(idle.elapsed,8000);assert(idle.advance(58000,true,'rest'));
const dir='app/src/main/assets/pet-sprites/',meta=JSON.parse(fs.readFileSync(dir+'motions24.json','utf8').replace(/^\uFEFF/,''));
for(const name of Object.keys(meta.motions)){
 const flow=JSON.parse(fs.readFileSync(dir+'motions24/'+name+'-flow.json','utf8'));assert.equal(flow.frames,60);assert.equal(flow.duration,5000);assert.equal(flow.vectors.length,24);
 for(const pair of flow.vectors){assert.equal(pair.length,2);for(const values of pair){assert.equal(values.length,578);assert(values.every(Number.isFinite));assert(values.some(v=>Math.abs(v)>.05),'actual nonzero motion vectors')}}
}
console.log('PASS: preview 60 phases, five-second boundaries, paused three-cycle idle, and all 13 motion grids');





