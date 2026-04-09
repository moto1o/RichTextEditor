package com.example.richtexteditor.ui

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ImageSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * 富文本编辑器 — 支持粘贴带格式图文内容
 *
 * 支持的富文本格式：
 *  - 粗体 / 斜体 / 下划线 / 删除线
 *  - 前景色 / 背景色
 *  - 相对字号缩放
 *  - 图片（通过 ImageSpan）
 */
class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyle) {

    // 粘贴富文本时的回调，用于通知 Activity 更新预览
    var onRichTextChanged: ((Spanned) -> Unit)? = null

    /**
     * 拦截粘贴操作：优先粘贴带格式的富文本
     */
    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .primaryClip ?: return super.onTextContextMenuItem(id)

            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i)
                // 1. 优先处理 HTML 富文本 (API 33+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val htmlText = item.coerceToHtmlText(context)
                    if (!htmlText.isNullOrBlank()) {
                        pasteHtml(htmlText)
                        return true
                    }
                }
                // 2. 降级：处理带 Span 的 CharSequence (API 33+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val styledText = item.coerceToStyledText(context)
                    if (styledText is Spanned) {
                        insertSpanned(styledText)
                        return true
                    }
                }
            }
        }
        return super.onTextContextMenuItem(id)
    }

    /**
     * 将 HTML 字符串解析为 Spanned 并插入光标处
     */
    private fun pasteHtml(html: String) {
        val spanned: Spanned = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
        insertSpanned(spanned)
    }

    /**
     * 在当前光标位置插入 Spanned 内容
     */
    private fun insertSpanned(spanned: Spanned) {
        val editable = editableText ?: return
        val start = selectionStart.coerceAtLeast(0)
        val end = selectionEnd.coerceAtLeast(start)
        editable.replace(start, end, spanned)
        notifyChanged()
    }

    /**
     * 公开的粘贴富文本方法（供 Activity 手动调用）
     */
    fun pasteRichFromClipboard() {
        val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipManager.primaryClip ?: return

        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)
            // 1. 优先处理 HTML 富文本 (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val htmlText = item.coerceToHtmlText(context)
                if (!htmlText.isNullOrBlank()) {
                    pasteHtml(htmlText)
                    return
                }
            }
            // 2. 降级：处理带 Span 的 CharSequence (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val styledText = item.coerceToStyledText(context)
                if (styledText is Spanned) {
                    insertSpanned(styledText)
                    return
                }
            }
            // 纯文本降级
            val text = item.coerceToText(context)
            if (!text.isNullOrBlank()) {
                val editable = editableText ?: return
                val start = selectionStart.coerceAtLeast(0)
                val end = selectionEnd.coerceAtLeast(start)
                editable.replace(start, end, text)
                notifyChanged()
                return
            }
        }
    }

    // ===== 格式化方法 =====

    /** 切换粗体 */
    fun toggleBold() = toggleSpan(StyleSpan(Typeface.BOLD)) { span ->
        span is StyleSpan && span.style == Typeface.BOLD
    }

    /** 切换斜体 */
    fun toggleItalic() = toggleSpan(StyleSpan(Typeface.ITALIC)) { span ->
        span is StyleSpan && span.style == Typeface.ITALIC
    }

    /** 切换下划线 */
    fun toggleUnderline() = toggleSpan(UnderlineSpan()) { span ->
        span is UnderlineSpan
    }

    /** 切换删除线 */
    fun toggleStrikethrough() = toggleSpan(StrikethroughSpan()) { span ->
        span is StrikethroughSpan
    }

    private fun <T : CharacterStyle> toggleSpan(
        newSpan: T,
        matcher: (CharacterStyle) -> Boolean
    ) {
        val editable = editableText ?: return
        val start = selectionStart
        val end = selectionEnd
        if (start == end) return

        val existingSpans = editable.getSpans(start, end, CharacterStyle::class.java)
            .filter { matcher(it) }

        @Suppress("UNCHECKED_CAST")
        if (existingSpans.isEmpty()) {
            editable.setSpan(newSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            existingSpans.forEach { span ->
                @Suppress("UNCHECKED_CAST")
                editable.removeSpan(span as CharacterStyle)
            }
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        val spanned = editableText ?: return
        onRichTextChanged?.invoke(SpannedString(spanned))
    }

    /** 获取当前全部内容为 HTML */
    fun getContentAsHtml(): String {
        val spanned = editableText ?: return ""
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.toHtml(spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            Html.toHtml(spanned)
        }
    }

    /** 获取当前内容的 Spanned（用于复制到剪切板） */
    fun getRichContent(): SpannedString = SpannedString(editableText ?: "")

    /**
     * 对文本中的关键字进行高亮（用于替换前的预览）
     */
    fun highlightKeyword(keyword: String) {
        if (keyword.isBlank()) return
        val editable = editableText ?: return
        val text = editable.toString()
        var idx = text.indexOf(keyword)
        while (idx >= 0) {
            editable.setSpan(
                BackgroundColorSpan(0x33FF9800.toInt()),
                idx, idx + keyword.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            idx = text.indexOf(keyword, idx + keyword.length)
        }
    }
}
