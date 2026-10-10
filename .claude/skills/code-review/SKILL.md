---
name: code-review
description: 代码就绪但尚未提交时审查完整工作区 diff。双轴并行（Standards / Spec），不合并、不重排、不跨轴选赢家。
---

# code-review

## 双轴（并行跑，**不合并、不重排序**）

- **Standards 轴**：代码是否符合仓库文档化的规范？对照 `specs/capabilities/*.md`、`CLAUDE.md` §2。
- **Spec 轴**：代码是否忠实实现了来源 change spec / capability contract？

为什么必须分开：一个变更可能过一条轴、挂另一条轴。
遵守了所有规范但实现错了东西 → Standards 过、Spec 挂；
完全按需求做了但破坏约定 → Spec 过、Standards 挂。分开报告才能阻止一轴掩盖另一轴。

## Standards 轴的坏味道基线（12 条，仓库没写也要用）

Mysterious Name / Duplicated Code / Feature Envy / Data Clumps / Primitive Obsession /
Repeated Switches / Shotgun Surgery / Divergent Change / Speculative Generality /
Message Chains / Middle Man / Refused Bequest

两条约束：**仓库文档优先于基线**；**基线永远是判断题，不是硬违规**。

## 聚合格式

```
## Standards
（发现逐条列出，每条带文件与行号）

## Spec
（发现逐条列出，每条对照 spec 的哪一段）

汇总：Standards N 条（最严重：…）；Spec M 条（最严重：…）
```

最后一行只汇总，**不跨轴选一个赢家**。

## 单人项目的强制诚实项

- 记录必须标注 `独立性：同源自审（不可得独立评审）`。
- 没发现问题就写"未发现问题"，不许编造。
- 产出写入 `docs/reviews/<NN>-code-review-<slug>.md`。
