# 代码自审：周期阶段与排卵期呈现

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/cycle-phases/spec.md` 的全部实现
触发原因：改动命中 `.github/CODEOWNERS` 中的预测路径与 `specs/capabilities/`，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

本轮 `detekt` **一次通过（0 条）**，没有硬违规。以下是判断题（没有现成规范可引用，必须留给后来者看）：

- `domain/prediction/CyclePredictor.kt` —— `positionOf` 与 `phaseOf` 本可以合成一个函数。**刻意保留拆分**：
  它让"`cycleDay` 是事实、`phase` 是推算"这个门槛差异在代码结构上可见；合成一个函数后，
  下一个人很容易顺手把两者一起 gate 在 MEDIUM 上（spec 明确禁止这一点）。
- `domain/model/CycleModels.kt` —— `CycleStats` 新增 `episodeCount`，动机之一是为了 UI 文案（"还差几次记录"）。
  判定：段数本身是客观统计量，且能避免 ViewModel 重算 episodes，**接受**；但这是一个"领域模型向 UI 妥协"的取舍，记录在案。
- `ui/common/Labels.kt` —— `notEnoughRecordsText()` 引用了 `CyclePredictor.MIN_EPISODES_FOR_PHASE`。
  判定：常量留在领域、文案留在 UI，依赖方向仍是 `ui → domain`，**符合分层**。
- `ui/calendar/CalendarScreen.kt` —— 预测卡现在最多显示 5 行文字（预计 / 置信度 / 排卵日 / 易孕窗口 / 阶段）。
  判定：与 `PRODUCT.md` §4"一眼可读"存在张力，**无法在本轮判定**（没有真机可看）。
  列为待观察项：若真机上显得拥挤，优先砍掉"置信度"那行（它相对最不重要）。
- `ui/calendar/CurrentPositionLine` —— `position.cycleDay == null` 时直接 `return`（不渲染任何东西）。
  判定：合法且有意（无记录时卡片已给出进度说明，不需要再显示空行）；但它是"静默不渲染"，故在此显式记录。

## Spec

- **遵守**：日历视觉未改动 ✅；排卵信息以**单日**呈现、"排卵期"未作为区间出现 ✅；
  阶段与排卵日同门槛（≥ MEDIUM）、周期天数为事实不受门槛限制 ✅；无任何评价性文案 ✅。
- `spec` User Story 4（数据不足要有进度说明）—— 实现为 `notEnoughRecordsText(episodeCount, required)`，
  文案形如"再记录 N 次经期开始，就会显示下次经期预计与周期阶段"。✅
- `spec` User Story 5（波动大时不显示排卵信息）—— 由既有置信度门槛保证；`confidence == LOW` 时
  `ovulationDay` 与阶段均为空，但"下次经期预计"仍显示。✅
- **未偿债，如实记录**：spec 的 Further Notes 提到"症状统计口径至今未沉淀进 capability，另开一轮处理"。
  本轮**没有**处理它（不在本轮范围），它仍然是一笔未偿的文档漂移。下次改到统计相关代码时必须先补。
- **一处实现细节超出 spec 文字**：`CycleStats.episodeCount` 是 spec 没写明的字段（spec 只写了 UI 要给出进度说明）。
  判定：属实现细化的最小手段，且 `cycle-prediction.md` 的口径表未要求它，**不算口径变更**。

### 汇总

Standards 5 条（最严重：预测卡可能过于拥挤，待真机判定；无硬违规）；
Spec 5 条（最严重：症状统计口径的文档漂移仍未偿，已明确记录为未达标项）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | `detekt` 0 条；`testDebugUnitTest` 40 个用例 0 失败；`jacocoTestReport` + 覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限、未新增依赖、未使用日志 API |
| 测试 | **1** | **红先绿后已真实执行**（上一轮记 0 的原因已消除）：先跑出 8 个失败用例，再实现转绿 |
| 范围 | **1** | 改动限于 domain 位置模型 + 文案 + 预测卡；日历视觉未动（spec 明确要求） |
| 验证 | **1** | 每条都有命令与实际输出（见下）；旧证据作废，全部为本轮重跑 |

**结果：5/5 —— 本轮判为完成**（唯一保留：Compose 界面的视觉正确性仍无人确认，见未达标项）。

## 证据（收口时重跑）

```
# 1) 红：实现之前，先写测试并运行，确认它失败
> Task :app:testDebugUnitTest FAILED
CyclePositionTest > 经期结束之后排卵日之前属于卵泡期 FAILED ... (共 8 个用例失败)
25 actionable tasks: 7 executed, 18 up-to-date

# 2) 绿：补上实现后重跑
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 4s
（FAILED / error 计数 = 0）

# 3) 全门禁
BUILD SUCCESSFUL in 8s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 4) 覆盖率（门槛 85%）
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（86/86，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 5) 测试总量
unit_tests=40 failures=0

# 6) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

**红先绿后过程中纠正的一处错误**：`CyclePositionTest` 里"长期未记录"用例的期望值我最初手算成 1462，
重算后是 1434（2026-01-01 → 2030-01-01 为 1461 天，最后一段起于 2026-01-29，即第 28 天）。
**期望值必须来自手算而不是实现的输出**，所以这个错误在实现之前就修掉了——这次不是"改测试迁就代码"。

## 未达标项与原因（不允许留空）

1. **UI 视觉未经人工确认**：无真机/模拟器，Compose 界面的排版、拥挤度、对比度都没有被看过。
   本轮对预测卡的文字行数有明确的不确定（Standard 轴第 4 条）。**不声称已验证。**
2. **症状统计口径的文档漂移仍未偿**：`spec` 已记录"另开一轮"，本轮没有处理。
3. **无独立评审**：本文结论仍由同一模型自审产生，系统性盲区不会被自己发现。
