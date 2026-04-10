package com.example.richtexteditor.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {

    fun saveHtmlFile(context: Context, html: String): String? {
        return try {
            val dir = getStorageDir(context) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "rich_text_$timestamp.html")
            FileWriter(file).use { it.write(buildFullHtml(html)) }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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

    /** 读取已保存的 HTML 文件内容，提取 body 内的 HTML 片段 */
    fun readHtmlFile(filePath: String): String? {
        return try {
            val content = FileReader(filePath).use { it.readText() }
            // 提取 <body>...</body> 内容
            val bodyStart = content.indexOf("<body>")
            val bodyEnd = content.lastIndexOf("</body>")
            if (bodyStart >= 0 && bodyEnd > bodyStart) {
                content.substring(bodyStart + 6, bodyEnd).trim()
            } else {
                content
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 列出保存目录下的所有 HTML 文件，按修改时间倒序 */
    fun listSavedFiles(context: Context): List<File> {
        val dir = getStorageDir(context) ?: return emptyList()
        return dir.listFiles { f -> f.extension == "html" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun getStorageDir(context: Context): File? {
        val dir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "RichTextEditor"
            )
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

