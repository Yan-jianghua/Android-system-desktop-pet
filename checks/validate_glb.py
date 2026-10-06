import json,struct,pathlib
p=pathlib.Path('model/qiuqiu.glb');data=p.read_bytes();size=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+size]);off=20+size;binary=data[off+8:]
assert len(g['skins'])==1
joints=len(g['skins'][0]['joints']);expected={'sit','lie','groom','walk','run','eat','happy','disgust','pee','poop','clean'}
assert {a['name'] for a in g['animations']}==expected
count=0
for mesh in g['meshes']:
 for primitive in mesh['primitives']:
  attrs=primitive['attributes'];assert 'JOINTS_0' in attrs and 'WEIGHTS_0' in attrs
  a=g['accessors'][attrs['WEIGHTS_0']];view=g['bufferViews'][a['bufferView']];start=view.get('byteOffset',0)+a.get('byteOffset',0)
  assert a['componentType']==5126
  stride=view.get('byteStride',16)
  for i in range(a['count']):
   w=struct.unpack_from('<4f',binary,start+i*stride);assert abs(sum(w)-1)<.001
  count+=a['count']
assert joints==24
print(f'PASS: {joints} joints, {len(g["animations"])} clips, {count} vertices with normalized skin weights')
