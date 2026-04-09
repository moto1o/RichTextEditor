package com.example.richtexteditor.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    /**
     * 保存 HTML 内容到 Documents 目录下的文件
     * @return 保存成功返回文件路径，失败返回 null
     */
    fun saveHtmlFile(context: Context, html: String): String? {
        return try {
            val dir = getStorageDir(context) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "rich_text_$timestamp.html")
            FileWriter(file).use { writer ->
                writer.write(buildFullHtml(html))
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存纯文本内容
     */
    fun savePlainText(context: Context, text: String): String? {
        return try {
            val dir = getStorageDir(context) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "rich_text_$timestamp.txt")
            FileWriter(file).use { it.write(text) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getStorageDir(context: Context): File? {
        // Android 10+ 优先使用应用私有目录
        val dir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        } else {
            File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "RichTextEditor")
        }
        return dir?.also { if (!it.exists()) it.mkdirs() }
    }

    private fun buildFullHtml(bodyHtml: String): String = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>富文本内容</title>
            <style>
                body { font-family: -apple-system, sans-serif; line-height: 1.6;
                       padding: 16px; color: #333; }
                img { max-width: 100%; height: auto; }
            </style>
        </head>
        <body>
        $bodyHtml
        </body>
        </html>
    """.trimIndent()
}
