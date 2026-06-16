package com.sgaf.universidadedoservidor.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Exporta uma ferramenta prática preenchida (SWOT, 5W2H) em PDF A4 retrato (v4 Item 2).
 */
object FerramentaPdfGenerator {

    private const val LARGURA = 595 // A4 retrato em pontos
    private const val ALTURA = 842
    private const val MARGEM = 48f
    private const val AZUL = 0xFF003882.toInt()
    private const val CINZA = 0xFF333333.toInt()

    fun gerar(
        context: Context,
        titulo: String,
        subtitulo: String,
        campos: List<Pair<String, String>>
    ): File {
        val documento = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create()
        val pagina = documento.startPage(pageInfo)
        val canvas = pagina.canvas
        canvas.drawColor(Color.WHITE)

        val tituloPaint = Paint().apply {
            color = AZUL; textSize = 22f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint().apply {
            color = CINZA; textSize = 13f; isAntiAlias = true
            typeface = Typeface.SANS_SERIF
        }
        val rotuloPaint = Paint().apply {
            color = AZUL; textSize = 14f; isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textoPaint = Paint().apply {
            color = CINZA; textSize = 12f; isAntiAlias = true
            typeface = Typeface.SANS_SERIF
        }

        var y = MARGEM + 10f
        canvas.drawText(titulo.ifBlank { "Ferramenta" }, MARGEM, y, tituloPaint)
        y += 22f
        canvas.drawText(subtitulo, MARGEM, y, subPaint)
        y += 30f

        val larguraUtil = LARGURA - 2 * MARGEM
        campos.forEach { (rotulo, texto) ->
            canvas.drawText(rotulo, MARGEM, y, rotuloPaint)
            y += 20f
            y = desenharParagrafo(canvas, texto.ifBlank { "—" }, MARGEM, y, larguraUtil, textoPaint)
            y += 16f
        }

        documento.finishPage(pagina)
        val pasta = File(context.cacheDir, "ferramentas").apply { mkdirs() }
        val arquivo = File(pasta, "ferramenta.pdf")
        arquivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return arquivo
    }

    /** Quebra o texto em linhas que cabem na largura e desenha; retorna o novo y. */
    private fun desenharParagrafo(
        canvas: android.graphics.Canvas,
        texto: String,
        x: Float,
        yInicial: Float,
        larguraMax: Float,
        paint: Paint
    ): Float {
        var y = yInicial
        val palavras = texto.split(" ")
        var linha = StringBuilder()
        for (palavra in palavras) {
            val tentativa = if (linha.isEmpty()) palavra else "$linha $palavra"
            if (paint.measureText(tentativa) > larguraMax) {
                canvas.drawText(linha.toString(), x, y, paint)
                y += 16f
                linha = StringBuilder(palavra)
            } else {
                linha = StringBuilder(tentativa)
            }
        }
        if (linha.isNotEmpty()) {
            canvas.drawText(linha.toString(), x, y, paint)
            y += 16f
        }
        return y
    }
}
