from PIL import Image
import json,sys
im=Image.open(sys.argv[1]); cols=int(sys.argv[2]); a=im.getchannel('A'); w,h=im.size
bands=[]
for col in range(cols):
 counts=[sum(v>128 for v in a.crop((col*w//cols,y,(col+1)*w//cols,y+1)).get_flattened_data()) for y in range(h)]
 active=[n>=12 for n in counts];runs=[];start=None
 for y,yes in enumerate(active+[False]):
  if yes and start is None:start=y
  if not yes and start is not None:
   if y-start>25:runs.append([start,y-1])
   start=None
 bands.append(runs)
print(json.dumps({'size':im.size,'bands':bands}))
