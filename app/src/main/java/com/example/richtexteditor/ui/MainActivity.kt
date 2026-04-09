package com.example.richtexteditor.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.richtexteditor.R
import com.example.richtexteditor.databinding.ActivityMainBinding
import com.example.richtexteditor.viewmodel.EditorViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupEditor()
        setupFormatToolbar()
        setupButtons()
        observeViewModel()
    }

    // ===========================
    //  编辑器初始化
    // ===========================
    private fun setupEditor() {
        // 监听文本变化 -> 更新预览区 + 字数统计
        binding.richEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val spanned = binding.richEditor.getRichContent()
                viewModel.onEditorContentChanged(spanned)
                updatePreview(spanned)
            }
        })

        // 富文本格式变化回调（粘贴/格式切换后手动刷新预览）
        binding.richEditor.onRichTextChanged = { spanned ->
            viewModel.onEditorContentChanged(spanned)
            updatePreview(spanned)
        }
    }

    /**
     * 统一更新预览区（复用逻辑，避免重复）
     */
    private fun updatePreview(spanned: CharSequence) {
        binding.tvPreview.text = spanned
    }

    // ===========================
    //  格式工具栏
    // ===========================
    private fun setupFormatToolbar() {
        binding.btnBold.setOnClickListener {
            binding.richEditor.toggleBold()
            showToast("粗体")
        }
        binding.btnItalic.setOnClickListener {
            binding.richEditor.toggleItalic()
            showToast("斜体")
        }
        binding.btnUnderline.setOnClickListener {
            binding.richEditor.toggleUnderline()
            showToast("下划线")
        }
        binding.btnInsertImage.setOnClickListener {
            showToast("请从相册选择图片（功能扩展中）")
        }
        // 粘贴富文本按钮
        binding.btnPasteRich.setOnClickListener {
            binding.richEditor.pasteRichFromClipboard()
            showToast("已粘贴剪切板内容")
        }
    }

    // ===========================
    //  五大功能按钮
    // ===========================
    private fun setupButtons() {
        // 按钮1: 替换关键字
        binding.btnReplace.setOnClickListener {
            val keyword = binding.etSearchKeyword.text?.toString() ?: ""
            val replacement = binding.etReplaceContent.text?.toString() ?: ""
            val current = binding.richEditor.editableText
            val result: SpannableString? = viewModel.replaceKeyword(keyword, replacement, current)
            if (result != null) {
                // 回写到编辑器，保留格式
                binding.richEditor.editableText.replace(
                    0, binding.richEditor.editableText.length, result
                )
            }
        }

        // 按钮2: 复制全部到剪切板
        binding.btnCopyAll.setOnClickListener {
            viewModel.copyAllToClipboard(this, binding.richEditor.editableText)
        }

        // 按钮3: 复制全部并清空编辑内容
        binding.btnCopyAndClear.setOnClickListener {
            val content = binding.richEditor.editableText
            if (content.isNullOrBlank()) {
                showStatus("内容为空", EditorViewModel.StatusType.WARNING)
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("复制并清空")
                .setMessage("将复制全部内容到剪切板，并清空编辑区。确认吗？")
                .setPositiveButton("确认") { _, _ ->
                    viewModel.copyAllToClipboard(this, content)
                    binding.richEditor.text?.clear()
                    binding.tvPreview.text = ""
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 按钮4: 清空编辑内容
        binding.btnClear.setOnClickListener {
            if (binding.richEditor.editableText.isNullOrBlank()) {
                showStatus("编辑内容已经为空", EditorViewModel.StatusType.WARNING)
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("清空内容")
                .setMessage("确认清空所有编辑内容？此操作不可撤销。")
                .setPositiveButton("清空") { _, _ ->
                    binding.richEditor.text?.clear()
                    binding.tvPreview.text = ""
                    showStatus("已清空 ✓", EditorViewModel.StatusType.SUCCESS)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 按钮5: 保存
        binding.btnSave.setOnClickListener {
            viewModel.saveToFile(this, binding.richEditor.editableText)
        }
    }

    // ===========================
    //  观察 ViewModel
    // ===========================
    private fun observeViewModel() {
        viewModel.charCount.observe(this) { count ->
            binding.tvCharCount.text = "$count 字"
        }

        viewModel.statusMessage.observe(this) { msg ->
            if (msg == null) {
                binding.tvStatus.visibility = View.GONE
                return@observe
            }
            showStatus(msg.message, msg.type)
            // 3秒后自动隐藏
            binding.tvStatus.postDelayed({ binding.tvStatus.visibility = View.GONE }, 3000)
        }
    }

    private fun showStatus(message: String, type: EditorViewModel.StatusType) {
        val color = when (type) {
            EditorViewModel.StatusType.SUCCESS -> Color.parseColor("#4CAF50")
            EditorViewModel.StatusType.WARNING -> Color.parseColor("#FF9800")
            EditorViewModel.StatusType.ERROR   -> Color.parseColor("#F44336")
        }
        binding.tvStatus.apply {
            text = message
            setTextColor(color)
            visibility = View.VISIBLE
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
