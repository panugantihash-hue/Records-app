package com.icadd.records.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds real, valid .xlsx and .pdf files without pulling in any extra library
 * (xlsx is hand-written as a minimal Office Open XML zip; PDF uses Android's
 * built-in android.graphics.pdf.PdfDocument).
 */
object ExportUtils {

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    fun buildXlsx(headers: List<String>, rows: List<List<String>>): ByteArray {
        val out = ByteArrayOutputStream()
        val zip = ZipOutputStream(out)

        fun entry(name: String, content: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        entry(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
        )
        entry(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        )
        entry(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""
        )
        entry(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""
        )

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            val allRows = listOf(headers) + rows
            allRows.forEachIndexed { rIdx, row ->
                append("<row r=\"${rIdx + 1}\">")
                row.forEachIndexed { cIdx, cell ->
                    val ref = "${colLetter(cIdx)}${rIdx + 1}"
                    append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXml(cell)}</t></is></c>")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        entry("xl/worksheets/sheet1.xml", sheetXml)

        zip.close()
        return out.toByteArray()
    }

    fun buildPdf(title: String, headers: List<String>, rows: List<List<String>>): ByteArray {
        val pageWidth = 842 // A4 landscape points
        val pageHeight = 595
        val margin = 24
        val doc = PdfDocument()
        val titlePaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
        val cellPaint = Paint().apply { textSize = 9f }
        val colWidth = (pageWidth - margin * 2) / headers.size.coerceAtLeast(1)

        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0
        var pageNum = 0

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page!!.canvas
            y = margin
            if (pageNum == 1) {
                canvas!!.drawText(title, margin.toFloat(), y.toFloat() + 16, titlePaint)
                y += 30
            }
            headers.forEachIndexed { i, h ->
                canvas!!.drawText(h.take(30), (margin + i * colWidth).toFloat(), y.toFloat(), headerPaint)
            }
            y += 16
            canvas!!.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), cellPaint)
            y += 10
        }

        newPage()
        rows.forEach { row ->
            if (y > pageHeight - margin - 20) newPage()
            row.forEachIndexed { i, cellText ->
                val text = if (cellText.length > 40) cellText.take(37) + "..." else cellText
                canvas!!.drawText(text, (margin + i * colWidth).toFloat(), y.toFloat(), cellPaint)
            }
            y += 14
        }
        page?.let { doc.finishPage(it) }

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        true
    } catch (_: Exception) { false }
}
