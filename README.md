# ShaveBuddy

一个安静、本地优先的剃须记录与刀片寿命助手。当前可体验 **Android 手动剃须刀 Demo**：简体中文界面，记录保存在本机，不需要账号或网络。

## 下载体验

当前版本：[0.1.2-demo（预发布）](https://github.com/AdvancedX/ShaveBuddy/releases/tag/0.1.2-release)。

**[下载 Android APK：ShaveBuddy.apk](https://github.com/AdvancedX/ShaveBuddy/releases/download/0.1.2-release/ShaveBuddy.apk)**

需要 Android 8.0（API 26）或更高版本。用手机下载 APK 后打开安装；如果系统提示，允许当前浏览器或文件管理器安装此应用。这个安装包是供个人试用的 Debug 构建。

首次启动时设置刀片安装日期和目标，随后可在「今天」记录剃须，在「日历」查看或补记，在「装备」更换刀片及调整设置。底部导航顺序为「日历 / 今天 / 装备」，默认打开「今天」。

卸载或清除应用数据会删除本地记录；Demo 暂无备份恢复功能。

## Demo 功能与范围

- 设置一把手动剃须刀与刀片安装日期。
- 编辑使用次数、天数目标与剃须间隔；目标留空可关闭。
- 一键记录今天，也可补记过去日期，同日支持多次记录。
- 补记早于首次安装日期时，确认调整首次安装日期后保存；不能选择未来日期。
- 查看当前刀片使用次数、自然日天数、上次与下次大约剃须日期。
- 按月查看日历、日期记录详情，确认后删除记录。
- 确认更换刀片，结束旧周期并保留历史。
- 跟随系统浅色/深色主题，支持旋转恢复与本地持久化。

当前仅支持单件手动剃须装备。多装备、电动剃须刀、通知、记录编辑、评分备注、导入导出、iOS 与商店发布属于后续工作。

## 获取源码

可运行的 Android Demo 在 **[feat/manual-razor-demo 分支](https://github.com/AdvancedX/ShaveBuddy/tree/feat/manual-razor-demo)**。默认 `main` 分支目前保留早期规划文档与此使用说明，尚未合入 Demo 源码。

```sh
git clone --branch feat/manual-razor-demo https://github.com/AdvancedX/ShaveBuddy.git
cd ShaveBuddy
```

[0.1.2-release 标签](https://github.com/AdvancedX/ShaveBuddy/tree/0.1.2-release)对应此次发布的源码。如需构建该版本，在仓库中执行 `git checkout 0.1.2-release`。先前的 `demo-release` 标签保留旧版本。

## 运行与打包 APK

用 Android Studio 打开 Demo 源码的仓库根目录，等待 Gradle 同步，选择 `app` 与模拟器或 Android 手机后运行。

构建需要 SDK Platform 37、Build Tools 36.0.0 和兼容 Gradle 9.3.1 的 JDK；本机已使用 JDK 25 验证。Java/Kotlin 字节码目标为 17。命令行构建前配置 `ANDROID_HOME`，或在不提交的 `local.properties` 中写入 `sdk.dir`。首次构建需要下载依赖。

```sh
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。通过 USB 连接并授权手机，或启动模拟器后，可用 ADB 安装：

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.shavebuddy.demo/.MainActivity
```

应用 ID 为 `com.shavebuddy.demo`。以上命令生成 Debug 签名的试用包；正式发布前需要确定发布者命名空间和发布签名。

## 开发验证

单元测试与静态检查：

```sh
./gradlew testDebugUnitTest lintDebug
```

连接模拟器或已授权设备后，可覆盖安装并直接运行设备测试，保留已有试用记录；测试本身使用独立数据库：

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
```

`./gradlew connectedDebugAndroidTest` 会装卸应用，可能删除试用记录，仅用于专用测试设备；执行后需要重新安装 Demo。

## 文档

以下链接指向 Demo 分支的文档：

- [产品基调与长期 MVP](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/product-brief.md)
- [当前技术方案与 ADR](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/technical-direction.md)
- [交付路线图](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/delivery-roadmap.md)
- [Demo 范围](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/superpowers/specs/2026-10-03-manual-razor-demo-design.md)
- [实现计划](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/superpowers/plans/2026-10-03-manual-razor-demo.md)
- [验证记录](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/demo-verification.md)
- [原始开工清单](https://github.com/AdvancedX/ShaveBuddy/blob/feat/manual-razor-demo/docs/pre-development-blockers.md)

Demo 已用于试用，完整 MVP 与阶段 0 验收仍未完成；开工清单中的真机、通知等事项继续保留。
