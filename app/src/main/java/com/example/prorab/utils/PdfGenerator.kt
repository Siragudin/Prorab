package com.example.prorab.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.prorab.data.ProfileData
import com.example.prorab.data.Project
import com.example.prorab.data.Record
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generateReport(
        context: Context,
        project: Project,
        records: List<Record>,
        profileData: ProfileData, // Получаем настройки (лого, печать)
        startDate: Long,
        endDate: Long
    ): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()

        // --- РАЗМЕРЫ (A4) ---
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = margin

        // --- ЦВЕТА ---
        val darkColor = Color.rgb(38, 50, 56) // Темный
        val blueColor = Color.rgb(21, 101, 192) // Синий

        // Утилита для рисования текста
        fun drawText(text: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) {
            paint.color = color
            paint.textSize = size
            paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textAlign = align
            canvas.drawText(text, x, y, paint)
        }

        fun drawLine(y: Float) {
            paint.color = Color.LTGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(margin, y, pageWidth - margin, y, paint)
        }

        // ================= ШАПКА =================

        // ЛОГОТИП (Слева)
        if (profileData.logoUri != null) {
            try {
                val bitmap = getBitmapFromUri(context, Uri.parse(profileData.logoUri))
                if (bitmap != null) {
                    val logoRect = RectF(margin, yPosition, margin + 80, yPosition + 80)
                    canvas.drawBitmap(bitmap, null, logoRect, null)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val textX = if (profileData.logoUri != null) margin + 100f else margin

        // НАЗВАНИЕ ФИРМЫ (Справа от лого)
        val companyName = if (profileData.companyName.isNotBlank()) profileData.companyName else "СМЕТА ПРОЕКТА"
        drawText(companyName, textX, yPosition + 25, 20f, blueColor, true)

        // КОНТАКТЫ
        if (profileData.phone.isNotBlank()) {
            drawText("Тел.: ${profileData.phone}", textX, yPosition + 50, 12f, Color.GRAY)
        }

        // ДАТА ОТЧЕТА (Справа в углу)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
        val dateRange = "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
        drawText(dateRange, pageWidth - margin, yPosition + 25, 12f, Color.GRAY, align = Paint.Align.RIGHT)

        yPosition += 100
        drawLine(yPosition)
        yPosition += 30

        // ОБЪЕКТ
        drawText("Объект: ${project.name}", margin, yPosition, 16f, darkColor, true)
        yPosition += 40

        // ================= ТАБЛИЦА =================

        // Проверяем, есть ли детализация
        val showDetails = records.any { it.quantity > 0 && it.unitPrice > 0 }

        val colNameWidth = if (showDetails) 240f else 380f
        val colQtyWidth = if (showDetails) 60f else 0f
        val colPriceWidth = if (showDetails) 80f else 0f

        val xName = margin
        val xQty = xName + colNameWidth + 10
        val xPrice = xQty + colQtyWidth + 10
        val xSum = pageWidth - margin // Правый край

        // Заголовки
        fun drawTableHeader(y: Float) {
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawRect(margin, y - 15, pageWidth - margin, y + 5, paint)

            drawText("НАИМЕНОВАНИЕ", xName, y, 10f, darkColor, true)
            if (showDetails) {
                drawText("КОЛ-ВО", xQty, y, 10f, darkColor, true)
                drawText("ЦЕНА", xPrice, y, 10f, darkColor, true)
            }
            drawText("СУММА", xSum, y, 10f, darkColor, true, Paint.Align.RIGHT)
        }

        drawTableHeader(yPosition)
        yPosition += 25

        // Рисуем секции
        val works = records.filter { it.type == 0 }
        val expenses = records.filter { it.type == 1 }

        fun drawSection(title: String, list: List<Record>) {
            if (list.isEmpty()) return

            yPosition += 15
            drawText(title, margin, yPosition, 12f, blueColor, true)
            canvas.drawLine(margin, yPosition + 5, margin + 150, yPosition + 5, paint)
            yPosition += 25

            list.forEach { record ->
                if (yPosition > pageHeight - 100) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = margin + 20
                    drawTableHeader(yPosition)
                    yPosition += 25
                }

                drawText(record.title, xName, yPosition, 10f, Color.BLACK)

                if (showDetails && record.quantity > 0) {
                    val q = if (record.quantity % 1.0 == 0.0) record.quantity.toInt().toString() else record.quantity.toString()
                    val p = if (record.unitPrice % 1.0 == 0.0) record.unitPrice.toInt().toString() else record.unitPrice.toString()
                    drawText("$q ${record.unit}", xQty, yPosition, 10f, Color.BLACK)
                    drawText(p, xPrice, yPosition, 10f, Color.BLACK)
                }

                drawText("${record.amount.toInt()} ₽", xSum, yPosition, 10f, Color.BLACK, false, Paint.Align.RIGHT)
                yPosition += 15
            }

            val sectionSum = list.sumOf { it.amount }
            yPosition += 5
            drawLine(yPosition)
            yPosition += 15
            drawText("Итого $title:  ${sectionSum.toInt()} ₽", xSum, yPosition, 10f, darkColor, true, Paint.Align.RIGHT)
            yPosition += 20
        }

        drawSection("РАБОТЫ", works)
        drawSection("МАТЕРИАЛЫ", expenses)

        // ================= ПОДВАЛ =================

        if (yPosition > pageHeight - 150) {
            pdfDocument.finishPage(page)
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = margin
        } else {
            yPosition += 30
        }

        val totalSum = records.sumOf { it.amount }
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(margin, yPosition - 15, pageWidth - margin, yPosition + 15, paint)

        drawText("ИТОГО К ОПЛАТЕ:", margin + 10, yPosition + 5, 14f, darkColor, true)
        drawText("${totalSum.toInt()} ₽", xSum - 10, yPosition + 5, 14f, darkColor, true, Paint.Align.RIGHT)

        yPosition += 80

        val signatureY = yPosition
        canvas.drawLine(margin, signatureY, margin + 150, signatureY, paint)
        drawText("Заказчик", margin, signatureY + 15, 8f, Color.GRAY)

        val contractorX = pageWidth - margin - 150
        canvas.drawLine(contractorX, signatureY, pageWidth - margin, signatureY, paint)

        val contractorName = if (profileData.companyName.isNotBlank()) profileData.companyName else "Подрядчик"
        drawText(contractorName, contractorX, signatureY + 15, 8f, Color.GRAY)

        // ПЕЧАТЬ
        if (profileData.stampUri != null) {
            try {
                val stampBitmap = getBitmapFromUri(context, Uri.parse(profileData.stampUri))
                if (stampBitmap != null) {
                    val stampRect = RectF(contractorX - 90, signatureY - 50, contractorX - 10, signatureY + 30)
                    val stampPaint = Paint()
                    stampPaint.alpha = 200
                    canvas.drawBitmap(stampBitmap, null, stampRect, stampPaint)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        pdfDocument.finishPage(page)

        val fileName = "Smeta_${project.name}_${System.currentTimeMillis()}.pdf"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Сохранено: $fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            return null
        } finally {
            pdfDocument.close()
        }
        return file
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { null }
    }
}