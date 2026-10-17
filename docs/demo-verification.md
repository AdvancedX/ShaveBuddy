# Android 手动剃须刀 Demo 验证记录

日期：2026-10-03

## 验证环境

- macOS Apple Silicon；Gradle 9.3.1，构建 JVM 为 Temurin JDK 25.0.2；Java/Kotlin 字节码目标 17。
- AGP 9.1.1，compile/target API 37，Build Tools 36.0.0，最低 API 26。
- `Medium_Phone_API_37.0` Android 17 ARM64 模拟器，Emulator 37.2.12。
- APK：`app/build/outputs/apk/debug/app-debug.apk`，约 18 MB，debug 签名，仅用于测试。

## 自动验证

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug lintDebug
git diff --check
```

全部命令成功。第二次设备测试用于确认测试隔离和重复运行；最后一次构建/Lint 用于确认补齐旧版 Android 备份配置后的安装包。

| 验证 | 结果 | 覆盖 |
| --- | --- | --- |
| 纯 Kotlin 单元测试 | 8 通过，0 失败 | 同日多次、旧周期排除、无记录、次数/天数阈值、关闭目标、删除重算、闰年/月末、夏令时、周期边界、非法设置 |
| 真实 Room 设备测试 | 3 通过，0 失败 | 初始化、重复写入、更换保留历史、补记归属、删除、数据库关闭重开、非法操作回滚、修改目标与间隔、同日多次更换 |
| Compose 设备测试 | 2 通过，0 失败 | 设置→记录→日历→更换→删除→设置保存返回；SQL trigger 实际阻止写入→显示错误→重新读取→人工再次记录 |
| 再次执行设备测试 | 5 通过，0 失败 | 每个 UI 测试使用唯一数据库文件，测试不读取/删除生产数据库 |
| assembleDebug | 通过 | 生成可安装 APK |
| lintDebug | 0 error，11 warning | warning 全部为依赖有新版本提示，当前组合保持锁定；无应用功能/备份/图标警告 |
| git diff --check | 通过 | 无新增空白错误 |

测试报告位于 `app/build/reports/tests/testDebugUnitTest/`、`app/build/reports/androidTests/connected/debug/`、`app/build/reports/lint-results-debug.html`（构建生成，不入 Git）。

规则测试在占位实现下先出现 7 个预期断言失败，规则实现后 8 项通过。写入恢复测试先因没有“重新读取”动作失败，修正文案与动作后通过。

## 实际安装包检查

安装最终 APK，冷启动打开首次设置页，检查中文布局与控件；完成默认手动装备设置后检查今天页面。记录一次，页面显示“1 次”“今天已记录 1 次”，下次日期为 2026-10-05。

通过 `am force-stop` 终止进程并重新冷启动，上述次数与下次日期仍存在。将模拟器旋转为横屏后恢复竖屏，统计和日期仍正确；已恢复模拟器原有自由旋转设置。模拟器中保留本次冒烟检查创建的装备与一条剃须记录，便于直接体验。

## 发现与修正

- Compose 间接依赖 Espresso 3.5.0 在 API 37 初始化输入管理器时失败，显式锁定 Espresso 3.7.0 后全部设备测试通过。依据 Google Maven 官方发布源码中对不可用接口的降级处理。
- UI 测试改为注入独立真实 Room 数据库，避免依赖或影响实际 Demo 数据。
- 错误恢复按钮使用“重新读取”，提示先检查当前状态，再决定是否重复原操作，避免误称已重试或自动重复写入。
- Room 当前周期使用唯一索引，初始化/更换使用事务，历史事件保持原周期；本次未发生 schema 版本升级。

独立代码审查与修正复查没有剩余 Critical/Important 问题。

## 尚未验证与后续范围

- Android 真机、API 26、国产厂商设备、设备重启和完整大字体/无障碍回归；
- Room 第一次版本升级的 Migration 测试；
- 通知权限、WorkManager 重排、提醒可靠性；
- 导入导出、备份恢复、多装备、评分备注与历史编辑；
- 正式 applicationId、发布签名、商店与 iOS。

以上不属于本次基本 Demo，不能从模拟器结果推断正式发布质量或宣称完整阶段 0 已通过。

## 回滚检查点

- `1d2c5c9`：冻结手动剃须刀 Demo 范围和实现计划。
- `a2046e0`：Android 工程、锁定依赖、领域模型与规则测试。
- `e0ebf77`：Room 存储、schema 与数据库设备测试。
- `517d5a4`：三个页面、交互闭环、错误恢复、备份配置与流程测试。
- 本文与更新后的 README/技术方案/路线图作为最后一个文档提交；完整历史可用 `git log --oneline` 查看。

开发分支：`feat/manual-razor-demo`。保留开发前已有的 `docs/pre-development-blockers.md`、`.DS_Store` 与 `package-lock.json` 修改，未将它们混入 Demo 提交。

## 2026-10-03 补记日期修复

复现：默认安装日期为今天，TodayScreen 将其作为补记日历的最早可选日，导致过去日期全部禁用。新增 UI 回归测试在旧版对过去日期的 `assertIsEnabled` 断言失败。

修复：补记日期选择器允许过去日期，仍禁用未来日期。若早于首次安装日，明确展示日期调整确认；确认后将首次刀片安装日向前调整并补记，取消不变更数据。日期调整与插入在同一 Room 事务内完成，旧记录归属不变，不需要数据库升级。

验证命令：

```sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
```

结果：8 个单元测试、8 个设备测试（3 个 UI / 5 个 Room）通过；构建通过，Lint 无错误。新增覆盖日期可选择、取消与确认、关联首次历史周期、保留既有记录、未来日期拒绝和真实插入失败后的日期回滚。独立审查无 Critical/Important 问题。

本次直接执行 instrumentation，避免 Gradle 设备测试任务的卸载清理影响试用数据；修复版使用覆盖安装。

## 2026-10-03 菜单顺序与日历布局调整

底部顺序改为“日历 / 今天 / 装备”，“今天”居中，默认页与系统返回行为保持一致。日期行和星期标题使用相同的 6dp 横向间距，日期行之间为 8dp 间距；日期格点击效果裁剪到各自圆角内。

`./gradlew assembleDebug assembleDebugAndroidTest lintDebug` 成功，直接运行 `DemoFlowTest` 的 3 个 UI 流程全部通过。覆盖安装后检查实际截图：默认今天居中，左侧菜单进入日历，日期格之间已有清晰的横纵间隔，星期标题对齐，原有记录仍保留。


## 2026-10-10 设置、应用内更新与日期标题

在基于 `feat/manual-razor-demo` 的独立托管工作区完成，原 main 未提交改动保持原样。

最终命令：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest connectedDebugAndroidTest
git diff --check
```

34 个单元测试、18 个设备测试通过；构建成功，Lint 0 errors / 17 warnings。新测试覆盖数值版本与预发布筛选、无 APK 与非法来源、长度/摘要/大小上限、空文件和中断删除、包名/版本号/签名拒绝、显式联网和更新状态、取消后完成文件清理、设置导航和日历状态恢复、窄屏及两倍字体的星期完整性、真实 PackageManager 和 FileProvider。

TDD 记录：版本与校验占位实现产生 17 个预期失败，状态机占位实现产生 5 个预期失败；日期测试在原排版捕获“星 / 期六”和孤立“六”。审查新增的排队调度器回归测试在旧代码中捕获“文件完成但结果尚未投递时取消”留下 APK 的问题，修复后通过。

修正现有 UI 测试中已过期的“还没有记录”“调整装备与目标”文案选择器，改从对应资源读取；新日历测试通过 selectable 语义区分标题和底部标签。一次 Gradle 设备测试进程意外终止后，直接 instrumentation 的日期测试通过；最终完整 Gradle 套件再次成功。

实际 APK：0.1.3-demo / versionCode 4，包名及签名与公开旧包一致。实际 GitHub 查询收到旧标签，明确显示暂无符合规则的包和重试；没有误报已是最新。浅色今天/设置与大字体深色今天截图保存在 `artifacts/0.1.3-demo/`，不入 Git；已恢复模拟器 font_scale=1.0、night=no。

尚未完成：真机、API 26/厂商权限界面、新版本实际下载后的系统安装确认/取消、升级后本地记录保留。没有修改数据库 schema，没有发布 Release。

详细安装包与发布约定见 `docs/releases/0.1.3-demo.md`。独立审查复查无剩余 Critical/Important 问题。

## 2026-10-10 剃须满意度

记录成功后展示四项独立可选评分：总体、剃净度、顺滑度、皮肤状况。三档按“不满意 / 一般 / 满意”排列；默认未评分，可重复点击取消、逐项跳过、全部跳过或返回关闭。剃须先保存，评分单独按事件 ID 更新，失败重试不重复记录。日历日期格显示当天最近一次已填写的总体评分，选择日期可查看每条记录的四项评分。

Room 升级至 v2，Migration 仅新增四个可空评分列。设备测试用导出的 v1 schema 和实际旧数据建库，验证升级保留装备、周期、事件 ID、时间与使用次数，旧评分均为 NULL；另验证部分评分在更换刀片及数据库关闭重开后保持归属，删除后不存在的记录不能被评分。

验证命令：

```sh
ANDROID_HOME=/Users/zheruixu/Library/Android/sdk ./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e class com.shavebuddy.demo.DemoFlowTest,com.shavebuddy.demo.RepositoryTest com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
git diff --check
```

结果：8 个单元测试、12 个设备测试（5 个 UI / 7 个 Room）全部通过，构建成功，Lint 无错误，14 项 warning 为依赖更新、已有 launcher icon 和 SDK 检查提示。本次使用独立临时 API 37 模拟器与独立数据库，避免共享模拟器被另一组测试替换应用造成干扰。

新增 UI 测试验证部分评分、默认未评分、逐项跳过、重复点击取消、返回关闭、同日最新评分、后续未评分不覆盖已有评分、补记后评分、真实 SQL 更新失败后重试不重复、失败后跳过不显示通用错误提示。两个旧 UI 断言改用当前字符串资源，修正之前残留的旧文案。

评分面板测试先因缺少面板失败，迁移测试先因数据库版本仍为 1 失败；失败后跳过的回归先复现通用错误提示残留，修复后通过。独立代码审查和修正复查无剩余 Critical/Important 问题。

已查看真实模拟器截图，评分面板四项与按钮均完整显示，日历格表情与详情评分正确。截图保存在构建产物 `app/build/verification/`，不入 Git。尚未验证真机、API 26、横屏大字体和评分草稿的进程恢复；进程退出后未保存评分不恢复，已保存剃须记录仍保留。

## 2026-10-16 0.1.3-demo 整合发布验证

在独立 `codex/release-0.1.3` 工作区合并设置/应用内更新与剃须满意度。三个冲突分别为字符串追加、UI 测试滚动和验证文档，保留两边功能及测试；原主目录未提交文件保持原样。

使用 JDK 25、SDK Platform 37 和 Build Tools 36.0.0 执行：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --offline
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
git diff --check HEAD
```

结果：34 项单元测试与 22 项 API 37 设备测试全部通过，构建成功；Lint 0 errors、6 warnings。独立整合审查无 Critical/Important 问题。

另建临时 API 37 模拟器，安装从 GitHub 下载的实际 0.1.2-release APK，完成默认装备设置并记录一次剃须。保存旧数据库后，以最终 0.1.3 APK 执行 `adb install -r`，启动应用触发 Room v1→v2 迁移。界面保留 1 次旧记录；再记录一次并保存总体满意，设置页显示 0.1.3-demo。导出升级后的临时测试数据库，以 SQL EXCEPT 比对全部旧装备、周期和事件字段，丢失数量均为 0；旧事件四项评分均为 NULL，新事件总体评分为 3。评分面板截图已检查，保存于主目录 `artifacts/0.1.3-demo/upgrade-rating.png`。

最终 APK SHA-256 为 `a8e45717a0d85a613c25706ca721da976b4d54806dc5f79ce4e81c3bd2332329`；新旧 APK 证书 SHA-256 均为 `d877770c3fa0d25a5ae03c0c9c669731d607b7810c10bb1309163d60d6f82cb0`。APK 与校验文件为构建产物，不入 Git。

此次实际覆盖升级使用 ADB 安装，未实测应用内真实下载→系统授权→安装完整链路；Android 真机、API 26 和厂商权限界面仍待验证。

## 2026-10-17 0.1.4-demo 满意度滑杆与发布验证

满意度面板改为四条滑杆：整体感受使用较粗轨道和较大圆点，三个子项使用更细轨道和较小圆点。加入引导、两端含义和当前评价标签。未填写是独立空值，不占据最低评分位置；点击清空才恢复空值，首次点选中间档也会保存为一般。数据库仍为 v2，评分继续使用既有的可空 1–3 编码。

先运行回归测试，旧实现拖回最左端时错误保存 NULL，期望的最低评分断言失败；分离空值和最低档后通过。新增及更新的测试同时覆盖全空保存、部分评分、触摸拖动、清空、首次点选中间档、状态恢复、忙碌禁用与全部跳过。

在最新主分支 bbbcb6e 基础上准备 0.1.4-demo / versionCode 5，执行：

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest --offline
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.shavebuddy.demo.test/androidx.test.runner.AndroidJUnitRunner
git diff --check
```

结果：34 项单元测试、29 项 API 37 设备测试通过，构建成功；Lint 为 0 errors、6 项既有 warnings。设备测试使用独立数据库，采用覆盖安装和直接 instrumentation，未执行会卸载应用的 connectedDebugAndroidTest。

另外验证浅色、深色和 320dp / 1.5 倍字体的评分流程，截图已检查，模拟器显示设置已恢复。独立代码审查与发布审查未发现阻塞问题。

实际安装包的 versionName 为 0.1.4-demo、versionCode 为 5；SHA-256 为 `7a2c157922cbb7bd8f6be6cf9f43fe919e4973c65ed8605235819b990ac3f185`。通过 apksigner 核对，新包和已发布 0.1.3 APK 的证书 SHA-256 均为 `d877770c3fa0d25a5ae03c0c9c669731d607b7810c10bb1309163d60d6f82cb0`。APK 与 SHA256SUMS 保存在主目录 artifacts/0.1.4-demo，不入 Git。

本次仍未验证 Android 真机、API 26、厂商安装权限界面及真实应用内下载至安装确认的完整链路。
