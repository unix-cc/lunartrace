# 主页标签页 + 签名发布包

Status: in-progress

## Problem Statement

上一轮把周期环放在日历页顶部。真机核对后确认这个位置不对：

1. 打开应用要看的是"我现在在哪"，但现在一进来先看到的是**预测卡**（一屏文字），环在它上面反而把日历与记录面板推得更低；
2. 记录面板在页面底部，环在最顶部 → 想记录要先滚过"环 + 预测卡 + 日历"三屏；
3. 环与日历是两件事：前者回答"我在哪"，后者回答"这个月哪几天是经期"、并承载记录。
   把它们塞进同一页是在为"少一个页面"付滚动代价。

另外，此前给使用者的都只是 debug 包（`io.lunartrace.app` 由 debug keystore 签名）。需要能长期安装、可覆盖升级的**签名包**。

## Solution

1. 新增**主页**标签页（启动页）：周期环 + 「下次经期预计」一行 + 「去做记录」按钮。
   底部导航变为 主页 / 日历 / 统计（顺序即展示顺序），**启动落在主页**。
2. 从日历页移除周期环（预测卡保留，它承载的是"依据与置信度"这类细节）。
3. 打通 release 签名：生成 keystore 与 `keystore.properties`（**均不入库**），
   产出可长期安装的签名 APK，并用 `apksigner` 验证签名与指纹。

## User Stories

1. 作为使用者，我打开应用第一眼就看到环，而不是一篇说明文字。
2. 作为使用者，我在主页看到环之后，一次点击就能去记录。
3. 作为使用者，我希望两页上的"第 N 天"与"下次经期预计"永远是同一个数，不能各说各话。
4. 作为使用者，我希望给我测的包能覆盖升级，而不是每次都要先卸载。

## Implementation Decisions

### 主页是只读的

主页**不做记录**：记录面板与日历强耦合（点日期即在下方记录），把它搬来搬去只会产生两处状态。
主页只做两件事：展示位置、把人送去日历。

### 两页不可能"各说各话"

主页与日历**读同一份数据、调同一个 `CyclePredictor`**，只是展示详略不同。
因此"第 N 天""下次经期预计"在两页必然一致——这是结构保证，不靠人工同步。

### 空的就真的不显示

主页在推不出预测时**不显示那一行**（`nextPeriodStart == null` 时整行不渲染），
而不是换一句话重复"还没有记录"——环的圆心已经说了。写两句话表达同一件事，是本仓库明确避免的。

### 文件搬迁

- `ui/calendar/CycleRing.kt`、`CycleRingData.kt` 及两个测试 → `ui/home/`（用 `git mv`，保留历史）。
- `CalendarUiState` / `CalendarViewModel` 里的 `ring` 字段删除：留着就是没人读的死字段。
- 日历页的 `CalendarTopSection` → 改名 `PredictionSection`（现在只剩预测卡）。

### 签名

- keystore：`keystore/lunartrace-release.jks`（RSA 2048，有效期 10000 天）。
  **`.gitignore` 已排除 `*.jks` 与 `keystore.properties`**，两者永不入库。
- 签名材料只放在 `keystore.properties`；`app/build.gradle.kts` 缺它时**明确失败**，不产出 unsigned 包
  （这条防线是上一轮就有的，本轮只是把它真正用起来）。
- **keystore 丢失 = 无法再覆盖升级**，因此必须单独备份（见 `DEVELOP.md` §3）。
- release 仍然 `isMinifyEnabled = false`：R8 混淆后的运行时行为**没有任何自动化验证**，
  给使用者测的包不该同时引入一个未验证的变量。

## Testing Decisions

- 本轮**无新增可测逻辑**：主页的取值只是把既有纯函数（`CyclePredictor.analyze` + `CycleRingData.of`）组合起来，
  这两者都已有测试。搬家后的 `CycleRingDataTest`（6 用例）与 `CycleRingGeometryTest`（3 用例）**必须仍然全绿**——
  包名变化不应改变行为，这是本次搬迁唯一的自动化保障。
- **不测**：导航本身（无导航测试基建）、签名（靠 `apksigner` 人工验证）、主页视觉。
- 签名验证命令：`apksigner verify --print-certs --verbose app-release.apk`
  —— 必须打印出签名者 DN 与 SHA-256 指纹，否则不算"签名成功"。

## Out of Scope

- "我的"标签页（设置仍从日历页右上角进入）。
- 主页显示症状/统计摘要。
- 主页的"经期结束"提示（参考设计里有，但需要"预计经期长度"的口径，另开一轮）。
- R8/混淆构建的验证。
- 应用图标与商店素材。

## Further Notes

- 主页顶栏本轮不加（保持纯内容）；设置入口暂时只有日历页右上角，多一次点击。
  若真机上觉得别扭，下一轮把设置提到主页顶栏或独立成"我的"页。
- **签名包与 debug 包签名不同**，同一个 `applicationId` 不能共存：
  从旧 debug 包升级必须先卸载。
