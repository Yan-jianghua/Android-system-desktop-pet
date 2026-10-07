const assert=require('node:assert/strict');
const {CareState,ActionTimeline,celebration,dateContext,answer,H}=require('../app/src/main/assets/pet-sprites/care.js');
const store=()=>{const m=new Map;return {getItem:k=>m.get(k),setItem:(k,v)=>m.set(k,v)}};
const base=1800000000000;
for(let count=0;count<=4;count++){
 const db=store(),s=new CareState(db,()=>base);for(let i=0;i<count;i++)assert(s.feed(base+i*10000));
 let e;while((e=s.nextDue(base+6*H)))s.finish(e);const reopen=new CareState(db,()=>base);
 assert.equal(reopen.data.urine,3);assert.equal(reopen.data.poop,count<=1?1:count<=3?2:3);
 reopen.clear();assert.equal(new CareState(db,()=>base).data.poop,0)
}
const date=s=>Date.parse(s+'T12:00:00+08:00');
assert.equal(dateContext(date('2026-10-07')),'今天是公历2026年10月7日，星期三；农历八月廿七。');
assert.equal(answer('今天农历几月几日？',date('2026-10-07')),'今天是公历2026年10月7日，星期三；农历八月廿七。');
for(const [d,a] of [['2026-10-27','爸爸生日快乐'],['2026-02-05','妈妈生日快乐'],['2027-01-25','妈妈生日快乐'],['2026-02-16','祝爸爸妈妈新年快乐'],['2026-02-17','祝爸爸妈妈新年快乐'],['2026-09-25','祝爸爸妈妈中秋节快乐'],['2026-04-05','祝爸爸妈妈清明节快乐'],['2026-12-22','祝爸爸妈妈冬至快乐']])assert.equal(celebration(date(d)),a);
assert.equal(answer('什么？',date('2026-10-27')),'球球祝爸爸生日快乐');
assert.equal(answer('什么？',date('2026-02-05')),'球球祝妈妈生日快乐');
assert.equal(answer('你最爱谁呀？',date('2026-10-06')),'最爱爸爸妈妈');
console.log('PASS: browser preview care history, windows, lunar birthday and festival parity');
const tasks=new ActionTimeline;let fed=0,urinated=0;
tasks.start('eat','猫条',5000,()=>{fed++;tasks.start('happy','',2500,null,10000)},0);
tasks.interrupt('pee',5000,()=>urinated++,2000);
assert(tasks.toilet);tasks.update(7000);assert.equal(tasks.food,'猫条');assert.equal(urinated,1);
assert.equal(tasks.elapsed(7000),2000);
tasks.update(9999);assert.equal(fed,0);tasks.update(10000);assert.equal(fed,1);assert.equal(tasks.mode,'happy');
tasks.update(12500);assert(!tasks.busy);
const cleaning=new ActionTimeline;let waste=2,removed=-1;
cleaning.start('clean','',7000,()=>{removed=waste;waste=0},0);
cleaning.interrupt('poop',5000,()=>waste+=3,4000);cleaning.update(9000);
assert.equal(waste,5);assert.equal(cleaning.mode,'clean');cleaning.update(11999);assert.equal(removed,-1);
cleaning.update(12000);assert.equal(removed,5);assert.equal(waste,0);
console.log('PASS: browser priority toilet, resumed feeding/reaction and seven seconds of interrupted cleaning');
