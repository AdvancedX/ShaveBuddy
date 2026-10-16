# ShaveBuddy

一个简洁的 Android 剃须记录应用，帮你记住上次剃须的日期，也看清当前刀片用了多少次、多久。

剃须后轻点一下，记录、刀片统计和下次大约剃须日期就会一起更新。无需注册，离线可用，数据只保存在自己的手机上。

## 下载与安装

**[下载 Android APK：ShaveBuddy.apk](https://github.com/AdvancedX/ShaveBuddy/releases/download/v0.1.3-demo/ShaveBuddy.apk)**

支持 **Android 8.0 及以上**，界面语言为简体中文。当前版本为 **0.1.3-demo（预发布）**，版本信息见 [发布页面](https://github.com/AdvancedX/ShaveBuddy/releases/tag/v0.1.3-demo)。

用手机下载 APK 后打开安装。如果系统提示，请允许当前浏览器或文件管理器安装此应用。安装完成后，打开 ShaveBuddy 即可开始使用。

已安装旧版时直接覆盖安装，无需卸载。首次安装此版后，后续可在应用右上角「设置」中检查和下载更新。

## 主要功能

- **一键记录**：记录今天的剃须，同一天可以记录多次。
- **剃须满意度**：记录后可为总体、剃净度、顺滑度和皮肤状况分别选择「不满意 / 一般 / 满意」，每项都可跳过。
- **补记与回顾**：补记过去的日期，按月查看剃须日历，查看或删除某一天的记录。
- **刀片统计**：查看当前刀片的使用次数和已安装天数。
- **个人目标**：设置刀片使用次数、天数目标，达到任一目标时显示更换提示；也可以留空关闭目标。
- **剃须节奏**：查看上次剃须日期，并根据自己设置的间隔推算下次大约日期。
- **更换与历史**：更换刀片后开始新的使用周期，旧刀片和剃须记录继续保留。
- **浅色与深色外观**：跟随系统主题。
- **应用内更新**：在设置中主动检查 GitHub 新版，自动下载并校验安装包，再由 Android 系统确认安装；支持取消和重试。

刀片目标由你自己决定，应用按照记录和设置显示状态；下次日期也只是按间隔推算，方便安排日常节奏。

## 快速上手

1. **添加剃须刀**：首次打开时，填写装备名称、当前刀片安装日期和剃须间隔。次数与天数目标可按需要填写。
2. **记录剃须**：在「今天」点击「记录剃须」，保存后可填写满意度或跳过。忘记记录时，点击「补记其他日期」。
3. **查看日历**：日期格显示当天最近一次已填写的总体满意度，点选日期查看每次剃须的具体评分，也可以补记或删除记录。
4. **更换刀片**：在「装备」点击「更换刀片」，确认后从今天开始新的周期；在同一页可以调整装备名称与目标。

补记不能选择未来日期。若补记日期早于首次刀片安装日期，应用会先请你确认调整首次安装日期，再保存记录。

## 数据与隐私

ShaveBuddy 不需要账号，不上传剃须记录，也不接入行为分析服务。记录与满意度保存在本机，关闭应用后仍会保留。剃须记录功能离线可用；仅在主动检查和下载更新时联网访问 GitHub，安装更新需由 Android 系统授权并确认。

**目前没有备份、导入导出或云同步功能。卸载应用或清除应用数据会删除记录，请在操作前留意。**

当前支持一把手动剃须刀。多装备、电动剃须刀、系统通知提醒和 iOS 版本尚未提供；更换提示与下次日期显示在应用内。

## 开发与构建

应用采用 Kotlin、Jetpack Compose、Material 3 和 Room。当前发布包的完整源码位于 `v0.1.3-demo` 标签。

```sh
git clone --branch v0.1.3-demo https://github.com/AdvancedX/ShaveBuddy.git
cd ShaveBuddy
```

[v0.1.3-demo 标签](https://github.com/AdvancedX/ShaveBuddy/tree/v0.1.3-demo)对应下载包的源码；已有仓库可执行 `git fetch --tags` 后运行 `git checkout v0.1.3-demo`。

用 Android Studio 打开仓库根目录，等待 Gradle 同步，选择 `app` 与 Android 手机或模拟器后运行。

构建需要 SDK Platform 37、Build Tools 36.0.0 和兼容 Gradle 9.3.1 的 JDK；已使用 JDK 25 验证，Java/Kotlin 字节码目标为 17。命令行构建前配置 `ANDROID_HOME`，或在本机 `local.properties` 中写入 `sdk.dir`，不要提交该文件。首次构建需要下载依赖。

```sh
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`，使用 Debug 签名。当前应用 ID 为 `com.shavebuddy.demo`。

### 开发验证

单元测试与静态检查：

```sh
./gradlew testDebugUnitTest lintDebug
```

连接模拟器或已授权设备后，可覆盖安装并运行设备测试。测试使用独立数据库：

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
```

`./gradlew connectedDebugAndroidTest` 会装卸应用，可能删除已有记录，仅用于专用测试设备。

## 文档

- [产品方向](docs/product-brief.md)
- [技术方案](docs/technical-direction.md)
- [后续路线图](docs/delivery-roadmap.md)
- [开发验证记录](docs/demo-verification.md)
