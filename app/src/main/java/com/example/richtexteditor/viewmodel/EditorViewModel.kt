package com.example.richtexteditor.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.SpannedString
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.richtexteditor.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 富文本编辑器 ViewModel
 * 负责管理编辑器状态、替换逻辑、剪切板操作、文件保存
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    // 当前富文本内容（Spanned）
    private val _richContent = MutableLiveData<SpannedString?>()
    val richContent: LiveData<SpannedString?> = _richContent

    // 操作状态提示
    private val _statusMessage = MutableLiveData<StatusMessage?>()
    val statusMessage: LiveData<StatusMessage?> = _statusMessage

    // 字符统计
    private val _charCount = MutableLiveData(0)
    val charCount: LiveData<Int> = _charCount

    // 是否有内容（用于控制按钮可用性）
    private val _hasContent = MutableLiveData(false)
    val hasContent: LiveData<Boolean> = _hasContent

    /**
     * 通知内容变更（来自编辑器）
     */
    fun onEditorContentChanged(spanned: SpannedString) {
        _richContent.value = spanned
        val len = spanned.toString().trim().length
        _charCount.value = len
        _hasContent.value = len > 0
    }

    /**
     * 关键字替换
     * @param searchKeyword 待替换的关键字
     * @param replacement   替换后的内容
     * @param currentSpanned 当前编辑器内容
     * @return 替换结果（替换后的 SpannableString）或 null（无内容/无关键字）
     */
    fun replaceKeyword(
        searchKeyword: String,
        replacement: String,
        currentSpanned: android.text.Spanned?
    ): android.text.SpannableString? {
        if (searchKeyword.isBlank()) {
            postStatus("请输入要替换的关键字", StatusType.WARNING)
            return null
        }
        if (currentSpanned == null || currentSpanned.toString().isBlank()) {
            postStatus("编辑内容为空", StatusType.WARNING)
            return null
        }

        val text = currentSpanned.toString()
        if (!text.contains(searchKeyword)) {
            postStatus("未找到关键字：\"$searchKeyword\"", StatusType.WARNING)
            return null
        }

        // 使用 SpannableStringBuilder 保留原始 Span，仅替换纯文字部分
        val builder = android.text.SpannableStringBuilder(currentSpanned)
        var searchStart = 0
        var count = 0
        while (searchStart < builder.length) {
            val found = builder.toString().indexOf(searchKeyword, searchStart)
            if (found < 0) break
            builder.replace(found, found + searchKeyword.length, replacement)
            searchStart = found + replacement.length
            count++
        }

        val result = android.text.SpannableString.valueOf(builder)
        postStatus("已替换 $count 处", StatusType.SUCCESS)
        return result
    }

    /**
     * 复制富文本到剪切板（保留格式）
     */
    fun copyAllToClipboard(context: Context, spanned: android.text.Spanned?) {
        if (spanned == null || spanned.toString().isBlank()) {
            postStatus("内容为空，无法复制", StatusType.WARNING)
            return
        }
        val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val html = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.toHtml(spanned, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.toHtml(spanned)
        }
        val clip = ClipData.newHtmlText("rich_text", spanned.toString(), html)
        clipManager.setPrimaryClip(clip)
        postStatus("已复制到剪切板 ✓", StatusType.SUCCESS)
    }

    /**
     * 保存内容到文件
     */
    fun saveToFile(context: Context, spanned: android.text.Spanned?) {
        if (spanned == null || spanned.toString().isBlank()) {
            postStatus("内容为空，无法保存", StatusType.WARNING)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val html = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.toHtml(spanned, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.toHtml(spanned)
            }
            val filePath = FileUtils.saveHtmlFile(context, html)
            if (filePath != null) {
                postStatus("已保存至：$filePath", StatusType.SUCCESS)
            } else {
                postStatus("保存失败，请检查存储权限", StatusType.ERROR)
            }
        }
    }

    fun clearStatus() {
        _statusMessage.postValue(null)
    }

    private fun postStatus(msg: String, type: StatusType) {
        _statusMessage.postValue(StatusMessage(msg, type))
    }

    data class StatusMessage(val message: String, val type: StatusType)

    enum class StatusType { SUCCESS, WARNING, ERROR }
}
