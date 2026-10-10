---
name: to-spec
description: 对话已经收敛、需要固化成跨会话可用的 change spec 时使用。综合已有结论，不重新访谈。
---

# to-spec

**职责是综合，不是再问一轮。** 对话里没结论的地方，从附近代码推断最高接缝并记录该决定。

## 必须写全的段

`specs/changes/<slug>/spec.md`，结构见 `docs/agents/issue-tracker.md` §3。
其中 **Testing Decisions 必填**：写清在哪个接缝测、什么算好测试、同类先例。

## 四条硬规矩

1. 不写具体文件路径与代码片段（会过期）。例外：状态机 / schema / type shape 用最小片段内联，并注明来源。
2. 接缝优先复用已有的，并选**尽可能高**的接缝。
3. User Stories 只写用户可见行为与重要边界，不写需求清单。
4. 完成后状态设为 `ready`；不创建 proposal / design / plan / delta / tasks 这类 sidecar 文档。

## 产出

- `specs/changes/<slug>/spec.md`（`ready`）
