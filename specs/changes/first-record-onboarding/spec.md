# 零记录用户的入口与空状态文案

Status: done

## Problem Statement

两个问题叠在一起：

1. **零记录的新用户没有任何锚点**。缺省周期长度解决的是"只有 1 段经期"的情况；
   但一个还没记录过任何一天的用户，即使填了缺省值，仍然看不到任何预计——没有起点可以往后推。
   而此刻界面上的指引是"再记录 3 次经期开始"（下一段问题），用户不知道从哪下手。
2. **上一轮引入的文案已经不准确**。预测卡在没有预测时统一说"再记录 N 次经期开始，
   就会显示下次经期预计与周期阶段"。但填了缺省值的用户**只需记录 1 次**就能看到"下次经期预计"，
   只有"周期阶段"才需要 3 次。文案把两档门槛混成了一句，等于对用户说了假话。

## Solution

1. **把空状态该说什么抽成纯函数**（三个输入 → 四种提示），让文案不再靠 `if` 拼凑，也不再可能说错门槛。
2. **零记录时提供一个入口**：「记录上次经期开始」→ 日期选择器 → 写入一条**正常的经期记录**，
   并立刻打开该日的记录弹层，让用户看到并修改流量（预填"中"）。
3. **未填缺省值的用户在同一条提示里被告知另一条路**：可以先去设置里填写周期长度。

## User Stories

1. 作为刚装好应用的用户，我想直接告诉我"上次经期是哪天开始的"，而不是让我在日历上翻回上个月自己找。
2. 作为使用者，我希望应用说清"再记几次能看到什么"：记录 1 次能看到下次预计，记录 3 次才看得到阶段。
3. 作为使用者，如果我不想记录日期、只想先看看预计，我希望知道还有"填周期长度"这条路。
4. 作为已经记录过但推不出预计的用户（例如很久没打开），我希望看到的不是"再记录 3 次"，而是"检查一下记录"。

## Implementation Decisions

### 关键决策：入口写入的是**记录**，不是设置项

**被拒方案**：把"上次经期开始日"存成一个 DataStore 设置项。
拒绝理由：这会造成**同一概念有两个事实源**——设置里的那个日期不会出现在日历上，
于是预测锚定在一个用户永远无法在日历上核对的日子；两个来源冲突时又得定优先级，
而"记录"本来就是表达这件事的既有通道。所以本特性只做**更省力的入口**，写进去的仍是正常记录。

### 具体实现

- `ui/calendar/PredictionHint.kt`：`enum PredictionHint { NONE, RECORD_FIRST_PERIOD,
  RECORD_FIRST_PERIOD_OR_SET_DEFAULTS, CHECK_RECORDS }` +
  `PredictionHints.of(hasPeriodRecords, hasDefaultCycleLength, hasPrediction)`。**纯函数**，可单测。
- `ui/calendar/DatePickerConversion.kt`：`toLocalDate(utcMillis)` / `toUtcMillis(date)`。
  Material 的 `DatePicker` 返回的是 **UTC 午夜**毫秒；转换必须固定按 UTC 解释，
  否则跨时区会整体差一天（这是本项目"日期身份"红线的一个具体落点，见 `local-data-model.md` §1）。
- `ui/calendar/CalendarScreen.kt`：预测卡改用 `PredictionHint` 决定文案；
  在"零记录"两种状态下显示「记录上次经期开始」按钮，打开 `DatePickerDialog`；
  **未来日期不允许确认**（同一天可以）。
- `ui/calendar/CalendarViewModel.kt`：新增 `onLastPeriodStartPicked(date)`：
  写入该日为经期日（`FlowIntensity.MEDIUM`）并**立刻把该日的记录弹层打开**。
  预填的流量是可见且可立即修改的，不是替用户断言。
- 删除 `Labels.notEnoughRecordsText`（它把两档门槛混在一起，正是问题 2 的根源）。

## Testing Decisions

**接缝**：两个纯函数（`PredictionHints.of`、`DatePickerConversion`），JVM 单测，CI 运行。

**本轮流程**：先写两个测试文件并运行**确认它们红**（桩实现故意返回错误值），再补实现。

必须覆盖的边界：

`PredictionHints`：有预测 → `NONE`；零记录 + 已填缺省值 → `RECORD_FIRST_PERIOD`；
零记录 + 未填缺省值 → `RECORD_FIRST_PERIOD_OR_SET_DEFAULTS`；有记录但无预测 → `CHECK_RECORDS`。

`DatePickerConversion`：`toLocalDate(0) == 1970-01-01`；`toUtcMillis(1970-01-01) == 0`；
2024-02-29 / 2025-12-31 / 2026-10-10 往返一致；非午夜毫秒值（+1 小时）仍归入同一天（证明按 UTC 解释）。

**明确不测**：`DatePickerDialog` 本身的交互（无设备，与既有决定一致）。

## Out of Scope

- 多步首次引导向导（本特性只有一个入口按钮，不做流程）。
- 在引导里顺带询问缺省周期长度（设置页已有入口，文案会指路）。
- 提醒通知、导出、体温。
- 允许用户"修改已记录的经期开始日"的专门界面（用既有的记录弹层即可）。

## Further Notes

- 本轮的起因是上一轮的一个**真实缺陷**（文案把 1 次与 3 次两档门槛混为一谈）。
  它说明"文案里的次数"也是会随口径变化而失效的资产——所以这次把判断抽成纯函数并测试，
  而不是继续在 Composable 里拼字符串。
- 走快速路径（不改 schema、不改口径、不碰隐私路径），但因为超过"小改"规模（6 个文件）
  仍落本 spec；未做计划级对抗审查，取舍直接写在上面"关键决策"一节。
