"""Read source pixels, write motion vectors only. Raster source assets stay unchanged.
The app uses these vectors to deform textured meshes into 60 playback poses.
"""
import sys,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(ROOT/'.tools/python-motion'))
import cv2
from PIL import Image
assets=ROOT/'app/src/main/assets/pet-sprites'
source=json.loads((assets/'motions24.json').read_text(encoding='utf8'))['motions']
N=16; SIZE=256
grid=np.linspace(0,255,N+1,dtype=np.float32)
gx,gy=np.meshgrid(grid,grid)
result={}
for name,a in source.items():
    atlas=np.array(Image.open(assets/a['file']))
    normalized=[]
    for l,t,r,b,ax,ay in a['frames']:
        s=a['scale'];x=a['anchorX']+(l-ax)*s;y=241+(t-ay-a['bottom'])*s
        tile=atlas[t:b,l:r]
        # Read-only spatial registration for optical flow, no output image is saved.
        mat=np.float32([[s,0,x],[0,s,y]])
        rgba=cv2.warpAffine(tile,mat,(SIZE,SIZE),flags=cv2.INTER_LINEAR)
        alpha=rgba[:,:,3].astype(np.float32)/255
        # Dark neutral background gives the white fur silhouette usable gradients.
        grey=cv2.cvtColor(rgba[:,:,:3],cv2.COLOR_RGB2GRAY).astype(np.float32)
        normalized.append(np.uint8(grey*alpha+32*(1-alpha)))
    vectors=[]
    for i,im in enumerate(normalized):
        other=normalized[(i+1)%24];pair=[]
        for first,second in [(im,other),(other,im)]:
            flow=cv2.calcOpticalFlowFarneback(first,second,None,.5,4,29,5,7,1.5,0)
            flow=cv2.GaussianBlur(flow,(0,0),2)
            flow=np.clip(flow,-28,28)
            sampled=cv2.remap(flow,gx,gy,cv2.INTER_LINEAR)
            # Border vertices stay fixed, preserving transparent outer margins.
            sampled[0]=0;sampled[-1]=0;sampled[:,0]=0;sampled[:,-1]=0
            pair.append(np.round(sampled.reshape(-1),3).tolist())
        vectors.append(pair)
    top=min(241+(f[1]-f[5]-a['bottom'])*a['scale'] for f in a['frames'])
    result[name]={'grid':N,'size':SIZE,'frames':288,'duration':5000,'visualTop':round(top,3),'vectors':vectors}
    (assets/'motions24'/(name+'-flow.json')).write_text(json.dumps(result[name],separators=(',',':')),encoding='utf8')
    print(name,'24 key poses -> 60 motion-interpolated playback frames',flush=True)


