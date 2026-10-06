'use strict';
/** Two registered meshes form each in-between pose. Source art stays intact. */
class MotionMesh {
 constructor(){
  this.canvas=document.createElement('canvas');this.canvas.width=this.canvas.height=256;
  const g=this.gl=this.canvas.getContext('webgl',{alpha:true,premultipliedAlpha:true,preserveDrawingBuffer:true,antialias:false});
  if(!g)throw Error('浏览器需要启用硬件加速以播放动作补间');
  const shader=(kind,text)=>{const s=g.createShader(kind);g.shaderSource(s,text);g.compileShader(s);if(!g.getShaderParameter(s,g.COMPILE_STATUS))throw Error(g.getShaderInfoLog(s));return s};
  const p=g.createProgram();g.attachShader(p,shader(g.VERTEX_SHADER,'attribute vec2 position;attribute vec2 uv;varying vec2 v;void main(){v=uv;gl_Position=vec4(position.x/128.0-1.0,1.0-position.y/128.0,0.0,1.0);}'));
  g.attachShader(p,shader(g.FRAGMENT_SHADER,'precision mediump float;uniform sampler2D image;uniform float weight;varying vec2 v;void main(){gl_FragColor=texture2D(image,v)*weight;}'));g.linkProgram(p);if(!g.getProgramParameter(p,g.LINK_STATUS))throw Error(g.getProgramInfoLog(p));g.useProgram(p);
  this.weight=g.getUniformLocation(p,'weight');this.vertices=new Float32Array(289*4);
  this.buffer=g.createBuffer();g.bindBuffer(g.ARRAY_BUFFER,this.buffer);g.bufferData(g.ARRAY_BUFFER,this.vertices,g.DYNAMIC_DRAW);
  for(const [name,offset] of [['position',0],['uv',8]]){const a=g.getAttribLocation(p,name);g.enableVertexAttribArray(a);g.vertexAttribPointer(a,2,g.FLOAT,false,16,offset)}
  const indices=[];for(let y=0;y<16;y++)for(let x=0;x<16;x++){const a=y*17+x;indices.push(a,a+1,a+17,a+1,a+18,a+17)}
  g.bindBuffer(g.ELEMENT_ARRAY_BUFFER,g.createBuffer());g.bufferData(g.ELEMENT_ARRAY_BUFFER,new Uint16Array(indices),g.STATIC_DRAW);
  this.textures=[g.createTexture(),g.createTexture()];this.images=[];g.pixelStorei(g.UNPACK_PREMULTIPLY_ALPHA_WEBGL,true);
  for(const t of this.textures){g.bindTexture(g.TEXTURE_2D,t);g.texParameteri(g.TEXTURE_2D,g.TEXTURE_MIN_FILTER,g.LINEAR);g.texParameteri(g.TEXTURE_2D,g.TEXTURE_MAG_FILTER,g.LINEAR);g.texParameteri(g.TEXTURE_2D,g.TEXTURE_WRAP_S,g.CLAMP_TO_EDGE);g.texParameteri(g.TEXTURE_2D,g.TEXTURE_WRAP_T,g.CLAMP_TO_EDGE)}
  g.viewport(0,0,256,256);g.enable(g.BLEND);g.blendFunc(g.ONE,g.ONE);g.clearColor(0,0,0,0);
 }
 layer(bitmap,flow,amount,weight,slot){const g=this.gl;g.bindTexture(g.TEXTURE_2D,this.textures[slot]);if(this.images[slot]!==bitmap){g.texImage2D(g.TEXTURE_2D,0,g.RGBA,g.RGBA,g.UNSIGNED_BYTE,bitmap);this.images[slot]=bitmap}
  const v=this.vertices,amplitude=.30;for(let y=0;y<=16;y++)for(let x=0;x<=16;x++){const n=y*17+x,p=n*4;v[p]=x*16+flow[n*2]*amount*amplitude;v[p+1]=y*16+flow[n*2+1]*amount*amplitude;v[p+2]=x/16;v[p+3]=y/16}
  g.bindBuffer(g.ARRAY_BUFFER,this.buffer);g.bufferSubData(g.ARRAY_BUFFER,0,v);g.uniform1f(this.weight,weight);g.drawElements(g.TRIANGLES,1536,g.UNSIGNED_SHORT,0);
 }
 render(sequence,phase){const first=Math.floor(phase),raw=phase-first,blend=raw*raw*(3-2*raw);this.gl.clear(this.gl.COLOR_BUFFER_BIT);this.layer(sequence.keys[first],sequence.flow.vectors[first][0],blend,1-blend,0);if(blend)this.layer(sequence.keys[(first+1)%24],sequence.flow.vectors[first][1],1-blend,blend,1);return this.canvas}
}
