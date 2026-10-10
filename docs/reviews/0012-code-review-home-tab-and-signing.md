# 代码自审：主页标签页 + 签名发布包

独立性：**同源自审（不可得独立评审）**
日期：2026-10-10
范围：`specs/changes/home-tab/spec.md` 的全部实现
触发原因：改动命中导航结构与签名材料，按 `docs/agents/workflow.md` §5 必须留下自审记录。

---

## Standards

本轮 `detekt` 0 条（上一轮它拦下 7 条，本轮的改动面更小）。以下是判断题：

- `ui/LunartraceRoot.kt` —— 两个手写的 `NavigationBarItem` 换成一个 `BottomTab` 枚举：
  路由、文案、图标、顺序收在一处，"顺序即展示顺序"变成结构事实而不是注释。
- `ui/LunartraceRoot.kt` —— 新增 `navigateToTab`：判断用 `currentDestination?.route`（而不是传入的字符串），
  判定与动作放在一起，也顺手去掉了原先两处重复的 `if (currentRoute != X)`。
- 搬迁用 `git mv`，且**只改了 package 行**，文件内容一字未动 —— 行为不可能因此漂移。
  三个测试文件随之搬迁，`CycleRingDataTest`（6 用例）与 `CycleRingGeometryTest`（3 用例）仍全绿，
  这是本次搬迁唯一的自动化保障。
- `ui/calendar/CalendarUiState.kt` / `CalendarViewModel.kt` —— 删掉 `ring` 字段。
  留着就是"没人读的死字段"，而且会让人以为日历页还会画环。
- `ui/home/HomeScreen.kt` —— 主页**只读**，不放任何记录控件：
  记录面板与日历强耦合（点日期即在下方记录），搬到主页会产生两处编辑同一天的状态。
- `ui/home/HomeScreen.kt` —— 推不出预测时**整行早退不渲染**，不写第二句"还没有记录"：
  环的圆心已经说过一次了。写两句话表达同一件事是本仓库明确避免的。

### 签名材料（本轮的新风险面）

- keystore 密码由脚本随机生成、**没有经过对话**，只写进入库被排除的 `keystore.properties`
  （`git check-ignore` 已确认 `keystore.properties` 与 `*.jks` 都被忽略）。
- **签名不是"构建成功"能证明的事**：`assembleRelease` 绿了几轮，但这一轮之前它产出的一直是 unsigned 包。
  因此本轮把 `apksigner verify --print-certs` 作为**必须执行的验证**，并把它写进 `DEVELOP.md`
  —— 结论必须有证据，这是我能在本仓库真正做到的那部分。
- release 仍保持 `isMinifyEnabled = false`：R8 混淆后的运行时行为**没有任何自动化验证**，
  给使用者测的包不该同时引入一个未验证的变量。**记为技术债**（见未达标项）。
- `dist/` 加入 `.gitignore`：APK 是构建产物，11 MB 的二进制一旦入库就永久留在历史里。

## Spec

- 主页标签页（启动页）+ 环迁到 `ui/home/` ✅
- 日历页移除环、`CalendarTopSection` 改名 `PredictionSection` ✅
- release 签名打通 + `apksigner` 验证 ✅
- **超出 spec 的两处文档修正（记录在案）**：
  1. `DEVELOP.md` 的签名段原来写的是通用路径 `lunartrace.jks`，与仓库里实际存在的
     `keystore/lunartrace-release.jks` 不一致 → 改为记录**实际**材料与验证命令。
  2. `keystore.properties.example` 的 `storeFile` 同步改为 `keystore/lunartrace-release.jks`。
     模板与现实不一致，就会在换机器时把人带进坑里。
- **未做（Out of scope，已写进 spec）**：不加"我的"页、主页不显示症状摘要、
  不做"经期结束"提示（需要"预计经期长度"的口径，另开一轮）、不做 R8 验证、不做商店素材。

### 汇总

Standards 11 条（最严重：签名长期处于"看起来能用、其实是 unsigned"的状态，
本轮才第一次拿 `apksigner` 证明；其余为判断题与一处真债记录）；
Spec 2 条（最严重：无 —— 范围与 spec 一致，两处文档漂移修正已记录）。

**两轴不跨轴选赢家。**

---

## 自评表（合并前，5 项全 1 才算完成）

| 维度 | 结论 | 依据 |
|---|---|---|
| 门禁 | **1** | detekt 0 条；`testDebugUnitTest` 84 用例 0 失败；覆盖率门禁通过；`assembleDebug` + `assembleRelease` 均成功 |
| 红线 | **1** | 隐私守卫 PASS（6 项）；签名材料经 `git check-ignore` 确认不入库；未新增权限/依赖；未触碰存储与日志 |
| 测试 | **1** | 本轮无新增逻辑（主页只是组合既有纯函数），搬迁后既有 9 个环用例仍全绿；**无红先绿后**——因为本轮没有新行为可红（不是"跳过了"，是没有对象） |
| 范围 | **1** | 改动限于 `ui/home/`、导航、日历页清理、`DEVELOP.md`/模板、`.gitignore`、spec 与自审 |
| 验证 | **1** | 见下；`apksigner` 指纹是新增的硬证据 |

**结果：5/5。**

## 证据（收口时重跑）

```
# 1) 全门禁 + 两种变体
BUILD SUCCESSFUL in 50s（detekt + testDebugUnitTest + jacocoTestReport + assembleDebug + assembleRelease）
detekt.txt 行数 = 0

# 2) 测试与覆盖率
unit_tests=84 failures=0
PASS io/lunartrace/app/domain/prediction: 行覆盖率 100.0%（99/99，门槛 85%）
PASS io/lunartrace/app/data/security:    行覆盖率 92.9%（13/14，门槛 85%）

# 3) 签名（本轮第一次真正验证，而不是靠 BUILD SUCCESSFUL）
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate DN: CN=Lunartrace, OU=Solo Developer, O=Lunartrace, L=Unknown, ST=Unknown, C=CN
Signer #1 certificate SHA-256 digest: 64732d4dde078b7569796c4daad8395bcff78f715c9b80ce1e941d7e8eabab9e
Signer #1 key algorithm: RSA / key size: 2048
产物：app/build/outputs/apk/release/app-release.apk（11.18 MB）

# 4) 不进库（签名材料的红线）
.gitignore:19:keystore.properties	keystore.properties
.gitignore:20:*.jks	keystore/lunartrace-release.jks
.gitignore:24:dist/	dist/lunartrace-0.1.0-release.apk

# 5) 脚本
check-privacy-guards.py → PASS（6 项）
check-docs.py → PASS（9 项）
```

## 未达标项与原因（不允许留空）

1. **主页视觉与导航未经真机确认**：无导航测试基建，底部三标签的排布、主页留白、环在无顶栏页面里的观感，
   都只能由使用者截图判断。
2. **R8 混淆构建从未验证**：release 的 `isMinifyEnabled` 仍为 `false`。
   这是刻意选择（不给测试包引入未验证变量），但它是真债：正式发布前必须单独验证一次混淆构建。
3. **主页没有直达设置**：设置入口仍在日历页右上角。已在 spec 的 Further Notes 记为待定项。
4. **签名方案只有 v2**（v1 与 v3 为 false）。minSdk 26 下这是正常的，Android 8.0+ 都能装；
   但若将来把 minSdk 降到 24 以下，必须重新开启 v1，否则旧机会拒装。
5. **无独立评审**：结论仍由同一模型自审产生。
