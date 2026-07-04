"""Página Matrículas — liberar um curso a um aluno (define o curso ativo no app)."""
import pandas as pd
import streamlit as st

import erros
import ui
from auth_operador import require_login
from services import cache, matriculas

ui.configurar_pagina("Matrículas — Painel RH", "🎓")
operador = require_login()
ui.pagina_header(
    "Matrículas", "🎓",
    "Liberar um curso define o **curso ativo** do aluno no app (lido no login/sync).",
)

with erros.protegido("lista de alunos", parar=True):
    lista = cache.alunos_lista()

# Catálogo dinâmico: vem do conteúdo publicado (config/conteudo) — cursos novos
# criados na página Conteúdo aparecem aqui sem mexer em código.
with erros.protegido("catálogo de cursos", parar=True):
    catalogo = cache.catalogo_cursos()

if lista:
    opcoes = {f"{a.get('nome', '?')} — {a.get('email', '')}": a["uid"] for a in lista}
    aluno_label = st.selectbox("Aluno", list(opcoes.keys()))
    col_curso, col_rec = st.columns([4, 1], vertical_alignment="bottom")
    curso_id = col_curso.selectbox("Curso", sorted(catalogo), format_func=lambda i: catalogo[i])
    if col_rec.button("↻ Recarregar cursos", width="stretch",
                      help="Busca cursos publicados há pouco na página Conteúdo."):
        cache.invalidar_catalogo()
        st.rerun()
    if st.button("Liberar curso", type="primary"):
        uid = opcoes[aluno_label]
        with erros.protegido("liberar curso"):
            # Regra: um curso ativo por aluno. Se há outra matrícula ativa, o operador
            # confirma a TROCA (encerra as demais e libera a nova).
            outras = [m for m in matriculas.matriculas_ativas(uid)
                      if m.get("cursoId") != curso_id]
            if outras:
                titulos = ", ".join(str(m.get("cursoTitulo") or m.get("cursoId"))
                                    for m in outras)

                def _trocar(uid=uid, curso_id=curso_id):
                    matriculas.liberar_curso(uid, curso_id, catalogo[curso_id], operador,
                                             encerrar_outras=True)
                    cache.invalidar_matriculas()

                ui.confirmar_acao(
                    "Trocar curso ativo",
                    f"**{aluno_label}** já tem matrícula ativa em: **{titulos}**. "
                    f"A regra é **um curso ativo por aluno** — encerrar e liberar "
                    f"**{catalogo[curso_id]}**? Cursos já concluídos permanecem acessíveis.",
                    _trocar,
                    rotulo="Trocar curso",
                )
            else:
                matriculas.liberar_curso(uid, curso_id, catalogo[curso_id], operador)
                cache.invalidar_matriculas()
                st.success("Curso liberado. O app o definirá como curso ativo "
                           "no próximo login/sync.")
else:
    st.info("Cadastre alunos primeiro (página **Alunos**).")

st.divider()
st.subheader("Matrículas existentes")
with erros.protegido("lista de matrículas"):
    lista_matriculas = cache.matriculas_lista()
nomes = {a["uid"]: a.get("nome", a["uid"]) for a in lista}
df_m = pd.DataFrame(lista_matriculas)
if not df_m.empty:
    df_m["aluno"] = df_m["uid"].map(nomes)
    df_m["status"] = df_m["status"].map(
        {"ativa": "Ativa", "encerrada": "Encerrada"}).fillna(df_m["status"])
st.dataframe(
    df_m,
    width="stretch",
    column_order=("aluno", "cursoTitulo", "status", "liberadoEm", "liberadoPor", "encerradoEm"),
    column_config={
        "aluno": st.column_config.TextColumn("Aluno"),
        "cursoTitulo": st.column_config.TextColumn("Curso"),
        "status": st.column_config.TextColumn("Status"),
        "liberadoEm": st.column_config.DatetimeColumn("Liberado em", format="DD/MM/YYYY HH:mm"),
        "liberadoPor": st.column_config.TextColumn("Liberado por"),
        "encerradoEm": st.column_config.DatetimeColumn("Encerrado em", format="DD/MM/YYYY HH:mm"),
    },
)
ui.aviso_truncamento(len(lista_matriculas), 500)

# Encerrar matrícula (desmatricular) — v7, Item 5.
ativas = [m for m in lista_matriculas if m.get("status") == "ativa"]
if ativas:
    st.divider()
    st.subheader("Encerrar matrícula")
    st.caption("Encerra o acesso ao curso no app (cursos já concluídos seguem acessíveis).")
    rotulos = {
        f"{nomes.get(m['uid'], m['uid'])} — {m.get('cursoTitulo', m.get('cursoId'))}": m
        for m in ativas
    }
    alvo = st.selectbox("Matrícula ativa", list(rotulos.keys()))
    if st.button("Encerrar matrícula"):
        m = rotulos[alvo]

        def _encerrar(m=m):
            matriculas.encerrar_matricula(m["uid"], m["cursoId"], operador)
            cache.invalidar_matriculas()

        ui.confirmar_acao(
            "Encerrar matrícula",
            f"Encerrar o acesso de **{nomes.get(m['uid'], m['uid'])}** ao curso "
            f"**{m.get('cursoTitulo', m.get('cursoId'))}**? "
            "Cursos já concluídos permanecem acessíveis.",
            _encerrar,
        )
