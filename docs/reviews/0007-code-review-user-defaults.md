# 代码自审：用户填写的缺省周期长度

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/user-default-cycle-length/spec.md` 的全部实现
触发原因：改动命中 `/specs/`、`/app/src/main/java/io/lunartrace/app/data/security/` 之外的隐私路径
（`data/prefs/`）与预测路径，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

`detekt` 最终 **0 条**，但在过程中出现过一次**编译失败**（见下），是真实发现：

- `ui/calendar/CalendarScreen.kt:159` —— **编译期拦下的错误**：我在 `PredictionCard` 里写了
  `insight.cycleDefaults`，但该字段挂在 `CalendarUiState` 上而不是 `CycleInsight` 上。
  这不是笔误问题而是设计提示：**"依据来源"属于领域（`prediction.basis`），
  "用户填的数字"属于界面状态（`uiState.cycleDefaults`）**，两者必须显式分别传递。
  记录在此，避免后人图省事把数字塞进 `CyclePrediction`。

以下是判断题：

- `domain/prediction/CyclePredictor.kt` —— `predict()` 现在同时承担：选依据、算周期长度、算经期长度、
  算顺推、算排卵。detekt 的 `LongMethod`（80 行）未触发，但**已经接近做太多**。
  判定：本轮不拆；**若再往里加规则（例如"按阶段调整"），必须先拆成 `chooseBasis()` / `periodLengthOf()`**。
- `domain/prediction/CyclePredictor.kt` —— 兜底状态下**经期长度也不用实测**（改用用户填的、再退回 5 天）。
  这条规则不直观：只有 1 段经期时，"实测长度"往往只是"用户还没记完"，用它会把预测区间压到 3 天（夹取下限）。
  判定：用注释 + 测试用例 6/7 双向固定，并写进 capability 表。**这是最容易被后续重构"顺手简化"掉的一条。**
- `ui/settings/SettingsScreen.kt` —— 越界天数在 UI 上**禁用保存**，领域侧 `validatedDefault*` 再校验一次。
  看似重复防线，但**领域侧必须自保**：DataStore 里可能存在旧版本写入的脏值，或将来被别处调用。
  判定：保留两层，理由记录在此。
- `ui/settings/SettingsScreen.kt` —— 新增"缺省周期长度"卡片，与 `PRODUCT.md` 的"克制"原则有张力。
  判定：接受，但已在 ADR-0005 代价 4 里写明"每加一个设置项都要能被用户看懂为什么需要它"，
  卡片文案因此包含"留空则什么都不预测"。

## Spec

- **口径逐条落地**，且每条都有对应测试：
  优先级（实测 > 用户缺省 > 不预测）✅；区间校验与有效样本同区间 ✅；越界视为未填写而非夹取 ✅；
  兜底置信度 `LOW` ✅；仍需 ≥1 段经期作锚点 ✅；统计量不含缺省值 ✅（用例 8）。
- **与 ADR-0003 的关系已写清**：ADR-0003 拒绝"应用自己编"，本特性是"用户主动填写"；
  ADR-0005 落档，ADR-0003 的 Status 已回标被扩展。✅
- **实现细化（spec 未明说，已记录）**：新增 `optionalDaysText()`（未填写时显示"未填写"而不是一个像默认值的数字）、
  对话框"留空表示未填写"的提示文案。属实现手段，不是口径变更。
- **未做（在 Out of Scope 内）**：首次引导里主动询问这两个值；根据缺省值做提醒。
- **诚实局限**：零记录的新用户填了值**仍然看不到预测**（无锚点）。spec 的 Problem Statement 与 ADR-0005
  都没有承诺解决那一档，UI 文案也不得暗示能解决。

### 汇总

Standards 5 条（最严重：一次真实编译失败，暴露"领域值 vs 界面值"的边界；其余为判断题）；
Spec 5 条（最严重：无 —— 口径全部落地，仅两处实现细化已记录）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条；`testDebugUnitTest` 55 用例 0 失败；覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限/依赖；未触碰日志与存储结构 |
| 测试 | **1** | 红先绿后：新测试先跑出 **3 个失败**（8 个用例），补实现后 55 个用例全绿 |
| 范围 | **1** | 改动限于 domain 预测、DataStore 设置、日历/统计/设置三处 UI、文档；无顺手重构 |
| 验证 | **1** | 见下；旧证据作废，全部本轮重跑 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 红：先写测试，实现里还没有用到 defaults
> Task :app:testDebugUnitTest FAILED
8 tests completed, 3 failed
（失败的正是三条依赖兜底路径的用例）

# 2) 过程中的一次真实编译失败（已修）
e: CalendarScreen.kt:159:55 Unresolved reference 'cycleDefaults'

# 3) 绿 + 全门禁
BUILD SUCCESSFUL in 9s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 4) 测试与覆盖率
unit_tests=55 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（98/98，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 5) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **设置页与预测卡的文字未经真机确认**：本轮新增了两处文案与一张设置卡片，
   拥挤度、层级、文案是否让人看懂"这是你填的、不是算出来的"，都需要使用者截图反馈。
2. **零记录用户仍然没有预测**：本特性不覆盖这一档（已在 spec / ADR-0005 中明确），
   若要覆盖需要新增"上次经期开始日"输入，属记录能力。
3. **无独立评审**：结论仍由同一模型自审产生。
