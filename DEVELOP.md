# DEVELOP.md — 命令、构建与排障

> 本文只回答"怎么跑"。命令事实的唯一来源。文档里引用的每个脚本都在 `scripts/` 下真实存在（由 `scripts/check-docs.py` 校验）。

## 0. 一次性环境准备

需要 JDK 17、Android SDK（platform-34、build-tools-34.0.0）。Android Studio 打开本目录即可自动生成 `local.properties`。

命令行方式（本机已验证可用）：

```bash
# 1) 指向 JDK 17（pre-commit 门禁也需要能找到 JDK，否则它会明确失败并提示怎么修）
set JAVA_HOME=<你的 JDK 17 路径>

# 2) 生成 local.properties（本文件不入库；路径用正斜杠，Windows 下同样合法）
echo sdk.dir=C:/Users/Administrator/AppData/Local/android-toolchain/android-sdk > local.properties

# 3) 安装本地门禁（把 scripts/hooks/* 复制进 .git/hooks）
powershell -ExecutionPolicy Bypass -File scripts/install-hooks.ps1
```

**不想每次设 `JAVA_HOME`**：在 `~/.gradle/gradle.properties` 写 `org.gradle.java.home=<JDK 17 路径>`。
（不要写进仓库的 `gradle.properties`——那是会入库的公共配置。）

## 1. 日常循环

```bash
./gradlew :app:assembleDebug      # 编译，产物 app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest  # JVM 单元测试（CI 跑的就是这个）
./gradlew detekt                  # 静态检查（L1 门禁的一部分）
./gradlew :app:jacocoTestReport   # 覆盖率报告 → app/build/reports/jacoco/
python scripts/check-coverage.py  # 覆盖率硬门禁（domain/prediction、data/security ≥85%）
python scripts/check-privacy-guards.py   # 隐私红线（无联网权限、无日志泄漏、allowBackup=false）
python scripts/check-docs.py             # 文档一致性（路由表目标存在、spec 状态合法）
```

**一次提交前的完整门禁**（与 pre-commit 等价）：

```bash
./gradlew detekt testDebugUnitTest && python scripts/check-privacy-guards.py && python scripts/check-docs.py
```

## 2. 本地门禁（L1）

- 唯一逻辑入口：`scripts/hooks/pre-commit`（仓库根只有这一份，`scripts/install-hooks.ps1` 负责复制到 `.git/hooks/`）。
- 绕过方式：`git commit --no-verify`。**绕过必须留痕** —— 在 commit message 里追加 `[skip-hook]` 与原因。
- 检查内容：提交信息格式、隐私红线脚本、文档一致性脚本、`detekt`、`testDebugUnitTest`。
- 性能策略：静态检查与测试由 Gradle 增量执行；Kotlin 文件未变更时跳过（`scripts/hooks/pre-commit` 内判断）。

## 3. dev → test → build → release

| 阶段 | 命令 | 产物 |
|---|---|---|
| dev | Android Studio 运行 `:app` | 调试安装包 |
| test | `./gradlew testDebugUnitTest` + `python scripts/check-coverage.py` | 测试报告 + 覆盖率 |
| build | `./gradlew :app:assembleRelease`（需 `keystore.properties`） | 已签名 APK/AAB |
| release | `./gradlew :app:bundleRelease` | `app/build/outputs/bundle/release/app-release.aab`，并在 `docs/changelog/CHANGELOG.md` 追加记录 |

**发布前 checklist**：

- [ ] 自评表 5 项全 1（见 `docs/reviews/`，模板在 `docs/reviews/SELF-REVIEW-TEMPLATE.md`）
- [ ] 版本号在 `app/build.gradle.kts` 递增，`CHANGELOG.md` 已更新
- [ ] 隐私守卫脚本通过，且人工确认清单里仍无 `INTERNET`
- [ ] 真机/模拟器手工过一遍：首次引导 → 标记经期 → 查看预测 → 开启应用锁 → 清除数据
- [ ] 无 `TODO`/`FIXME` 残留在 `app/src/main`（`scripts/check-docs.py` 会扫）

## 4. Runbook

| 症状 | 处置 |
|---|---|
| 构建报 `SDK location not found` | 检查 `local.properties` 的 `sdk.dir` 转义格式 |
| pre-commit 未触发 | 重新执行 `scripts/install-hooks.ps1`；确认 `.git/hooks/pre-commit` 存在且非空 |
| pre-commit 在 Windows 上报文件锁 | 钩子使用 `--hook-dir "$(git rev-parse --git-dir)/hooks"` 风格规避；若仍失败，检查是否有杀软锁定 `.git/hooks` |
| 覆盖率脚本报模块缺失 | 先跑 `./gradlew :app:jacocoTestReport` 生成报告 |
| Room schema 变更导致编译失败 | 在 `AppDatabase` 提升 `version` 并提供 `Migration`；`exportschema` 的 JSON 一并提交 |
| 用户忘记 PIN | 无后门。只能引导"清除数据"（产品层面已明示后果） |

## 5. 发布记录

唯一人工维护源：`docs/changelog/CHANGELOG.md`。格式 `## [版本] - 日期` + 变更条目。
