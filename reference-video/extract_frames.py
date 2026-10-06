import bpy,os,json
root=os.path.abspath('reference-video')
path="C:/Users/y'j'h/Desktop/屏幕录制 2026-10-05 184240.mp4"
s=bpy.context.scene;ed=s.sequence_editor_create();strip=ed.strips.new_movie('Target pet reference',path,channel=1,frame_start=1)
el=strip.elements[0];s.render.resolution_x=el.orig_width;s.render.resolution_y=el.orig_height;s.render.resolution_percentage=100;s.render.use_sequencer=True;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard'
print('VIDEO_INFO',el.orig_width,el.orig_height,strip.frame_duration,strip.fps)
for i,f in enumerate([1,max(1,int(strip.frame_duration*.25)),max(1,int(strip.frame_duration*.5)),max(1,int(strip.frame_duration*.75))]):
 s.frame_set(f);s.render.filepath=os.path.join(root,f'frame-{i}.png');bpy.ops.render.render(write_still=True)
