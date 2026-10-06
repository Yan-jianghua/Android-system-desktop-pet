from PIL import Image
im=Image.open('app/src/main/assets/pet-sprites/actions.png')
for col in range(4):
 a=im.getchannel('A').crop((col*256,0,(col+1)*256,1536));counts=[sum(v>128 for v in a.crop((0,y,256,y+1)).getdata()) for y in range(1536)]
 active=[n>=12 for n in counts];runs=[];start=None
 for y,yes in enumerate(active+[False]):
  if yes and start is None:start=y
  if not yes and start is not None:
   if y-start>12:runs.append((start,y-1))
   start=None
 print(col,runs)
