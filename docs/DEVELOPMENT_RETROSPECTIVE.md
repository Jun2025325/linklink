# linklin / PICO Spatial 开发复盘与避坑指南

日期：2026-09-25  
适用范围：PICO Spatial SDK 6.1.x、Full-space DefaultStage、SpatialUI AttachmentPanel、手部追踪交互、模拟器与真机联调。

本文沉淀 linklin 开发过程中反复调整的问题、根因、最终方案和后续规范，目的是避免后续项目重复踩坑。

---

## 1. 总览：本次最容易反复出错的六类问题

| 类别 | 典型现象 | 反复原因 | 最终原则 |
|---|---|---|---|
| Stage / 坐标 | 游戏在身后、系统背景漏出、画面不可见 | 模拟器和真机前后方向、Stage 生命周期、系统环境层理解不一致 | 区分模拟器与真机坐标，显式设置深度和朝向，用日志确认 Stage visible/focus |
| 面板交互 | 面板拖不动、按钮选不中、文字反向 | 把 2D UI 当 3D Entity 命中，或把 3D Entity 手势照搬到 AttachmentPanel | AttachmentPanel 的拖拽挂到 UI 节点，`targetedToEntity = null` |
| 手势语义 | 移动、旋转、选卡、双手配对互相冲突 | 同一个捏合手势承担太多含义 | 先定义手势语义，再写 recognizer；游戏主体和面板手势分开 |
| 手感模式 | 左右手分配错、拍掌尴尬、配对不触发 | GazePinch 不携带手别，双手距离和卡片距离混淆 | 用实际 pinch/aim 信号分左右手；判定卡片接触而非手掌接触 |
| 音效 | 完全无声、声音像狗叫 | 音频路由和具体 sample 混淆；系统辅助音路由被过滤 | 使用 `USAGE_GAME`，为 load/play 打日志，逐个替换音色 |
| 验证方式 | 截图黑屏/系统房间，误判代码坏 | Full Stage 截图和模拟器状态不稳定 | 截图只能参考，必须结合 Stage 日志、实体日志和用户肉眼确认 |

---

## 2. Stage 与空间坐标

### 2.1 问题一：游戏主体出现在玩家身后

#### 当时现象

真机上启动游戏后，玩家反馈“游戏没了”或“在我身后”。日志显示：

```text
MainStage foreground
MainStage focus=true
游戏进程存活
Spatial FPS 正常
```

但用户视野中没有棋盘，或需要转身才能看到。

#### 错误方向

一开始怀疑：

- 应用没有启动
- 渲染崩溃
- Full Stage 被系统隐藏
- APK 没安装成功

#### 根因

模拟器与真机的默认前后深度方向相反：

```text
模拟器：+Z 位于玩家前方
真机：  -Z 位于玩家前方
```

同一套 `+Z` 坐标在模拟器可见，在真机会跑到玩家身后。

#### 最终方案

按硬件区分深度：

```kotlin
private val STAGE_CONTENT_Z =
    if (IS_PICO_EMULATOR) 1.0f else -1.0f
```

工具面板、棋盘、初始菜单都遵循同一方向策略。

#### 沉淀规则

1. 不要假设模拟器和真机坐标完全一致。
2. 所有关键实体首次放置时必须打印 Stage 坐标。
3. 真机验证必须包含“正视前方是否可见”。
4. 如果日志显示进程和 FPS 正常但用户看不到，优先查坐标和 Stage visible 状态。

---

### 2.2 问题二：PICO 系统背景漏出

#### 当时现象

黑色宇宙背景只在某些视野尺寸下完整，放大或缩小后露出 PICO 系统背景。

#### 根因

最初使用有限的 Volumetric Window Container 和局部背景层，视口缩放或 dolly 变化时，系统背景没有被完整覆盖。

#### 最终方案

切换为 Full-space `DefaultStage`：

- 使用大尺寸内表面星空立方体。
- 使用 front-face culling 只渲染内表面。
- 保持 Stage 独占视野。
- 保留必要的 2D keep-alive anchor，避免 Android surface 无可见像素时 Stage 被系统回收。

#### 沉淀规则

1. 需要全视野沉浸背景时优先 Full Stage。
2. 不要只靠一个 2D 背景层解决所有视口缩放。
3. 保留极小 keep-alive anchor，但不要让它遮挡 3D 合成层。

---

### 2.3 问题三：Stage 状态异常和系统 UI 覆盖

#### 当时现象

日志偶发：

```text
MainStage visible=false
focus=true
callerPkg=com.picoxr.systemui
```

用户看到系统环境或“游戏没了”。

#### 根因

系统 UI、权限弹窗、重新聚焦、模拟器异常状态都可能让 Stage 不可见。进程活着不代表画面可见。

#### 处理经验

1. 先确认进程。
2. 再看 `LifeCycle` 中 MainStage 的 `visible/focus/life`。
3. 查 caller 是 app 自己还是 `com.picoxr.systemui`。
4. 让用户戴上头显并正视前方后重启应用。
5. 模拟器状态长期异常时使用 `--wipe-data` 重置，而不是继续改业务代码。

#### 沉淀规则

```text
进程活着 ≠ Stage 可见
FPS 正常 ≠ 用户看得见
截图正常 ≠ 3D Stage 状态正常
```

---

## 3. 初始菜单、AttachmentPanel 与 Direct 3D 双路径

### 3.1 问题一：模拟器菜单可见，真机菜单不可见

#### 当时现象

模拟器里 Direct 3D 开始菜单可见，真机上看不到游戏或菜单。

#### 根因

Direct 3D fallback 是为了解决模拟器 AttachmentPanel 丢失而引入的，但真机上坐标和面板路径更适合 SpatialUI AttachmentPanel。

#### 最终方案

按硬件分支：

```kotlin
if (Build.HARDWARE.equals("ranchu", ignoreCase = true)) {
    // 模拟器：Direct 3D fallback
} else {
    // 真机：完整 SpatialUI AttachmentPanel
}
```

#### 沉淀规则

1. 模拟器修复不能直接假设真机可用。
2. 对面板路径保留双路径时，必须同时验证两侧。
3. 每次新增 UI 功能都要明确测试路径：AttachmentPanel / Direct 3D。

---

### 3.2 问题二：菜单逻辑反复偏离需求

#### 需求演进

最初需求是：

```text
首次进入：完整开始面板
开始游戏后：面板收起
游戏中：只保留菜单按钮
点菜单：重新展开开始菜单
```

中间曾出现过：

- 开始后面板仍在
- 只有菜单按钮，但点击后面板直接消失
- 菜单样式和游戏风格不一致
- 菜单点击穿透到棋盘

#### 最终方案

1. 首次进入显示完整设置面板。
2. 点开始后收起面板并进入游戏。
3. 游戏中只保留右侧「菜单」按钮。
4. 点击菜单后展开同一套开始面板。
5. 展开时：
   - 面板置前
   - 棋盘隐藏
   - 工具按钮隐藏
   - 棋盘和工具 recognizer 禁用
6. 面板关闭后恢复游戏。

#### 沉淀规则

UI 状态要画状态机：

```text
Initial
Playing
MenuOpen
Cleared
TimedOut
```

每次改菜单前先确认每个状态下：

- 哪些面板可见
- 哪些实体 enabled
- 哪些手势生效
- 点击是否穿透

---

## 4. 面板拖动与旋转的交互演进

这是本项目调整次数最多的问题。

### 4.1 版本一：外层 SpatialView 绑定 AttachmentPanel Entity

#### 实现

```kotlin
detectSpatialDragGesture(
    targetedToEntity = TargetEntity.hit(panelEntity)
)
```

#### 问题

真机上经常选不中、拖不动。原因是 2D AttachmentPanel 的命中和 3D Entity 树并不是同一种稳定路径。

---

### 4.2 版本二：增加「拖动」区域

#### 实现

在面板上显示：

```text
⠿ 拖动
```

并放大命中区域。

#### 问题

样式变丑，且依然选不中。用户明确反馈：

```text
不需要显示「拖动」两个字
不需要指定拖动位置
要像游戏主体一样捏住直接拖
```

---

### 4.3 最终方案：手势挂在 UI 节点上

不再从外层 SpatialView 绑定 panel entity，而是直接在 Compose UI 节点上使用：

```kotlin
detectSpatialDragGesture(
    context = context,
    targetedToEntity = null
)
```

适用：

- 初始开始面板
- 游戏内展开菜单
- 右侧四个工具按钮面板

效果：

```text
捏住面板任意位置 → 直接拖动
短点按钮 → 正常点击
```

### 沉淀规则

1. AttachmentPanel 的直接拖拽优先挂在 UI 节点。
2. 不依赖外层 3D entity 命中。
3. 不给用户暴露“拖动把手” unless 是明确的产品设计。
4. 拖拽和点击必须同时回归测试。

---

### 4.4 面板旋转：单手长按方案被淘汰

#### 中间方案

```text
立刻拖动 = 移动
捏住停留 0.28 秒再拖动 = 旋转
```

#### 问题

- 时间阈值难感知
- 容易误触发
- 用户不知道当前是移动还是旋转
- 游戏主体一旦套用类似逻辑会出现“跳变”

#### 最终方案：双手调整面板

面板支持两种模式：

```text
单手捏住拖动：移动位置
双手捏住面板：同时移动和旋转
```

双手模式中：

- 双手中心点变化 → 面板位置移动
- 双手连线变化 → 调整 yaw / pitch
- 位置和角度可同时变化

### 沉淀规则

1. 连续空间操作不要用时间阈值切换模式。
2. 双手操作比“长按后切换”更符合空间直觉。
3. 面板和游戏主体手势必须分开设计。
4. 旋转参数要有增益和 clamp，避免失控。

---

## 5. 游戏主体移动与旋转

### 5.1 中间问题

曾经把游戏主体也做成“长按后切换移动/旋转”，导致：

- 用户感觉跳变
- 短拖和长拖语义不稳定
- 与面板需求混淆

### 5.2 最终规则

游戏主体保持稳定语义：

```text
DirectPinch 拖动棋盘 = 移动棋盘
GazePinch / RayBasedPinch 拖动棋盘 = 旋转棋盘
```

不做基于停留时间的模式切换。

### 沉淀规则

1. 游戏主体与 UI 面板是两类交互对象。
2. 游戏主体旋转属于视角/棋盘观察，面板旋转属于 UI 摆放。
3. 不要把 UI 面板的舒适手势直接复制到游戏主体。

---

## 6. 手感模式与左右手识别

### 6.1 问题一：拍掌动作尴尬

#### 原方案

左右手各拿一张卡，双手靠近到阈值后判定。

#### 问题

- 动作幅度大
- 社交尴尬
- 双手靠近时追踪不稳定
- 用户难以理解“手碰手”还是“卡碰卡”

#### 最终方案

改为双卡相碰：

```text
左手拿一张
右手拿一张
两张卡彼此接近并接触 → 判定
```

并加入：

- 卡片接近能量线
- 轻微磁吸
- 接触星尘
- 合法汇聚成星
- 非法软弹开

### 沉淀规则

1. 手势确认对象应该是“被拿起的物体”，不是玩家身体。
2. 减少动作幅度，避免双手遮挡追踪。
3. 用视觉反馈告诉用户接近状态。

---

### 6.2 问题二：左右手分配错误

#### 当时日志

手感模式已启动：

```text
handTrackingStart SUCCESS
mode=FEEL
```

但点击卡片时反复：

```text
feelCardTap hand=RIGHT
```

导致第二张卡一直替换右手卡，无法形成左右手各一张。

#### 根因

`GazePinch` 本身不携带明确手别。旧逻辑用 view-local tap position 和 global hand position 比较，坐标系不一致。

#### 最终方案

综合以下信号：

1. 左右手实际 pinch strength
2. 左右手 aim strength
3. 单手可见性
4. 当前左右手占用状态

模糊情况下：

```text
一只手已有卡 → 第二张分给空手
```

并输出日志：

```text
feelHandSignal
leftPinch / rightPinch
leftAim / rightAim
leftVisible / rightVisible
occupiedLeft / occupiedRight
```

### 沉淀规则

1. 不要用 view-local 点击坐标直接和全局追踪坐标算距离。
2. 手势事件没有手别时，用 HandTrackingProvider 补信号。
3. 必须打印原始信号，而不是只打印最终判断。
4. 左右手状态要考虑“已有卡”的占位。

---

## 7. 连连看规则与 3D 路径算法

### 7.1 问题一：相同卡片无法消除

#### 当时现象

用户反馈猴子或青蛙相同卡点击无效。

#### 排查步骤

加了三类日志：

1. 卡片点击：id/type/坐标/选中状态
2. 配对判定：两张卡完整数据
3. 路径检测：尝试路径、阻挡、最终结果

#### 根因方向

- 早期路径逻辑偏 2D，Z 轴处理不足
- 转弯数与 3D 方向组合不完整
- 外围空壳路径处理不稳定

#### 最终方案

1. 使用 6 方向 3D 寻路。
2. 放宽为最多 3 弯。
3. 支持外层 shell 空间绕行。
4. 卡片相同性同时校验动物和底色。
5. 为 solver 建立 JVM 单元测试。

### 沉淀规则

1. 3D 消除规则必须先用纯逻辑模块实现，不直接写在 UI 里。
2. 路径 solver 必须覆盖：
   - 直线
   - 一弯
   - 二弯
   - 三弯
   - 外壳绕行
   - 阻挡拒绝
3. 出现“看起来能连但不能消”时，先加日志，不盲目改规则。

---

### 7.2 问题二：奇数棋盘 3×3×3 少一组

#### 根因

3×3×3 = 27 张，奇数无法全部成对。

#### 最终方案

保留 26 张卡 + 1 个空位。空位放在最内侧中位，例如 `Cell(1,1,1)`。

好处：

1. 外观仍是完整 3×3×3。
2. 空位可作为路径空间。
3. 不会破坏 exterior 视觉完整性。

### 沉淀规则

奇数棋盘不要随机藏空位，优先选择内侧 median cell。

---

## 8. 卡片渲染与旋转

### 8.1 问题一：旋转后贴纸/底色跟不上

#### 根因

早期用多面材质或贴纸方式，旋转后不同面的纹理状态不一致。

#### 最终方案

一张卡 = 一个 cube = 一个完整不透明纹理。

纹理中一次性烘焙：

- 背景颜色
- 动物 emoji
- 边框
- 选中态样式

所有六面共享同一纹理。

### 沉淀规则

1. 简单 cube 卡片不要拆成多个面分别更新。
2. 选中态可以通过完整纹理切换，避免面状态漂移。
3. 纹理资源必须 `toGlobal()` 并在 Scene destroy 时显式 close。

---

### 8.2 问题二：大棋盘卡片太小

#### 根因

自定义边长取消上限后，仍用固定容器拟合，导致卡片缩小到难以点击。

#### 最终方案

1. Full Space 不再强制缩回旧 960dp 容器。
2. 设置最小可玩卡片尺寸，约 `0.058m`。
3. 大于 5 的边长使用更紧的 gap。
4. 让大棋盘向外生长，而不是缩小卡片。

### 沉淀规则

自定义尺寸功能必须同时定义：

- 最小可点击尺寸
- 最大合理视野
- gap 策略
- 性能预算

---

## 9. UI 视觉与空间按钮的反复调整

### 9.1 常见反复问题

- 按钮太小
- 文字反向
- 按钮挡住棋盘
- 有奇怪线条
- 空白底板太大
- 菜单样式像网页表单
- 通关面板风格不统一

### 9.2 最终视觉原则

统一为：

```text
黑色宇宙
深色玻璃
星光边框
简洁卡通卡片
低饱和强调色
```

工具面板：

- 独立胶囊按钮
- 无外层大背板
- 无多余空白区域
- 图标 + 清晰文字
- 可拖动、可旋转

### 9.3 沉淀规则

1. 空间 UI 不要照搬移动端表单布局。
2. 每个面板要有明确边界和朝向。
3. 按钮尺寸要按物理距离验证，而不是只看 dp。
4. UI 修改后必须检查旋转棋盘时是否遮挡。
5. 所有 2D UI 使用 SpatialUI / `PicoTheme`，禁止 Material3。

---

## 10. 声音问题复盘

### 10.1 问题一：真机完全无声

#### 排查结果

系统音量正常：

```text
STREAM_MUSIC speaker 15/15
Muted: false
```

音效资源加载成功，但用户听不到。

#### 根因

SoundPool 使用：

```kotlin
AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
```

在 PICO 真机上可能被归入系统辅助音路由，被系统过滤或音量独立控制。

#### 最终方案

改为：

```kotlin
AudioAttributes.USAGE_GAME
```

并添加日志：

```text
soundLoad sample=... status=0
soundPlay sample=... stream=...
```

### 10.2 问题二：点击声音像小狗叫

#### 根因

用户说“选中物体声音像小狗叫”。第一次只替换了 `card_select.wav`，但菜单按钮实际使用 `ui_click.wav`，所以菜单仍然有旧声音。

#### 最终方案

1. 区分事件音效：
    - `card_select.wav`
    - `ui_click.wav`
    - `ui_hover.wav`
    - `card_contact.wav`
    - `card_land.wav`
    - `shuffle_whoosh.wav`
2. 去掉升调、噪声、过长尾音。
3. 使用固定音高、快速衰减的 neutral tick。

### 沉淀规则

1. 声音问题先确认“哪个事件触发哪个 sample”。
2. SoundPool 必须记录 load 和 play 结果。
3. 真机音频路由优先使用 `USAGE_GAME`。
4. 每类 UI 事件单独一个 sample，不要复用不明语义声音。

---

## 11. 特效实现的性能与资源管理

### 11.1 已实现特效

- 双卡接近能量线
- 接触星尘
- 成功汇聚成星
- 失败软弹开
- 匹配路径星点
- 提示路径
- 洗牌飞行拖尾
- combo 浮动卡片
- 倒计时状态变化
- 通关星座动画
- 烟花、礼炮、confetti

### 11.2 关键实现原则

1. 使用固定 particle pool，不在每帧创建 Entity。
2. Mesh / Texture / Material 使用 `toGlobal()` 持久化。
3. Scene destroy 时显式 close。
4. 特效循环要能被 generation 取消。
5. 避免大量 Bitmap 在主线程生成。
6. 通关庆祝必须有限时长，不能无限循环。

### 11.3 沉淀规则

先做“反馈清晰”，再做“效果华丽”。

每个特效要回答：

- 触发时机
- 生命时长
- 最大粒子数
- 谁负责销毁
- 是否影响点击命中
- 真机 FPS 是否可接受

---

## 12. 模拟器与真机验证差异

### 12.1 模拟器不能完全代表真机

| 项目 | 模拟器 | 真机 |
|---|---|---|
| 前后深度 | 与真机相反 | 实际佩戴方向决定 |
| AttachmentPanel 稳定性 | 可能丢失，需要 Direct 3D fallback | 通常更稳定 |
| 手部追踪 | 无法完整验证 | 必须验证 |
| 音频路由 | 不能完全代表 | 必须验证 |
| 左右手信号 | 无法可靠验证 | 必须看 pinch/aim 日志 |
| UI 舒适度 | 屏幕视角不同于头显 | 必须肉眼确认 |

### 12.2 验证顺序

推荐固定流程：

```text
1. 纯逻辑单元测试
2. assembleDebug
3. SpatialUI verifier
4. 模拟器启动 / 无崩溃 / 基础视觉
5. 真机安装
6. 真机日志
7. 用户肉眼确认
8. 交互手感确认
9. 保存版本快照
```

### 12.3 禁止事项

不要用 2D ADB tap 证明空间交互成功。  
不要用黑屏截图直接判断 Full Stage 崩溃。  
不要只在模拟器验证手部追踪。  
不要在真机掉线时继续猜测代码根因。

---

## 13. 截图与日志的判断标准

### 13.1 截图限制

出现过：

- 截图纯黑
- 截图显示系统房间
- UIAutomator 有层级但截图无画面
- Stage 实体日志正常但截图不可见

### 13.2 正确判断链路

```text
进程是否存在
→ MainStage visible/focus
→ 关键 Entity 是否创建
→ FPS 是否持续
→ 是否有 FATAL/ANR
→ 用户肉眼反馈
```

### 13.3 建议

截图只能作为辅助证据。  
空间应用必须有日志级验证。  
最终交互必须由真机用户确认。

---

## 14. 真机连接与版本管理

### 14.1 常见连接问题

- USB 掉线
- 头显未授权调试
- 只出现“外接设备”弹窗
- 头显电量耗尽
- ADB daemon 权限受限

### 14.2 处理规范

1. 先跑 `adb devices -l`。
2. 确认 `PB325XKGL4200027B device`，不是 unauthorized/offline。
3. 授权弹窗必须在头显内确认。
4. 若掉线，先检查线缆和电量。
5. 不要在设备 offline 时反复安装。

---

### 14.3 版本管理教训

#### 问题

源码快照已经到 `0.2.0`，但 Android 包仍是：

```text
versionCode=1
versionName=1.0
```

导致系统出现“存在新版本 / 优化了位置”等模糊提示，难以判断来源。

#### 最终方案

建立三层版本：

1. Android 版本：

```text
versionCode=3
versionName=0.2.1-position-polish
```

2. 项目版本文件：

```text
VERSION
CHANGELOG.md
```

3. 可回滚快照：

```text
releases/linklin-0.2.1-position-polish.apk
versions/linklin-0.2.1-position-polish-full.tar.gz
versions/linklin-0.2.1.SHA256
```

### 14.4 版本保存规则

每完成一个可玩节点保存一次：

```text
版本号
APK
源码快照
SHA256
CHANGELOG
验证记录
已知限制
```

不要只保存 APK，不保存源码状态。

---

## 15. 推荐的开发流程模板

### 15.1 新交互需求先写语义表

示例：

| 对象 | 单手短点 | 单手拖动 | 双手 | 射线 |
|---|---|---|---|---|
| 卡片 | 选卡/粘卡 | 不移动卡片 | 双卡接触 | 选卡 |
| 游戏主体 | 不处理 | 移动 | 面板专用，不套用 | 旋转 |
| 开始面板 | 按钮点击 | 移动 | 移动 + 旋转 | 点击/拖动 |
| 工具面板 | 按钮点击 | 移动 | 移动 + 旋转 | 点击/拖动 |

### 15.2 每个空间 UI 都要有 transform 契约

记录：

- position
- yaw
- pitch
- clamp range
- 默认值
- 持久化策略
- 真机/模拟器差异

### 15.3 每个手势都要有日志标签

推荐标签：

```text
setupPanelDragStart
toolRailDragStart
spatialDragStart
boardMoveEnd
feelHandSignal
feelCardTap
feelCardsTouch
soundLoad
soundPlay
```

### 15.4 每次修 Bug 先收集证据

最小证据集：

```text
1. 用户看到什么
2. 进程状态
3. MainStage 状态
4. 关键实体创建日志
5. 交互日志
6. 崩溃日志
7. 当前版本号
```

---

## 16. 最终沉淀的开发原则

### 原则 1：先定义交互语义，再写代码

不要把“拖动”默认理解为移动、旋转或高亮。每个对象的行为必须明确。

### 原则 2：模拟器和真机是两条验证路径

模拟器解决基础渲染和崩溃，真机解决空间、手部、音频、舒适度。

### 原则 3：日志比截图可靠

Full Stage 下截图不稳定，日志能确认进程、Stage、实体、交互、音频状态。

### 原则 4：纯逻辑与渲染分离

连连看 solver 放在 `LianliankanGame`，不写在 `HomeVolume`。这样才能单元测试。

### 原则 5：资源生命周期必须显式

`toGlobal()` 的资源不会被 Entity destroy 自动释放，必须在 Scene destroy close。

### 原则 6：手势不要依赖隐式时间阈值

长按切换模式容易跳变。双手或不同输入类型更清晰。

### 原则 7：反馈要对应对象

手感模式确认的是“两张卡接触”，不是“两只手接触”。

### 原则 8：UI 不做网页表单

空间 UI 要考虑朝向、厚度、遮挡、物理尺寸、拖动和旋转。

### 原则 9：版本快照必须包含源码和 APK

只留 APK 无法回溯实现，只留源码无法快速装机验证。

### 原则 10：每个“看不见”问题先查状态，不猜渲染

特别是：

```text
MainStage visible=false
systemui caller
tracking stopped
headset not worn
```

---

## 17. 后续建议

### 17.1 建立固定调试面板

开发模式显示：

- FPS
- Stage visible/focus
- board position
- panel position/rotation
- 当前手势类型
- 左右手 pinch/aim
- 音效 load/play 状态

### 17.2 建立交互自动化 smoke test

至少覆盖：

- 菜单打开/关闭
- 开始游戏
- 卡片选中
- 配对成功/失败
- 提示
- 洗牌
- 放弃
- 通关
- 限时失败

### 17.3 建立音频试听表

| 事件 | 文件 | 期望感受 | 当前状态 |
|---|---|---|---|
| 卡片选中 | card_select.wav | 干净 soft tap | 已调整 |
| UI 点击 | ui_click.wav | neutral tick | 已调整 |
| UI hover | ui_hover.wav | 极轻 tick | 待长期试听 |
| 双卡接触 | card_contact.wav | 清脆星尘感 | 待真机调音 |
| 匹配成功 | match_success.wav | 上扬 chime | 已接 combo pitch |
| 匹配失败 | match_failure.wav | 柔和低频 | 待调音 |

### 17.4 版本发布前 checklist

```text
[ ] testDebugUnitTest 通过
[ ] assembleDebug 通过
[ ] SpatialUI verifier 0 error
[ ] 模拟器启动无崩溃
[ ] 真机启动无崩溃
[ ] 开始面板可见可拖可旋
[ ] 工具面板可见可拖可旋
[ ] 游戏主体位置/旋转稳定
[ ] 手感模式左右手正确
[ ] 双卡接触可消除
[ ] 所有音效可听且不刺耳
[ ] 通关流程正常
[ ] 保存 APK + 源码快照 + SHA256
[ ] 更新 CHANGELOG 和 AGENTS
```

---

## 18. 当前版本基线

当前保存版本：

```text
0.2.1-position-polish
versionCode=3
```

APK：

```text
releases/linklin-0.2.1-position-polish.apk
```

完整快照：

```text
versions/linklin-0.2.1-position-polish-full.tar.gz
```

SHA256：

```text
APK:
4be25583f3a30c9bad21ee08b0bf109b7ca0f509abd4723ccce298ebfb21c80d

Snapshot:
12ab29d3c19f883ef942de454146cc91bbda6de82235070e0447a07e66fa3942
```

