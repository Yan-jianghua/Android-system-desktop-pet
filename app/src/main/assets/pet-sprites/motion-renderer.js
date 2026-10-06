'use strict';
/** Shares measured rectangles and anchors with the Android renderer. */
class CatRenderer {
 constructor(canvas){this.canvas=canvas;this.c=canvas.getContext('2d');this.images=new Map;this.pending=new Map;this.sequences=new Map;this.loading=new Map;this.mesh=new MotionMesh;this.ready=fetch('motions24.json').then(r=>{if(!r.ok)throw Error('动作坐标加载失败');return r.json()}).then(data=>{this.motions=data.motions;for(const a of Object.values(this.motions))a.duration=5000;return Promise.all([this.sequence('sit'),this.load('care-props.png')])});this.ready.catch(e=>{this.error=e.message})}
 sequence(key){if(this.sequences.has(key))return Promise.resolve(this.sequences.get(key));if(this.loading.has(key))return this.loading.get(key);const a=this.motions[key],task=Promise.all([this.load(a.file),fetch('motions24/'+key+'-flow.json').then(r=>{if(!r.ok)throw Error('补间数据加载失败');return r.json()})]).then(([im,flow])=>{const keys=a.frames.map(([l,t,r,b,ax,ay])=>{const c=document.createElement('canvas');c.width=c.height=256;c.getContext('2d').drawImage(im,l,t,r-l,b-t,a.anchorX+(l-ax)*a.scale,241+(t-ay-a.bottom)*a.scale,(r-l)*a.scale,(b-t)*a.scale);return c});const seq={keys,flow};this.sequences.set(key,seq);this.loading.delete(key);this.images.delete(a.file);this.pending.delete(a.file);while(this.sequences.size>3)this.sequences.delete(this.sequences.keys().next().value);return seq});this.loading.set(key,task);task.catch(e=>{this.error=e.message});return task}
 key(pose,food){if(pose==='rest')return 'sit';return pose==='eat'?({'猫条':'treat','巧克力':'chocolate'}[food]||'kibble'):pose}
 bounds(pose,food,mode){const a=this.motions?.[this.key(pose,food)];if(!a)return {top:60,bottom:273};const top=Math.min(...a.frames.map(f=>241+(f[1]-f[5]-a.bottom)*a.scale)),tray=['pee','poop','clean'].includes(mode),factor=pose==='clean'?.66:tray?.86:1,ground=pose==='clean'?157:tray?185:241;return {top:32+ground+(top-241)*factor,bottom:tray?300:273}}
 load(file){if(this.images.has(file))return Promise.resolve(this.images.get(file));if(this.pending.has(file))return this.pending.get(file);const p=new Promise((resolve,reject)=>{const im=new Image;im.onload=()=>{this.images.set(file,im);resolve(im)};im.onerror=()=>reject(Error('动作素材加载失败：'+file));im.src=file});this.pending.set(file,p);p.catch(e=>{this.error=e.message});return p}
 prop(index,x,y,w,h){const im=this.images.get('care-props.png');if(!im)return;const cell=im.width/2;this.c.drawImage(im,index%2*cell,Math.floor(index/2)*cell,cell,cell,x,y,w,h)}
 basin(front){const c=this.c;c.save();if(front){c.beginPath();c.rect(0,197,256,71);c.clip()}this.prop(0,0,12,256,256);c.restore()}
 scoop(elapsed){const c=this.c,phase=motionFrame(elapsed,5000,true)*Math.PI*2/60;c.save();c.translate(Math.sin(phase)*20,Math.cos(phase)*5);c.translate(159,180);c.rotate(Math.sin(phase)*5*Math.PI/180);c.translate(-159,-180);c.translate(41,31);c.scale(.34,.34);c.beginPath();[[460,0],[627,0],[558,92],[570,113],[553,204],[497,280],[458,315],[466,337],[425,438],[356,454],[260,452],[207,425],[294,339],[347,294],[382,246],[381,189],[422,115]].forEach(([x,y],i)=>i?c.lineTo(x,y):c.moveTo(x,y));c.closePath();c.clip();this.prop(1,0,0,627,627);c.restore()}
 render({pose='sit',mode=pose,food='',elapsed=0,face=1,urine=0,poop=0,heartsUntil=0,now=0,frameIndex=null}){
  if(!this.motions)return false;const key=this.key(pose,food),a=this.motions[key]||this.motions.sit;
  const seq=this.sequences.get(key);if(!seq){this.sequence(key);return false}
  const c=this.c,canvas=this.canvas,index=frameIndex??motionIndex(elapsed,a.duration,a.loop);this.lastIndex=index;
  c.clearRect(0,0,canvas.width,canvas.height);c.save();c.scale(canvas.width/320,canvas.height/320);c.translate(32,32);
  const tray=['pee','poop','clean'].includes(mode);if(tray){this.basin(false);for(let i=0;i<Math.min(urine,18);i++)this.prop(2,13+(i*47)%179,130+(i*17)%40,26,30);for(let i=0;i<Math.min(poop,18);i++)this.prop(3,17+(i*61)%175,136+(i*11)%31,30,30)}
  const ground=pose==='clean'?157:tray?185:241,centre=pose==='clean'?69:128,factor=pose==='clean'?.66:tray?.86:1;
  const phase=frameIndex!=null?motionPhase(index,a.loop):motionPhaseAt(elapsed,a.duration,a.loop),im=Number.isInteger(phase)?seq.keys[phase]:this.mesh.render(seq,phase);
  c.save();if(pose==='rest')factor*=1+Math.sin(now*.002)*.008;if(['walk','run'].includes(pose)&&face<0){c.translate(256,0);c.scale(-1,1)}c.drawImage(im,centre-128*factor,ground-241*factor,256*factor,256*factor);c.restore();
  if(tray)this.basin(true);if(mode==='clean')this.scoop(elapsed);
  if(now<heartsUntil&&pose==='happy'){for(let i=0;i<3;i++){const p=(1-(heartsUntil-now)/5000+i*.22)%1;c.globalAlpha=1-p;c.fillStyle='#d58f9c';c.font=(16+i*2)+'px serif';c.fillText('♥',72+i*55+Math.sin(p*5+i)*7,105-p*62)}}c.restore();return true
 }
}





