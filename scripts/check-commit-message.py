#!/usr/bin/env python3
"""提交信息规范检查（L1，commit-msg 阶段）。

格式：type(scope): 描述        scope 可省略
type ∈ feat|fix|docs|refactor|test|chore|build|perf|ci|revert
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

# Windows 控制台默认 GBK，输出中文会抛 UnicodeEncodeError
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

TYPES = ("feat", "fix", "docs", "refactor", "test", "chore", "build", "perf", "ci", "revert")
PATTERN = re.compile(rf"^({'|'.join(TYPES)})(\([a-z0-9_./-]+\))?: \S.{{1,}}")
MAX_SUBJECT_LENGTH = 100
BYPASS_MARKER = "[skip-hook]"


def main() -> int:
    if len(sys.argv) < 2:
        print("用法: check-commit-message.py <commit-msg-file>")
        return 1

    subject = Path(sys.argv[1]).read_text(encoding="utf-8").splitlines()[0].strip()

    # 合并提交与显式绕过留痕
    if subject.startswith("Merge ") or subject.startswith("Revert "):
        print(f"PASS: 合并/回滚提交，跳过格式检查：{subject}")
        return 0
    if BYPASS_MARKER in subject and len(subject.split(BYPASS_MARKER)[1].strip()) > 0:
        print(f"WARN: 显式绕过门禁（已留痕）：{subject}")
        return 0

    if len(subject) > MAX_SUBJECT_LENGTH:
        print(f"FAIL: 提交信息超过 {MAX_SUBJECT_LENGTH} 字符：{subject}")
        return 1

    if not PATTERN.match(subject):
        print("FAIL: 提交信息不符合 'type(scope): 描述'")
        print(f"      实际：{subject}")
        print(f"      可用 type：{', '.join(TYPES)}")
        print("      示例：feat(calendar): 支持点击日期记录流量")
        return 1

    print(f"PASS: 提交信息格式正确：{subject}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
