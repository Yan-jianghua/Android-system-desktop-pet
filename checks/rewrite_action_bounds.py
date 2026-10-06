from pathlib import Path
p=Path('app/src/main/assets/pet-sprites/index.html');s=p.read_text(encoding='utf-8')
s=s.replace("function actionSprite(row,col){c.drawImage(actions,col*256,row*256,256,256,-8,12,208,208)}", "function actionSprite(row,col){const rows=[0,238,464,714,957,1233,1536],ground=[224,457,699,948,1227,1516],top=rows[row],h=rows[row+1]-top,scale=Math.min(208/256,208/h),w=256*scale,y=198-(ground[row]-top)*scale;c.drawImage(actions,col*256,top,256,h,96-w/2,y,w,h*scale)}")
p.write_text(s,encoding='utf-8')
