#!/bin/sh
# 安装本地门禁（L1）：把 scripts/hooks/* 复制为 .git/hooks/* 并赋予可执行权限。
set -e

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPTS_DIR")"
HOOKS_DIR="$REPO_ROOT/.git/hooks"

if [ ! -d "$REPO_ROOT/.git" ]; then
    echo "找不到 .git 目录：请先在本目录执行 git init（$REPO_ROOT）" >&2
    exit 1
fi

mkdir -p "$HOOKS_DIR"

for name in pre-commit commit-msg; do
    cp "$SCRIPTS_DIR/hooks/$name" "$HOOKS_DIR/$name"
    chmod +x "$HOOKS_DIR/$name"
    echo "已安装 .git/hooks/$name"
done

echo "完成。绕过门禁需在提交信息中写 [skip-hook] 并说明原因。"
