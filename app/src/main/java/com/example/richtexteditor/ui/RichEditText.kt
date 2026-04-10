package com.example.richtexteditor.ui

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.style.BackgroundColorSpan
import android.text.style.AlignmentSpan
import android.text.style.BulletSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import android.graphics.Typeface
import androidx.appcompat.widget.AppCompatEditText

/**
 * 富文本编辑器
 * - 自动内嵌 ScrollMovementMethod，支持内部滚动
 * - 粘贴时保留全部 Span 格式
 * - 图片 Span 支持异步加载显示
 */
class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyle) {

    /** 内容变化回调（格式变化后通知外部） */
    var onRichTextChanged: ((Spanned) -> Unit)? = null

    // ============================================================
    //  粘贴拦截
    // ============================================================

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .primaryClip ?: return super.onTextContextMenuItem(id)

            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)

                // 1. 优先 HTML（API 33+）
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val html = item.coerceToHtmlText(context)
                    if (!html.isNullOrBlank()) {
                        insertSpanned(parseHtml(html))
                        return true
                    }
                }

                // 2. 带 Span 的 Spanned（API 33+）
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val styled = item.coerceToStyledText(context)
                    if (styled is Spanned) {
                        insertSpanned(styled)
                        return true
                    }
                }

                // 3. 降级：纯文本
                val plain = item.coerceToText(context)
                if (!plain.isNullOrBlank()) {
                    insertRaw(plain)
                    return true
                }
            }
        }
        return super.onTextContextMenuItem(id)
    }

    // ============================================================
    //  公开 API：粘贴富文本
    // ============================================================

    fun pasteRichFromClipboard() {
        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .primaryClip ?: return

        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val html = item.coerceToHtmlText(context)
                if (!html.isNullOrBlank()) {
                    insertSpanned(parseHtml(html))
                    return
                }
                val styled = item.coerceToStyledText(context)
                if (styled is Spanned) {
                    insertSpanned(styled)
                    return
                }
            }

            val plain = item.coerceToText(context)
            if (!plain.isNullOrBlank()) {
                insertRaw(plain)
                return
            }
        }
    }

    /**
     * 从外部加载 HTML 内容到编辑器（打开文件）
     */
    fun loadHtml(html: String) {
        val spanned = parseHtml(html)
        val editable = editableText ?: return
        editable.replace(0, editable.length, spanned)
        notifyChanged()
    }

    // ============================================================
    //  格式化操作
    // ============================================================

    fun toggleBold() = toggleSpan(StyleSpan(Typeface.BOLD)) { it is StyleSpan && it.style == Typeface.BOLD }
    fun toggleItalic() = toggleSpan(StyleSpan(Typeface.ITALIC)) { it is StyleSpan && it.style == Typeface.ITALIC }
    fun toggleUnderline() = toggleSpan(UnderlineSpan()) { it is UnderlineSpan }
    fun toggleStrikethrough() = toggleSpan(StrikethroughSpan()) { it is StrikethroughSpan }

    // ============================================================
    //  新增格式控制：字体大小
    // ============================================================

    /**
     * 应用字体大小（选中区域或全段）
     * @param size 字体大小选项索引：0=小(12sp), 1=中(14sp), 2=默认(16sp), 3=大(18sp), 4=超大(22sp)
     */
    fun applyFontSize(option: Int) {
        val size = when (option) {
            0 -> 12  // 小
            1 -> 14  // 中
            2 -> 16  // 默认
            3 -> 18  // 大
            4 -> 22  // 超大
            else -> 16
        }
        val editable = editableText ?: return
        val start = selectionStart.takeIf { it >= 0 } ?: return
        val end = selectionEnd.takeIf { it > start } ?: return

        // 移除同段落内所有 AbsoluteSizeSpan
        editable.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { editable.removeSpan(it) }
        editable.setSpan(AbsoluteSizeSpan(size, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        notifyChanged()
    }

    // ============================================================
    //  新增格式控制：缩进
    // ============================================================

    /**
     * 应用首行缩进（选中段落）
     * @param option 缩进选项：0=默认（无缩进）, 1=小缩进(16dp), 2=中缩进(32dp), 3=大缩进(48dp)
     */
    fun applyIndent(option: Int) {
        val indent = when (option) {
            0 -> 0         // 默认：无缩进
            1 -> dip(16)   // 小缩进
            2 -> dip(32)   // 中缩进
            3 -> dip(48)   // 大缩进
            else -> 0
        }
        val editable = editableText ?: return
        val text = editable.toString()
        val start = selectionStart.takeIf { it >= 0 } ?: return
        val end = (selectionEnd.takeIf { it > start } ?: return)

        // 扩展到段落边界
        val paraStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val paraEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }

        // 移除旧 LeadingMarginSpan
        editable.getSpans(paraStart, paraEnd, LeadingMarginSpan::class.java).forEach { editable.removeSpan(it) }
        // 首行缩进 = indent，普通行缩进 = 0
        editable.setSpan(LeadingMarginSpan.Standard(indent, 0), paraStart, paraEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        notifyChanged()
    }

    // ============================================================
    //  新增格式控制：对齐
    // ============================================================

    /**
     * 应用段落对齐（选中段落）
     * @param option 对齐选项：0=两端, 1=居左, 2=居中, 3=居右
     */
    fun applyAlignment(option: Int) {
        val alignment = when (option) {
            0 -> android.text.Layout.Alignment.ALIGN_NORMAL    // 两端
            1 -> android.text.Layout.Alignment.ALIGN_NORMAL    // 居左
            2 -> android.text.Layout.Alignment.ALIGN_CENTER   // 居中
            3 -> android.text.Layout.Alignment.ALIGN_OPPOSITE // 居右
            else -> android.text.Layout.Alignment.ALIGN_NORMAL
        }
        val editable = editableText ?: return
        val text = editable.toString()
        val start = selectionStart.takeIf { it >= 0 } ?: return
        val end = (selectionEnd.takeIf { it > start } ?: return)

        // 扩展到段落边界
        val paraStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
        val paraEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }

        // 移除旧 AlignmentSpan
        editable.getSpans(paraStart, paraEnd, AlignmentSpan::class.java).forEach { editable.removeSpan(it) }
        editable.setSpan(AlignmentSpan.Standard(alignment), paraStart, paraEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        notifyChanged()
    }

    // ============================================================
    //  新增格式控制：行距
    // ============================================================

    /**
     * 应用行距（整篇内容）
     * @param option 行距选项：0=小(1.0x), 1=默认(1.2x), 2=中(1.5x), 3=大(2.0x)
     */
    fun applyLineSpacing(option: Int) {
        val mult = when (option) {
            0 -> 1.0f   // 小
            1 -> 1.2f   // 默认
            2 -> 1.5f   // 中
            3 -> 2.0f   // 大
            else -> 1.2f
        }
        if (editableText == null) return
        // setLineSpacing 是 TextView 方法，直接在 EditText 本身调用
        setLineSpacing(0f, mult)
        notifyChanged()
    }

    // ============================================================
    //  辅助方法
    // ============================================================

    /** dp 转 px */
    private fun dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun <T : CharacterStyle> toggleSpan(
        newSpan: T,
        matcher: (CharacterStyle) -> Boolean
    ) {
        val editable = editableText ?: return
        val start = selectionStart.takeIf { it >= 0 } ?: return
        val end = selectionEnd.takeIf { it > start } ?: return

        val existing = editable.getSpans(start, end, CharacterStyle::class.java).filter { matcher(it) }
        if (existing.isEmpty()) {
            editable.setSpan(newSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            existing.forEach { editable.removeSpan(it) }
        }
        notifyChanged()
    }

    // ============================================================
    //  关键字高亮（供外部调用）
    // ============================================================

    fun highlightKeyword(keyword: String) {
        if (keyword.isBlank()) return
        val editable = editableText ?: return
        val text = editable.toString()
        var idx = text.indexOf(keyword)
        while (idx >= 0) {
            editable.setSpan(
                BackgroundColorSpan(0x44FF9800.toInt()),
                idx, idx + keyword.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            idx = text.indexOf(keyword, idx + keyword.length)
        }
    }

    // ============================================================
    //  公开 API：插入图片
    // ============================================================

    /**
     * 在光标位置插入图片
     * @param bitmap 要插入的图片
     */
    fun insertImage(bitmap: android.graphics.Bitmap) {
        val editable = editableText ?: return
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(start)

        // 限制最大宽度为屏幕宽度
        val maxWidth = resources.displayMetrics.widthPixels - (paddingLeft + paddingRight)
        val w = bitmap.width
        val h = bitmap.height
        val scale = if (w > maxWidth) maxWidth.toFloat() / w else 1f
        val scaledWidth = (w * scale).toInt()
        val scaledHeight = (h * scale).toInt()

        // 缩放图片
        val scaledBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap
        }

        val drawable = BitmapDrawable(resources, scaledBitmap)
        drawable.setBounds(0, 0, scaledWidth, scaledHeight)

        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
        editable.setSpan(imageSpan, start, end + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        notifyChanged()
    }

    // ============================================================
    //  内容获取
    // ============================================================

    fun getRichContent(): SpannedString = SpannedString(editableText ?: "")

    fun getContentAsHtml(): String {
        val spanned = editableText ?: return ""
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.toHtml(spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            Html.toHtml(spanned)
        }
    }

    // ============================================================
    //  内部工具
    // ============================================================

    /**
     * 解析 HTML 字符串，并通过自定义 ImageGetter 加载图片
     */
    private fun parseHtml(html: String): Spanned {
        val imageGetter = Html.ImageGetter { src ->
            loadImageFromSrc(src)
        }
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, null)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html, imageGetter, null)
        }
    }

    /**
     * 加载图片资源：支持 content://、file://、data: URI
     * 返回一个已设置 bounds 的 Drawable；加载失败返回透明占位符
     */
    private fun loadImageFromSrc(src: String?): Drawable {
        if (src.isNullOrBlank()) return createPlaceholder()
        return try {
            val uri = Uri.parse(src)
            val drawable = when {
                src.startsWith("data:") -> {
                    // Base64 Data URI
                    val base64 = src.substringAfter(",")
                    val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    BitmapDrawable(resources, bmp)
                }
                uri.scheme == "content" || uri.scheme == "file" -> {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                        BitmapDrawable(resources, bmp)
                    }
                }
                else -> null
            } ?: return createPlaceholder()

            // 限制最大宽度为屏幕宽度
            val maxWidth = resources.displayMetrics.widthPixels - (paddingLeft + paddingRight)
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val scale = if (w > maxWidth) maxWidth.toFloat() / w else 1f
            drawable.setBounds(0, 0, (w * scale).toInt(), (h * scale).toInt())
            drawable
        } catch (e: Exception) {
            createPlaceholder()
        }
    }

    private fun createPlaceholder(): Drawable {
        // 返回一个 16x16 的透明占位符，防止 ImageSpan 崩溃
        val bmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
        return BitmapDrawable(resources, bmp).also { it.setBounds(0, 0, 16, 16) }
    }

    private fun insertSpanned(spanned: Spanned) {
        val editable = editableText ?: return
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(start)
        editable.replace(start, end, spanned)
        notifyChanged()
    }

    private fun insertRaw(text: CharSequence) {
        val editable = editableText ?: return
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(start)
        editable.replace(start, end, text)
        notifyChanged()
    }

    private fun notifyChanged() {
        val spanned = editableText ?: return
        onRichTextChanged?.invoke(SpannedString(spanned))
    }
}
