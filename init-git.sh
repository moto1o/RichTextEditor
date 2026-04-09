#!/bin/bash
#
# RichTextEditor — GitHub 推送初始化脚本
# 用法: bash init-git.sh
#

set -e

echo "📦 正在初始化 Git 仓库..."

# 初始化 Git（如果尚未初始化）
if [ ! -d .git ]; then
  git init
  echo "✅ Git 仓库初始化完成"
fi

# 添加所有文件
git add .

# 提交
git commit -m "feat: 初始版本 - Android 富文本编辑器

- 支持图文带格式粘贴（HTML Span 保留）
- 实时预览区
- 关键字替换功能（保留格式）
- 五大操作按钮：替换/复制/复制清空/清空/保存
- MVVM + Material 3 架构
- GitHub Actions 自动构建 APK"

# 检查是否已设置 remote
if ! git remote -v | grep -q origin; then
  echo ""
  echo "⚠️  未检测到 GitHub remote 地址"
  echo ""
  echo "请先在 GitHub 上创建仓库，然后运行："
  echo ""
  echo "  git remote add origin https://github.com/YOUR_USERNAME/RichTextEditor.git"
  echo "  git branch -M main"
  echo "  git push -u origin main"
  echo ""
  echo "推送成功后，打开 GitHub 仓库 → Actions 标签页"
  echo "等待构建完成，点击 Artifacts 下载 APK"
  echo ""
else
  echo "🚀 正在推送到 GitHub..."
  git branch -M main
  git push -u origin main
  echo ""
  echo "✅ 推送成功！"
  echo ""
  echo "接下来："
  echo "1. 打开 GitHub 仓库 → 点击 Actions 标签"
  echo "2. 等待 'Build Android APK' 工作流运行完成（约 3-5 分钟）"
  echo "3. 点击运行 → Artifacts → 下载 rich-text-editor-debug"
fi
