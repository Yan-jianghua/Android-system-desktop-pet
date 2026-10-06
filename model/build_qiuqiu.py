"""Editable photo-guided model, explicit armature and mobile glTF export.
Run: blender --background --python model/build_qiuqiu.py
"""
import bpy, math, random, os, json
from mathutils import Vector, Matrix
random.seed(18)
ROOT=os.path.dirname(os.path.abspath(__file__))
bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)

def material(name,color,rough=.6,metal=0):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    p=m.node_tree.nodes.get('Principled BSDF');p.inputs['Base Color'].default_value=(*color,1)
    p.inputs['Roughness'].default_value=rough;p.inputs['Metallic'].default_value=metal
    return m
coat=material('Ivory silver undercoat',(.78,.77,.72),.88)
fur=material('White long guard hairs',(.91,.90,.85),.65)
pink=material('Warm grey pink inner ears',(.45,.32,.30),.8)
rim=material('Dark eye rims',(.042,.045,.039),.42)
iris=material('Olive green iris',(.11,.17,.12),.18)
pupil=material('Glossy black pupils',(.004,.007,.005),.08)
nosemat=material('Dark rose nose',(.13,.075,.074),.4)
whisker=material('Ivory whiskers',(.85,.85,.8),.4)
tongue=material('Tongue',(.57,.22,.27),.5)
arm_data=bpy.data.armatures.new('QiuqiuSkeleton');arm=bpy.data.objects.new('QiuqiuRig',arm_data)
bpy.context.collection.objects.link(arm);bpy.context.view_layer.objects.active=arm;arm.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
spec=[('root',(0,0,.2),(0,0,.4),None),('hips',(0,.22,.5),(0,.13,.7),'root'),
 ('spine',(0,.13,.7),(0,-.1,.82),'hips'),('neck',(0,-.1,.82),(0,-.23,1.05),'spine'),
 ('head',(0,-.23,1.05),(0,-.24,1.32),'neck'),('jaw',(0,-.40,1.06),(0,-.44,.99),'head')]
for s,x in [('L',-.16),('R',.16)]:
    spec += [(f'front_upper_{s}',(x,-.14,.75),(x,-.23,.43),'spine'),(f'front_lower_{s}',(x,-.23,.43),(x,-.29,.16),f'front_upper_{s}'),(f'front_paw_{s}',(x,-.29,.16),(x,-.43,.12),f'front_lower_{s}'),(f'hind_upper_{s}',(x,.22,.55),(x*1.25,.29,.28),'hips'),(f'hind_lower_{s}',(x*1.25,.29,.28),(x*1.25,.07,.12),f'hind_upper_{s}'),(f'ear_{s}',(x,-.20,1.38),(x*1.4,-.19,1.54),'head'),(f'lid_{s}',(x,-.43,1.22),(x,-.44,1.25),'head')]
tailpoints=[(0,.40,.42),(.15,.54,.35),(.36,.55,.23),(.55,.40,.18),(.60,.15,.21)]
for i in range(4):spec.append((f'tail_{i}',tailpoints[i],tailpoints[i+1],'hips' if i==0 else f'tail_{i-1}'))
for name,a,b,parent in spec:
    bone=arm_data.edit_bones.new(name);bone.head=a;bone.tail=b
    if parent:bone.parent=arm_data.edit_bones[parent]
bpy.ops.object.mode_set(mode='OBJECT');arm.select_set(False)
parts=[]
def bind(obj,bone):
    g=obj.vertex_groups.new(name=bone);g.add(list(range(len(obj.data.vertices))),1,'REPLACE')
    mod=obj.modifiers.new('Skeleton deformation','ARMATURE');mod.object=arm
    obj.parent=arm;parts.append(obj);obj.select_set(False)
def ellipsoid(name,center,scale,bone,mat=coat,segments=40,rings=24):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=rings,location=center)
    o=bpy.context.object;o.name=name;o.scale=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    o.data.materials.append(mat)
    for p in o.data.polygons:p.use_smooth=True
    bind(o,bone);return o

# Anatomical masses use measured visual proportions rather than texture projection.
regions=[('Rump',(0,.20,.51),(.29,.32,.34),'hips',4000,.09),
 ('Chest',(0,-.02,.70),(.28,.29,.36),'spine',4000,.10),
 ('Long chest ruff',(0,-.18,.88),(.29,.25,.29),'neck',5000,.11),
 ('Round skull',(0,-.24,1.20),(.29,.24,.235),'head',4000,.045),
 ('Left cheek',(-.19,-.28,1.105),(.15,.18,.15),'head',1800,.06),
 ('Right cheek',(.19,-.28,1.105),(.15,.18,.15),'head',1800,.06)]
for s,x in [('L',-.16),('R',.16)]:
    regions += [(f'Foreleg {s}',(x,-.23,.46),(.074,.08,.28),f'front_upper_{s}',750,.065),(f'Lower foreleg {s}',(x,-.29,.24),(.072,.08,.14),f'front_lower_{s}',450,.055),(f'Paw {s}',(x,-.36,.115),(.088,.13,.075),f'front_paw_{s}',400,.035),(f'Haunch {s}',(x*1.23,.22,.32),(.13,.20,.22),f'hind_upper_{s}',1000,.10),(f'Hind foot {s}',(x*1.25,.045,.12),(.095,.14,.08),f'hind_lower_{s}',350,.035)]
for i in range(4):
    a=Vector(tailpoints[i]);b=Vector(tailpoints[i+1]);regions.append((f'Tail volume {i}',tuple((a+b)/2),(.105,.135,.105),f'tail_{i}',1500,.105))

def hair_region(name,center,scale,bone,count,length):
    # Tapered triangular strands are actual geometry and survive glTF export.
    verts=[];faces=[];center=Vector(center);rx,ry,rz=scale
    for j in range(count):
        z=random.uniform(-1,1);phi=random.uniform(0,2*math.pi);r=math.sqrt(1-z*z)
        n=Vector((r*math.cos(phi),r*math.sin(phi),z));p=center+Vector((n.x*rx,n.y*ry,n.z*rz))
        normal=Vector((n.x/rx,n.y/ry,n.z/rz)).normalized()
        # Short sparse facial hair leaves the eye surfaces and muzzle readable.
        if bone=='head' and normal.y<-.7 and 1.08<p.z<1.33 and abs(p.x)<.23:continue
        l=length*random.uniform(.65,1.15);direction=(normal*.70+Vector((0,.05,-.32))).normalized()
        tangent=normal.cross(Vector((0,0,1)))
        if tangent.length<.01:tangent=Vector((1,0,0))
        tangent.normalize();second=direction.cross(tangent).normalized()
        width=random.uniform(.00035,.00085);idx=len(verts)
        for k in range(3):
            f=k/3;point=p+direction*(l*f)+Vector((0,0,-l*f*f*.24))+tangent*(math.sin(f*3+j)*l*.07)
            for a in range(3):
                angle=a*2*math.pi/3;verts.append(tuple(point+(tangent*math.cos(angle)+second*math.sin(angle))*width*(1-f*.8)))
        verts.append(tuple(p+direction*l));
        for k in range(2):
            for a in range(3):
                v=idx+k*3+a;w=idx+k*3+(a+1)%3;faces.append((v,w,w+3,v+3))
        for a in range(3):faces.append((idx+6+a,idx+6+(a+1)%3,idx+9))
    mesh=bpy.data.meshes.new(name+' strands');mesh.from_pydata(verts,[],faces);mesh.update()
    obj=bpy.data.objects.new(name+' groom',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(fur)
    for p in mesh.polygons:p.use_smooth=True
    bind(obj,bone)
undercoat=[]
for name,center,scale,bone,count,length in regions:
    undercoat.append(ellipsoid(name,center,scale,bone));hair_region(name,center,scale,bone,int(count*1.65),length)

# Fuse the overlapping anatomical masses into a continuous skin surface.
bpy.ops.object.select_all(action='DESELECT')
for obj in undercoat:
    obj.select_set(True)
    for mod in list(obj.modifiers):obj.modifiers.remove(mod)
    parts.remove(obj)
bpy.context.view_layer.objects.active=undercoat[0];bpy.ops.object.join();skin=bpy.context.object;skin.name='Continuous feline skin'
bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
remesh=skin.modifiers.new('Sculpt union','REMESH');remesh.mode='VOXEL';remesh.voxel_size=.013;remesh.use_smooth_shade=True;bpy.ops.object.modifier_apply(modifier=remesh.name)
smooth=skin.modifiers.new('Sculpt relax','SMOOTH');smooth.factor=.7;smooth.iterations=4;bpy.ops.object.modifier_apply(modifier=smooth.name)
for group in list(skin.vertex_groups):skin.vertex_groups.remove(group)
groups={bone:skin.vertex_groups.new(name=bone) for bone in dict.fromkeys(region[3] for region in regions)}
for v in skin.data.vertices:
    if v.co.z>.99:
        groups['head'].add([v.index],1,'REPLACE');continue
    candidates=[]
    for _,center,scale,bone,_,_ in regions:
        d=sum(((v.co[k]-center[k])/scale[k])**2 for k in range(3))
        candidates.append((d,bone))
    nearest=sorted(candidates)[:2];weights=[math.exp(-d*3) for d,_ in nearest];total=sum(weights)
    for (_,bone),weight in zip(nearest,weights):groups[bone].add([v.index],weight/total,'ADD')
mod=skin.modifiers.new('Smooth skeleton skin','ARMATURE');mod.object=arm;parts.append(skin);skin.select_set(False)

for s,x in [('L',-.125),('R',.125)]:
    ellipsoid('Eye socket '+s,(x,-.427,1.245),(.073,.029,.078),'head',rim)
    ellipsoid('Iris '+s,(x,-.450,1.246),(.064,.021,.068),'head',iris)
    ellipsoid('Pupil '+s,(x,-.467,1.25),(.050,.009,.058),'head',pupil)
    # Convex cornea produces specular reflections instead of painted highlights.
    ellipsoid('Corneal shine '+s,(x,-.475,1.252),(.045,.007,.051),'head',pupil)
    ellipsoid('Upper eyelid '+s,(x,-.422,1.319),(.071,.024,.008),f'lid_{s}',coat)
    ex=-.195 if s=='L' else .195
    vertices=[(ex-.07,-.22,1.365),(ex+.07,-.22,1.365),(ex*1.23,-.18,1.54),(ex,-.08,1.39),(ex*1.23,-.14,1.52)]
    mesh=bpy.data.meshes.new('Ear');mesh.from_pydata(vertices,[],[(0,1,2),(0,3,4,2),(1,2,4,3),(0,1,3)]);mesh.update()
    obj=bpy.data.objects.new('Ear '+s,mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(coat)
    bevel=obj.modifiers.new('Soft ear edges','BEVEL');bevel.width=.018;bevel.segments=3
    sub=obj.modifiers.new('Ear surface','SUBSURF');sub.levels=2
    bpy.context.view_layer.objects.active=obj;obj.select_set(True)
    bpy.ops.object.modifier_apply(modifier=bevel.name);bpy.ops.object.modifier_apply(modifier=sub.name);obj.select_set(False)
    bind(obj,f'ear_{s}')
    ellipsoid('Inner ear '+s,(ex,-.228,1.435),(.039,.008,.071),f'ear_{s}',pink)
ellipsoid('Muzzle left',(-.057,-.442,1.106),(.065,.041,.049),'head')
ellipsoid('Muzzle right',(.057,-.442,1.106),(.065,.041,.049),'head')
ellipsoid('Lower jaw',(0,-.409,1.049),(.115,.063,.043),'jaw')
ellipsoid('Tongue',(0,-.454,1.045),(.025,.04,.008),'jaw',tongue)
mesh=bpy.data.meshes.new('Nose');mesh.from_pydata([(-.037,-.493,1.14),(.037,-.493,1.14),(0,-.505,1.101),(0,-.462,1.131)],[],[(0,1,2),(0,3,1),(0,2,3),(1,3,2)]);mesh.update()
obj=bpy.data.objects.new('Triangular nose',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(nosemat);bind(obj,'head')
def strand(name,points,radius,mat,bone):
    bpy.ops.object.select_all(action='DESELECT')
    curve=bpy.data.curves.new(name,'CURVE');curve.dimensions='3D';curve.bevel_depth=radius;curve.bevel_resolution=1
    spline=curve.splines.new('BEZIER');spline.bezier_points.add(len(points)-1)
    for p,co in zip(spline.bezier_points,points):p.co=co;p.handle_left_type=p.handle_right_type='AUTO'
    obj=bpy.data.objects.new(name,curve);bpy.context.collection.objects.link(obj);curve.materials.append(mat)
    bpy.context.view_layer.objects.active=obj;obj.select_set(True);bpy.ops.object.convert(target='MESH');obj=bpy.context.object;obj.select_set(False);bind(obj,bone)
strand('Philtrum',[(0,-.493,1.105),(0,-.49,1.075)],.0025,nosemat,'head')
for side in [-1,1]:
    strand('Mouth',[(0,-.49,1.075),(side*.03,-.483,1.065),(side*.053,-.46,1.074)],.0018,nosemat,'jaw')
    for i in range(7):strand('Whisker',[(side*.05,-.47,1.115-i*.006),(side*.19,-.46,1.125-i*.014),(side*(.33+i*.005),-.38,1.15-i*.025)],.0009,whisker,'head')

def lying_pose(phase):
    """Photo-guided low sphinx pose, with a lifted relaxed tail chain."""
    targets={
      'root':((0,0,.20),(0,0,.40)),
      'hips':((0,.25,.25),(0,.12,.34)),
      'spine':((0,.12,.34),(0,-.15,.38)),
      'neck':((0,-.15,.38),(0,-.34,.43)),
      'head':((0,-.34,.43),(0,-.34,.70))}
    for side,x in [('L',-.16),('R',.16)]:
        targets[f'front_upper_{side}']=((x,-.18,.33),(x,-.33,.15))
        targets[f'front_lower_{side}']=((x,-.33,.15),(x,-.53,.105))
        targets[f'front_paw_{side}']=((x,-.53,.105),(x,-.67,.105))
        targets[f'hind_upper_{side}']=((x,.28,.27),(x*1.2,.045,.14))
        targets[f'hind_lower_{side}']=((x*1.2,.045,.14),(x*1.2,.265,.11))
    tail=[]
    for i,(y,z) in enumerate([(.45,.25),(.52,.45),(.55,.67),(.53,.87),(.46,1.04)]):
        # Root hardly moves; the tip follows with a small temporal lag.
        tail.append((.08+math.sin(phase-i*.20)*[.0,.05,.11,.17,.22][i],y,z))
    for i in range(4):targets[f'tail_{i}']=(tail[i],tail[i+1])
    # Parents first. Children not explicitly posed retain their local rest relation.
    for bone in arm.pose.bones:
        if bone.name in targets:
            a,b=map(Vector,targets[bone.name]);rotation=(b-a).to_track_quat('Y','Z')
            stretch=(b-a).length/bone.bone.length
            bone.matrix=Matrix.Translation(a)@rotation.to_matrix().to_4x4()@Matrix.Diagonal((1,stretch,1,1))
            bpy.context.view_layer.update()
    # Half-closed eyelids like the relaxed photograph, not angry brows.
    for side in ['L','R']:arm.pose.bones[f'lid_{side}'].location.y=-.025

# Every clip contains explicit bone rotation/location keys (not image frames).
clips={'sit':96,'lie':96,'groom':96,'walk':32,'run':24,'eat':60,'happy':48,'disgust':48,'pee':72,'poop':72,'clean':96}
for name,duration in clips.items():
    arm.animation_data_create();action=bpy.data.actions.new(name);arm.animation_data.action=action
    for frame in range(1,duration+1,4):
        a=(frame-1)/(duration-1)*math.pi*2
        for bone in arm.pose.bones:bone.rotation_mode='XYZ';bone.rotation_euler=(0,0,0);bone.location=(0,0,0);bone.scale=(1,1,1)
        arm.pose.bones['head'].rotation_euler[1]=math.sin(a)*(.16 if name=='sit' else .035)
        for i in range(4):arm.pose.bones[f'tail_{i}'].rotation_euler[2]=math.sin(a-i*.4)*(.23 if name=='lie' else .09)
        if name in ['walk','run']:
            amp=.46 if name=='walk' else .70
            for s,phase in [('L',0),('R',math.pi)]:
                arm.pose.bones[f'front_upper_{s}'].rotation_euler.x=math.sin(a+phase)*amp
                arm.pose.bones[f'front_lower_{s}'].rotation_euler.x=max(0,-math.sin(a+phase))*.35
                arm.pose.bones[f'hind_upper_{s}'].rotation_euler.x=-math.sin(a+phase)*amp
            arm.pose.bones['root'].location.y=abs(math.sin(a))*(.035 if name=='walk' else .08)
        if name=='groom':
            arm.pose.bones['head'].rotation_euler.x=.28;arm.pose.bones['head'].rotation_euler[1]=-.22
            arm.pose.bones['front_upper_L'].rotation_euler.x=-1.05;arm.pose.bones['front_lower_L'].rotation_euler.x=-.48
            arm.pose.bones['jaw'].rotation_euler.x=.15+math.sin(a*3)*.13
        if name=='eat':arm.pose.bones['neck'].rotation_euler.x=.38;arm.pose.bones['head'].rotation_euler.x=.16+math.sin(a*3)*.05;arm.pose.bones['jaw'].rotation_euler.x=.12+math.sin(a*4)*.10
        if name=='happy':arm.pose.bones['head'].rotation_euler.z=math.sin(a)*.13;arm.pose.bones['root'].location.y=max(0,math.sin(a))*.055
        if name=='disgust':arm.pose.bones['head'].rotation_euler[1]=math.sin(a*2)*.32;arm.pose.bones['neck'].rotation_euler.x=-.12
        if name in ['pee','poop']:arm.pose.bones['hips'].rotation_euler.x=-.25;arm.pose.bones['root'].location.y=-.12;arm.pose.bones['tail_0'].rotation_euler.x=.65
        if name=='clean':arm.pose.bones['head'].rotation_euler.x=.20;arm.pose.bones['head'].rotation_euler[1]=math.sin(a)*.12
        if name=='lie':lying_pose(a)
        for bone in arm.pose.bones:bone.keyframe_insert(data_path='rotation_euler',frame=frame);bone.keyframe_insert(data_path='location',frame=frame);bone.keyframe_insert(data_path='scale',frame=frame)
    # Exact first-pose endpoint removes jumps when looping.
    for fc in action.fcurves:
        fc.keyframe_points.insert(duration,fc.evaluate(1))
    track=arm.animation_data.nla_tracks.new();track.name=name;track.strips.new(name,1,action);track.mute=True
arm.animation_data.action=None
for bone in arm.pose.bones:bone.rotation_euler=(0,0,0);bone.location=(0,0,0);bone.scale=(1,1,1)
scene=bpy.context.scene;scene.frame_start=1;scene.frame_end=96;scene.render.fps=24
scene.world.color=(.25,.25,.25)
def area(name,loc,power,size):
    data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size
    obj=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(obj);obj.location=loc;obj.rotation_euler=(Vector((0,0,.9))-obj.location).to_track_quat('-Z','Y').to_euler()
area('Large soft key',(-3,-4,5),450,4);area('Eye catchlight',(2,-4,2.8),180,2);area('Fur rim',(1,3,4),500,3)
data=bpy.data.cameras.new('Portrait');camera=bpy.data.objects.new('Portrait',data);bpy.context.collection.objects.link(camera)
camera.location=(2.2,-4.3,1.9);camera.rotation_euler=(Vector((.05,0,.83))-camera.location).to_track_quat('-Z','Y').to_euler();data.type='ORTHO';data.ortho_scale=2.05;scene.camera=camera
scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=True;scene.render.resolution_x=800;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.film_transparent=True
scene.view_settings.view_transform='AgX'
# Preserve all four photo references in the editable source, outside the export.
refs=bpy.data.collections.new('User photo references');scene.collection.children.link(refs)
for i,filename in enumerate(['reference-front.jpg','reference-body.jpg','reference-rest.jpg','reference-lying.jpg']):
    path=os.path.join(ROOT,filename)
    if os.path.exists(path):
        img=bpy.data.images.load(path);img.pack()
        ref=bpy.data.objects.new(filename,None);ref.empty_display_type='IMAGE';ref.data=img;ref.empty_display_size=2
        ref.location=(3+i*2.5,0,1);ref.hide_render=True;refs.objects.link(ref)
for obj in parts:
    assert any(mod.type=='ARMATURE' for mod in obj.modifiers), 'Missing skin modifier: '+obj.name
    assert all(group.name in arm.data.bones for group in obj.vertex_groups), 'Unmatched bone group: '+obj.name
bpy.ops.wm.save_as_mainfile(filepath=os.path.join(ROOT,'qiuqiu.blend'))
bpy.ops.object.select_all(action='DESELECT');arm.select_set(True)
for obj in parts:obj.select_set(True)
bpy.ops.export_scene.gltf(filepath=os.path.join(ROOT,'qiuqiu.glb'),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_skins=True,export_yup=True)
with open(os.path.join(ROOT,'model-info.json'),'w',encoding='utf8') as f:json.dump({'bones':len(arm.data.bones),'clips':list(clips),'meshes':len(parts),'vertices':sum(len(o.data.vertices) for o in parts),'reference':'Four user supplied photographs; procedural photo-guided reconstruction, not photogrammetry'},f,ensure_ascii=False,indent=2)
scene.render.filepath=os.path.join(ROOT,'qiuqiu-preview.png');bpy.ops.render.render(write_still=True)
arm.animation_data.action=bpy.data.actions['lie'];scene.frame_set(24)
camera.location=(2.2,-4.3,1.6);camera.rotation_euler=(Vector((0,-.05,.55))-camera.location).to_track_quat('-Z','Y').to_euler()
scene.render.filepath=os.path.join(ROOT,'qiuqiu-lying-preview.png');bpy.ops.render.render(write_still=True)



