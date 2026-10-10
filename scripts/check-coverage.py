#!/usr/bin/env python3
"""覆盖率硬门禁。

只对"改错代价最高"的两个模块设门禁，不对全仓设软门禁：
  - io/lunartrace/app/domain/prediction  预测口径（改错会让历史数据的解读变味）
  - io/lunartrace/app/data/security      PIN 派生（安全路径）

数据来源：./gradlew jacocoTestReport 生成的 XML。
"""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

# Windows 控制台默认 GBK，输出中文会抛 UnicodeEncodeError
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO_ROOT = Path(__file__).resolve().parent.parent
REPORT = REPO_ROOT / "app" / "build" / "reports" / "jacoco" / "jacocoTestReport" / "jacocoTestReport.xml"

GATES = {
    "io/lunartrace/app/domain/prediction": 0.85,
    "io/lunartrace/app/data/security": 0.85,
}


def line_coverage(package: ET.Element) -> tuple[int, int]:
    for counter in package.findall("counter"):
        if counter.get("type") == "LINE":
            return int(counter.get("covered", "0")), int(counter.get("missed", "0"))
    return 0, 0


def main() -> int:
    if not REPORT.exists():
        print(f"FAIL: 找不到覆盖率报告 {REPORT}")
        print("      先执行 ./gradlew jacocoTestReport")
        return 1

    packages = {package.get("name"): package for package in ET.parse(REPORT).getroot().findall("package")}
    failures: list[str] = []

    for name, threshold in sorted(GATES.items()):
        package = packages.get(name)
        if package is None:
            failures.append(f"{name}: 报告中没有该模块的任何覆盖数据（说明它没有被测试）")
            continue
        covered, missed = line_coverage(package)
        total = covered + missed
        if total == 0:
            failures.append(f"{name}: 没有任何可执行行被统计")
            continue
        ratio = covered / total
        verdict = "PASS" if ratio >= threshold else "FAIL"
        print(f"{verdict} {name}: 行覆盖率 {ratio:.1%}（{covered}/{total}，门槛 {threshold:.0%}）")
        if ratio < threshold:
            failures.append(f"{name}: {ratio:.1%} < {threshold:.0%}")

    if failures:
        print("FAIL: 覆盖率门禁未通过")
        for item in failures:
            print(f"  - {item}")
        return 1

    print(f"PASS: 覆盖率门禁通过（模块数 {len(GATES)}）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
