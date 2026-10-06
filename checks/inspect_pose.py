import bpy
from mathutils import Vector
arm=bpy.data.objects['QiuqiuRig'];arm.animation_data.action=bpy.data.actions['lie'];bpy.context.scene.frame_set(24)
for name in ['head','jaw','front_upper_L','front_lower_L','front_paw_L']:
 b=arm.pose.bones[name];print('BONE',name,tuple(b.head),tuple(b.tail),tuple(b.scale))
for name in ['Tongue','Lower jaw','Paw L','Paw L groom']:
 o=bpy.data.objects[name];e=o.evaluated_get(bpy.context.evaluated_depsgraph_get());vs=[e.matrix_world@v.co for v in e.data.vertices];print('OBJECT',name,tuple(sum(vs,Vector())/len(vs)),[(g.name,g.index) for g in o.vertex_groups])
