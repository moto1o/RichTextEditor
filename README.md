# 📝 富文本编辑器 - Android App

支持图文带格式粘贴、实时预览、关键字替换的 Android 富文本编辑器。

## 功能特性

- ✅ **富文本粘贴**：从微信/浏览器/Office 等 App 复制图文内容，直接粘贴保留格式
- ✅ **实时预览**：编辑内容实时同步显示预览效果
- ✅ **格式工具栏**：粗体 / 斜体 / 下划线 / 插入图片
- ✅ **关键字替换**：输入原文字 → 替换为新内容，保留原有格式
- ✅ **五大操作按钮**：替换 / 复制 / 复制并清空 / 清空 / 保存为 HTML

## 技术栈

| 维度 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM + ViewBinding |
| UI | Material Design 3 |
| 富文本 | SpannableString + Html |
| 目标平台 | Android API 24+ (Android 7.0+) |

## CI 自动构建

本项目使用 **GitHub Actions** 自动编译 APK：

1. 每次 `push` 或 `pull_request` 到 `main` 分支自动触发构建
2. 编译完成后在 **Actions** 页面下载 APK
3. APK 下载路径：`项目页面 → Actions → 最新运行 → Artifacts`

## 本地构建（需要 Android Studio）

```bash
# 1. 用 Android Studio 打开本目录
# 2. 等待 Gradle 同步完成
# 3. 点击 Run ▶ 或执行
./gradlew assembleDebug

# APK 输出位置
app/build/outputs/apk/debug/app-debug.apk
```

## 界面预览

![UI Preview](preview.png)

## License

MIT
