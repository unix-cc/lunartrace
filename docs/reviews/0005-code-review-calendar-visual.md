# 代码自审：日历视觉对标（胶囊标记 / 四项图例 / 粉红配色）

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/calendar-visual-refresh/spec.md` 的全部实现
触发原因：改动命中 `.github/CODEOWNERS` 中的 `/specs/`，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

本轮 `detekt` **0 条**，无硬违规。以下是判断题：

- `ui/calendar/MonthGridLayout.kt`（新增）—— 把网格布局从 `CalendarViewModel` 的私有方法搬成纯函数。
  这不是"顺手重构"：搬之前那段逻辑**没有任何测试**，而它最容易出 off-by-one（周起始偏移、跨月、跨年）。
  搬完立刻补了 7 个边界用例。记录在此，供后来者判断这次搬动是否值得。
- `ui/theme/Theme.kt` —— 用 Material 的 `tertiary` / `tertiaryContainer` 承担"排卵日 / 易孕窗口"语义，
  而不是新增自定义颜色常量。**取舍**：复用现有槽位等于免费获得深浅色两套变体，代价是语义只能靠注释绑定
  （已在文件里写明四条映射关系）。若将来有第五种语义色，这个做法会开始撑不住。
- `ui/calendar/CalendarScreen.kt` —— 图例去掉了"有症状记录"，但格子右上角的症状小点**仍然存在**，
  即图例与标记不再一一对应。判定：可接受（小点属次级信息、不占图例位），
  但若真机上让人困惑，应二选一：把点也放进图例，或彻底去掉这个点。**列为待观察。**
- `ui/calendar/MonthGrid.kt` —— "今天"的高亮从"描边圆环"改成"数字用主色 + 加粗"。
  理由：新方案里同一格可能已经叠了胶囊条与浅黄底，再加圆环是三重叠加。
  这是**我无法在真机上确认的判断**，如实记录。
- `ui/calendar/MonthGrid.kt` —— `MonthGrid` 参数已到 6 个（detekt 阈值 8，未触发），但已接近 `Data Clumps`。
  判定：本轮不处理；**若再增加标记类型，应改为传一个 `CalendarMarks` 数据类**，而不是继续加参数。
- **未做且知道**：没有做 edge-to-edge，所以头部渐变不延伸到状态栏（参考设计是延伸的）。
  理由：`enableEdgeToEdge()` 会改变所有页面的 insets 处理，没有真机我无法保证不错位。
  这是一次刻意延后，不是遗漏。

## Spec

- **遵守**：网格周日起始 ✅；经期实心胶囊 / 预测经期空心胶囊 / 连段只在外侧倒角 ✅；
  易孕窗口浅黄底 + 排卵日橙点 + 症状点右上角 ✅；图例四项与参考设计一致 ✅；
  不新增颜色资源文件 ✅。
- **低置信度不显示排卵/易孕标记**（spec 的 User Story 6）：由既有链路保证——
  `confidence < MEDIUM` 时 `ovulationDay` 与 `fertileWindow` 均为 `null`，日历拿不到标记。
  **没有为此新增任何判断**，即这条要求是"结构上成立"，不是靠 if 兜住。✅
- **Testing Decisions 遵守**：先写 `MonthGridLayoutTest` 并运行确认失败，再补实现。✅
- **实现细节超出 spec 文字**（如实记录）：`CalendarUiState` 增加了 `predictedDates` 字段。
  spec 只写了"`DayMarks` 增加 `ovulation`"，但网格要判断"连段"就必须拿到整个集合。
  属必要实现手段，不是口径变更。
- **未做（在 spec 的 Out of Scope 内）**：底部记录面板、主页环形图、"我"页、统计图表美化。
- **已确认不做（来自本轮决策）**：「爱爱」记录与「备孕中」状态开关，已写入 `PRODUCT.md` §3 反例表，
  理由是产品定位（会导向备孕/避孕用途，需要医学背书），**不是隐私问题**。

### 汇总

Standards 6 条（最严重：今天的高亮方式与"图例↔标记不一一对应"两项需真机确认；无硬违规）；
Spec 5 条（最严重：`predictedDates` 属 spec 未写明的实现手段，已记录）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条；`testDebugUnitTest` 47 用例 0 失败；覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限/依赖；未触碰存储与日志 |
| 测试 | **1** | 红先绿后已执行：`MonthGridLayoutTest` 先跑出 **6 个失败**，补实现后 47 个用例全绿 |
| 范围 | **1** | 改动限于 `ui/calendar/`、`ui/theme/`、`PRODUCT.md`、新增 spec；无顺手重构（网格搬动已注明理由） |
| 验证 | **1** | 见下方证据；旧证据作废，全部本轮重跑 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 红：新测试先失败（当时 MonthGridLayout 仍是周一起始、无连段判定）
> Task :app:testDebugUnitTest FAILED
MonthGridLayoutTest > 跨年月份正确 FAILED
MonthGridLayoutTest > 一周从周日开始 FAILED
MonthGridLayoutTest > 连续三天的中间一天不倒角 FAILED
MonthGridLayoutTest > 网格恒为 42 格并覆盖到下月中旬 FAILED
MonthGridLayoutTest > 当月一日恰好是周日时首格就是一日 FAILED
（Failed 计数 = 6；"单日标记两端都要倒角" 被桩实现意外满足，故恰好 6 失败而非 7）

# 2) 绿 + 全门禁
BUILD SUCCESSFUL in 8s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 3) 测试与覆盖率
unit_tests=47 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（86/86，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 4) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **视觉正确性完全未经确认**：本轮的产出本质上是视觉，而我没有设备。
   `detekt`/单测/编译都不能证明"看起来对不对"。**必须由使用者装包截图反馈**，
   这是本轮唯一的验证手段（也因此把改动切成"可单独看效果"的一片）。
2. **未做 edge-to-edge**：头部渐变不延伸到状态栏，与参考设计有可见差异（已注明为刻意延后）。
3. **无独立评审**：结论仍由同一模型自审产生。
