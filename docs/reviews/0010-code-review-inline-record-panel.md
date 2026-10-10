# 代码自审：内联记录面板与点选即时保存

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/inline-record-panel/spec.md` 的全部实现
触发原因：改动命中 `/specs/` 与 `/app/src/main/java/io/lunartrace/app/domain/prediction/` 之外的
记录写入路径，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

`detekt` **0 条**，无硬违规。以下是判断题——本轮有**三个"看不出来但很关键"的机制**：

- `ui/calendar/CalendarViewModel.kt` —— **覆盖层（`localEdits`）**。后人若把它"简化"成"直接读数据库"，
  会出现**偶发丢写**（快速连点两次症状，第二次读到旧集合，第一次的点选被覆盖）。
  这类 bug 极难复现，因此理由写在类注释里，而不是留给 git 历史。
- `ui/calendar/CalendarViewModel.kt` —— **写队列必须是无限缓冲，不能是 conflated**。
  conflated 只保留最新一条，若用户在两个不同日期上快速操作会**丢掉其中一天**。
  这是实现时才浮现的陷阱，已写进 spec 与代码注释。
- `ui/calendar/CalendarViewModel.kt` —— **切换日期前 flush 备注**：否则防抖里的备注会写到新日期上。
  `applyEdit` 里那三个 `takeIf { it.date == date }` 回退链看起来啰嗦，但它防的是
  "切换日期后同一帧内立刻点选"把编辑写到前一天。**不要删。**
- `ui/calendar/DayRecordPanel.kt` —— 面板明确显示"改动会立即保存"。没有这句，用户会去找保存按钮
  （改动前它就在这里，删掉按钮而不给替代提示属于 UX 回归）。
- **删除动作（正向）**：`DayRecordSheet.kt` 与 `CalendarOverlays` 被删除。前者被 `DayRecordPanel`
  取代，后者因为只剩一个日期选择器而内联——这是**减少重复防线**，不是在"清理现场"。
- `ui/calendar/CalendarScreen.kt` —— 「记录今天」文案改为「把编辑切回今天」。
  语义从"打开今天的弹层"变成"把面板切到今天"，文案必须跟着改，否则按钮在撒谎。
- **未测（明确记录）**：防抖与写队列的时序没有自动化测试——需要 `kotlinx-coroutines-test` 与可注入时钟，
  本仓库暂不引入。它们的正确性依赖"单一队列 + 完整快照"的**结构**，而不是测试。
  这是本轮最大的测试空白，如实记录。

## Spec

- **逐条落地**：内联面板在日历下方 ✅；点选即时保存（无保存按钮）✅；选中日数字外中性描边 ✅；
  「记录今天」语义调整 ✅；`LastPeriodStartDialog` 保持并选完即切到该日 ✅；
  「清空这一天」保留 ✅。
- **spec 措辞的一处错误（已修）**：原文写"单一**无缓冲**队列"，而实现是 `Channel.UNLIMITED`（有缓冲）。
  正确说法是"单一队列、无限缓冲、单消费者、顺传入队"，且必须说明"不能是 conflated，否则跨日期会丢写"。
  已改回 spec——**文档与代码不一致时，改文档，并把理由写进去**。
- **实现细化**：`applyEdit` 的三重回退链、`onLastPeriodStartPicked` 显式构造显示值（不走 `applyEdit`，
  因为组合流在切换选中日的那一帧还没重算）。均属必要手段，已在代码注释说明。
- **行为未变**：备注仍限 500 字；空条目仍会被仓库删除行；预测口径、隐私路径、schema 均未触碰。

### 汇总

Standards 7 条（最严重：三个隐式机制若被"简化"会引入偶发丢写；另有一处测试空白已明确记录）；
Spec 4 条（最严重：spec 自身措辞有误，已修正并补上 conflated 的反例）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条；`testDebugUnitTest` 75 用例 0 失败；覆盖率门禁通过；`assembleDebug` 成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；未新增权限/依赖；未触碰存储结构、日志、PIN |
| 测试 | **1** | 纯编辑逻辑红先绿后：`DayRecordEditingTest` 先 3/6 失败，补实现后 75 用例全绿（防抖/队列不测已在 spec 明示） |
| 范围 | **1** | 改动限于日历包（新增 2 个文件、删除 1 个）、`CalendarScreen`、`MonthGrid`、spec 与自审 |
| 验证 | **1** | 见下；旧证据作废，全部本轮重跑 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 红：DayRecordEditingTest 先 3/6 失败（桩实现不做覆盖层合并）
> Task :app:testDebugUnitTest FAILED
6 tests completed, 3 failed
（失败：勾选/取消症状不动心情备注、取消经期标记保留症状、心情再点取消、备注截断）

# 2) 绿 + 全门禁
BUILD SUCCESSFUL in 7s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug）
detekt.txt 行数 = 0

# 3) 测试与覆盖率
unit_tests=75 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（98/98，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 4) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **内联面板与"选中环"的视觉未经真机确认**：6 行日历 + 预测卡 + 面板，很可能需要滚动才能看到面板，
   而滚动位置是否合适、选中环在真实密度下是否够明显，都只能由使用者截图判断。
2. **防抖与写队列没有自动化测试**：见上文，需要协程测试基建与可注入时钟（本仓库暂不引入）。
3. **两个残余数据窗口**（已在 spec 明示）：备注在 600ms 防抖窗口内、以及 ViewModel 被清理时队列中未落库的写操作。
   前者可接受（低价值数据），后者窗口极小（除备注外都是同步入队）。
4. **无独立评审**：结论仍由同一模型自审产生。
