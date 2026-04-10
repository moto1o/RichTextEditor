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
import kotlinx.coroutines.withContext

/**
 * 富文本编辑器 ViewModel
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _richContent = MutableLiveData<SpannedString?>()
    val richContent: LiveData<SpannedString?> = _richContent

    private val _statusMessage = MutableLiveData<StatusMessage?>()
    val statusMessage: LiveData<StatusMessage?> = _statusMessage

    private val _charCount = MutableLiveData(0)
    val charCount: LiveData<Int> = _charCount

    private val _hasContent = MutableLiveData(false)
    val hasContent: LiveData<Boolean> = _hasContent

    /** 打开文件时，向 View 传递 HTML 内容 */
    private val _openHtmlContent = MutableLiveData<String?>()
    val openHtmlContent: LiveData<String?> = _openHtmlContent

    fun onEditorContentChanged(spanned: SpannedString) {
        _richContent.value = spanned
        val len = spanned.toString().trim().length
        _charCount.value = len
        _hasContent.value = len > 0
    }

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
        postStatus("已替换 $count 处", StatusType.SUCCESS)
        return android.text.SpannableString.valueOf(builder)
    }

    fun copyAllToClipboard(context: Context, spanned: android.text.Spanned?) {
        if (spanned == null || spanned.toString().isBlank()) {
            postStatus("内容为空，无法复制", StatusType.WARNING)
            return
        }
        val html = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.toHtml(spanned, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.toHtml(spanned)
        }
        val clip = ClipData.newHtmlText("rich_text", spanned.toString(), html)
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        postStatus("已复制到剪切板 ✓", StatusType.SUCCESS)
    }

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

    /**
     * 读取指定路径的 HTML 文件，通过 LiveData 传递给 View 加载
     */
    fun openFile(context: Context, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val html = FileUtils.readHtmlFile(filePath)
            withContext(Dispatchers.Main) {
                if (html != null) {
                    _openHtmlContent.value = html
                    postStatus("已打开：${filePath.substringAfterLast('/')}", StatusType.SUCCESS)
                } else {
                    postStatus("文件读取失败", StatusType.ERROR)
                }
            }
        }
    }

    /** 消费一次 openHtmlContent 后重置 */
    fun clearOpenHtmlContent() {
        _openHtmlContent.value = null
    }

    fun clearStatus() { _statusMessage.postValue(null) }

    private fun postStatus(msg: String, type: StatusType) {
        _statusMessage.postValue(StatusMessage(msg, type))
    }

    data class StatusMessage(val message: String, val type: StatusType)
    enum class StatusType { SUCCESS, WARNING, ERROR }
}

