package com.example.richtexteditor.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.SpannedString
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.richtexteditor.databinding.ActivityMainBinding
import com.example.richtexteditor.utils.FileUtils
import com.example.richtexteditor.viewmodel.EditorViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // 默认替换关键字
        binding.etSearchKeyword.setText("【城市】")

        setupEditor()
        setupFormatToolbar()
        setupSpinners()
        setupButtons()
        observeViewModel()
    }

    // ===========================
    //  编辑器初始化
    // ===========================
    private fun setupEditor() {
        binding.richEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val spanned = binding.richEditor.getRichContent()
                viewModel.onEditorContentChanged(spanned)
            }
        })

        binding.richEditor.onRichTextChanged = { spanned ->
            viewModel.onEditorContentChanged(SpannedString(spanned))
        }
    }

    // ===========================
    //  格式工具栏
    // ===========================
    private fun setupFormatToolbar() {
        binding.btnBold.setOnClickListener { binding.richEditor.toggleBold() }
        binding.btnItalic.setOnClickListener { binding.richEditor.toggleItalic() }
        binding.btnUnderline.setOnClickListener { binding.richEditor.toggleUnderline() }
        binding.btnInsertImage.setOnClickListener {
            showToast("请从相册选择图片（功能扩展中）")
        }
        binding.btnPasteRich.setOnClickListener {
            binding.richEditor.pasteRichFromClipboard()
            showToast("已粘贴剪切板内容")
        }
    }

    // ===========================
    //  格式 Spinner 初始化
    // ===========================
    private fun setupSpinners() {
        // Spinner item 布局
        val itemLayout = android.R.layout.simple_spinner_item
        val dropdownLayout = android.R.layout.simple_spinner_dropdown_item

        // 标志位：跳过初始化时的自动触发（Android Spinner 绑定监听器时会立即回调一次）
        var skipNextSelection = true

        fun wrapListener(fn: (Int) -> Unit): android.widget.AdapterView.OnItemSelectedListener {
            return object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    if (skipNextSelection) return@onItemSelected
                    fn(position)
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        // 字体大小 Spinner
        val fontAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            com.example.richtexteditor.R.array.font_size_options,
            itemLayout
        ).apply {
            setDropDownViewResource(dropdownLayout)
        }
        binding.spinnerFontSize.adapter = fontAdapter
        binding.spinnerFontSize.onItemSelectedListener = wrapListener { pos ->
            binding.richEditor.applyFontSize(pos)
        }

        // 缩进 Spinner
        val indentAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            com.example.richtexteditor.R.array.indent_options,
            itemLayout
        ).apply {
            setDropDownViewResource(dropdownLayout)
        }
        binding.spinnerIndent.adapter = indentAdapter
        binding.spinnerIndent.onItemSelectedListener = wrapListener { pos ->
            binding.richEditor.applyIndent(pos)
        }

        // 对齐 Spinner
        val alignAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            com.example.richtexteditor.R.array.alignment_options,
            itemLayout
        ).apply {
            setDropDownViewResource(dropdownLayout)
        }
        binding.spinnerAlignment.adapter = alignAdapter
        binding.spinnerAlignment.onItemSelectedListener = wrapListener { pos ->
            binding.richEditor.applyAlignment(pos)
        }

        // 行距 Spinner
        val spacingAdapter = android.widget.ArrayAdapter.createFromResource(
            this,
            com.example.richtexteditor.R.array.line_spacing_options,
            itemLayout
        ).apply {
            setDropDownViewResource(dropdownLayout)
        }
        binding.spinnerLineSpacing.adapter = spacingAdapter
        binding.spinnerLineSpacing.onItemSelectedListener = wrapListener { pos ->
            binding.richEditor.applyLineSpacing(pos)
        }

        // 所有 Spinner 绑定完成后，解除跳过标志，用户首次选择时才生效
        skipNextSelection = false
    }

    // ===========================
    //  操作按钮
    // ===========================
    private fun setupButtons() {
        // 打开文件
        binding.btnOpen.setOnClickListener {
            showOpenFileDialog()
        }

        // 关键字替换
        binding.btnReplace.setOnClickListener {
            val keyword = binding.etSearchKeyword.text?.toString() ?: ""
            val replacement = binding.etReplaceContent.text?.toString() ?: ""
            val current = binding.richEditor.editableText
            val result: SpannableString? = viewModel.replaceKeyword(keyword, replacement, current)
            if (result != null) {
                binding.richEditor.editableText.replace(0, binding.richEditor.editableText.length, result)
            }
        }

        // 复制全部
        binding.btnCopyAll.setOnClickListener {
            viewModel.copyAllToClipboard(this, binding.richEditor.editableText)
        }

        // 复制并清空
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
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 清空
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
                    showStatus("已清空 ✓", EditorViewModel.StatusType.SUCCESS)
                }
                .setNegativeButton("取消", null)
                .show()
        }

        // 保存
        binding.btnSave.setOnClickListener {
            viewModel.saveToFile(this, binding.richEditor.editableText)
        }
    }

    // ===========================
    //  打开文件 Dialog
    // ===========================
    private fun showOpenFileDialog() {
        val files = FileUtils.listSavedFiles(this)
        if (files.isEmpty()) {
            showStatus("暂无已保存的文件", EditorViewModel.StatusType.WARNING)
            return
        }
        val names = files.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("选择要打开的文件")
            .setItems(names) { _, index ->
                viewModel.openFile(this, files[index].absolutePath)
            }
            .setNegativeButton("取消", null)
            .show()
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
            binding.tvStatus.postDelayed({ binding.tvStatus.visibility = View.GONE }, 3000)
        }

        // 打开文件内容 -> 加载到编辑器
        viewModel.openHtmlContent.observe(this) { html ->
            if (html != null) {
                binding.richEditor.loadHtml(html)
                viewModel.clearOpenHtmlContent()
            }
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

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
