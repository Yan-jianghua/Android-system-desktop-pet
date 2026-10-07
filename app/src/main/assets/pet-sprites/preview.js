'use strict';
const canvas=document.querySelector('canvas'),panel=document.getElementById('panel'),speech=document.getElementById('speech'),memory=document.getElementById('memory'),status=document.getElementById('status');
let offset=Number(localStorage.getItem('qiuqiu-preview-offset')||0),mode='sit',food='',travel='',since=performance.now(),busy=false,face=1,drag=null,speechVersion=0,speechTimer=0,heartsUntil=0;
const now=()=>Date.now()+offset,state=new CareState(localStorage,now),actions=new ActionTimeline,renderer=new CatRenderer(canvas);
const companion=new Companion(localStorage,()=>Date.now());
const idle=new IdleClock;idle.reset(performance.now());
function restartIdle(){set('rest',true);idle.reset(performance.now())}
function layoutPet(){
 if(!canvas.getBoundingClientRect)return;
 const wrap=document.getElementById('pet-wrap'),stage=document.getElementById('pet-stage'),scale=canvas.getBoundingClientRect().width/320,bounds=renderer.bounds(travel||mode,food,mode);
 const top=Math.max(0,speech.offsetHeight+10-bounds.top*scale),below=panel.classList.contains('dialogue-mode')||panel.classList.contains('below-mode');
 stage.style.top=top+'px';panel.style.top=(below?bounds.bottom*scale+3:-14)+'px';
 wrap.style.height=(top+Math.max(203,panel.hidden?0:below?bounds.bottom*scale+3+panel.offsetHeight:210)+4)+'px';
}
function button(parent,label,fn){const b=document.createElement('button');b.textContent=label;b.setAttribute('aria-label',label);b.onclick=fn;parent.append(b);return b}
function set(m,replay=false){if(mode!==m||replay){mode=m;since=performance.now();canvas.setAttribute('aria-label','球球'+({sit:'正在坐着左右看',groom:'正在舔毛',lie:'正在趴着摇尾巴'}[m]||'')+'，点击可以喂食、铲屎或对话')}}
function say(text,after,duration=5000){
 const version=++speechVersion,done=()=>{if(speechVersion===version)after?.()};
 clearTimeout(speechTimer);
 speech.textContent=text;
 if(after)setTimeout(done,2200);
 speechTimer=setTimeout(()=>{if(speechVersion===version){speech.textContent='';layoutPet()}},duration)
}
function sayChat(text){say(text,null,10000)}
function greet(){const hour=new Date().getHours();say((hour>=5&&hour<12?'早上好':hour>=12&&hour<18?'下午好':'晚上好')+'，主人',()=>say('请尽情吩咐球球，主人'))}
const selected=document.getElementById('date');
function dialogueDate(){return selected.value?new Date(selected.value+'T12:00:00+08:00').getTime():Date.now()}
selected.onchange=()=>{const greeting=celebration(dialogueDate());say(greeting||'今天球球也陪着你')};
document.getElementById('today').onclick=()=>{selected.value='';say('已恢复今天的日期')};
function close(){panel.hidden=true;panel.classList.remove('dialogue-mode','below-mode');layoutPet()}
function menu(clicked=true){
 panel.classList.remove('dialogue-mode','below-mode');
 panel.replaceChildren();panel.hidden=false;
 if(clicked){const f=celebration(dialogueDate());if(f)say(f,()=>say('请尽情吩咐球球，主人'));else say('请尽情吩咐球球，主人')}
 button(panel,'喂食',()=>{panel.replaceChildren();for(const f of ['猫粮','猫条','巧克力'])button(panel,f,()=>feed(f));button(panel,'返回',()=>menu(false))});
 button(panel,'铲屎',clean);
 button(panel,'对话',()=>{panel.classList.add('dialogue-mode');panel.replaceChildren();const input=document.createElement('input');input.placeholder='和球球说话…';input.setAttribute('aria-label','对球球说的话');panel.append(input);const send=()=>{sayChat(answer(input.value,dialogueDate()));input.value=''};button(panel,'发送',send);button(panel,'返回',()=>menu(false));input.onkeydown=e=>{if(e.key==='Enter')send()};layoutPet();input.focus()});
 button(panel,'陪球球玩',companionMenu);button(panel,'让球球回家',close);button(panel,'收起',close);button(panel,'锁定球球',()=>{close();say('手机端锁定后，请在球球应用内点击“解救球球”')})
}
function companionMenu(){
 panel.classList.remove('dialogue-mode');panel.classList.add('below-mode');panel.replaceChildren();panel.hidden=false;const p=document.createElement('p');p.textContent=companion.title()+' · 亲密度 '+companion.data.affection+'/100\n相伴第 '+companion.days()+' 天 · '+companion.mood();panel.append(p);
 button(panel,'摸摸球球',()=>{if(busy){say('球球正在忙，稍等一下哦');return}const earned=companion.pet();close();actions.start('happy','',5000,null,performance.now());syncAction();heartsUntil=performance.now()+5000;say(earned?'蹭蹭主人，亲密度 +2 ♡':'主人，再摸摸球球嘛');refresh()});
 button(panel,'今日悄悄话',()=>{const earned=companion.visit();say(companion.note()+(earned?' 亲密度 +3 ♡':''));companionMenu();refresh()});button(panel,'返回',()=>menu(false))
}
function syncAction(){const wasBusy=busy;busy=actions.busy;food=actions.food;if(busy)set(actions.mode);else if(wasBusy)restartIdle()}
function feed(f){
 if(busy){say(now()<state.data.feedUntil?'十秒内不能再次喂食，还剩 '+Math.max(1,Math.ceil((state.data.feedUntil-now())/1000))+' 秒':'球球正在忙，稍等一下哦');return}if(!state.feed(now())){say('十秒内不能再次喂食，还剩 '+Math.max(1,Math.ceil((state.data.feedUntil-now())/1000))+' 秒');return}
 close();actions.start('eat',f,5000,()=>{if(f==='猫条'||f==='巧克力'){actions.start(f==='猫条'?'happy':'disgust','',5000,null,performance.now());say(f==='猫条'?'主人你真好':'狗屎东西')}},performance.now());syncAction();refresh()
}
function clean(){
 if(busy){say('球球正在忙，稍等一下哦');return}close();
 actions.start('clean','',7000,()=>{const poop=state.data.poop,urine=state.data.urine;state.clear();say('铲完啦！一共有 '+poop+' 个便便、'+urine+' 个尿团');panel.replaceChildren();panel.hidden=false;const p=document.createElement('p');p.textContent='本次清理：便便 '+poop+' 个，尿团 '+urine+' 个。猫砂盆已清空。';panel.classList.add('below-mode');panel.append(p);button(panel,'知道啦',close)},performance.now());syncAction()
}
function writeText(element,text){if(element.textContent!==text)element.textContent=text}
function refresh(){
 writeText(document.getElementById('bond'),'相伴第 '+companion.days()+' 天 · '+companion.title()+' · ♡ '+companion.data.affection);
 const d=state.data;writeText(memory,'猫砂盆 · 便便 '+d.poop+' 个 / 尿团 '+d.urine+' 个');
 const fmt=t=>new Date(t-offset).toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit',second:'2-digit'});
 writeText(status,'下一次尿尿 '+fmt(d.nextUrine)+' · 便便 '+fmt(d.nextPoop)+' · 喂食冷却 '+Math.max(0,Math.ceil((d.feedUntil-now())/1000))+' 秒');
}
function tick(){
 const motionNow=performance.now();actions.update(motionNow);syncAction();refresh();if(document.hidden){idle.advance(motionNow,false);return}
 const e=actions.toilet?null:state.nextDue(now());if(e){actions.interrupt(e.kind===1?'pee':'poop',5000,()=>state.finish(e),motionNow);syncAction()}
 if(idle.advance(motionNow,!busy&&!drag,mode)){set(mode==='rest'?['groom','lie','sit'][Math.floor(Math.random()*3)]:'rest')}
 if(!busy)since=motionNow-idle.elapsed;
}
setInterval(tick,50);document.addEventListener('visibilitychange',()=>{idle.advance(performance.now(),false);if(!document.hidden){tick();greet()}});refresh();greet();
for(const [label,m] of [['走路','walk'],['跑步','run'],['开心','happy'],['嫌弃','disgust']])button(document.getElementById('checks'),label,()=>{if(!busy){close();actions.start(m,'',5000,null,performance.now());syncAction()}});
for(const [label,delta] of [['经过10秒',10000],['经过2小时',2*H],['经过6小时',6*H]])button(document.getElementById('checks'),label,()=>{offset+=delta;localStorage.setItem('qiuqiu-preview-offset',offset);tick()});
button(document.getElementById('checks'),'触发尿尿',()=>{state.data.nextUrine=now();state.save();tick()});
button(document.getElementById('checks'),'触发便便',()=>{state.data.nextPoop=now();state.save();tick()});
canvas.onpointerdown=e=>{canvas.setPointerCapture(e.pointerId);drag={x:e.clientX,y:e.clientY,lastX:e.clientX,lastY:e.clientY,time:performance.now(),speed:0,fastSince:0,moved:false}};
canvas.onpointermove=e=>{
 if(!drag)return;const dx=e.clientX-drag.x,dy=e.clientY-drag.y;if(Math.hypot(dx,dy)>6)drag.moved=true;
 if(drag.moved){const n=performance.now(),v=Math.hypot(e.clientX-drag.lastX,e.clientY-drag.lastY)/Math.max(1,n-drag.time)*1000;
 drag.speed=.55*v+.45*drag.speed;if(drag.speed>2200){if(!drag.fastSince)drag.fastSince=n}else drag.fastSince=0;
 face=e.clientX<drag.lastX?-1:1;travel=drag.fastSince&&n-drag.fastSince>=60?'run':'walk';
 document.getElementById('pet-wrap').style.transform='translate('+Math.max(-80,Math.min(80,dx))+'px,'+Math.max(-70,Math.min(70,dy))+'px)';
 drag.lastX=e.clientX;drag.lastY=e.clientY;drag.time=n
 }
};
function release(click){if(!drag)return;const moved=drag.moved,open=click&&!moved;drag=null;travel='';document.getElementById('pet-wrap').style.transform='';if(moved&&!busy)restartIdle();if(open)menu()}
canvas.onpointerup=()=>release(true);canvas.onpointercancel=()=>release(false);
let lastDraw=0;
function draw(ms){
 requestAnimationFrame(draw);if(document.hidden)return;lastDraw=ms;layoutPet();
 const pose=travel||mode,label='球球'+({sit:'正在坐着左右看',groom:'正在舔毛',lie:'正在趴着摇尾巴',eat:'正在吃'+food,clean:'正在铲屎',pee:'正在尿尿',poop:'正在便便',happy:'开心地感谢主人',disgust:'露出嫌弃的表情',walk:'正在走路',run:'正在跑步'}[pose]||'')+'，点击可以喂食、铲屎或对话';
 if(canvas.getAttribute('aria-label')!==label)canvas.setAttribute('aria-label',label);
 renderer.render({pose,mode,food,elapsed:actions.busy?actions.elapsed(ms):ms-since,face,urine:state.data.urine,poop:state.data.poop,heartsUntil,now:ms});
 if(renderer.error)speech.textContent=renderer.error;
}
requestAnimationFrame(draw);


