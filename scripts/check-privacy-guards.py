#!/usr/bin/env python3
"""隐私红线守卫（L1，唯一强制层）。

对应 specs/capabilities/data-privacy.md 中可自动判定的条目。
任何一条不通过即退出码 1，pre-commit 会直接拦住提交。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

# Windows 控制台默认 GBK，输出中文会抛 UnicodeEncodeError
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO_ROOT = Path(__file__).resolve().parent.parent
MAIN_SOURCES = REPO_ROOT / "app" / "src" / "main"
MANIFEST = MAIN_SOURCES / "AndroidManifest.xml"
KOTLIN_ROOT = MAIN_SOURCES / "java"

FAILURES: list[str] = []
CHECKS: list[str] = []

# 扫描前先去掉注释：否则"文档里说不要用 Log"的注释本身会被当成违规。
# 已知局限：Kotlin 行注释的截断会让同一行里 `//` 之后的内容不参与检查（本仓库不存在这种写法）。
XML_COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)
KOTLIN_BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.DOTALL)
KOTLIN_LINE_COMMENT = re.compile(r"//[^\n]*")


def fail(message: str) -> None:
    FAILURES.append(message)


def record(description: str) -> None:
    CHECKS.append(description)


def read_sources() -> list[Path]:
    return sorted(KOTLIN_ROOT.rglob("*.kt"))


def strip_xml_comments(text: str) -> str:
    return XML_COMMENT.sub("", text)


def strip_kotlin_comments(text: str) -> str:
    return KOTLIN_LINE_COMMENT.sub("", KOTLIN_BLOCK_COMMENT.sub("", text))


def check_manifest_has_no_permissions() -> None:
    record("清单未声明任何权限（尤其是 INTERNET）")
    text = strip_xml_comments(MANIFEST.read_text(encoding="utf-8"))
    permissions = re.findall(r"<uses-permission[^>]*>", text)
    if permissions:
        fail(f"{MANIFEST}: 声明了权限 {permissions}；本应用必须是零网络应用")


def check_allow_backup_disabled() -> None:
    record('清单 android:allowBackup="false"')
    text = MANIFEST.read_text(encoding="utf-8")
    if 'android:allowBackup="false"' not in text:
        fail(f"{MANIFEST}: 必须显式声明 android:allowBackup=\"false\"（否则健康数据会进入系统云备份）")


def check_no_permissions_anywhere_in_main() -> None:
    record("main 源码树中不存在 permission 声明")
    for path in MAIN_SOURCES.rglob("*.xml"):
        text = strip_xml_comments(path.read_text(encoding="utf-8", errors="ignore"))
        if "uses-permission" in text:
            fail(f"{path}: 出现权限声明")


def check_forbidden_api_usage() -> None:
    record("禁止日志 API、SharedPreferences 与破坏性迁移")
    forbidden = {
        re.compile(r"\bLog\.[vdiew]\("): "调用 android.util.Log（数据会进入 logcat）",
        re.compile(r"android\.util\.Log"): "引用 android.util.Log",
        re.compile(r"\bprintln\("): "调用 println",
        re.compile(r"System\.out"): "使用 System.out",
        re.compile(r"printStackTrace\("): "调用 printStackTrace",
        re.compile(r"\bSharedPreferences\b"): "使用 SharedPreferences（必须用 DataStore）",
        re.compile(r"fallbackToDestructiveMigration"): "使用 fallbackToDestructiveMigration()（会静默清空用户记录）",
    }
    for path in read_sources():
        text = strip_kotlin_comments(path.read_text(encoding="utf-8"))
        for pattern, reason in forbidden.items():
            if pattern.search(text):
                fail(f"{path}: {reason}")


def check_flag_secure_present() -> None:
    record("应用锁使用 FLAG_SECURE")
    if "FLAG_SECURE" not in "\n".join(
        strip_kotlin_comments(path.read_text(encoding="utf-8")) for path in read_sources()
    ):
        fail("main 源码中找不到 FLAG_SECURE：应用锁必须阻止最近任务列表泄漏记录内容")


def check_pin_hardening() -> None:
    record("PIN 使用 PBKDF2-HMAC-SHA256 与常量时间比较")
    hasher = KOTLIN_ROOT / "io" / "lunartrace" / "app" / "data" / "security" / "PinHasher.kt"
    if not hasher.exists():
        fail(f"{hasher}: 文件缺失")
        return
    text = hasher.read_text(encoding="utf-8")
    if "PBKDF2WithHmacSHA256" not in text:
        fail(f"{hasher}: 必须使用 PBKDF2WithHmacSHA256")
    if "MessageDigest.isEqual" not in text:
        fail(f"{hasher}: PIN 校验必须使用常量时间比较")


def main() -> int:
    check_manifest_has_no_permissions()
    check_allow_backup_disabled()
    check_no_permissions_anywhere_in_main()
    check_forbidden_api_usage()
    check_flag_secure_present()
    check_pin_hardening()

    if FAILURES:
        print("FAIL: 隐私守卫发现以下问题")
        for item in FAILURES:
            print(f"  - {item}")
        return 1

    print(f"PASS: 隐私守卫通过（检查 {len(CHECKS)} 项）")
    for description in CHECKS:
        print(f"  - {description}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
