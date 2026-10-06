# 球球真实三维模型 · 初稿

本目录保留四张参考照片、可编辑建模脚本、Blender 源文件和 glTF 模型。模型根据照片中的比例与特征由程序辅助建立，不是照片扫描重建；背面由可见轮廓推测。

- `qiuqiu.blend`：网格、材质、24 根骨骼、权重、11 个动作以及渲染设置。
- `qiuqiu.glb`：安卓和浏览器实际加载的三维网格、骨骼蒙皮及动画。
- `qiuqiu-preview.png`：由模型渲染的静态图，不是图像生成工具生成的概念图。
- `build_qiuqiu.py`：可重建模型的 Blender Python 脚本。
- `model-info.json`：当前网格和动画统计。

动作：sit、lie、groom、walk、run、eat、happy、disgust、pee、poop、clean。尾巴为四段骨骼，前后腿有独立关节，头部、下颌、耳朵有独立骨骼。动画还需要进一步调整自然程度。

安卓通过打包在应用内的 Three.js 与 GLTFLoader，使用透明 WebView 离线实时渲染。Java 传入动作名，AnimationMixer 驱动 glTF 动画；喂食、猫砂盆和清理道具也是实时三维几何。原有本地记忆机制保留。

质量状态：这是可运行的真实三维初稿，尚未达到照片级写实或高品质宠物成品。脸部拓扑、眼睛和耳朵细节、毛发分层/走向、动作接触和手机性能仍需继续打磨；不能将当前渲染质量说成最终写实效果。

运行预览：从项目根目录执行 `python -m http.server 8765 --bind 127.0.0.1 --directory app/src/main/assets/pet3d`，打开 `http://127.0.0.1:8765/?preview`。该页面展示应用使用的同一个 GLB 和渲染代码。

开源渲染库：Three.js 0.160.0（MIT），随源码保留其许可证注释。手机需支持 WebGL 2 的较新 Android System WebView。APK 未做真机性能测试。

趴卧动作已按最新照片改为低身、前爪向前；尾巴竖起，以约四秒一个周期缓慢左右甩，尾尖有延迟跟随。蒙皮权重归一化检查通过。

