# ShaveBuddy 技术方案与架构决策

状态：暂定采用，完成双端技术验证后转为正式决策  
日期：2026-08-15

## 1. 决策摘要

MVP 暂定采用 **React Native + Expo + TypeScript** 构建 Android / iOS 单代码库应用。

采用本地优先架构：业务数据存入设备 SQLite，提醒使用系统本地通知，不建设后端，不要求账号。开发开始前先做一个可丢弃的纵向技术验证，同时在 Android 和 iOS 真机上验证数据库持久化、通知调度/取消和基础导航。

这是针对当前约束的工程选择，不是对所有跨平台技术的普遍排名。

## 2. 已知约束

- 开发者有后端和简单前端经验，但没有移动应用开发经验；
- 需要同时覆盖 Android 与 iOS；
- 产品主要由表单、列表、日历、日期计算、本地存储和通知组成；
- MVP 没有重度动画、音视频、蓝牙或持续后台任务；
- 预期先由一人维护，学习成本和双端一致性比极致原生定制更重要；
- 核心功能应离线工作，短期没有后端存在的必要。

## 3. 方案比较

评分为针对本项目的相对判断：5 最有利，1 最不利。

| 方案 | 单人首版速度 | 现有知识复用 | 平台能力 | 双端维护成本 | 本项目综合判断 |
| --- | ---: | ---: | ---: | ---: | --- |
| SwiftUI + Kotlin/Compose 双原生 | 1 | 1 | 5 | 1 | 同时学习两套语言、UI 与构建系统，首版成本过高 |
| Flutter + Dart | 4 | 2 | 4 | 5 | 成熟的一体化体验，但需要同时学习 Dart 与 Flutter 范式 |
| React Native + Expo + TypeScript | 5 | 4 | 4 | 5 | 与前端知识衔接最好，所需本地能力均有官方模块 |
| Kotlin Multiplatform / Compose Multiplatform | 2 | 2–3 | 5 | 4 | 灵活且原生能力强，但 Gradle、Xcode 和跨平台边界增加首个移动项目的认知负担 |

### 为什么选择 Expo 路线

- React Native 官方对新项目建议从框架开始，并把 Expo 作为推荐方案；
- TypeScript、React 组件和前端调试方式更接近已有经验；
- `expo-sqlite` 可持久保存结构化本地数据；
- `expo-notifications` 同时支持 Android / iOS 的本地通知调度与取消；
- 文件式路由、开发构建和配置插件减少直接维护原生工程的早期成本；
- 当确实需要原生能力时仍可生成原生项目或编写模块，不构成永久封闭。

### 为什么当前不选 Flutter

Flutter 同样完全能胜任，并且官方已有清晰的应用架构建议。当前不选它的主要原因不是能力不足，而是本项目业务简单，TypeScript/React 的知识复用收益大于学习 Dart 与新 UI 体系的收益。

若开发者过往前端经验并非 React/TypeScript，或技术验证中 Expo 的日期、日历、通知存在不可接受的问题，应安排同等范围的 Flutter 小样再复核决策。

### 为什么当前不选 KMP 或双原生

它们更适合已有原生团队、需要大量平台特化，或必须渐进共享既有原生代码的场景。本项目没有既有移动代码，使用这些方案会先承担两端工程与工具链复杂度，却暂时得不到相称收益。

## 4. MVP 技术栈

| 领域 | 选择 | 说明 |
| --- | --- | --- |
| 应用框架 | React Native + 最新稳定 Expo SDK | 初始化时锁定确切版本和 lockfile，不在文档中写死会快速过期的版本 |
| 语言 | TypeScript，开启 strict | 让日期、可空阈值和导入数据错误尽早暴露 |
| 路由 | Expo Router | 三个一级页面加少量详情/模态页面，文件路由足够清晰 |
| UI | React Native 基础组件 + 自有设计 token | MVP 不引入大型 UI 框架；优先系统行为、动态字体和可访问性 |
| 数据库 | `expo-sqlite` | 数据量小但关系明确，SQLite 比键值存储更适合历史查询和迁移 |
| 数据访问 | 薄 Repository 层 + 参数化 SQL | MVP 表少，先避免 ORM、代码生成和额外构建配置；通过接口保留替换空间 |
| Schema 迁移 | 有序 SQL 迁移 + `PRAGMA user_version` | 每次升级可重复、可测试，不在运行时临时改表 |
| 通知 | `expo-notifications` 本地通知 | 不申请推送 token，不依赖服务端 |
| 状态管理 | 页面局部状态 + Repository 查询 + 轻量 Context | 暂不引入 Redux；出现跨页面复杂客户端状态后再评估 Zustand 等方案 |
| 日期时间 | 标准 `Date` + 小型纯函数边界 | 不提前引入庞大日期库；发现时区/本地化缺口时再选择专用库 |
| 数据导入校验 | 小型显式解析器或轻量 schema 校验库 | 只在不可信的导入边界校验，不把数据库模型直接暴露给文件格式 |
| 测试 | 业务规则单元测试、Repository 集成测试、关键页面组件测试 | 端到端工具在发布阶段根据真机流程再选 |

依赖原则：每个第三方包都必须解决当前问题；能用 Expo 官方模块或少量清晰代码完成的功能，不为了“以后可能需要”增加依赖。

## 5. 建议目录结构

```text
app/                         # Expo Router 路由，只负责页面组合
  (tabs)/
  gear/
  settings/
src/
  features/
    dashboard/
    shave-log/
    calendar/
    gear/
    settings/
  domain/
    models/
    rules/                   # 日期、寿命状态、下次剃须等纯函数
  data/
    db/
      migrations/
    repositories/
  services/
    notifications/
    import-export/
  ui/
    components/
    theme/
  shared/
assets/
tests/
docs/
```

原则是“按用户功能组织 UI，按稳定边界组织领域和数据层”。路由文件保持薄，不在页面组件中直接拼 SQL 或调度通知。

## 6. 数据模型草案

字段命名只是架构草案，技术验证后再生成第一版迁移。

### `equipment`

- `id`: 文本 UUID；
- `name`: 用户定义名称；
- `kind`: `safety_razor`、`cartridge`、`electric`、`other`；
- `is_archived`；
- `created_at`、`updated_at`。

### `consumable_cycles`

代表一枚刀片、一个刀头或一次切割组件使用周期。

- `id`；
- `equipment_id`；
- `installed_on`: 本地日历日期 `YYYY-MM-DD`；
- `retired_on`: 可空的本地日历日期；
- `target_uses`: 可空正整数；
- `target_days`: 可空正整数；
- `created_at`、`updated_at`。

同一件装备同一时刻只能有一个未结束周期。第一版由 Repository 的事务保证，并用数据库约束尽可能防止重复。

### `shave_events`

- `id`；
- `consumable_cycle_id`；
- `occurred_at`: UTC 时间戳；
- `local_date`: 发生时的 `YYYY-MM-DD`，用于历史日历稳定显示；
- `time_zone`: 发生时的 IANA 时区；
- `comfort_rating`: 可空，1–5；
- `note`: 可空，设合理长度上限；
- `created_at`、`updated_at`。

### `preferences`

MVP 可使用单行设置表，保存：

- 默认装备；
- 偏好剃须间隔天数；
- 提醒时间和开关；
- 主题、语言。

### `scheduled_notifications`

保存业务提醒与系统通知任务之间的映射：

- `id`；
- `business_key`: 例如某装备的 `next_shave` 或 `replace_by_date`；
- `platform_notification_id`: 系统 API 返回的任务标识；
- `scheduled_for`；
- `created_at`。

该表不是通知是否最终展示的事实记录，只用于在业务数据变化时精确取消和重新安排待处理任务。

## 7. 业务与副作用边界

以下规则实现为无平台依赖的纯函数：

- 当前耗材使用次数；
- 使用天数；
- 是否达到次数或日期阈值；
- 最近一次剃须；
- 下一次建议日期；
- 月历中应标记的日期集合。

数据库写入成功后，再通过单独的通知服务同步系统提醒：

```text
用户动作
  → Repository 事务写入
  → 重新读取领域状态
  → 计算下一组提醒
  → 取消相关旧通知
  → 调度新通知
  → 刷新页面
```

如果通知调度失败，记录仍应保存，并在设置页显示可恢复的提醒状态；不能因为通知失败回滚用户历史。

## 8. 通知策略

- 只使用本地通知，不注册 APNs / FCM / Expo push token；
- 用户主动开启提醒时才申请系统权限，并先展示用途说明；
- 使用稳定的业务键映射通知任务，修改记录、更换耗材或更改设置后可取消旧任务；
- 产品默认接受系统允许的时间浮动，不要求日常护理提醒在某一分钟精确触发；
- 技术验证必须确认 Expo 的日期触发器能否在不申请 Android 精确闹钟权限时满足这一需求；若不能，应先验证非精确调度替代方案，而不是直接增加特殊权限；
- 文案应说明提醒时间是“大约”，避免承诺分钟级准确；
- 必须在 Android 与 iOS 真机验证权限拒绝、权限关闭、应用重启、设备重启及跨时区行为。

Android 官方建议大多数应用使用非精确闹钟，精确闹钟还涉及特殊权限和商店政策。Expo 文档也明确指出，Android 12 及以上若要精确触发，需要额外声明权限。ShaveBuddy 的提醒不属于必须在某一分钟触发的关键闹钟，因此把“无需精确闹钟权限”作为技术验证项，而不是未经验证的假设。

## 9. 本地优先与隐私

- SQLite 是设备内唯一事实来源；
- MVP 不包含账号标识、广告 SDK 和行为分析 SDK；
- 不把 SQLite 本地存储描述为端到端加密；
- 导出 JSON 包含明确的 `formatVersion`、`exportedAt` 和业务数据；
- 导入先完整校验，再在事务中写入；失败不留下半套数据；
- 导入覆盖现有数据属于破坏性操作，必须展示摘要并二次确认；
- 日后如加入崩溃报告或分析，需单独记录隐私决策并更新商店隐私声明。

## 10. 不建设后端的理由与触发条件

当前所有核心计算都在设备上完成，本地通知也不需要服务器。现在增加后端会同时引入认证、授权、同步冲突、隐私、部署和运维，却不增加核心价值。

只有出现下列经过验证的需求时，才重新评估后端：

- 用户明确需要跨设备同步或换机自动恢复；
- 需要多人共享同一份数据；
- 需要远程推送或服务端订阅；
- 需要付费权益跨设备校验；
- 经用户同意的聚合分析对产品决策确有必要。

届时应先设计同步协议和冲突规则，不应把本地 SQLite 简单替换成网络请求。

## 11. 架构护栏

- 页面组件不得直接调用 SQLite 和通知 API；
- 日期业务规则不得散落在 UI 文案中；
- 数据库迁移和导出格式分别版本化；
- 每次修改记录或耗材周期，都覆盖统计与通知重排测试；
- 不缓存可从记录可靠计算出的使用次数；
- 所有面向用户的字符串从第一天集中管理；
- 每个发布候选版本必须经过 Android 与 iOS 真机验证，模拟器不足以证明通知可靠。

## 12. 决策转正条件

只有技术验证同时满足以下条件，本决策才从“暂定采用”转为“正式采用”：

1. 同一项目能在 Android 与 iOS 启动并完成基础导航；
2. SQLite 数据在应用重启后存在，迁移可重复执行；
3. 两端真机都能申请权限、调度、触发和取消本地通知；
4. Android 能在不申请精确闹钟特殊权限的情况下提供可接受的提醒；
5. 编辑或删除记录后，旧通知不会残留；
6. 日期在跨日、夏令时样例和至少两个时区下计算正确；
7. 没有为了这些基础功能大量修改原生工程。

若失败，先判断是实现问题还是框架限制；确认是结构性限制后，再用相同验收项验证 Flutter，而不是直接开始双原生开发。

## 13. 官方依据

- [React Native：新项目建议使用框架，Expo 是推荐路线](https://reactnative.dev/blog/2024/06/25/use-a-framework-to-build-react-native-apps)
- [Expo：SQLite 本地持久化能力](https://docs.expo.dev/develop/user-interface/store-data/)
- [Expo：Android / iOS 通知调度 API](https://docs.expo.dev/versions/latest/sdk/notifications/)
- [Android：闹钟调度与精确闹钟权限](https://developer.android.com/develop/background-work/services/alarms)
- [Apple：在应用本地调度通知](https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app)
- [Flutter：官方应用架构指南](https://docs.flutter.dev/app-architecture)
- [Kotlin Multiplatform：共享逻辑或 UI 的官方说明](https://kotlinlang.org/docs/mpp-get-started.html)
