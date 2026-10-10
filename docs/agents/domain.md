# 领域文档约定

> 本文只回答"文档之间怎么分工、事实从哪来"。**不写任何能力细节**（那在 `specs/capabilities/`）。

## 1. 单一事实源，职责不重叠

| 问题 | 唯一归属 |
|---|---|
| 这个词是什么意思 | `CONTEXT.md` |
| 这个能力长期怎么工作 | `specs/capabilities/` |
| 这次要改什么 | `specs/changes/<feature>/spec.md` |
| 为什么当初这么决定 | `docs/adr/` |
| 日常怎么做 | `docs/agents/` |
| 怎么跑命令 | `DEVELOP.md` |

**禁止复制**：不要在 `CONTEXT.md` 或 ADR 中复制 capability。代码符号、表字段、组件树和测试断言由代码维护。

每份文档开头必须有一句"本文只管 X，Y 去别处"。

## 2. 事实来源优先级

写 capability 时按此顺序取证，**以代码为准、以事实说话、可核对、不夸大**：

```
1. 源代码（最高）
   - 枚举 / 状态机 / 默认值 → domain/model/、constants
   - 表结构 → data/local/entity/、AppDatabase 的 schema JSON
   - 算法口径 → domain/prediction/
2. change spec —— 功能域划分与业务规则主线
3. PRODUCT.md —— 仅理解产品意图，不得作为功能或数字的唯一依据
```

capability **不是规划**，是现状描述。只记录"当前版本已实现的能力"，不写规划、演示或营销话术。

## 3. 漂移治理

- change spec 完成后**原地改状态、不 archive**；只允许一份主 spec。
- 每次改行为，同步更新对应 capability（否则 capability 就变成谎言）。
- 定期做对账：`python scripts/check-docs.py` 做机械检查（文件存在性、状态合法性、必填段、AGENTS≡CLAUDE）；人工部分靠"改行为即改文档"的纪律。
- 已知无法自动化的漂移：capability 里描述的口径与代码实现不一致。**对此不设假门禁**，标注为 L3，靠 code review 的 Spec 轴发现（见 `docs/agents/workflow.md` §5）。

## 4. capability 写法要点

- 一个能力一个文件，文件名 kebab-case。
- 全篇用 **✅/❌ 对**，不写抽象原则（"注意隐私"无效，"❌ 在 logcat 打印日期 → 本机任意应用可读"才可判定）。
- **按高频程度排序**，不按逻辑完整性排序；第一节应该能标"最高频 —— 务必先看"。
- 结尾放**提交前自检清单**，把整篇压缩成可勾选项。
- 只写"现有版本已实现的能力"；未落地的门禁必须显式标注为 L3，禁止写成已生效。
