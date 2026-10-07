// Run the real preview controller with a deterministic clock and a small DOM.
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
let clock=0;const timers=[],nodes=new Map,db=new Map;
class Element {
 constructor(){this.style={};this.hidden=false;this.value='';this.children=[];this.attrs={};this.textContent='';this.classList={add(){},remove(){},contains(){return false}}}
 append(e){this.children.push(e)}replaceChildren(){this.children=[]}setAttribute(k,v){this.attrs[k]=v}
 getAttribute(k){return this.attrs[k]}focus(){}setPointerCapture(){}getContext(){return {}}
}
for(const id of ['canvas','panel','speech','memory','status','date','today','checks','bond','pet-wrap'])nodes.set(id,new Element);
nodes.get('panel').hidden=true;
const context=vm.createContext({
 document:{hidden:false,querySelector:s=>nodes.get(s),getElementById:id=>nodes.get(id),createElement:()=>new Element,addEventListener(){}},
 localStorage:{getItem:k=>db.get(k),setItem:(k,v)=>db.set(k,v)},performance:{now:()=>clock},
 Date:class extends Date{static now(){return 1800000000000+clock}},Image:class{},CatRenderer:class{render(){}},window:{},
 setInterval(){},setTimeout:(fn,delay)=>timers.push({fn,at:clock+delay}),clearTimeout(){},requestAnimationFrame(){},console
});
for(const file of ['care.js','preview.js'])vm.runInContext(fs.readFileSync('app/src/main/assets/pet-sprites/'+file,'utf8'),context);
const run=code=>vm.runInContext(code,context);
function advance(to){clock=to;for(const task of timers.splice(0)){if(task.at<=clock)task.fn();else timers.push(task)}run('tick()')}

run("feed('猫条')");assert.equal(run('actions.mode'),'eat');
advance(2000);run('menu();state.data.nextUrine=now();state.save();tick()');
assert.equal(run('panel.hidden'),false);assert.equal(run('actions.mode'),'pee');
run('canvas.onpointerdown({pointerId:1,clientX:10,clientY:10})');clock=2100;
run('canvas.onpointermove({clientX:20,clientY:10})');assert.equal(run('travel'),'walk');
assert.equal(run('actions.mode'),'pee');assert(run('document.getElementById("pet-wrap").style.transform').includes('10px'));
run('canvas.onpointerup()');assert.equal(run('travel'),'');assert.equal(run('actions.mode'),'pee');
advance(6999);assert.equal(run('state.data.urine'),0);
advance(7000);assert.equal(run('state.data.urine'),1);assert.equal(run('actions.food'),'猫条');
assert.equal(run('actions.elapsed(performance.now())'),2000);
advance(10000);assert.equal(run('actions.mode'),'happy');assert.equal(run('speech.textContent'),'主人你真好');
advance(15000);assert.equal(run('busy'),false);

run('state.data.poop=2;state.save();clean()');
advance(17000);run('state.data.nextPoop=now();state.save();tick()');assert.equal(run('actions.mode'),'poop');
advance(22000);assert.equal(run('actions.mode'),'clean');assert.equal(run('state.data.poop'),3);
advance(26999);assert.equal(run('state.data.poop'),3);
advance(27000);assert.equal(run('state.data.poop'),0);assert.equal(run('state.data.urine'),0);
assert.equal(run('speech.textContent'),'铲完啦！一共有 3 个便便、1 个尿团');
assert.equal(run('panel.hidden'),false);assert.equal(run('busy'),false);
console.log('PASS: real preview controller interrupts care with menus open, allows busy dragging, resumes food reaction and reports/clears updated waste correctly');

run("companionMenu();panel.children.find(b=>b.textContent==='摸摸球球').onclick()");
assert.equal(run('actions.mode'),'happy');assert.equal(run('companion.data.affection'),2);assert(run('heartsUntil')>clock);
run("companionMenu();panel.children.find(b=>b.textContent==='摸摸球球').onclick()");assert.equal(run('companion.data.affection'),2);
advance(32000);run("companionMenu();panel.children.find(b=>b.textContent==='今日悄悄话').onclick()");
assert.equal(run('companion.data.affection'),5);assert.equal(run('state.data.poop'),0);
run("panel.children.find(b=>b.textContent==='今日悄悄话').onclick()");assert.equal(run('companion.data.affection'),5);
console.log('PASS: real preview petting animation/hearts, busy guard, daily note and reward deduplication');

run("menu();panel.children.find(b=>b.textContent==='对话').onclick();panel.children[0].value='你最爱谁呀';panel.children.find(b=>b.textContent==='发送').onclick()");
assert.equal(run('speech.textContent'),'最爱爸爸妈妈');const chatAt=clock;advance(chatAt+9999);assert.equal(run('speech.textContent'),'最爱爸爸妈妈');advance(chatAt+10000);assert.equal(run('speech.textContent'),'');
console.log('PASS: chat bubble remains for ten seconds and then disappears');

run("say('普通气泡')");const normalAt=clock;advance(normalAt+4999);assert.equal(run('speech.textContent'),'普通气泡');advance(normalAt+5000);assert.equal(run('speech.textContent'),'');
run('restartIdle();menu()');const menuAt=clock;advance(menuAt+1000);assert.equal(run('idle.elapsed'),1000);
console.log('PASS: ordinary bubbles disappear after five seconds and menu keeps idle motion running');
