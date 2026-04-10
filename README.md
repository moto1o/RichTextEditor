# 📝 富文本编辑器 - Android App

支持图文带格式粘贴、关键字替换、文件读写的 Android 富文本编辑器。

## 功能特性

- ✅ **富文本粘贴**：从微信/浏览器/Office 等 App 复制图文内容，直接粘贴保留格式（含图片）
- ✅ **编辑即预览**：编辑区直接渲染富文本效果，所见即所得，无需独立预览区
- ✅ **格式工具栏**：粗体 / 斜体 / 下划线 / 插入图片 / 粘贴富文本
- ✅ **字体调整**：小 / 中 / 默认 / 大 / 超大 五档字体大小（AbsoluteSizeSpan）
- ✅ **缩进调整**：默认 / 小 / 中 / 大 四档首行缩进（LeadingMarginSpan.Standard）
- ✅ **对齐调整**：两端 / 居左 / 居中 / 居右 四种段落对齐（AlignmentSpan.Standard）
- ✅ **行距调整**：小 / 默认 / 中 / 大 四档行距（setLineSpacing）
- ✅ **关键字替换**：输入原文字 → 替换为新内容，完整保留原有 Span 格式，默认值「【城市】」
- ✅ **六大操作按钮**：打开 / 复制 / 复制并清空 / 清空 / 保存为 HTML
- ✅ **打开文件**：列出历史保存记录，一键加载并恢复富文本内容
- ✅ **固定工具栏**：操作栏和格式栏固定在顶部，不随内容滚动（两行工具栏）

## 界面层次

```
┌──────────────────────────┐
│  Toolbar（标题栏）         │  ← 固定
├──────────────────────────┤
│  关键字替换输入 + [替换]   │  ← 固定
│  [打开][复制][复制清空]    │
│  [清空][保存]             │
├──────────────────────────┤
│  [B][I][U] | [🖼][📋]  字数│  ← 固定格式工具栏（第一行）
│  [字体][缩进][对齐][行距]  │  ← 固定格式工具栏（第二行，可横滚）
├──────────────────────────┤
│                          │
│  富文本编辑区             │  ← 内部可滚动，编辑即预览
│  （图文混排，所见即所得）  │
│                          │
├──────────────────────────┤
│  状态提示栏               │  ← 固定，操作反馈
└──────────────────────────┘
```

## 技术栈

| 维度 | 技术 |
|------|------|
| 语言 | Kotlin |
| 架构 | MVVM + ViewBinding |
| UI | Material Design 3 |
| 富文本 | SpannableString + Html.fromHtml + ImageGetter + AbsoluteSizeSpan / LeadingMarginSpan / AlignmentSpan / setLineSpacing |
| 异步 | Coroutines + LiveData |
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

> 打开 [`preview.html`](preview.html) 查看交互式 UI 预览（含架构说明）

![UI Preview](preview.png)

## License

MIT
