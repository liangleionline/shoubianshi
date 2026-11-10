# 手边事

[![Release](https://img.shields.io/github/v/release/liangleionline/shoubianshi?label=release&color=4E7CF6)](https://github.com/liangleionline/shoubianshi/releases)
[![Downloads](https://img.shields.io/github/downloads/liangleionline/shoubianshi/total?label=downloads&color=2FB56C)](https://github.com/liangleionline/shoubianshi/releases)
[![Latest downloads](https://img.shields.io/github/downloads/liangleionline/shoubianshi/latest/total?label=latest%20downloads&color=2FB56C)](https://github.com/liangleionline/shoubianshi/releases/latest)
[![Visits](https://hits.sh/github.com/liangleionline/shoubianshi.svg?label=visits&color=4E7CF6)](https://hits.sh/github.com/liangleionline/shoubianshi/)
[![Last commit](https://img.shields.io/github/last-commit/liangleionline/shoubianshi)](https://github.com/liangleionline/shoubianshi/commits/main)
[![Commit activity](https://img.shields.io/github/commit-activity/m/liangleionline/shoubianshi)](https://github.com/liangleionline/shoubianshi/commits/main)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/liangleionline/shoubianshi/releases/latest)
[![minSdk](https://img.shields.io/badge/minSdk-26-3DDC84)](https://github.com/liangleionline/shoubianshi/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Repo size](https://img.shields.io/github/repo-size/liangleionline/shoubianshi)](https://github.com/liangleionline/shoubianshi)

不提醒、不排期、不社交——只把眼下该做的事放在手边。

「手边事」是一个极简待办工具：平时把琐事存进「全部事项」素材库，需要安排接下来 1–2 小时做什么时，点一下胶囊，它就飞进上方「接下来待办」。做完的事点回去、或直接在管理页删掉，然后再安排下一件。

## 截图与交互

首页是一整块屏幕，上下分两个区：

- **上区（占 1/3）「接下来待办」**：眼下要做什么，一眼看得见
- **下区（占 2/3）「全部事项」**：所有备选琐事，按使用频率从高到低排
- **点一下就飞**：点下区的胶囊，它飞进上区；点上区的胶囊，它飞回下区。不用长按、不用拖拽
- **飞完才补位**：后面胶囊不会在动画中途挤上来，飞行结束才重新排布
- **多了能滚**：任一区的事项超出可视范围，上下滑动即可查看
- **好看也认得出**：8 组浅底深字配色循环给胶囊上色，同一事项颜色固定

管理页可以新增、编辑、删除全部事项，数据存在手机本地。

## 下载安装

最新安装包见 [Releases](https://github.com/liangleionline/shoubianshi/releases/latest)：

```
https://github.com/liangleionline/shoubianshi/releases/latest/download/shoubianshi-v1.4.1.apk
```

- 系统要求：Android 8.0（API 26）及以上
- 安装包为精简版 release（代码混淆 + 资源压缩，约 1.6 MB），使用 debug 签名，可覆盖升级并保留数据
- 无需联网、无需账号，安装后即可使用

## 使用说明

1. 打开 App，点右上角「管理」，添加常用事项（刷牙、洗脸、上班、做饭、洗漱……）
2. 回到首页，点下区的胶囊 → 飞进上区「接下来待办」
3. 做完一件事，点它 → 飞回下区素材库；不需要了就在管理页删除
4. 事项一多，上区、下区都能上下滑动查看
5. 同一天反复安排的事，会被下区按使用频率自动往前排

## 技术栈

- Kotlin 2.0.20 + Android 原生 View（AppCompatActivity + ViewBinding）
- 自定义 `FlowLayout` 流式排布胶囊（自动换行、支持边距）
- Room 2.6.1（KSP）本地持久化，含 1→2 升级迁移
- Material3 主题（仅取主题与控件样式），自主设计的低饱和静谧蓝视觉
- 单击飞行动画：幽灵视图挂在根布局，直线飞向目标胶囊真实落点

## 构建

```bash
# 精简 release（当前交付形态）
gradlew.bat assembleRelease

# 调试包
gradlew.bat assembleDebug
```

- 产物：`app/build/outputs/apk/release/app-release.apk`、`app/build/outputs/apk/debug/app-debug.apk`
- 环境：JDK 17+（建议 JDK 21）、compileSdk 35、minSdk 26、Gradle 8.14（wrapper 已内置）

## 目录结构

```
app/src/main/
├── java/com/liangleionline/shoubianshi/
│   ├── MainActivity.kt      # 首页：上下双分区、单击飞行、滚动与延迟补位
│   ├── MainViewModel.kt     # 数据流与增删改、移入/移出待办
│   ├── FlowLayout.kt        # 自定义流式布局（胶囊自动换行）
│   ├── ManageActivity.kt    # 管理页：新增 / 编辑 / 删除
│   ├── ManageAdapter.kt     # 管理页列表适配器
│   └── data/
│       ├── Task.kt          # 事项实体（isPinned 区分上下区，useCount 记录使用频率）
│       ├── TaskDao.kt       # 数据访问（含频率排序查询）
│       └── AppDatabase.kt   # Room 数据库与迁移
├── res/
│   ├── layout/              # 首页、管理页、胶囊与列表项
│   ├── drawable/            # 圆角卡片 / 输入框 / 区块背景等形状
│   └── values/              # 配色、尺寸、文案、主题
└── AndroidManifest.xml      # 无任何网络权限
```

## 数据与隐私

- 所有事项只存在本机 Room 数据库中，App 不申请网络权限
- 无账号体系、无统计上报、无广告、无第三方 SDK
- 覆盖安装（同签名）会保留原有数据

## 仓库统计怎么看

README 顶部徽章可直接反映仓库热度，无需额外后台：

| 徽章 | 含义 | 数据来源 |
| --- | --- | --- |
| release | 当前最新版本号 | GitHub Releases |
| downloads | 全部版本累计下载次数 | GitHub 资产下载计数 |
| latest downloads | 最新版本下载次数 | GitHub 资产下载计数 |
| visits | README 访问次数（近似值） | hits.sh 计数器（图片被加载即计一次） |
| last commit / commit activity | 最近提交时间、每月提交数 | 仓库提交历史 |
| repo size | 仓库体积 | GitHub API |

想看单个安装包的下载明细，可在 Releases 页面展开对应资产的 `Downloads` 数字。

## 未完成需求清单

截至 v1.4.1（versionCode 21），此前提出的需求已全部实现，没有未完成的开发项。

以下仅是尚未被提出的可选方向，不算需求，供以后参考：深色模式、事项分组/标签、桌面小组件、数据导出备份。

## 版本记录

- v1.4.1：代码与资源清理（移除未使用样式、无引用颜色、废弃 DAO / ViewModel 方法与冗余属性），README 重写并加入统计徽章
- v1.4.0：上下区域 1:2 布局、双区可滚动、飞行结束才补位
- v1.3.1：移除拖拽只留单击，冗余清理并启用精简 release
- v1.3.0 及更早：双分区交互与胶囊样式逐步成型（旧版本安装包已清理，仓库仅保留最新版）
