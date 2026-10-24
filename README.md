# 手边事

拖拽式简易待办 Android APP。

把平时要做的琐事先存进「全部事项」素材库，需要安排接下来 1–2 小时做什么时，直接把事项拖到上方「接下来待办」区。做完一件点「完成」删除，或再拖回素材库。

## 特性

- 上下双分区：上方「接下来待办」+ 下方「全部事项」
- 长按卡片拖拽：
  - 下区 → 上区：加入待办计划
  - 上区 → 下区：撤回素材库
- 上区卡片点「完成」＝任务完成并删除
- 后台管理页：新增 / 编辑 / 删除全部事项
- 数据本地存储（Room），无需联网、无账号、无广告
- 现代极简 UI：低饱和静谧蓝、圆角卡片、柔和阴影、分区色差分层

## 技术栈

- Kotlin、Android 原生 SDK（View + RecyclerView）
- Android 原生拖拽 API（`startDragAndDrop` / `OnDragListener`）
- Room（本地持久化）
- 现代极简 UI（Material3 风格，自主设计）

## 环境要求

- JDK 17+（构建时建议使用 JDK 21）
- Android SDK：compileSdk 35，minSdk 26（Android 8.0+）
- Gradle 8.14（已内置 wrapper）

## 构建

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

## 目录结构

```
app/src/main/java/com/liangleionline/shoubianshi/
├── MainActivity.kt        # 首页：上下双分区拖拽交互
├── ManageActivity.kt      # 后台：事项管理
├── MainViewModel.kt       # 状态与数据操作
├── TaskAdapter.kt         # 首页任务卡片适配器
├── ManageAdapter.kt       # 管理页适配器
└── data/
    ├── Task.kt            # 事项实体（isPinned 区分上下区）
    ├── TaskDao.kt         # 数据访问
    └── AppDatabase.kt     # Room 数据库
```

## 使用说明

1. 打开「管理」进入事项管理页，添加常用事项（如：刷牙、洗脸、上班、做饭、洗漱）。
2. 回到首页，长按「全部事项」里的卡片，拖到上方「接下来待办」。
3. 做完一件事，点卡片右侧「完成」删除；想取消规划，把卡片拖回下方即可。
