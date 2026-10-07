'use strict';
const H=3600000,KEY='qiuqiu-own-memory-v1';
class ActionTimeline {
 constructor(){this.current=null;this.paused=[];this.deadline=0}
 get busy(){return this.current!==null}
 get toilet(){return this.current&&['pee','poop'].includes(this.current.mode)}
 get mode(){return this.current?.mode||'sit'}
 get food(){return this.current?.food||''}
 elapsed(n){return this.current?Math.max(0,Math.min(this.current.duration,this.current.duration-(this.deadline-n))):0}
 start(mode,food,duration,done,n){this.current={mode,food,duration,remaining:duration,done};this.deadline=n+duration}
 interrupt(mode,duration,done,n){this.update(n);if(this.current){this.current.remaining=Math.max(0,this.deadline-n);this.paused.push(this.current)}this.start(mode,'',duration,done,n)}
 update(n){if(!this.current||n<this.deadline)return;const finished=this.current;this.current=null;finished.done?.();if(!this.current&&this.paused.length){this.current=this.paused.pop();this.deadline=n+this.current.remaining}}
}
class CareState {
 constructor(store=localStorage,clock=Date.now){
  this.store=store;this.clock=clock;let saved;try{saved=JSON.parse(store.getItem(KEY))}catch{}
  const n=clock();this.data=Object.assign({urine:0,poop:0,nextUrine:n+2*H,nextPoop:n+6*H,feedUntil:0,feeds:[]},saved||{});this.save();
 }
 save(){this.store.setItem(KEY,JSON.stringify(this.data))}
 feed(n=this.clock()){if(n<this.data.feedUntil)return false;this.data.feeds.push(n);this.data.feedUntil=n+10000;this.save();return true}
 nextDue(n=this.clock()){
  const d=this.data;if(d.nextUrine<=n&&(d.nextUrine<=d.nextPoop||d.nextPoop>n))return {kind:1,at:d.nextUrine,pieces:1};
  if(d.nextPoop<=n){const count=d.feeds.filter(f=>f>=d.nextPoop-6*H&&f<d.nextPoop).length;return {kind:2,at:d.nextPoop,pieces:count<=1?1:count<=3?2:3}}return null
 }
 finish(e){const d=this.data;if(e.kind===1&&d.nextUrine===e.at){d.urine++;d.nextUrine+=2*H}else if(e.kind===2&&d.nextPoop===e.at){d.poop+=e.pieces;d.nextPoop+=6*H;d.feeds=d.feeds.filter(f=>f>=d.nextPoop-6*H)}else return;this.save()}
 clear(){this.data.urine=this.data.poop=0;this.save()}
}
const lunarFormatter=new Intl.DateTimeFormat('en-u-ca-chinese',{month:'numeric',day:'numeric',timeZone:'Asia/Shanghai'});
function lunar(now){const p=lunarFormatter.formatToParts(new Date(now)),m=p.find(x=>x.type==='month').value;return {month:parseInt(m),day:Number(p.find(x=>x.type==='day').value),leap:!/^\d+$/.test(m)}}
function birthday(n){const l=lunar(n);return !l.leap&&l.day===18?(l.month===9?'爸爸':l.month===12?'妈妈':''):''}
function solarDay(year,month,target){
 let lo=Date.UTC(year,month-1,1)-8*H,hi=Date.UTC(year,month,1)-8*H;
 const sin=a=>Math.sin(a*Math.PI/180);
 function longitude(ms){const t=(ms/86400000+2440587.5+69/86400-2451545)/36525,l=280.46646+t*(36000.76983+t*.0003032),m=357.52911+t*(35999.05029-.0001537*t),centre=sin(m)*(1.914602-t*(.004817+.000014*t))+sin(2*m)*(.019993-.000101*t)+sin(3*m)*.000289;return ((l+centre-.00569-.00478*sin(125.04-1934.136*t))%360+360)%360}
 while(hi-lo>1000){const mid=Math.floor((lo+hi)/2);if(longitude(mid)<target)lo=mid;else hi=mid}
 return new Date(hi+8*H).getUTCDate();
}
function celebration(n){
 const who=birthday(n);if(who)return who+'生日快乐';
 const l=lunar(n);let f='';if(!l.leap){
  const m=l.month,d=l.day;f=m===1&&d<=3?'新年':({ '1-15':'元宵节','2-2':'龙抬头','5-5':'端午节','7-7':'七夕节','8-15':'中秋节','9-9':'重阳节','12-8':'腊八节','12-23':'小年','12-24':'小年'})[m+'-'+d]||'';
  const next=lunar(n+24*H);if(!f&&!next.leap&&next.month===1&&next.day===1)f='新年'
 }
 const g=new Date(n+8*H),year=g.getUTCFullYear(),m=g.getUTCMonth()+1,d=g.getUTCDate();
 if(!f&&m===4&&d===solarDay(year,4,15))f='清明节';
 if(!f&&m===12&&d===(year===2021?21:solarDay(year,12,270)))f='冬至';
 return f?'祝爸爸妈妈'+f+'快乐':'';
}
function dateContext(n){const solar=new Date(n+8*H),l=lunar(n),months=['正月','二月','三月','四月','五月','六月','七月','八月','九月','十月','冬月','腊月'],days=['','初一','初二','初三','初四','初五','初六','初七','初八','初九','初十','十一','十二','十三','十四','十五','十六','十七','十八','十九','二十','廿一','廿二','廿三','廿四','廿五','廿六','廿七','廿八','廿九','三十'],weeks=['星期日','星期一','星期二','星期三','星期四','星期五','星期六'];return '今天是公历'+solar.getUTCFullYear()+'年'+(solar.getUTCMonth()+1)+'月'+solar.getUTCDate()+'日，'+weeks[solar.getUTCDay()]+'；农历'+(l.leap?'闰':'')+months[l.month-1]+days[l.day]+'。'}
function asksToday(s){return ['今天几月几日','今天几号','今天日期','今天是几月','今天农历','今天阴历','今天阳历','今天公历','今天星期'].some(k=>s.includes(k))||['几月几日','几号','日期','农历','阴历','阳历','公历','星期几'].includes(s)}
function answer(text,n){const s=text.replace(/[\s，。！？,.!?：:；;‘’'"“”]/g,'').toLowerCase();if(asksToday(s))return dateContext(n);return ({'你最爱谁呀':'最爱爸爸妈妈','你最爱谁':'最爱爸爸妈妈','爸爸妈妈是谁':'爸爸是闫江桦，妈妈是张春华','叫爸爸':'爸爸','叫妈妈':'妈妈'})[s]||(birthday(n)?'球球祝'+birthday(n)+'生日快乐':'球球不知道')}
class Companion {
 constructor(store,clock=Date.now){this.store=store;this.clock=clock;let saved;try{saved=JSON.parse(store.getItem('qiuqiu-companion-v1'))}catch{}this.data=Object.assign({affection:0,pets:0,adopted:clock(),lastPet:0,visited:''},saved||{});this.save()}
 day(n){const d=new Date(n);return String(d.getFullYear())+String(d.getMonth()+1).padStart(2,'0')+String(d.getDate()).padStart(2,'0')}
 mood(n=this.clock()){return ['想被摸摸','安静陪伴','有点好奇','想晒太阳','软乎乎的一天','今天很黏人'][Number(this.day(n))%6]}
 note(n=this.clock()){return ['主人，今天也摸摸球球的头吧。','你忙你的，球球就在这里陪着你。','今天会遇到什么有趣的事情呢？','找个暖暖的角落，一起歇一会儿吧。','把今天的小烦恼交给球球，喵。','爸爸妈妈在的地方，就是球球的家。'][Number(this.day(n))%6]}
 title(){const n=this.data.affection;return n<20?'初见的小伙伴':n<50?'熟悉的朋友':n<80?'黏人的小棉袄':'最爱的家人'}
 days(n=this.clock()){return Math.max(1,Math.floor((n-this.data.adopted)/86400000)+1)}
 pet(n=this.clock()){const d=this.data;if(d.lastPet&&n-d.lastPet<30000)return false;d.lastPet=n;d.pets++;d.affection=Math.min(100,d.affection+2);this.save();return true}
 visit(n=this.clock()){const d=this.data,today=this.day(n);if(d.visited===today)return false;d.visited=today;d.affection=Math.min(100,d.affection+3);this.save();return true}
 save(){this.store.setItem('qiuqiu-companion-v1',JSON.stringify(this.data))}
}
class IdleClock {
 constructor(){this.elapsed=0;this.last=null}
 reset(now){this.elapsed=0;this.last=now}
 advance(now,playing,mode='sit'){const delta=this.last===null?0:Math.max(0,now-this.last);this.last=now;if(!playing)return false;this.elapsed+=delta;const span=mode==='rest'?10000:15000;if(this.elapsed<span)return false;this.elapsed-=span;return true}
}
function motionIndex(elapsed,duration=5000,loop=true){const t=Math.max(0,elapsed);return !loop&&t>=duration?59:Math.floor((t%duration)*60/duration)}
function motionPhase(index,loop){return loop?index/2.5:index*23/59}
function motionFrame(elapsed,duration=5000,loop=true){const t=Math.max(0,elapsed);return !loop&&t>=duration?59:(t%duration)*60/duration}
function motionPhaseAt(elapsed,duration=5000,loop=true){const t=Math.max(0,elapsed);if(loop)return (t%duration)*24/duration;if(t>=duration)return 23;return t*23/duration}
if(typeof module!=='undefined')module.exports={motionIndex,motionPhase,motionFrame,motionPhaseAt,IdleClock,Companion,CareState,ActionTimeline,lunar,birthday,celebration,dateContext,answer,H};
