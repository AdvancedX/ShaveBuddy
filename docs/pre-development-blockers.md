# Android 开工前阻塞清单

状态：待执行

日期：2026-08-15

负责人：项目维护者

## 1. 目的

这份清单用于判断 ShaveBuddy 是否已经具备初始化 Android 工程并进入阶段 0 技术验证的条件。

完成本清单不代表可以直接开发全部 MVP。它只代表以下关键问题已经解决：

- 技术方向与文档一致；
- 应用身份不会在工程初始化后立即返工；
- Android 工具链和测试设备可用；
- Git 中存在干净、可回退的开发起点；
- 第一批页面流程和业务状态足够明确；
- 阶段 0 有可验证的退出条件。

## 2. 阻塞一：确认 Android 原生技术方向

### 待办

- [ ] 确认首个可发布版本只要求 Android；
- [ ] 确认采用 Kotlin + Jetpack Compose，不再以 Expo 作为当前实现方案；
- [ ] 确认 iOS 延后评估，不承诺当前 Android UI 可以直接复用；
- [ ] 更新 `docs/technical-direction.md`，将状态和技术栈改为 Android 原生；
- [ ] 更新 `docs/delivery-roadmap.md`，将双端验证改为 Android 单端阶段 0；
- [ ] 新增 ADR，记录从 Expo 转向 Android 原生的原因、代价与复核条件。

### 建议技术栈

| 领域 | 当前建议 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation 3 稳定版 |
| UI 状态 | ViewModel + immutable `UiState` + StateFlow |
| 异步 | Kotlin Coroutines + Flow |
| 业务数据 | Room |
| 简单设置 | Preferences DataStore |
| 提醒调度 | WorkManager |
| 通知展示 | NotificationManagerCompat |
| 日期时间 | `java.time` 的 `LocalDate`、`Instant`、`ZoneId` |
| 导入导出 | kotlinx.serialization + Storage Access Framework |
| 依赖注入 | MVP 使用手动构造和构造器注入 |
| 构建 | Gradle Kotlin DSL + Version Catalog + KSP |

### 架构约束

- [ ] 第一阶段只使用单个 `app` module；
- [ ] Compose 页面不得直接访问 Room、DataStore 或 WorkManager；
- [ ] ViewModel 通过 Repository 读取和修改业务数据；
- [ ] 日期、寿命和下次剃须计算保持为纯 Kotlin 规则；
- [ ] 不为了未来 iOS 提前引入 Kotlin Multiplatform；
- [ ] 不提前引入 Hilt、Firebase、分析 SDK 或第三方日历组件。

### 完成证据

- 更新后的技术方案文档；
- 一份状态为 `Accepted` 的 Android 原生 ADR；
- 文档中不再把 iOS 真机验证列为 Android 阶段 0 的退出条件。

## 3. 阻塞二：确定应用身份与分发方向

### 必须确认

- [ ] 应用显示名称；
- [ ] `applicationId`；
- [ ] 首发分发渠道；
- [ ] 首发语言；
- [ ] 最低支持 Android 版本；
- [ ] 是否仅支持手机，还是首版也承诺平板适配。

### 建议默认值

```text
displayName: ShaveBuddy
applicationId: com.<publisher>.shavebuddy   # 必须替换 publisher
namespace: 与 applicationId 相同
minSdk: 26
compileSdk: 当前最新稳定 SDK
targetSdk: 36 或更高
首发设备: Android 手机
首发语言: 简体中文
屏幕方向: 不强制锁定；先保证手机竖屏体验，避免破坏旋转恢复
```

`applicationId` 应使用自己可长期控制的发布者命名空间，不要保留示例值。应用上传商店后再更改标识会被视为另一个应用。

2026 年 8 月 31 日起，Google Play 新应用和更新需要面向 Android 16 / API 36 或更高版本。如果可能发布到 Google Play，应从项目初始化时就按这一要求配置。

### 分发渠道需要回答的问题

```text
[ ] Google Play
[ ] 中国大陆应用商店
[ ] GitHub / 官网 APK
[ ] 仅个人或小范围测试
```

如果目标包含中国大陆用户，应在至少一台常见国产 Android 设备上验证 WorkManager 和通知行为；不能只用模拟器或单一系统设备得出结论。

### 决策记录模板

```text
显示名称：
applicationId：
首发渠道：
首发语言：
minSdk：
目标设备：
决定日期：
```

## 4. 阻塞三：准备 Android 开发环境

### 当前机器检查结果

2026-08-15 检查到：

- macOS 26.6.1，Apple Silicon `arm64`；
- `/Applications` 中未发现 Android Studio；
- `adb` 不在当前 PATH；
- `sdkmanager` 不在当前 PATH；
- 系统存在 Temurin JDK 25.0.2。

JDK 25 的存在不代表 Android Gradle 构建已经配置正确。项目应优先使用 Android Studio 自带的 JetBrains Runtime，并让 Gradle JDK 使用 `GRADLE_LOCAL_JAVA_HOME`。工程内显式声明与所选 Android Gradle Plugin 兼容的 Java toolchain，不依赖全局 JDK。

### 安装清单

- [ ] 安装最新稳定版 Android Studio for Apple Silicon；
- [ ] 通过 Setup Wizard 安装 Android SDK；
- [ ] 安装 Android SDK Platform 36 或更新的商店目标版本；
- [ ] 安装对应 SDK Build Tools；
- [ ] 安装 Android SDK Platform-Tools；
- [ ] 安装 Android Emulator；
- [ ] 创建至少一个 API 36 手机模拟器；
- [ ] 启动模拟器并确认能进入系统；
- [ ] 准备至少一台开启开发者选项和 USB 调试的 Android 真机；
- [ ] 在 Android Studio 中确认 Gradle JDK 使用项目默认或内置 JBR。

### 验证方式

安装完成后确认以下操作可执行：

```shell
adb version
adb devices
```

工程初始化后再确认：

```shell
./gradlew --version
./gradlew tasks
./gradlew assembleDebug
```

验证目标：

- [ ] `adb devices` 能看到模拟器；
- [ ] `adb devices` 能看到已授权真机；
- [ ] Gradle 使用的 JVM 与 Android Studio 配置一致；
- [ ] 一个空 Compose 应用可以构建并分别安装到模拟器和真机；
- [ ] 不需要依赖系统全局 JDK 25 才能完成构建。

## 5. 阻塞四：建立 Git 开发基线

当前 README、产品文档和 `.DS_Store` 都处于未跟踪状态。在生成 Android 工程前先建立清晰基线。

### 待办

- [ ] 添加适用于 macOS、Android Studio、Kotlin 和 Gradle 的 `.gitignore`；
- [ ] 忽略 `.DS_Store`；
- [ ] 忽略 `.idea/` 中不应共享的本地状态；
- [ ] 忽略 `local.properties`；
- [ ] 忽略 `.gradle/`、`build/` 和各模块构建目录；
- [ ] 确认签名文件、密钥和本地凭据不会进入 Git；
- [ ] 提交 README、产品文档和技术决策；
- [ ] 确认 `git status` 干净；
- [ ] Android 空工程使用独立提交。

建议提交边界：

```text
docs: define Android-first technical direction
chore: initialize native Android project
```

## 6. 阻塞五：补齐低保真页面流程

不需要先做高保真设计，但以下流程必须能用方框和箭头说明。

### 必须覆盖的流程

- [ ] 首次启动 → 创建装备 → 安装首枚耗材 → 今天页；
- [ ] 今天页 → 一键记录 → 状态更新；
- [ ] 今天页 → 补记历史日期；
- [ ] 日历 → 日期详情 → 编辑或删除记录；
- [ ] 装备 → 更换耗材 → 查看旧周期；
- [ ] 设置 → 开启提醒 → 解释用途 → 请求系统权限；
- [ ] 通知权限被拒绝 → 应用内状态提示 → 前往系统设置；
- [ ] 导入文件 → 校验摘要 → 确认覆盖 → 成功或完整回滚。

### 每个页面至少定义的状态

| 状态 | 需要回答的问题 |
| --- | --- |
| 首次/空状态 | 用户下一步应该点哪里？ |
| 正常状态 | 核心信息和主操作是什么？ |
| 接近阈值 | 如何提醒但不制造焦虑？ |
| 达到阈值 | 如何建议更换并允许用户忽略？ |
| 权限拒绝 | 核心功能如何继续使用？ |
| 加载状态 | 是否真的需要阻塞式加载？ |
| 错误状态 | 数据是否已保存，用户如何恢复？ |
| 破坏性确认 | 删除、更换或导入覆盖会影响哪些数据？ |

### 最低交付物

以下任一种即可：

- 一份 Markdown 页面流程；
- 手绘草图照片；
- Figma 低保真线框；
- 可点击但无视觉细节的原型。

重点是状态和跳转，不是颜色、阴影和图标。

## 7. 阻塞六：冻结阶段 0 技术验证

阶段 0 只验证高风险平台能力，不实现正式产品页面。

### 验证范围

- [ ] 创建 Kotlin + Compose 空项目；
- [ ] 配置 Material 3 和三个占位导航目的地；
- [ ] 创建一张临时 Room 表；
- [ ] 完成一次写入、查询和应用重启后的持久化验证；
- [ ] 创建一条可替换的唯一 WorkManager 任务；
- [ ] Worker 到时通过 NotificationManagerCompat 展示通知；
- [ ] 修改业务时间后取消或替换旧任务；
- [ ] 验证通知权限允许和拒绝两种情况；
- [ ] 在模拟器和至少一台真机上验证；
- [ ] 为日期规则覆盖跨日、月底、闰年和时区测试。

### 明确不属于阶段 0

- 正式数据库 Schema；
- 正式首页视觉；
- 完整月历；
- 导入导出实现；
- 多设备同步；
- 发布签名；
- 商店素材；
- iOS 或 Kotlin Multiplatform 工程。

### 退出条件

只有同时满足以下条件，才进入正式 MVP 开发：

1. Compose 导航在模拟器和真机正常工作；
2. Room 数据在应用进程重启后仍然存在；
3. Room Migration 测试能够执行；
4. WorkManager 任务可以唯一替换和取消；
5. 通知在真机上能够展示，拒绝权限时应用不会崩溃；
6. 允许系统产生时间浮动，不申请精确闹钟特殊权限；
7. 日期纯函数测试通过；
8. `assembleDebug`、单元测试和 Android Lint 通过；
9. 阶段 0 的临时代码被明确标注为保留、重写或删除。

## 8. 不阻塞开工的事项

以下内容可以在阶段 0 之后再决定：

- 正式应用图标和启动画面；
- 最终颜色、字体、插画和动画；
- 应用商店截图和介绍文案；
- 发布签名与 Play App Signing；
- 隐私政策最终文本；
- 英文翻译；
- 崩溃分析和产品埋点；
- Hilt 与多模块拆分；
- 云同步、账号、远程推送；
- 小组件、手表与平板专用布局。

这些事项不应被带入阶段 0，也不应成为延迟技术验证的理由。

## 9. 最终 Go / No-Go 清单

以下项目全部完成后，状态改为 `Ready for Stage 0`：

- [ ] Android 原生技术方向已记录并与其他文档一致；
- [ ] `applicationId`、分发渠道、语言、minSdk 和目标设备已确定；
- [ ] Android Studio、SDK、模拟器和真机可用；
- [ ] Git 文档基线已提交且工作区干净；
- [ ] 六条核心页面流程已有低保真说明；
- [ ] 阶段 0 范围和退出条件已经冻结；
- [ ] 没有把非阻塞事项提前加入阶段 0。

当前结论：**No-Go，完成上述阻塞项后进入阶段 0。**

## 10. 官方参考

- [安装 Android Studio](https://developer.android.com/studio/install)
- [Android 构建使用的 Java/JDK](https://developer.android.com/build/jdks)
- [Android 应用架构建议](https://developer.android.com/topic/architecture/recommendations)
- [Jetpack Compose](https://developer.android.com/compose)
- [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)
- [Room](https://developer.android.com/training/data-storage/room/)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [WorkManager 与持久任务](https://developer.android.com/develop/background-work/background-tasks/persistent)
- [Google Play Target API 要求](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-AU)
