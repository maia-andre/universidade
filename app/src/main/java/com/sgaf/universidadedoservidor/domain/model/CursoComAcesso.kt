package com.sgaf.universidadedoservidor.domain.model

/**
 * Estado de acesso de um curso para o aluno logado (v7, Item 1).
 *
 * Regra: `acessível = matriculado OU concluído`. O catálogo mostra todos os cursos, mas só
 * deixa abrir os acessíveis. (Independente de [Curso.isAvailable], que sinaliza conteúdo
 * ainda não publicado — "Em breve".)
 */
enum class AcessoCurso {
    /** Matriculado pelo RH e ainda não concluído — em andamento. */
    ATIVO,

    /** Já concluído — acessível para sempre (revisar aulas e rever o certificado). */
    CONCLUIDO,

    /** Nem matriculado nem concluído — bloqueado (cadeado). */
    BLOQUEADO
}

/** Curso com o seu estado de acesso resolvido para o aluno logado. */
data class CursoComAcesso(
    val curso: Curso,
    val acesso: AcessoCurso
) {
    /** Conveniência: o aluno pode abrir este curso? */
    val acessivel: Boolean get() = acesso != AcessoCurso.BLOQUEADO
}
