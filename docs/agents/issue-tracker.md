# change spec 与 ticket 的本地约定

> 本文只回答"spec / ticket 放在哪、长什么样、状态怎么流转"。**内容怎么写**看 `$to-spec` skill 与 `docs/agents/domain.md`。

## 1. 目录与命名

```
specs/changes/<feature-slug>/
    spec.md                 ← 唯一主规格，固定名字
    tickets/                ← 仅在工作超出上下文窗口或存在真实阻塞边时创建
        <NN>-<slug>.md      ← 按依赖顺序编号
```

- 一个 material feature 一个目录；slug 用 kebab-case。
- **不创建** proposal / design / plan / delta / tasks sidecar。
- 完成后**原地更新状态，不 archive**（少一个目录就少一处漂移源）。

## 2. 状态机

```
draft → ready → in-progress → done
                            ↘ cancelled
```

| 状态 | 含义 |
|---|---|
| `draft` | 还在收敛需求，禁止据此写生产代码 |
| `ready` | 已通过对抗审查，可以开工 |
| `in-progress` | 正在实现 |
| `done` | 已收口：门禁跑过、自审记录已落、capability 已同步 |
| `cancelled` | 明确放弃（保留文件与原因，别删——删了就不知道当初为什么不做） |

`scripts/check-docs.py` 会校验：所有 `spec.md` 首部必须有合法的 `Status:` 行，且五个必填段齐全。

## 3. spec.md 必填结构

```markdown
# <Feature title>

Status: draft

## Problem Statement      ← 从用户视角描述问题
## Solution               ← 从用户视角描述方案
## User Stories           ← 只列 material 的、用户可见的行为
## Implementation Decisions ← 模块/接口/技术澄清/schema/契约/交互
## Testing Decisions      ← 必填：在哪个接缝测、什么算好测试、同类先例
## Out of Scope
## Further Notes
```

## 4. 四条硬规矩

1. **不写具体文件路径与代码片段** —— 会很快过期。
   例外：如果片段比散文更精确地编码了某个决定（状态机、schema、type shape），内联它，裁剪到只有决定的部分，并注明来源。
2. **Testing Decisions 是必填项** —— 写 spec 时就要定"在哪个接缝测"。已有接缝优先，用**尽可能高的接缝**。如果对话没点名接缝，从附近代码推断最高接缝并记进 spec，而不是重开一轮访谈。
3. **User Stories 不是需求清单** —— 只写用户可见的行为与重要边界情况。
4. **不重新访谈** —— `$to-spec` 的职责是综合已收敛的对话。

## 5. ticket 约定

- 每片切一条**窄但完整**的路径穿过所有层（schema → 数据 → ViewModel → UI → 测试），垂直而非水平。
- 完成后能**独立演示或验证**。
- 每片尺寸 ≈ 一个干净的上下文窗口。
- 预重构先做：Make the change easy, then make the easy change.
- 每片声明 **blocking edges**：必须哪些片先完成。无 blocker 的可以立刻开工。
- 一次只做**一个 frontier ticket**，做完清空上下文。

宽重构不硬切垂直片，走 expand–contract：Expand（新形式与旧形式并存）→ Migrate（按目录/按包分批，逐片 `blocked by expand`）→ Contract（无调用方后删旧形式，`blocked by` 每个迁移批次）。
