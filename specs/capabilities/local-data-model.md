# 本地数据模型规范 / 高频陷阱清单

> 用法：改表结构、转换器、Repository 或任何持久化相关代码前，对照本文件。
> 本文只管"数据怎么存、什么不变量不能被破坏"。隐私处置见 `data-privacy.md`，预测口径见 `cycle-prediction.md`。

## 1. 日期存储（最高频 —— 务必先看）

- ✅ 表里存 `Long epochDay`（`LocalDate.toEpochDay()`），经 `Converters` 与领域层的 `LocalDate` 互转。
  - ❌ 存毫秒时间戳 → 时区变化、跨日边界都会让"同一天"变成不同的值，日历上会出现重影。
- ✅ `LocalDate` ↔ `epochDay` 的转换**只有一处**（`data/local/Converters.kt`）。
  - ❌ 在 DAO/Repository/ViewModel 里各自 `toEpochDay()` → 口径分叉。
- ✅ 日期是**主键**：一天最多一条经期记录、一天最多一条症状记录。
  - ❌ 用自增 id 做主键 + 无唯一约束 → 重复标记产生重复行，统计翻倍。

## 2. 表结构不变量

| 表 | 主键 | 字段 | 不变量 |
|---|---|---|---|
| `period_days` | `date` | `flow: String`（枚举名） | 删除即真删；`flow` 必须是 `FlowIntensity` 的合法名 |
| `symptom_entries` | `date` | `symptoms: String`（逗号分隔，**字典序**）、`mood: String?`、`note: String?` | 空集合必须删行，而不是存空串行 |

- ✅ 枚举 ↔ 字符串的转换集中在一处，未知字符串**降级为忽略**而不是抛异常。
  - ❌ 直接 `FlowIntensity.valueOf(raw)` → 历史数据里出现被删掉的枚举名时，应用当场崩溃。
- ✅ 症状集合编码前**排序**后再拼接，保证同样的集合得到同样的字符串。
  - ❌ 依赖 `Set` 迭代顺序 → 同一集合产生不同字符串，去重与比较失效。
- ✅ `note` 长度上限 500 字符，UI 侧限制输入。
  - ❌ 无上限 → 单条记录膨胀，导出/渲染卡顿（对应"可膨胀输入"风险）。

## 3. 观察与刷新

- ✅ 所有查询返回 `Flow<...>`，UI 随数据自动刷新；不使用一次性查询 + 手动刷新。
  - ❌ 写库后靠 `notifyDataSetChanged` 式手动刷新 → 统计与日历不同步。
- ✅ Repository 的写操作是 `suspend`，在 `Dispatchers.IO` 上执行。
- ✅ 主键冲突用 `OnConflictStrategy.REPLACE`（"一天一条"语义下的正确选择）。
  - ❌ 用默认 ABORT → 重复标记直接抛异常闪退。

## 4. schema 与迁移

- ✅ `exportSchema = true`，schema JSON 提交入库（`app/schemas/`）。
- ✅ 改表结构必须同时：提升 `version`、提供 `Migration`、提交新 schema JSON、在 change spec 里说明数据影响（**触发 Grill 路径**）。
  - ❌ 用 `fallbackToDestructiveMigration()` → 用户历史记录被静默清空（这是最高严重度的数据事故）。
- ✅ 当前版本无历史包袱：`version = 1`，无 `Migration` 对象。
  - ❌ 现在就写空 `Migration` 占位 → 预防性过度设计（`CLAUDE.md` §4）。

## 5. 清除数据

- ✅ `wipeAll()` 清空两张表并经 `SettingsStore` 复位锁设置（真删除）。
- ✅ 该操作是**幂等**的：重复调用结果一致，不抛异常。
  - ❌ 依赖"表非空"才执行 → 空库时崩溃。

## 6. 事务

- ✅ 需要跨表一致性的写操作放在 `@Transaction` 或 `withTransaction { }` 内。
- ❌ 当前版本**没有**跨表事务需求；不要为"以后可能需要"预留事务抽象。

## 提交前快速自检

- [ ] 没有新增第二种日期存储形式
- [ ] 日期仍是主键，且冲突策略是 REPLACE
- [ ] 枚举解析对未知值做了降级处理
- [ ] 症状集合编码前已排序
- [ ] 表结构改动已提升 version + 提供 Migration + 提交 schema JSON
- [ ] `wipeAll()` 仍然覆盖所有表与设置
