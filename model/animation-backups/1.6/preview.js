'use strict';
const canvas=document.querySelector('canvas'),screen=canvas.getContext('2d'),panel=document.getElementById('panel'),speech=document.getElementById('speech'),memory=document.getElementById('memory'),status=document.getElementById('status');
let offset=Number(localStorage.getItem('qiuqiu-preview-offset')||0),mode='sit',food='',travel='',since=performance.now(),busy=false,face=1,drag=null,idleAt=Date.now()+7000,speechVersion=0;
const now=()=>Date.now()+offset,state=new CareState(localStorage,now),actions=new ActionTimeline,images={};
let config,c=screen,heartsUntil=0;
const companion=new Companion(localStorage,()=>Date.now());
let sceneBuffer,lastBuffer,oldBuffer,sceneContext,lastContext,oldContext,renderKey='',shownKey='',shownPose='',changedAt=0,blendDuration=110,rendered=false;
Promise.all(['qiuqiu-own.png','qiuqiu-care.png','qiuqiu-toilet.png','care-props.png','qiuqiu-disgust-v3.png'].map(file=>new Promise((resolve,reject)=>{const im=new Image;im.onload=()=>{images[file]=im;resolve()};im.onerror=()=>{speech.textContent='动作素材加载失败';reject(new Error(file))};im.src=file}))).then(()=>fetch('own-atlas.json').then(r=>r.json())).then(data=>{config=data;requestAnimationFrame(draw)});
function button(parent,label,fn){const b=document.createElement('button');b.textContent=label;b.onclick=fn;parent.append(b);return b}
function set(m,replay=false){if(mode!==m||replay){mode=m;since=performance.now();canvas.setAttribute('aria-label','球球'+({sit:'正在坐着左右看',groom:'正在舔毛',lie:'正在趴着摇尾巴'}[m]||'')+'，点击可以喂食、铲屎或对话')}}
function say(text,after){
 const version=++speechVersion,done=()=>{if(speechVersion===version)after?.()};
 speech.textContent=text;
 if(after)setTimeout(done,2200)
}
function greet(){const hour=new Date().getHours();say((hour>=5&&hour<12?'早上好':hour>=12&&hour<18?'下午好':'晚上好')+'，主人',()=>say('请尽情吩咐球球，主人'))}
const selected=document.getElementById('date');
function dialogueDate(){return selected.value?new Date(selected.value+'T12:00:00+08:00').getTime():Date.now()}
selected.onchange=()=>{const greeting=celebration(dialogueDate());say(greeting||'今天球球也陪着你')};
document.getElementById('today').onclick=()=>{selected.value='';say('已恢复今天的日期')};
function close(){panel.hidden=true;idleAt=Date.now()+7000}
function menu(clicked=true){
 if(!busy)set('sit');
 panel.replaceChildren();panel.hidden=false;
 if(clicked){const f=celebration(dialogueDate());if(f)say(f,()=>say('请尽情吩咐球球，主人'));else say('请尽情吩咐球球，主人')}
 button(panel,'喂食',()=>{panel.replaceChildren();for(const f of ['猫粮','猫条','巧克力'])button(panel,f,()=>feed(f));button(panel,'返回',()=>menu(false))});
 button(panel,'铲屎',clean);
 button(panel,'对话',()=>{panel.replaceChildren();const input=document.createElement('input');input.placeholder='想对球球说什么？';input.setAttribute('aria-label','对球球说的话');panel.append(input);const send=()=>{say(answer(input.value,dialogueDate()));input.value=''};button(panel,'发送',send);button(panel,'返回',()=>menu(false));input.onkeydown=e=>{if(e.key==='Enter')send()};input.focus()});
 button(panel,'陪球球玩',companionMenu);button(panel,'收起',close)
}
function companionMenu(){
 panel.replaceChildren();panel.hidden=false;const p=document.createElement('p');p.textContent=companion.title()+' · 亲密度 '+companion.data.affection+'/100\n相伴第 '+companion.days()+' 天 · '+companion.mood();panel.append(p);
 button(panel,'摸摸球球',()=>{if(busy){say('球球正在忙，稍等一下哦');return}const earned=companion.pet();close();actions.start('happy','',3500,null,performance.now());syncAction();heartsUntil=performance.now()+3500;say(earned?'蹭蹭主人，亲密度 +2 ♡':'主人，再摸摸球球嘛');refresh()});
 button(panel,'今日悄悄话',()=>{const earned=companion.visit();say(companion.note()+(earned?' 亲密度 +3 ♡':''));companionMenu();refresh()});button(panel,'返回',()=>menu(false))
}
function syncAction(){const wasBusy=busy;busy=actions.busy;food=actions.food;if(busy)set(actions.mode);else if(wasBusy){set('sit');idleAt=Date.now()+7000}}
function feed(f){
 if(busy){say(now()<state.data.feedUntil?'十秒内不能再次喂食，还剩 '+Math.max(1,Math.ceil((state.data.feedUntil-now())/1000))+' 秒':'球球正在忙，稍等一下哦');return}if(!state.feed(now())){say('十秒内不能再次喂食，还剩 '+Math.max(1,Math.ceil((state.data.feedUntil-now())/1000))+' 秒');return}
 close();actions.start('eat',f,5000,()=>{if(f==='猫条'||f==='巧克力'){actions.start(f==='猫条'?'happy':'disgust','',f==='巧克力'?2800:2500,null,performance.now());say(f==='猫条'?'主人你真好':'狗屎东西')}},performance.now());syncAction();refresh()
}
function clean(){
 if(busy){say('球球正在忙，稍等一下哦');return}close();
 actions.start('clean','',7000,()=>{const poop=state.data.poop,urine=state.data.urine;state.clear();say('铲完啦！一共有 '+poop+' 个便便、'+urine+' 个尿团');panel.replaceChildren();panel.hidden=false;const p=document.createElement('p');p.textContent='本次清理：便便 '+poop+' 个，尿团 '+urine+' 个。猫砂盆已清空。';panel.append(p);button(panel,'知道啦',close)},performance.now());syncAction()
}
function writeText(element,text){if(element.textContent!==text)element.textContent=text}
function refresh(){
 writeText(document.getElementById('bond'),'相伴第 '+companion.days()+' 天 · '+companion.title()+' · ♡ '+companion.data.affection);
 const d=state.data;writeText(memory,'猫砂盆 · 便便 '+d.poop+' 个 / 尿团 '+d.urine+' 个');
 const fmt=t=>new Date(t-offset).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit',second:'2-digit'});
 writeText(status,'下一次尿尿 '+fmt(d.nextUrine)+' · 便便 '+fmt(d.nextPoop)+' · 喂食冷却 '+Math.max(0,Math.ceil((d.feedUntil-now())/1000))+' 秒');
}
function tick(){
 const motionNow=performance.now();actions.update(motionNow);syncAction();refresh();if(document.hidden)return;
 const e=actions.toilet?null:state.nextDue(now());if(e){actions.interrupt(e.kind===1?'pee':'poop',5000,()=>state.finish(e),motionNow);syncAction()}
 else if(!busy&&!drag&&panel.hidden&&Date.now()>idleAt){const choices=['groom','lie','sit'].filter(m=>m!==mode);set(choices[Math.floor(Math.random()*choices.length)]);idleAt=Date.now()+7000+Math.random()*6000}
}
setInterval(tick,250);document.addEventListener('visibilitychange',()=>{if(!document.hidden){tick();greet()}});refresh();greet();
for(const [label,m] of [['走路','walk'],['跑步','run'],['开心','happy'],['嫌弃','disgust']])button(document.getElementById('checks'),label,()=>{if(!busy){close();set(m,true);idleAt=Date.now()+20000}});
for(const [label,delta] of [['经过10秒',10000],['经过2小时',2*H],['经过6小时',6*H]])button(document.getElementById('checks'),label,()=>{offset+=delta;localStorage.setItem('qiuqiu-preview-offset',offset);tick()});
button(document.getElementById('checks'),'触发尿尿',()=>{state.data.nextUrine=now();state.save();tick()});
button(document.getElementById('checks'),'触发便便',()=>{state.data.nextPoop=now();state.save();tick()});
canvas.onpointerdown=e=>{canvas.setPointerCapture(e.pointerId);drag={x:e.clientX,y:e.clientY,lastX:e.clientX,lastY:e.clientY,time:performance.now(),speed:0,fastSince:0,moved:false}};
canvas.onpointermove=e=>{
 if(!drag)return;const dx=e.clientX-drag.x,dy=e.clientY-drag.y;if(Math.hypot(dx,dy)>6)drag.moved=true;
 if(drag.moved){const n=performance.now(),v=Math.hypot(e.clientX-drag.lastX,e.clientY-drag.lastY)/Math.max(1,n-drag.time)*1000;
 drag.speed=.55*v+.45*drag.speed;if(drag.speed>2200){if(!drag.fastSince)drag.fastSince=n}else drag.fastSince=0;
 face=e.clientX<drag.lastX?-1:1;travel=drag.fastSince&&n-drag.fastSince>=60?'run':'walk';
 speech.style.transform=canvas.style.transform='translate('+Math.max(-80,Math.min(80,dx))+'px,'+Math.max(-70,Math.min(70,dy))+'px)';
 drag.lastX=e.clientX;drag.lastY=e.clientY;drag.time=n
 }
};
function release(click){if(!drag)return;const open=click&&!drag.moved;drag=null;travel='';speech.style.transform=canvas.style.transform='';if(!busy)set('sit');idleAt=Date.now()+7000;if(open)menu()}
canvas.onpointerup=()=>release(true);canvas.onpointercancel=()=>release(false);
function prop(tile,x,y,w,h){const p=images['care-props.png'],cell=p.width/2;c.drawImage(p,(tile%2)*cell,Math.floor(tile/2)*cell,cell,cell,x,y,w,h)}
function basin(front){c.save();if(front){c.beginPath();c.rect(0,197,256,59);c.clip()}prop(0,0,12,256,256);c.restore()}
function deposits(){
 for(let i=0;i<Math.min(state.data.urine,18);i++){const x=26+(i*47)%179,y=145+(i*17)%40;prop(2,x-13,y-15,26,30)}
 for(let i=0;i<Math.min(state.data.poop,18);i++){const x=32+(i*61)%175,y=151+(i*11)%31;prop(3,x-15,y-15,30,30)}
}
function scoop(t){
 c.save();c.translate(Math.sin(t*2.3)*20,Math.cos(t*2.3)*5);c.translate(159,180);c.rotate(Math.sin(t*2.3)*5*Math.PI/180);c.translate(-159,-180);c.translate(41,31);c.scale(.34,.34);
 c.beginPath();const pts=[[460,0],[627,0],[558,92],[570,113],[553,204],[497,280],[458,315],[466,337],[425,438],[356,454],[260,452],[207,425],[294,339],[347,294],[382,246],[381,189],[422,115]];pts.forEach(([x,y],i)=>i?c.lineTo(x,y):c.moveTo(x,y));c.closePath();c.clip();prop(1,0,0,627,627);c.restore()
}
function frame(row,col,group,ground,factor,centre){
 const a=config[group],im=images[group==='own'?'qiuqiu-own.png':group==='care'?'qiuqiu-care.png':'qiuqiu-toilet.png'],tile=im.width/(group==='own'?4:2),top=a.rows[row],h=a.rows[row+1]-top,s=Math.min(224/tile,224/h)*factor,w=tile*s,y=ground-(a.ground[row]-top)*s;c.save();if(group==='own'){const inset=a.clipLeft[row][col]*s;c.beginPath();c.rect(centre-w/2+inset,y,w-inset,h*s);c.clip()}c.drawImage(im,col*tile,top,tile,h,centre-w/2,y,w,h*s);c.restore()
}
function lying(t){
 const im=images['qiuqiu-own.png'],top=config.own.rows[2],cell=im.width/4,h=config.own.rows[3]-top;const tile=()=>c.drawImage(im,cell,top,cell,h,0,0,256,256);
 c.save();c.translate(16,28);c.scale(.875,.875);c.save();c.translate(132,132);c.rotate(Math.sin(t*1.55)*9*Math.PI/180);c.translate(-132,-132);
 c.save();c.beginPath();c.rect(65,65,145,67);c.clip();tile();c.restore();
 c.save();c.translate(131,70);c.rotate(Math.sin(t*1.55-.35)*5*Math.PI/180);c.translate(-131,-70);c.beginPath();c.rect(65,0,145,70);c.clip();tile();c.restore();c.restore();
 c.save();c.beginPath();c.rect(config.own.clipLeft[2][1],124,256-config.own.clipLeft[2][1],132);c.clip();tile();c.restore();c.restore()
}
function disgust(t,ground,factor,centre){
 const im=images['qiuqiu-disgust-v3.png'],a=config.disgust,tile=im.width/2;
 const current=t<.56?0:t<1.22?1:t<1.50?2:3;renderKey+='/'+current;let lean=t<.60?t/.60:t<1.50?1:Math.max(0,1-(t-1.50)/.70);lean=lean*lean*(3-2*lean);
 const row=Math.floor(current/2),top=a.rows[row],h=a.rows[row+1]-top,s=Math.min(224/tile,224/h)*factor;
 c.save();c.translate(1.2*lean*factor,-.8*lean*factor);c.drawImage(im,(current%2)*tile,top,tile,h,centre-tile*s/2,ground-(a.ground[row]-top)*s,tile*s,h*s);c.restore()
}
function draw(ms){
 requestAnimationFrame(draw);if(!config||document.hidden)return;
 if(!sceneBuffer){[sceneBuffer,lastBuffer,oldBuffer]=Array.from({length:3},()=>{const b=document.createElement('canvas');b.width=b.height=512;return b});sceneContext=sceneBuffer.getContext('2d');lastContext=lastBuffer.getContext('2d');oldContext=oldBuffer.getContext('2d')}
 c=sceneContext;c.clearRect(0,0,512,512);c.save();c.scale(2,2);
 const t=actions.busy?actions.elapsed(ms)/1000:(ms-since)/1000,toilet=['pee','poop','clean'].includes(mode),pose=travel||mode;
 const label='球球'+({sit:'正在坐着左右看',groom:'正在舔毛',lie:'正在趴着摇尾巴',eat:'正在吃'+food,clean:'正在铲屎',pee:'正在尿尿',poop:'正在便便',happy:'开心地感谢主人',disgust:'露出嫌弃的表情',walk:'正在走路',run:'正在跑步'}[pose]||'')+'，点击可以喂食、铲屎或对话';if(canvas.getAttribute('aria-label')!==label)canvas.setAttribute('aria-label',label);
 if(toilet){basin(false);deposits()}
 let row=0,col=Math.floor(t*1.6)%4,group='own',ground=toilet?185:241,factor=toilet?.86:1,centre=128;
 if(pose==='groom'){row=1;col=Math.floor(t*2.6)%4}else if(pose==='walk'){row=3;col=Math.floor(t*6)%4}else if(pose==='run'){row=4;col=Math.floor(t*9)%4}else if(pose==='happy'){row=5;col=Math.floor(t*3)%4}else if(pose==='eat'){group='care';row=food==='猫粮'?0:food==='猫条'?1:2;col=Math.floor(t*2)%2}else if(pose==='pee'||pose==='poop'){group='toilet';row=pose==='pee'?0:1;col=pose==='poop'?(t<4?0:1):Math.floor(t*1.4)%2}else if(pose==='clean'){row=0;col=2;ground=157;factor=.66;centre=69}
 renderKey=[pose,food,row,['lie','disgust'].includes(pose)?0:col,face].join('/');
 c.save();if(!toilet&&!['walk','run'].includes(pose)){const breath=1+Math.sin(ms/1400)*.003;c.translate(128,ground);c.scale(1,breath);c.translate(-128,-ground)}if(['walk','run'].includes(pose)&&face<0){c.translate(256,0);c.scale(-1,1)}
 if(pose==='lie')lying(t);else if(pose==='disgust')disgust(t,ground,factor,centre);else frame(row,col,group,ground,factor,centre);c.restore();
 if(toilet)basin(true);if(mode==='clean')scoop(t);c.restore();
 if(renderKey!==shownKey){oldContext.clearRect(0,0,512,512);oldContext.drawImage(lastBuffer,0,0);blendDuration=pose===shownPose?95:170;changedAt=ms;shownKey=renderKey;shownPose=pose}
 let blend=rendered?Math.min(1,(ms-changedAt)/blendDuration):1;blend=blend*blend*(3-2*blend);
 lastContext.clearRect(0,0,512,512);lastContext.globalAlpha=1-blend;lastContext.drawImage(oldBuffer,0,0);lastContext.globalCompositeOperation='lighter';lastContext.globalAlpha=blend;lastContext.drawImage(sceneBuffer,0,0);lastContext.globalAlpha=1;lastContext.globalCompositeOperation='source-over';rendered=true;
 screen.clearRect(0,0,canvas.width,canvas.height);screen.drawImage(lastBuffer,0,0,canvas.width,canvas.height);
 if(ms<heartsUntil&&pose==='happy'){screen.save();screen.scale(3,3);for(let i=0;i<3;i++){const p=(1-(heartsUntil-ms)/3500+i*.22)%1;screen.globalAlpha=1-p;screen.fillStyle='#d58f9c';screen.font=(16+i*2)+'px serif';screen.fillText('♥',72+i*55+Math.sin(p*5+i)*7,105-p*62)}screen.restore()}
}




