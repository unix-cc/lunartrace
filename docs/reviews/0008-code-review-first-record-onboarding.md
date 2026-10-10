# 代码自审：零记录用户的入口与空状态文案

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/first-record-onboarding/spec.md` 的全部实现
触发原因：改动命中 `/specs/`，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

- `ui/calendar/CalendarScreen.kt` —— **detekt 拦下的硬违规（已修）**：加了浮层与日期选择器后，
  `CalendarScreen` 达到 `LongMethod 80/80`。抽成 `CalendarOverlays`。
  这不是"为了让检查器闭嘴"：那个函数已经同时负责状态、骨架、卡片、网格、图例与两个浮层，
  确实到了读不下去的程度。
- `ui/common/Labels.kt` —— **判断题**：它现在 `import ui.calendar.PredictionHint`，
  即"公共文案层"知道了日历层的概念。
  取舍：为了守住"面向用户的中文文案集中一处"的约定而接受这次同层横向依赖。
  **若 hint 类型继续增长，应把映射移回 `ui/calendar/`（例如 `PredictionHintText.kt`）**，届时这条要重判。
- `ui/calendar/CalendarViewModel.kt` —— **判断题（关键实现理由）**：`onLastPeriodStartPicked` 写库后
  直接构造 `DayDraft`，**不回读 `uiState`**。原因是写库是异步的，回读可能拿到尚未刷新的旧状态，
  于是弹层会显示"这一天尚未标记为经期"——一个刚写完记录的界面显示未记录。
  **不要把这行"优化"成回读。**
- `ui/calendar/CalendarViewModel.kt` —— **判断题**：预填 `FlowIntensity.MEDIUM` 等于替用户预设了一个流量值。
  取舍：立刻打开记录弹层让它**可见且可改**，比新增"无流量"状态（要改 Room schema 与枚举）轻得多。
  若以后收到"我根本没选流量"的反馈，再考虑让 `flow` 可空。
- `ui/calendar/CalendarScreen.kt` —— **判断题**：没有用 Material 的 `selectableDates` 去禁用未来日期，
  而是在确认按钮上校验。原因：`selectableDates` 的参数形状在不同 material3 版本间变过，我不愿意赌;
  按钮上校验的行为是确定的，并且有测试覆盖转换函数。
- `ui/calendar/CalendarScreen.kt` —— **判断题**：`DatePickerDialog` 的标题被覆盖为中文问句
  （Material 默认是英文 "Select date"），避免界面上混入英文。

## Spec

- **逐条落地**：空状态判定抽成纯函数且按**两档门槛**分别说话 ✅；日期转换抽成纯函数并固定按 UTC 解释 ✅；
  入口写入的是**正常记录**（无新增设置项）✅；未来日期不可确认 ✅；删除把两档混成一句的旧文案 ✅。
- **关键决策遵守**：没有把"上次经期开始日"存成 DataStore 设置项（被拒方案），因此没有出现第二事实源。✅
- **实现细化（spec 未写明，已记录）**：抽出 `CalendarOverlays`。属结构手段，不是口径或行为变更。
- **本轮的起因得到验证**：spec 里说"上一轮文案把 1 次与 3 次两档混为一谈"，
  这条缺陷在本轮被 `PredictionHintTest` 的四个用例永久固定下来，不会再退化。

### 汇总

Standards 6 条（最严重：1 条被 detekt 拦下的结构性硬违规，已修；其余为判断题与关键实现理由）；
Spec 5 条（最严重：无 —— 口径与行为全部落地，一处结构细化已记录）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条（期间被拦下 1 次并已修复）；`testDebugUnitTest` 64 用例 0 失败；覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限/依赖；未触碰日志、存储结构与 PIN |
| 测试 | **1** | 红先绿后：两个新测试文件先跑出 **7/9 失败**（桩实现故意返回错误值），补实现后 64 用例全绿 |
| 范围 | **1** | 改动限于日历包、`Labels`、新增 spec 与自审；无顺手重构（`CalendarOverlays` 的抽取已注明理由） |
| 验证 | **1** | 见下；旧证据作废，全部本轮重跑 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 红：两个新测试文件先失败（9 个用例，7 个失败）
> Task :app:testDebugUnitTest FAILED
DatePickerConversionTest > 闰日正午的毫秒值仍归入同一天 FAILED
PredictionHintTest > 有记录却推不出来时提示检查记录 FAILED
（其余失败项略；另有 2 个用例被桩实现意外满足，故是 7 而非 9）

# 2) 过程中被 detekt 拦下一次（真实信号，已修）
LongMethod - 80/80 - [CalendarScreen] ... ui/calendar/CalendarScreen.kt:69:5
（抽成 CalendarOverlays 后通过）

# 3) 绿 + 全门禁
BUILD SUCCESSFUL in 5s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 4) 测试与覆盖率
unit_tests=64 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（98/98，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 5) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **日期选择器与空状态按钮的视觉/交互未经真机确认**：`DatePickerDialog` 的中文标题排版、
   "记录上次经期开始"按钮与"去设置填周期长度"文字按钮并排是否显得拥挤，都需要截图反馈。
2. **`Labels` 对 `ui.calendar` 的横向依赖**：已记录取舍与重判条件，但本轮没有更好的落点。
3. **无独立评审**：结论仍由同一模型自审产生。
