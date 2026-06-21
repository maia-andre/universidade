package com.sgaf.universidadedoservidor.core.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.sgaf.universidadedoservidor.R
import java.io.File

/**
 * Gera o certificado em PDF (A4 paisagem), 100% offline, seguindo o `docs/Modelo.pdf` institucional:
 * ornamentos angulares azul/ouro, título "CERTIFICADO", texto da Escola de Governo –
 * Universidade do Servidor (com carga horária) e rodapé com o logo + Prefeitura/URL. (v6)
 */
object CertificadoPdfGenerator {

    private const val LARGURA = 842 // A4 paisagem em pontos (1/72")
    private const val ALTURA = 595
    private const val AZUL = 0xFF003882.toInt()        // azul oficial SJC
    private const val AZUL_TITULO = 0xFF2C6BB3.toInt() // azul mais claro do título (Modelo.pdf)
    private const val OURO = 0xFFFFD700.toInt()
    private const val CINZA_TEXTO = 0xFF555555.toInt()
    private const val CINZA_FORMA = 0xFFC2C7CC.toInt()

    fun gerar(
        context: Context,
        nomeAluno: String,
        cursoTitulo: String,
        cargaHoraria: Int?,
        dataTexto: String
    ): File {
        val documento = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create()
        val pagina = documento.startPage(pageInfo)
        val canvas = pagina.canvas
        val centroX = LARGURA / 2f

        canvas.drawColor(Color.WHITE)
        desenharOrnamentos(canvas)
        desenharMoldura(canvas)
        desenharTitulo(canvas, centroX)
        desenharCorpo(canvas, centroX, nomeAluno, cursoTitulo, cargaHoraria, dataTexto)
        desenharRodape(context, canvas)

        documento.finishPage(pagina)

        val pasta = File(context.cacheDir, "certificados").apply { mkdirs() }
        val arquivo = File(pasta, "certificado.pdf")
        arquivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return arquivo
    }

    /** Faixas angulares: triângulo ouro + faixa azul no topo; faixa cinza sutil na base. */
    private fun desenharOrnamentos(canvas: Canvas) {
        val ouro = Paint().apply { color = OURO; style = Paint.Style.FILL; isAntiAlias = true }
        val azul = Paint().apply { color = AZUL; style = Paint.Style.FILL; isAntiAlias = true }
        val cinza = Paint().apply { color = CINZA_FORMA; style = Paint.Style.FILL; isAntiAlias = true }

        canvas.drawPath(Path().apply {
            moveTo(0f, 0f); lineTo(330f, 0f); lineTo(0f, 140f); close()
        }, ouro)
        canvas.drawPath(Path().apply {
            moveTo(150f, 0f); lineTo(LARGURA.toFloat(), 0f)
            lineTo(LARGURA.toFloat(), 92f); lineTo(330f, 92f); close()
        }, azul)
        canvas.drawPath(Path().apply {
            moveTo(0f, ALTURA.toFloat()); lineTo(LARGURA.toFloat(), ALTURA.toFloat())
            lineTo(LARGURA.toFloat(), ALTURA - 16f); lineTo(0f, ALTURA - 28f); close()
        }, cinza)
    }

    private fun desenharMoldura(canvas: Canvas) {
        val moldura = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = AZUL; isAntiAlias = true
        }
        canvas.drawRect(22f, 22f, LARGURA - 22f, ALTURA - 22f, moldura)
    }

    private fun desenharTitulo(canvas: Canvas, centroX: Float) {
        val titulo = Paint().apply {
            color = AZUL_TITULO
            textAlign = Paint.Align.CENTER
            textSize = 52f
            letterSpacing = 0.28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("CERTIFICADO", centroX, 195f, titulo)
    }

    private fun desenharCorpo(
        canvas: Canvas,
        centroX: Float,
        nomeAluno: String,
        cursoTitulo: String,
        cargaHoraria: Int?,
        dataTexto: String
    ) {
        val corpo = Paint().apply {
            color = CINZA_TEXTO; textAlign = Paint.Align.CENTER; textSize = 17f
            typeface = Typeface.SANS_SERIF; isAntiAlias = true
        }
        canvas.drawText(
            "A Prefeitura Municipal de São José dos Campos certifica que",
            centroX, 268f, corpo
        )

        // Nome em destaque (reduz a fonte se for muito longo, para não estourar a moldura)
        val nomePaint = Paint().apply {
            color = AZUL; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD); isAntiAlias = true
        }
        val nome = nomeAluno.ifBlank { "________________________" }
        nomePaint.textSize = ajustarTamanho(nome, nomePaint, 34f, LARGURA - 160f)
        canvas.drawText(nome, centroX, 312f, nomePaint)

        // Parágrafo institucional (quebra automática), com a carga horária quando definida
        val horas = cargaHoraria?.let { ", com $it horas de duração" } ?: ""
        val paragrafo =
            "participou do $cursoTitulo promovido pela Escola de Governo " +
                "– Universidade do Servidor$horas."
        desenharTextoCentralizado(canvas, paragrafo, corpo, centroX, 350f, LARGURA - 180f, 24f)

        val data = Paint(corpo).apply { textSize = 14f }
        canvas.drawText("São José dos Campos, $dataTexto.", centroX, 432f, data)
    }

    private fun desenharRodape(context: Context, canvas: Canvas) {
        val baseY = 506f

        val logo = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_uniservidor)
        } catch (e: Exception) {
            null
        }
        if (logo != null) {
            val altura = 36f
            val largura = altura * logo.width / logo.height
            val x = 64f
            canvas.drawBitmap(
                logo, null,
                RectF(x, baseY, x + largura, baseY + altura),
                Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            )
            logo.recycle()
        }

        val prefeitura = Paint().apply {
            color = AZUL; textAlign = Paint.Align.RIGHT; textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); isAntiAlias = true
        }
        canvas.drawText("Prefeitura de São José dos Campos", LARGURA - 64f, baseY + 16f, prefeitura)
        val url = Paint().apply {
            color = CINZA_TEXTO; textAlign = Paint.Align.RIGHT; textSize = 11f
            typeface = Typeface.SANS_SERIF; isAntiAlias = true
        }
        canvas.drawText("www.SJC.sp.gov.br", LARGURA - 64f, baseY + 33f, url)
    }

    /** Quebra [texto] em linhas que cabem em [maxWidth] e desenha centralizado a partir de [startY]. */
    private fun desenharTextoCentralizado(
        canvas: Canvas, texto: String, paint: Paint,
        centroX: Float, startY: Float, maxWidth: Float, lineHeight: Float
    ) {
        val linhas = mutableListOf<String>()
        var atual = StringBuilder()
        for (palavra in texto.split(" ")) {
            val tentativa = if (atual.isEmpty()) palavra else "$atual $palavra"
            if (paint.measureText(tentativa) <= maxWidth) {
                atual = StringBuilder(tentativa)
            } else {
                if (atual.isNotEmpty()) linhas.add(atual.toString())
                atual = StringBuilder(palavra)
            }
        }
        if (atual.isNotEmpty()) linhas.add(atual.toString())

        var y = startY
        for (linha in linhas) {
            canvas.drawText(linha, centroX, y, paint)
            y += lineHeight
        }
    }

    /** Reduz a fonte até [texto] caber em [maxWidth], partindo de [tamanhoBase]. */
    private fun ajustarTamanho(texto: String, paint: Paint, tamanhoBase: Float, maxWidth: Float): Float {
        var size = tamanhoBase
        paint.textSize = size
        while (paint.measureText(texto) > maxWidth && size > 16f) {
            size -= 1f
            paint.textSize = size
        }
        return size
    }
}
