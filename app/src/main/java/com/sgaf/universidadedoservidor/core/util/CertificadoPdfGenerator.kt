package com.sgaf.universidadedoservidor.core.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Gera um certificado de conclusão em PDF, 100% offline, com a identidade visual de SJC
 * (azul #003882 e ouro #FFD700). Item 4.
 */
object CertificadoPdfGenerator {

    private const val LARGURA = 842 // A4 paisagem em pontos (1/72")
    private const val ALTURA = 595
    private const val AZUL = 0xFF003882.toInt()
    private const val OURO = 0xFFFFD700.toInt()
    private const val CINZA = 0xFF444444.toInt()

    /**
     * Desenha o certificado e salva em cacheDir/certificados/. Retorna o arquivo gerado.
     */
    fun gerar(
        context: Context,
        nomeAluno: String,
        cursoTitulo: String,
        aproveitamento: Int,
        dataTexto: String
    ): File {
        val documento = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create()
        val pagina = documento.startPage(pageInfo)
        val canvas = pagina.canvas

        // Fundo branco
        canvas.drawColor(Color.WHITE)

        // Bordas (dupla) em ouro/azul
        val bordaOuro = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = OURO
            isAntiAlias = true
        }
        canvas.drawRect(24f, 24f, LARGURA - 24f, ALTURA - 24f, bordaOuro)
        val bordaAzul = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = AZUL
            isAntiAlias = true
        }
        canvas.drawRect(34f, 34f, LARGURA - 34f, ALTURA - 34f, bordaAzul)

        val centroX = LARGURA / 2f

        // Título
        val tituloPaint = Paint().apply {
            color = AZUL
            textAlign = Paint.Align.CENTER
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("CERTIFICADO", centroX, 130f, tituloPaint)

        val subtituloPaint = Paint().apply {
            color = OURO
            textAlign = Paint.Align.CENTER
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("DE CONCLUSÃO", centroX, 158f, subtituloPaint)

        // Corpo
        val corpoPaint = Paint().apply {
            color = CINZA
            textAlign = Paint.Align.CENTER
            textSize = 18f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        canvas.drawText("Certificamos que", centroX, 230f, corpoPaint)

        val nomePaint = Paint().apply {
            color = AZUL
            textAlign = Paint.Align.CENTER
            textSize = 34f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(nomeAluno, centroX, 280f, nomePaint)

        canvas.drawText("concluiu com aproveitamento de $aproveitamento% o curso", centroX, 325f, corpoPaint)

        val cursoPaint = Paint().apply {
            color = AZUL
            textAlign = Paint.Align.CENTER
            textSize = 24f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("\"$cursoTitulo\"", centroX, 365f, cursoPaint)

        // Data
        val dataPaint = Paint().apply {
            color = CINZA
            textAlign = Paint.Align.CENTER
            textSize = 14f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        canvas.drawText("São José dos Campos, $dataTexto", centroX, 430f, dataPaint)

        // Rodapé institucional
        val rodapePaint = Paint().apply {
            color = AZUL
            textAlign = Paint.Align.CENTER
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Universidade do Servidor", centroX, 510f, rodapePaint)
        val orgaoPaint = Paint().apply {
            color = CINZA
            textAlign = Paint.Align.CENTER
            textSize = 12f
            typeface = Typeface.SANS_SERIF
            isAntiAlias = true
        }
        canvas.drawText("Prefeitura Municipal de São José dos Campos", centroX, 530f, orgaoPaint)

        documento.finishPage(pagina)

        val pasta = File(context.cacheDir, "certificados").apply { mkdirs() }
        val arquivo = File(pasta, "certificado.pdf")
        arquivo.outputStream().use { documento.writeTo(it) }
        documento.close()

        return arquivo
    }
}
