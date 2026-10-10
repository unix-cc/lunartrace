# skill 清单与准入规则

> 本文只回答"有哪些流程 skill、能不能加新的"。**不写 skill 内部内容**（在各 `SKILL.md`）。

## 1. 现有 skill

| skill | 何时用 | 产物 |
|---|---|---|
| `grill-plan` | 需求模糊 / 跨域 / 破坏性 / 难回滚时，把想法收敛成可审计划 | `specs/changes/<slug>/spec.md`(draft) + `docs/reviews/<NN>-plan-review-*.md` |
| `to-spec` | 对话已收敛，要固化成跨会话可用的规格 | `specs/changes/<slug>/spec.md`(ready) |
| `code-review` | 代码就绪、未提交时，审查完整工作区 diff | `docs/reviews/<NN>-code-review-*.md`（双轴分列） |

## 2. 准入规则

1. 每个 skill 必须包含 `SKILL.md` 与 `agents/openai.yaml`。
2. 新增 skill 前必须回答：**它替代哪一次重复劳动？现有 skill 为什么覆盖不了？**
3. 同一职责只允许一个 skill。

## 3. 禁止事项

> **禁止重新引入多套并行工作流。** 本仓库只允许这一套流程入口。
> 历史上（bk-lite 样本）曾同时维护 OpenSpec、OPSX、Superpowers、ProjectMem、CodeGraph，
> 结果是多套强制入口重复加载指令、制造额外工具回合，并让清晰的小改承担与风险不匹配的流程成本。

具体禁止：引入任何第二套 spec 体系、第二套 review 协议、并行 UI/UX skill 套件、或把同一份约束复制到多个入口文件。

## 4. 关闭隐式调用（关键）

高成本入口**默认关闭**：`grill-plan` 与 `to-spec` 只在 `docs/agents/workflow.md` §3 的触发条件成立时**显式**调用。
日常对话不自行进入高成本流程。
