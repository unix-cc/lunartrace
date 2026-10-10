---
name: grill-plan
description: 需求模糊、跨域、破坏性迁移或难回滚时使用。把想法收敛成 draft 状态的 change spec，并做一轮带证据的对抗审查（轮次封顶 5 轮）。
---

# grill-plan

**只在 `docs/agents/workflow.md` §3 的触发条件成立时使用。默认关闭，不自动进入。**

## 步骤

1. **先核对事实**：读相关 `specs/capabilities/`、受影响代码、`docs/adr/`。不臆测现状。
2. **写 plan 草案**：范围、口径表（可验收的定义）、不做什么、Out of Scope。
3. **对抗审查**：逐条给发现，每条必须满足二者之一：
   - 有直接代码/文件证据；或
   - 会影响数据正确性、隐私或资源边界。
   把"我认为"挡在门外。
4. **每轮结尾写 `VERDICT: REVISE` 或 `VERDICT: APPROVED`**，并把轮次与处置记入 `docs/reviews/<NN>-plan-review-<slug>.md`。
5. **轮次封顶 5 轮**。到达上限仍未收敛 → 缩小范围重来，而不是继续磨。
6. 收敛后把 spec 状态改为 `ready`。

## 单人项目的强制诚实项

- 审查记录必须标注 `独立性：同源自审（不可得独立评审）`。
- 不许把"我审查过了"写成"已获得独立评审"。
- 找不到任何实质性问题时，**明确写"本轮未发现新的实质性阻塞问题"**，不许为了显得认真而编造意见。

## 产出

- `specs/changes/<slug>/spec.md`（`draft` → `ready`）
- `docs/reviews/<NN>-plan-review-<slug>.md`
