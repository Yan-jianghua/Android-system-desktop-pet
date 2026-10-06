from pathlib import Path
p=Path('app/src/main/assets/pet-sprites/index.html');s=p.read_text(encoding='utf-8')
needle='function tail(t)'
fun="""function lyingSprite(t){c.save();c.translate(-8,12);c.scale(208/256,208/256);c.save();c.translate(123,111);c.rotate(Math.sin(t*1.57)*8*Math.PI/180);c.translate(-123,-111);c.save();c.beginPath();c.rect(86,62,88,55);c.clip();c.drawImage(actions,256,0,256,256,0,0,256,256);c.restore();c.save();c.translate(127,63);c.rotate(Math.sin(t*1.57-.28)*5*Math.PI/180);c.translate(-127,-63);c.beginPath();c.rect(86,0,88,66);c.clip();c.drawImage(actions,256,0,256,256,0,0,256,256);c.restore();c.restore();c.save();c.beginPath();c.rect(0,99,256,157);c.clip();c.drawImage(actions,256,0,256,256,0,0,256,256);c.restore();c.restore()}"""
s=s.replace(needle,fun+needle).replace('if(custom)actionSprite(row,col);else sprite(row,col);',"if(mode==='lie')lyingSprite(t);else if(custom)actionSprite(row,col);else sprite(row,col);")
p.write_text(s,encoding='utf-8')
