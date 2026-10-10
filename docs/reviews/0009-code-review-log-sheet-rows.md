# 代码自审：记录弹层改为行式布局与水滴流量选择

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/log-sheet-rows/spec.md` 的全部实现
触发原因：改动命中 `/specs/`，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

`detekt` **0 条**，无硬违规。以下是判断题：

- `ui/calendar/DayRecordSheet.kt` —— 水滴选择器用 `flow.ordinal` 表达"填充到第 N 个 = 第 N 档强度"，
  这引入了一个**隐式依赖：`FlowIntensity` 的声明顺序必须由轻到重**。
  在中间插入一个新档位（例如把"中等偏多"插进 LIGHT 与 MEDIUM 之间）会**悄悄改变语义**。
  → 处置：本轮补了 `FlowIntensityOrderTest` 把顺序钉住。
  **诚实说明：这条是事后补的守护测试，不是红先绿后的一部分**——该依赖是写选择器时才出现的。
- `ui/calendar/DayRecordSheet.kt` —— `RecordRow(label) { content }` 的 `content` 是普通
  `@Composable () -> Unit` 而不是 `ColumnScope` 上的扩展，所以内容里不能用 `Modifier.align`。
  判定：当前四个区块都不需要，**保持最窄的接口**；一旦有区块需要对齐，再扩成 `ColumnScope`。
- `ui/calendar/DayRecordSheet.kt` —— 6 个私有 composable + 1 个公共入口，文件约 190 行，
  但每个函数都很短（`LongMethod` 未触发）。判定：这次拆分是**上一版被 detekt 拦下后形成的习惯**，
  保留。
- `ui/common/Labels.kt` —— 星期映射依赖 `DayOfWeek.value`（1=周一..7=周日）与数组下标的一一对应。
  判定：这是"用下标隐式表达顺序"的同类风险，但已由 `LabelsTest` 的四个用例覆盖
  （周六/周日/周一各一 + 跨年），比注释可靠。
- `ui/calendar/DayRecordSheet.kt` —— 症状与心情**保留 chip**，与参考设计的极简行不同。
  判定：10 个症状用 checkbox 或图标只会更糟（要么占三行、要么图标无文字不可辨），
  已在 spec 的 Out of Scope 写明。

## Spec

- **逐条落地**：日期带星期 ✅；四个区块统一为"标签行 + 内容" ✅；
  流量改成 4 个水滴（填充语义 + `contentDescription` 为该档文字）✅；提示区保留 ✅。
- **明确不改的两件事都遵守了**：没有把弹层改成内联面板（即时保存是产品决策）✅；
  没有在网格上高亮选中日（格子已经有五种视觉元素）✅。
- **实现细化（spec 未写明）**：`FlowIntensityOrderTest`。属测试补强，已在上文标注是事后补的。
- **行为未变的部分**：仍然显式"保存"；"清空这一天"仍在；备注仍限 500 字。
  **本轮纯呈现改动，没有触碰任何口径、schema 或隐私路径。**

### 汇总

Standards 5 条（最严重：水滴填充依赖枚举声明顺序，已用测试钉住；其余为判断题）；
Spec 4 条（最严重：无 —— 口径未动，行为保持一致）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条；`testDebugUnitTest` 69 用例 0 失败；覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限/依赖；未触碰存储、日志、PIN |
| 测试 | **1** | 主行为红先绿后：`LabelsTest` 先 4 个用例全失败，补实现后全绿（另有 1 条事后补的守护测试，已单独标注） |
| 范围 | **1** | 改动限于 `DayRecordSheet`、`Labels`、新增 spec 与两条测试；无顺手重构 |
| 验证 | **1** | 见下；旧证据作废，全部本轮重跑 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 红：LabelsTest 先全红（实现里只有日期、没有星期）
> Task :app:testDebugUnitTest FAILED
LabelsTest > 周六日期带周六 FAILED
LabelsTest > 周日日期带周日 FAILED
LabelsTest > 周一日期带周一 FAILED
LabelsTest > 跨年日期正确 FAILED

# 2) 绿 + 全门禁
BUILD SUCCESSFUL in 5s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 3) 测试与覆盖率（69 = 68 + 后补的顺序守护测试）
unit_tests=69 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（98/98，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 4) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **弹层的新布局未经真机确认**：最需要看的是一行里放"4 个 36dp 水滴 + 当前档位文字 + 取消按钮"
   是否会在窄屏上换行或被挤压。
2. **内联记录面板与"即时保存"刻意未做**：这是产品决策（点一下是否立刻写库），
   不应该由我在无法核对真机的情况下顺手定掉，已写进 spec 等待明确选择。
3. **网格内选中日高亮刻意未做**：格子已有易孕底色、胶囊条、排卵点、症状点、今天加粗五种元素。
4. **无独立评审**：结论仍由同一模型自审产生。
