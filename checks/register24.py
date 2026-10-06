"""Read-only alpha analysis: register source rectangles; never rewrite PNG pixels."""
import json, shutil
from pathlib import Path
import numpy as np
from PIL import Image

root=Path(__file__).resolve().parent.parent
assets=root/'app/src/main/assets/pet-sprites'
record=json.loads((root/'model/24帧素材记录.json').read_text(encoding='utf8'))
out=assets/'motions24'
out.mkdir(exist_ok=True)

def cuts(counts,n):
    length=len(counts); result=[0]
    for i in range(1,n):
        mid=length*i/n; radius=length/n*.19
        lo,hi=round(mid-radius),round(mid+radius)
        part=counts[lo:hi]; low=part.min()
        # Middle of a genuinely empty gap, nearest the expected divider.
        choices=np.where(part<=low+1)[0]+lo
        runs=np.split(choices,np.where(np.diff(choices)>1)[0]+1)
        best=min(runs,key=lambda r:abs(float(r.mean())-mid)-min(len(r),20)*.4)
        result.append(round(float(best.mean())))
    return result+[length]

durations={'sit':2400,'groom':1600,'lie':2400,'walk':1000,'run':800,'happy':2500,'disgust':2800,'kibble':1500,'treat':1500,'chocolate':1500,'pee':2400,'poop':2400,'clean':2400}
motions={}
for name,source in record['outputs'].items():
    if name=='lie' and (root/'model/staging24/lie-repaired.png').exists(): source=str(root/'model/staging24/lie-repaired.png')
    target=out/(name+'.png');shutil.copyfile(source,target)
    im=Image.open(target); alpha=np.array(im.getchannel('A'))
    ys=cuts((alpha>100).sum(axis=1),4);frames=[];zones=[]
    for j in range(4):
        top,bot=ys[j:j+2];xs=cuts((alpha[top:bot]>100).sum(axis=0),6)
        for i in range(6):
            left,right=xs[i:i+2]; tile=alpha[top:bot,left:right]
            yy,xx=np.where(tile>64)
            # Gaps found from alpha, not nominal cell dimensions.
            l=max(left,left+int(xx.min())-3);r=min(right,left+int(xx.max())+4)
            t=max(top,top+int(yy.min())-3);b=min(bot,top+int(yy.max())+4)
            solid=tile>120;counts=solid.sum(axis=1)
            floor=int(np.where(counts>max(7,(right-left)*.045))[0][-1])
            fy,fx=np.where(solid[max(0,floor-9):floor+1])
            ax=left+float(np.median(fx));ay=top+floor
            frames.append([l,t,r,b,round(ax,2),ay]);zones.append([left,top,right,bot])
    minL=min(f[0]-f[4] for f in frames);maxR=max(f[2]-f[4] for f in frames)
    minT=min(f[1]-f[5] for f in frames);maxB=max(f[3]-f[5] for f in frames)
    scale=min(216/(maxR-minL),216/(maxB-minT))
    motions[name]={'file':'motions24/'+name+'.png','width':im.width,'height':im.height,
      'scale':round(scale,6),'anchorX':round(128-(minL+maxR)*scale/2,6),'bottom':maxB,
      'duration':durations[name],'loop':name!='disgust','frames':frames,'zones':zones}
    print(name,'rows',ys,'bounds',round(maxR-minL),round(maxB-minT),'scale',round(scale,3))
(assets/'motions24.json').write_text(json.dumps({'motions':motions},ensure_ascii=False,indent=2),encoding='utf8')
