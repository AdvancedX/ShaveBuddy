# ShaveBuddy 技术方案与架构决策

状态：Android Demo 已采用；正式 MVP 平台能力待验证
日期：2026-10-03

## ADR-001：Android 原生手动剃须刀 Demo

**Status: Accepted（Demo 范围）**

最新开工清单提出 Android 原生方向，用户要求先开发仅覆盖手动剃须刀的基本 Demo。当前实现采用 **Kotlin + Jetpack Compose + Material 3**，优先跑通本地核心循环。旧 Expo 双端方案保存在 [历史方案](archive/2026-08-15-expo-technical-direction.md)。

这个决策缩小首批平台与功能范围，便于在现有 Android Studio、API 37 模拟器环境上验证。代价是 Compose UI 不能直接复用到 iOS；当前不引入 KMP，iOS 技术路线以后重新评估。用户对 Demo 的授权不代表已完成通知或真机的正式验收。

## 已锁定技术栈

| 领域 | 实现 |
| --- | --- |
| 构建 | Gradle 9.3.1 / AGP 9.1.1 / Version Catalog |
| 语言 | AGP 内置 Kotlin 2.2.10，Compose 编译插件同版本，字节码目标 17 |
| SDK | minSdk 26 / compileSdk 37 / targetSdk 37 |
| UI | Compose BOM 2026.03.00 / Material 3；中文资源集中于 strings.xml |
| 状态 | ViewModel / StateFlow / lifecycle-aware collection |
| 业务数据 | Room 2.8.4 / KSP 2.3.12，schema 导出入 Git |
| 异步 | Kotlin Coroutines 1.10.2 |
| 日期 | java.time LocalDate / Instant / ZoneId |
| 导航 | Demo 使用可恢复的三个 tab 状态，系统返回回到今天；复杂页面再引入导航库 |
| 注入 | Application 中构造数据库与 Repository，构造器注入 ViewModel |
| 测试 | JUnit 4、真实 Room 设备测试、Compose 流程测试；Espresso 显式锁定 3.7.0 |

依赖版本为本次验证组合，不自动追随最新版。[AGP 官方兼容表](https://developer.android.com/build/releases/agp-9-1-0-release-notes)明确 AGP 9.1.1 支持 API 37，Gradle 9.3.1 和 Build Tools 36.0.0。

## 架构与数据

```text
Compose 页面 → ShaveViewModel → ShaveRepository → Room
                            ↓
                      ShaveRules（纯 Kotlin）
```

只有一个 `app` module。`domain` 包定义快照、周期、记录与日期/阈值规则；`data` 包定义 Room 实体、DAO 与事务；`ui` 按页面分文件。UI 不直接访问数据库。

数据库 v1 包含 `equipment`、`cycles`、`events`。Demo 装备 ID 固定为 1；周期 `activeSlot` 唯一索引保证只存在一个当前周期，历史周期为 NULL；记录使用外键关联周期。初始化与更换在事务内完成。当前没有旧发布 schema，因此无版本升级迁移；未来修改 schema 必须新增 Migration 和设备迁移测试，禁止 destructive migration。

Repository 按三张表的失效通知重新读取事务快照，避免页面读取到更换过程中的中间状态。计数不存累计值，每次从记录推导。每条剃须记录保存发生时本地日期、UTC 时间戳与时区，历史日期不随当前时区平移。

补记匹配安装日期（含）到结束日期（不含）的周期，同日更换后新补记归当前周期；既有记录保持原周期。同日允许多条记录。未来日期拒绝写入；补记早于首次安装日期时，先确认将首次安装日期前移，再在同一事务中调整日期并插入记录。未经确认仍拒绝早于安装日期的写入；失败时连同日期调整一起回滚。下次日期为最近本地记录日期加间隔，无记录不预测。

## 本地优先与边界

没有网络权限、服务端、账号或分析 SDK。系统备份/设备迁移排除本地数据；卸载或清除数据后无法恢复，直到后续完成用户控制的导出与恢复。

Demo 不实现 WorkManager、通知、DataStore、导入导出、评分备注、记录编辑或多装备。当前开工清单中的这些验证项保持待完成，不能据 Demo 结果声称正式平台决策的全部条件已满足。

## 复核条件

后续进入完整 MVP 前，必须验证 Room 版本升级、本地通知权限允许/拒绝、唯一任务重排、设备重启与真实 Android 手机行为。若这些能力在当前架构下不可接受，先记录实现与平台证据，再修改相应技术决策。iOS 在 Android 核心循环被实际使用后独立评估。

## 官方参考

- [AGP 9.1.1 兼容性](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [AGP 内置 Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Room](https://developer.android.com/training/data-storage/room)
- [Compose 测试](https://developer.android.com/develop/ui/compose/testing)
- [AndroidX Test 发布记录](https://developer.android.com/jetpack/androidx/releases/test)
