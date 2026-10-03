# ShaveBuddy

一个安静、本地优先的剃须记录与刀片寿命助手。当前已实现 **Android 手动剃须刀 Demo**，界面为简体中文，数据存于本机 Room 数据库，不需要账号或网络。

## Demo 功能

- 首次设置一把手动剃须刀与刀片安装日期；
- 可编辑的使用次数、天数目标与剃须间隔，目标留空可关闭；
- 一键记录今天，也可补记历史日期，同日支持多次；
- 补记可选择过去日期；早于首次安装日期时，确认调整首次安装日期后保存；
- 查看当前刀片使用次数、自然日天数、上次与下次大约日期；
- 月历、日期记录详情及确认删除；
- 确认更换刀片，结束旧周期并保留历史；
- 跟随系统的浅色/深色主题，旋转恢复与本地持久化。

Demo 仅支持单件手动装备；通知、记录编辑、评分备注、导入导出、多装备、iOS 与商店发布属于后续工作。卸载/清除应用数据会删除本地记录，Demo 暂无备份恢复功能。

## 运行

用 Android Studio 打开仓库根目录，等待 Gradle 同步，选择 `app` 与模拟器/Android 手机后运行。需要 SDK Platform 37、Build Tools 36.0.0，最低运行版本为 Android 8.0（API 26）。Java/Kotlin 字节码目标为 17；Gradle 使用兼容 JDK（本机验证为 JDK 25 与 Gradle 9.3.1）。

首次命令行构建前配置 `ANDROID_HOME`，或在不提交的 `local.properties` 写入 `sdk.dir`。依赖已通过 Version Catalog 锁定；首次构建需要下载依赖。

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.shavebuddy.demo/.MainActivity
```

连接模拟器或已授权设备后，覆盖安装并直接运行设备测试（保留试用记录）：

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
```

安装包：`app/build/outputs/apk/debug/app-debug.apk`。应用 ID `com.shavebuddy.demo` 仅用于 Demo 本地测试；正式发布前需要确定发布者命名空间和签名。

`./gradlew connectedDebugAndroidTest` 适合专用测试设备；该任务会装卸应用，完成后需要重新安装 Demo。测试本身使用独立数据库。

## 文档

- [产品基调与长期 MVP](docs/product-brief.md)
- [当前技术方案与 ADR](docs/technical-direction.md)
- [交付路线图](docs/delivery-roadmap.md)
- [Demo 范围](docs/superpowers/specs/2026-10-03-manual-razor-demo-design.md)
- [实现计划](docs/superpowers/plans/2026-10-03-manual-razor-demo.md)
- [验证记录](docs/demo-verification.md)
- [原始开工清单](docs/pre-development-blockers.md)

开工清单中的真机、通知与完整阶段 0 验收仍需完成。Demo 不等于全部 MVP 或阶段 0 已通过。
