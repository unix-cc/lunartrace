# 代码自审：周期环（第 N 天 + 阶段 + 经期/排卵标记）

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/cycle-ring/spec.md` 的全部实现
触发原因：改动命中 `/specs/` 与预测路径（`CyclePrediction` 增加字段），
按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

本轮 `detekt` **拦下了 7 条**并全部修复——比单元测试抓到的问题还多，值得记录：

| 条目 | 处置 |
|---|---|
| `LongMethod 85/80`（`CalendarScreen`） | 抽出 `CalendarTopSection`（环 + 预测卡）——不是为取悦检查器，那个函数确实已经读不下去 |
| `NoUnusedImports` ×3（`CalendarScreen` ×2、`CycleRing` ×1） | 删掉（删掉 `CurrentPositionLine` 后 `CyclePosition` 与 `labelOrNull` 不再被用到；改用 `Math.toRadians` 后 `kotlin.math.PI` 不再被用到） |
| `MagicNumber` ×4（`CycleRing`） | 抽出 `DEGREES_IN_CIRCLE`、`SLOT_CENTER_OFFSET`，并用 `Math.toRadians` 取代 `× PI / 180.0` |

以下是判断题：

- `domain/model/CycleModels.kt` —— `CyclePrediction` 新增 `basisCycleLengthDays`。
  它是**输出**（"我这次用了多少天当基准"），不是输入。**口径与预测行为完全不变**（既有用例全绿可证）。
  记录风险：后来者可能误以为它可以拿来反推日期（不行，顺推后的起点不是 `起点 = 今天 − basis`）。
- `ui/calendar/CycleRing.kt` —— `isOverdue` 只用在"超出的部分用浅色再画一遍"这一处，
  **没有**配任何评价性文案（"已推迟 N 天"是 `PRODUCT.md` §3 的明确反例）。
  这是刻意的：环画满 + 浅色重画已经表达了"又走了一圈"，不需要再补一句评价。
- `ui/calendar/CycleRing.kt` —— **环不画预测经期**：预测经期属于**下一个**周期，
  画在"本周期"的环上会误导（它与日历上"预测经期空心胶囊"是两件事）。已写进 spec。
- `ui/calendar/CycleRing.kt` —— `ringAngleForDay` 被从 `Canvas` 里抽成纯函数。
  原因：角度约定（0 度在正上方、顺时针、落在格中心）是环最容易差一格的地方，
  而**画出来的东西没法靠单元测试发现**。诚实标注：这是**事后补的守护测试**（依赖在写 Canvas 时才出现），
  不是红先绿后的一部分。
- `ui/calendar/CalendarScreen.kt` —— 环被放在页面**最顶部**，会把日历与记录面板推低（需要滚动）。
  这是本轮最需要真机确认的取舍，备选方案（独立成"主页"页）已写进 spec 的 Further Notes。

## Spec

- **逐条落地**：环覆盖本周期（起点由 `position.cycleDay` 反推）✅；格数 = 预测实际使用的基准长度 ✅；
  标记换算并按 `1..格数` 过滤 ✅；实测经期日与排卵日各自的标记 ✅；
  无预测时不画刻度与弧、圆心直说"还没有记录" ✅；预测卡里那行重复的"今天处于…"已删除 ✅。
- **超出 spec 的测试补强（记录在案）**：`CycleRingGeometryTest`。
  spec 只要求"角度约定写进注释 + 真机核对"，我认为把它变成可测的纯函数更符合本仓库的取向。
- **实现细化**：`CalendarTopSection` 抽取（结构手段）；`CalendarUiState` 增加 `ring` 字段（spec 已写）。
- **顺带修正的两处过期注释**：`CalendarUiState` 里"记录弹层"（早已改为内联面板）、
  "从周一开始"（早已改为周日起始）。注释比代码更早腐烂，看到就改。

### 汇总

Standards 6 条（最严重：detekt 拦下 7 条，其中 `LongMethod` 是真实的结构问题；其余为判断题与一处测试补强）；
Spec 4 条（最严重：无 —— 口径未动，行为与 spec 一致，两处补强/细化已记录）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条（期间拦下 7 条并全部修复）；`testDebugUnitTest` 84 用例 0 失败；覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限/依赖；未触碰存储、日志、PIN |
| 测试 | **1** | 数据映射红先绿后：`CycleRingDataTest` 先 4/6 失败，补实现后全绿；另有 1 组事后补的角度守护测试（已标注） |
| 范围 | **1** | 改动限于日历包（新增 3 个文件）、`CyclePrediction` 一个字段、预测器赋值、spec 与自审 |
| 验证 | **1** | 见下；旧证据作废，全部本轮重跑 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 红：CycleRingDataTest 先 4/6 失败（桩实现返回空数据）
> Task :app:testDebugUnitTest FAILED
CycleRingDataTest > 正常周期里经期日与排卵日都落在正确的格号上 FAILED
CycleRingDataTest > 超出基准长度时标记仍落在前几格并标记为走完 FAILED
CycleRingDataTest > 排卵日落在本周期之外时不画到环上 FAILED
CycleRingDataTest > 没有排卵日时环上没有排卵标记 FAILED
6 tests completed, 4 failed

# 2) detekt 拦下的 7 条（真实信号，已全部修复）
LongMethod - 85/80 - [CalendarScreen] ... CalendarScreen.kt:69:5
NoUnusedImports ×3 / MagicNumber ×4（CycleRing.kt）

# 3) 绿 + 全门禁
BUILD SUCCESSFUL in 5s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 4) 测试与覆盖率
unit_tests=84 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（99/99，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 5) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **环的视觉完全未经真机确认**：弧的粗细/配色、标记点大小、圆心排版、以及"环放在顶部会不会把日历挤走"，
   都只能由使用者截图判断。本轮最不确定的就是位置取舍。
2. **角度守护测试是事后补的**：不满足本仓库自己定的"红先绿后"（依赖在写 Canvas 时才出现）。
3. **超出基准长度的浅色重画**未经真机确认是否可读（这是本轮唯一的"新几何表达"）。
4. **无独立评审**：结论仍由同一模型自审产生。
