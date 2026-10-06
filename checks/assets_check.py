import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parent.parent/'app/src/main/assets/pet-sprites'
data=json.loads((root/'motions24.json').read_text(encoding='utf8'))['motions']
assert len(data)==13
total=0
for name,a in data.items():
    im=Image.open(root/a['file']); assert im.size==(1536,1024) and im.mode=='RGBA'
    assert len(a['frames'])==24 and len(a['zones'])==24
    for f,z in zip(a['frames'],a['zones']):
        l,t,r,b,ax,ay=f; zl,zt,zr,zb=z
        assert zl<=l<r<=zr and zt<=t<b<=zb and 0<=l<r<=im.width and 0<=t<b<=im.height
        assert zl<=ax<=zr and zt<=ay<=zb
    total+=len(a['frames'])
print(f'PASS: 13 actions, {total} measured frames, all bounds and floor anchors valid')
