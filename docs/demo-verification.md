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
