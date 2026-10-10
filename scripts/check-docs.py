#!/usr/bin/env python3
"""文档一致性检查（L2）。

只做机械可判定的检查：文件是否存在、状态是否合法、必填段是否齐全、
AGENTS.md 是否与 CLAUDE.md 漂移、CODEOWNERS 是否被注释成空转。
**不检查内容质量**——那是人的事。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

# Windows 控制台默认 GBK，输出 ✅/中文会抛 UnicodeEncodeError
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO_ROOT = Path(__file__).resolve().parent.parent

VALID_SPEC_STATUS = {"draft", "ready", "in-progress", "done", "cancelled"}
REQUIRED_SPEC_SECTIONS = [
    "## Problem Statement",
    "## Solution",
    "## User Stories",
    "## Implementation Decisions",
    "## Testing Decisions",
    "## Out of Scope",
    "## Further Notes",
]
MIN_ACTIVE_CODEOWNERS_RULES = 3

FAILURES: list[str] = []
CHECKS: list[str] = []


def fail(message: str) -> None:
    FAILURES.append(message)


def record(description: str) -> None:
    CHECKS.append(description)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def check_entry_files() -> None:
    record("AGENTS.md 与 CLAUDE.md 内容一致")
    agents = REPO_ROOT / "AGENTS.md"
    claude = REPO_ROOT / "CLAUDE.md"
    if not claude.exists():
        fail("CLAUDE.md 缺失（仓库唯一入口）")
        return
    if not agents.exists():
        fail("AGENTS.md 缺失（应为 CLAUDE.md 的硬链接）")
        return
    if read(agents) != read(claude):
        fail("AGENTS.md 与 CLAUDE.md 内容不一致：两份入口必然漂移，请重建硬链接")


def check_claude_route_table() -> None:
    record("CLAUDE.md 事实入口表指向的文件都存在")
    text = read(REPO_ROOT / "CLAUDE.md")
    referenced = set(re.findall(r"`([A-Za-z0-9_./-]+\.md)`", text))
    referenced |= set(re.findall(r"`(specs/[A-Za-z0-9_./-]+)`", text))
    for target in sorted(referenced):
        if not (REPO_ROOT / target).exists():
            fail(f"CLAUDE.md 引用了不存在的文件：{target}")


def check_change_specs() -> None:
    record("change spec 的状态合法且必填段齐全")
    spec_files = sorted((REPO_ROOT / "specs" / "changes").glob("*/spec.md"))
    if not spec_files:
        fail("specs/changes/ 下没有任何 spec.md")
    for path in spec_files:
        text = read(path)
        status = re.search(r"^Status:\s*(\S+)\s*$", text, flags=re.MULTILINE)
        if status is None:
            fail(f"{path}: 缺少 'Status:' 行")
        elif status.group(1) not in VALID_SPEC_STATUS:
            fail(f"{path}: 非法状态 {status.group(1)}（允许：{sorted(VALID_SPEC_STATUS)}）")
        for section in REQUIRED_SPEC_SECTIONS:
            if section not in text:
                fail(f"{path}: 缺少必填段 {section}")


def check_capabilities() -> None:
    record("capability 含量化反例（✅/❌）与提交前自检清单")
    capability_files = sorted((REPO_ROOT / "specs" / "capabilities").glob("*.md"))
    if not capability_files:
        fail("specs/capabilities/ 下没有任何 capability")
    for path in capability_files:
        text = read(path)
        if "✅" not in text or "❌" not in text:
            fail(f"{path}: 必须用 ✅/❌ 对写可判定的约束，不写抽象原则")
        if "自检" not in text:
            fail(f"{path}: 缺少提交前自检清单")
        if len(text.splitlines()) < 30:
            fail(f"{path}: 内容过短，疑似占位")


def check_adrs() -> None:
    record("ADR 都有 Status 行")
    for path in sorted((REPO_ROOT / "docs" / "adr").glob("*.md")):
        if not re.search(r"^Status:\s*\S+", read(path), flags=re.MULTILINE):
            fail(f"{path}: 缺少 'Status:' 行")


def check_context_entries_have_avoid() -> None:
    record("CONTEXT.md 每条术语都带 _Avoid_ 反例")
    text = read(REPO_ROOT / "CONTEXT.md")
    entries = len(re.findall(r"^- \*\*", text, flags=re.MULTILINE))
    # 只数"独立成行"的 _Avoid_（正文里提到这三个字不算反例）
    avoids = len(re.findall(r"^\s*_Avoid_", text, flags=re.MULTILINE))
    if entries == 0:
        fail("CONTEXT.md 中没有任何术语条目")
    elif entries != avoids:
        fail(f"CONTEXT.md: {entries} 条术语，但只有 {avoids} 个 _Avoid_ 反例")


def check_develop_scripts_exist() -> None:
    record("DEVELOP.md 引用的 scripts/* 都存在")
    text = read(REPO_ROOT / "DEVELOP.md")
    for target in sorted(set(re.findall(r"scripts/[A-Za-z0-9_./-]+", text))):
        if not (REPO_ROOT / target).exists() and not (REPO_ROOT / target).with_suffix("").exists():
            fail(f"DEVELOP.md 引用了不存在的脚本：{target}")


def check_codeowners_active() -> None:
    record("CODEOWNERS 已启用（不是全注释空转）")
    path = REPO_ROOT / ".github" / "CODEOWNERS"
    if not path.exists():
        fail(".github/CODEOWNERS 缺失：评审的强制力来自它，不来自工具的存在")
        return
    active = [
        line
        for line in read(path).splitlines()
        if line.strip() and not line.strip().startswith("#")
    ]
    if len(active) < MIN_ACTIVE_CODEOWNERS_RULES:
        fail(f"CODEOWNERS 只有 {len(active)} 条生效规则，少于 {MIN_ACTIVE_CODEOWNERS_RULES} 条（等于空转）")


def check_no_todos_in_main() -> None:
    record("生产源码中没有 TODO / FIXME")
    main_root = REPO_ROOT / "app" / "src" / "main"
    for path in sorted(main_root.rglob("*")):
        if path.is_file() and path.suffix in {".kt", ".xml"}:
            text = read(path)
            for token in ("TODO", "FIXME"):
                if token in text:
                    fail(f"{path}: 存在 {token}，完成前必须清理")


def main() -> int:
    check_entry_files()
    check_claude_route_table()
    check_change_specs()
    check_capabilities()
    check_adrs()
    check_context_entries_have_avoid()
    check_develop_scripts_exist()
    check_codeowners_active()
    check_no_todos_in_main()

    if FAILURES:
        print("FAIL: 文档一致性检查未通过")
        for item in FAILURES:
            print(f"  - {item}")
        return 1

    print(f"PASS: 文档一致性检查通过（检查 {len(CHECKS)} 项）")
    for description in CHECKS:
        print(f"  - {description}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
