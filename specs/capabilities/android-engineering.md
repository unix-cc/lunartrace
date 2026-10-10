# Android 工程规范 / 高频陷阱清单

> 用法：动 `app/` 下任何代码前，对照与你的改动相关的小节。
> 本文只管"Kotlin / Compose / 架构写法"。隐私红线在 `data-privacy.md`，日期语义在 `local-data-model.md`。

## 1. 时间与"今天"（最高频 —— 务必先看）

- ✅ 所有"哪一天"用 `java.time.LocalDate`；"今天"从 `Today` 注入（`core/time/Today.kt`）。
  - ❌ ViewModel / Composable 里直接 `LocalDate.now()` → 预测不可复现，单元测试无法固定日期，且跨时区/跨日边界的行为不确定。
- ✅ 日期比较与算术用 `LocalDate.plusDays/isBefore`。
  - ❌ 把日期转成 `Instant`/毫秒后做差值 → 夏令时与 UTC 偏移会让"相隔天数"差 1。
- ✅ 跨层传递日期区间用闭区间（`start..endInclusive`）。
  - ❌ 传 `(start, length)` → 调用方各自 +1/-1，off-by-one 无法集中修。

## 2. Compose 与状态

- ✅ ViewModel 暴露单一不可变 `StateFlow<XxxUiState>`（data class）；Screen 用 `collectAsStateWithLifecycle()`。
  - ❌ `collectAsState()` → 后台时仍持有收集，浪费且可能触发无用重组。
- ✅ 多个字段派生出的状态在 ViewModel 里组合成 UiState 一次算完。
  - ❌ 在 Composable 里分散 `remember(x, y, z) { ... }` 拼接 → 重组时反复计算，且逻辑逃出测试范围。
- ✅ 事件向下传 lambda（`onDateClick: (LocalDate) -> Unit`），状态向上流。
  - ❌ 把 ViewModel 直接传进子组件 → 组件无法预览、无法独立测试。
- ✅ `LazyColumn/Row` 的 item 必须给 `key`。
  - ❌ 日历格子不给 key → 滚动/翻月时状态错位到别的格子。
- ✅ 预览函数用 `@Preview` + 假数据，不访问 ViewModel。
- ✅ "弹层是否打开、里面对应的业务数据是什么"放在 UiState 里，不用 `remember { mutableStateOf }` 私藏。
  - ❌ 私藏 → 旋转屏幕后弹层状态丢失。
- ✅ 例外（明确允许）：**敏感或瞬时**的输入框内容（如 PIN、二次确认）用 `remember`（**不是** `rememberSaveable`）留在组件内。
  - ❌ 把 PIN 输入放进 UiState 或 `rememberSaveable` → 旋转屏幕/进程恢复时 PIN 会被留住，与"应用锁"的意图相反。

## 3. 协程

- ✅ 启动协程只用 `viewModelScope`（VM 内）或 `rememberCoroutineScope()`（UI 内）。
  - ❌ `GlobalScope` → 生命周期失控，页面销毁后仍写库。
- ✅ 写库等 IO 在 `Repository` 内切到 `Dispatchers.IO`；domain 纯函数不接触线程调度。
  - ❌ 在 Composable 里 `runBlocking` → UI 卡顿，且是可判定的严重错误。
- ✅ `Flow` 在 VM 中用 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`。
  - ❌ 用 `SharingStarted.Eagerly` → 无人订阅时也持续查询数据库。

## 4. Kotlin 写法

- ✅ 数据模型用 `data class`；枚举用于有固定集合的领域概念（`FlowIntensity`、`Symptom`、`Confidence`）。
- ✅ 可空性表达真实语义：`CyclePrediction.nextPeriodStart: LocalDate?` 在 `Confidence.NONE` 时必须为 `null`。
  - ❌ 用 `LocalDate.EPOCH` 之类哨兵值表示"没有" → 哨兵值会一路传播到 UI。
- ❌ 禁止 `!!`（可空断言）。用 `requireNotNull` / 提前返回 / 提供默认值。
- ❌ 生产代码禁止 `TODO()` 与 `FIXME`（`scripts/check-docs.py` 会扫 `app/src/main`）。
- ✅ public 函数只暴露接口类型（`CycleRepository`），实现类留在 `data/`。

## 5. 依赖与模块

- ✅ 依赖声明集中在 `gradle/libs.versions.toml`（version catalog），版本号不在模块里散落。
- ✅ 新增依赖必须在 change spec 或 `CLAUDE.md` §3 写理由。
  - ❌ "顺手加个库" → 版本兼容矩阵扩张，单人无法维护。
- ✅ 单模块 `:app`；只有当构建时间成为真实瓶颈时才拆模块。

## 6. 编译期检查（不要靠人眼）

- ✅ `./gradlew detekt` 无 error 才算完成（L1）。
- ⚠️ 未落地（L3，别假装有效）：无 `./gradlew lint`（AGP lint）硬门禁；无 instrumented 测试；无 UI 截图测试。

## 7. CI 与构建环境（2026-10-10 的血泪教训，务必先看）

背景：`main` 上前 4 次 CI 运行全部失败，**没有一次是代码问题**，而"代码到底过没过"在前 4 次里完全不可知。

- ✅ 不需要 JDK/SDK 的检查（隐私守卫、文档一致性）放在 workflow **最前面**。
  - ❌ 放在环境准备之后 → 环境准备失败时这些步骤被 skip，代码检查的信号被一起吞掉。
- ✅ Android SDK 组件用 `sdkmanager` 显式安装（runner 镜像自带；缺失时从 dl.google.com 下载 cmdline-tools）。
  - ❌ 继续用 `android-actions/setup-android@v3` → 该 action 仍在 Node 20，被强制跑在 Node 24，且已弃用。
- ✅ 接受许可这样喂输入：`printf 'y\n%.0s' $(seq 1 50) > f && sdkmanager --licenses < f`
  - ❌ `yes | sdkmanager --licenses` → 在 `set -o pipefail` 下 `yes` 会因 SIGPIPE 被终止，整条管道失败，把"正常结束"当成错误（这就是第 4 次运行的失败原因）。
- ✅ `gradlew` 与 `scripts/` 下的 shell 脚本在 git 里必须是 `100755`（`git ls-files -s` 可验）；并在 workflow 里加 `chmod +x ./gradlew` 兜底，因为 Windows 提交时默认不带 exec 位。
- ✅ 读不到 CI 日志时（`/actions/runs/{id}/logs` 需鉴权），把步骤**拆到"失败步骤名即可定位"的粒度**。
  - ❌ 把 SDK 准备与组件安装塞在一步里 → 失败时只知道"SDK 有问题"，只能靠猜。

## 提交前快速自检

- [ ] 新增的"今天"都来自 `Today`，没有直接 `LocalDate.now()`
- [ ] 日期差值与区间没有用毫秒/`Instant`
- [ ] 新的 `StateFlow` 用 `stateIn(..., WhileSubscribed)` 且用 `collectAsStateWithLifecycle` 收集
- [ ] 列表 item 都有稳定 `key`
- [ ] 没有 `!!`、`GlobalScope`、`runBlocking`、`TODO()`
- [ ] 新增依赖已在 spec/CLAUDE.md 写理由，并进了 version catalog
- [ ] `./gradlew detekt testDebugUnitTest` 通过
