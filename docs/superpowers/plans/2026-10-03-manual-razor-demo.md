# Manual Razor Demo Implementation Plan

> **For agentic workers:** Use executing-plans to implement these tasks inline, with verification and incremental commits.

**Goal:** Android 本地手动剃须刀 Demo 能完成设置、记录、回顾与更换。

**Architecture:** 单 app module；Compose → ViewModel → Repository → Room。纯 Kotlin 规则计算日期、目标状态与周期匹配。

**Tech Stack:** AGP 9.1.1 / Gradle 9.3.1 / Kotlin 2.2.10 / Compose / Room / KSP。

**Spec:** `docs/superpowers/specs/2026-10-03-manual-razor-demo-design.md`

## Global Constraints

- 最低 API 26，编译和目标 API 37。
- 单个 app module；只覆盖单件手动装备。
- 无账号、网络权限、通知、多装备或导入导出。
- 同日多次允许；本地日期稳定；旧周期历史保留；统计不存累计字段。
- 分批 commit，只暂存本次开发文件。

## Task 1: 工程与规则

Files: Gradle wrapper/config、`app/src/main/AndroidManifest.xml`、`domain/Models.kt`、`domain/ShaveRules.kt`、`app/src/test/.../ShaveRulesTest.kt`。

- [x] 配置 Version Catalog 与构建；锁定版本，生成 wrapper。
- [x] 写规则测试：2 月 28 日加两天在闰年为 3 月 1 日；无记录 next=null；同日两条 currentUses=2；任一阈值达到 reached=true；历史不计入当前次数。
- [x] 运行 `./gradlew testDebugUnitTest`，确认规则缺失导致失败。
- [x] 实现 `summarize(snapshot, today)`、`cycleForDate(cycles, date)` 与 `validateSetup(...)`，返回稳定日期和派生统计。
- [x] 运行测试确认通过；提交工程与规则。

## Task 2: 本地存储

Files: `data/ShaveDatabase.kt`、`data/ShaveRepository.kt`、`app/src/androidTest/.../RepositoryTest.kt`；Room schema。

Interfaces: Repository 产生 `Flow<ShaveSnapshot>`，提供 setup/add/delete/replace/updateSettings suspend 方法；snapshot 包含装备、周期、记录。

- [x] 写真实数据库设备测试：初始化后两次记录计数 2；更换后 current=0 且 old=2；删除后重算；关闭再打开文件数据库数据仍存在；非法日期不能写入。
- [x] 运行设备测试确认 Repository 缺失时失败。
- [x] 实现实体、DAO、Room 数据库和 Repository 事务，导出 schema，不允许 destructive migration。
- [x] 运行单元测试与设备测试；提交本地存储。

## Task 3: 交互 Demo

Files: `MainActivity.kt`、`ui/ShaveViewModel.kt`、`ui/ShaveApp.kt`、`ui/SetupScreen.kt`、`ui/TodayScreen.kt`、`ui/CalendarScreen.kt`、`ui/GearScreen.kt`、`ui/Theme.kt`、`res/values/strings.xml`。

- [x] 写 Compose 流程测试：创建装备→记录→日历有记录→装备更换→当前次数归零→历史仍有记录。
- [x] 实现集中中文文案、系统深浅主题、三个入口、表单校验、日期选择、确认删除/更换、loading/error 状态。
- [x] 执行 `./gradlew testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest`。
- [x] 安装 APK，检查屏幕/旋转/进程重启；提交 UI。设备重启仍为后续真机验收。

## Task 4: 交付与文档

- [x] 更新 README、技术方向与路线图，解释旧 Expo 方案已被 Android Demo 替代及仍待验收的能力。
- [x] 记录验证命令、结果与真机限制，提供 APK 位置。
- [x] 检查 `git diff --check` 和最终状态；提交文档/验证结果。

## 执行记录

所有 Demo 交付任务完成。UI 自动测试采用独立数据库与测试宿主 Activity；最终 MainActivity 的旋转和进程冷启动通过实际安装包另行验证。错误恢复按审查修改为重新读取后人工决定是否重复操作。详细命令、结果与限制见 `docs/demo-verification.md`。
