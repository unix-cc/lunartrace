# CLAUDE.md — 月迹（Lunartrace）仓库入口

> 本文件是仓库**唯一入口**。`AGENTS.md` 是它的硬链接（同一份内容，避免双份漂移）。
> 本文只写"仓库特有、且会改变实现决策的约束"。判据：不写这句，实现会做错什么？

## 0. 这是什么

单人开发的 Android 月经 / 症状记录应用：**纯离线、零网络、本地优先**。
技术栈：Kotlin + Jetpack Compose + Room + DataStore，单模块 `:app`。minSdk 26。

## 1. 事实入口表（要什么 → 去哪找）

| 我要知道 | 去哪找 |
|---|---|
| 这个词是什么意思 | `CONTEXT.md` |
| 这次要改什么、改到什么程度算完 | `specs/changes/<slug>/spec.md` |
| 某个能力长期怎么工作（高频陷阱） | `specs/capabilities/` |
| 为什么当初这么决定 | `docs/adr/` |
| 日常怎么干活、什么算小改 | `docs/agents/workflow.md` |
| change spec / ticket 的本地约定 | `docs/agents/issue-tracker.md` |
| 领域文档写法（禁止复制 capability） | `docs/agents/domain.md` |
| 可用 skill 与准入规则 | `docs/agents/skills.md` |
| 全部命令 / 构建 / 发版 / 排障 | `DEVELOP.md` |
| 产品原则、品牌人格、反例 | `PRODUCT.md` |
| 评审与自评产物 | `docs/reviews/` |

## 2. 硬约束（按模块分节）

### 2.1 隐私与数据 —— 优先级最高，违反即事故

- ✅ 清单不声明任何联网权限，数据只留在设备本地。
  - ❌ 声明 `INTERNET` 或引入任何上报 SDK → 产品定位崩塌，健康数据被未经同意外传。
- ✅ 健康数据（经期日期、症状、心情、PIN）**永不**进日志、异常消息、Intent extra、`SharedPreferences` 明文。
  - ❌ `Log.d("cycle", date.toString())` → 数据进 logcat，本机任意应用可读。
- ✅ `app/src/main` 内完全不使用 `android.util.Log` / `println` / `System.out` / `printStackTrace`，也不用 `SharedPreferences`（统一用 DataStore）。调试靠调试器，不靠日志。
  - ❌ "临时打个日志回头删" → 回头往往不会删；`scripts/check-privacy-guards.py` 会直接拦住提交。
- ✅ `android:allowBackup="false"`，不参与任何自动云备份。
  - ❌ 依赖系统默认备份 → 健康数据被自动上传到云端。
- ✅ PIN 只存 PBKDF2-HMAC-SHA256 哈希 + 每设备随机 salt；忘记 PIN 的唯一出路是清除数据。
  - ❌ 明文 / 可逆加密 / 预留"万能码" → 后门就是漏洞。
- ✅ 应用锁开启时窗口加 `FLAG_SECURE`。
  - ❌ 不加 → 最近任务列表缩略图泄漏记录内容。

### 2.2 时间与日期 —— 最容易写错的业务基础

- ✅ "哪一天"一律用 `java.time.LocalDate`（本地日期）；持久化经 `epochDay`(Long) 转换器。
  - ❌ 用 `Instant.now()` / `System.currentTimeMillis()` 判断"今天" → UTC 与本地跨日错位，日历上出现错位标记。
- ✅ "今天"只能来自 `core/time/Today.kt`（可注入），UI 与 domain 不得直接调 `LocalDate.now()`。
  - ❌ ViewModel 里直接 `LocalDate.now()` → 预测不可复现，测试不可固定。
- ✅ 日期区间一律用两端闭区间的 `LocalDate` 表达，禁止用 `(startIndex, count)` 这种下标风格跨层传递。

### 2.3 架构

- ✅ 依赖方向单向：`ui → domain ← data`；`domain` 不 import 任何 `android.*`。
  - ❌ domain 里出现 `Context`/`Log`/`Toast` → 逻辑无法在 JVM 单测里验证，接缝丢失。
- ✅ ViewModel 只通过 `CycleRepository` 访问数据，不直接持有 DAO。
- ✅ 状态单向：`Room(Flow) → Repository(Flow) → ViewModel(StateFlow<UiState>) → Compose`；事件反向走 `ViewModel.onEvent()`。

### 2.4 构建与依赖

- ✅ 新增依赖必须在 `specs/changes/**/spec.md` 或本文件 §3 写一句理由。
- ✅ 只改必要文件，禁止顺手重构与全仓格式化。（此条为 **L3**，靠自律，别假装有门禁。）

## 3. 依赖理由（新增依赖前先读这里）

| 依赖 | 理由 |
|---|---|
| Compose BOM + material3 | 日历这类重复视图声明式实现成本最低 |
| Room (+KSP) | 结构化记录 + Flow 观察，日历/统计自动刷新 |
| DataStore Preferences | 锁开关与 PIN 元数据的键值存储，带 Flow |
| `java.time`（minSdk 26） | 本地日期语义开箱即用，免去 desugaring |
| 手动 DI（`AppContainer`） | 注入点 <10 个，Hilt 的注解处理成本大于收益 |
| detekt | L1 门禁需要可自动判定的坏味道/复杂度规则 |
| JUnit4 + coroutines-test | 单元测试跑在 JVM，CI 无设备也能执行 |

**不引入**：Retrofit/OkHttp（无网络）、图表库（自绘 Canvas）、任何崩溃上报 SDK、Hilt/Dagger、Robolectric。

## 4. 拒绝预防性过度设计（自我膨胀的刹车）

> 默认不为未来重构或假想风险新增重复契约清单、兼容层、预留抽象或重复防线；
> 只有明确具体事故、并说明现有机制为何挡不住时，才允许新增。

新增任何约束、目录或门禁前，先回答两句：**具体是什么事故？现有机制为什么挡不住？** 答不出就别加。

## 5. 按需深读（**不要**默认通读全文）

- 改预测算法 → `specs/capabilities/cycle-prediction.md`（全文很短）+ `docs/adr/0003`
- 改表结构 / 持久化 → `specs/capabilities/local-data-model.md`
- 改锁、存储、清除数据 → `specs/capabilities/data-privacy.md` + `docs/adr/0002`
- 新增/修改 Compose 界面 → `specs/capabilities/android-engineering.md` 的"Compose 与状态"一节
- 不确定流程 → `docs/agents/workflow.md`
- 不确定该不该写 ADR → `docs/adr/0001` 的准入三门槛

## 6. 门禁命令（对应改动落在哪）

| 改动落在 | 命令 |
|---|---|
| `app/**` | `./gradlew detekt testDebugUnitTest assembleDebug` |
| 隐私相关（清单 / data / security） | `python scripts/check-privacy-guards.py` |
| `docs/**`、`specs/**` | `python scripts/check-docs.py` |
| 任意提交 | 提交信息 `type(scope): 描述` |
