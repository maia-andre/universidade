"""Página Relatórios — conclusões enviadas pelos alunos (pull) e KPIs."""
import pandas as pd
import streamlit as st

import erros
import ui
from auth_operador import require_login
from services import cache

ui.configurar_pagina("Relatórios — Painel RH", "📊")
require_login()
ui.pagina_header("Relatórios", "📊", "Situação por aluno e conclusões enviadas pelo app.")

with erros.protegido("resumo de indicadores", parar=True):
    r = cache.resumo_kpis()
    c1, c2, c3 = st.columns(3)
    c1.container(border=True).metric("📋 Matrículas", r["matriculas"])
    c2.container(border=True).metric("🏆 Conclusões", r["conclusoes"])
    c3.container(border=True).metric("📈 Taxa de conclusão", f"{r['taxa']:.0f}%")

st.divider()
st.subheader("Situação por aluno")
st.caption("Cruzamento de matrículas × conclusões.")
with erros.protegido("situação por aluno"):
    df_s = pd.DataFrame(cache.situacao_alunos())
    if not df_s.empty:
        filtro = st.segmented_control(
            "Filtrar matrículas", ["Todas", "Ativas", "Encerradas"], default="Todas"
        )
        if filtro == "Ativas":
            df_s = df_s[df_s["matrícula"] == "ativa"]
        elif filtro == "Encerradas":
            df_s = df_s[df_s["matrícula"] == "encerrada"]
    st.dataframe(
        df_s,
        width="stretch",
        column_config={
            "aluno": st.column_config.TextColumn("Aluno"),
            "email": st.column_config.TextColumn("E-mail"),
            "curso": st.column_config.TextColumn("Curso"),
            "matrícula": st.column_config.TextColumn("Matrícula"),
            "concluído": st.column_config.CheckboxColumn("Concluído"),
        },
    )

st.divider()
st.subheader("Conclusões")
with erros.protegido("lista de conclusões"):
    conclusoes = cache.conclusoes_lista()
    st.dataframe(
        pd.DataFrame(conclusoes),
        width="stretch",
        column_order=("uid", "cursoId", "nota", "certificadoId", "concluidoEm"),
        column_config={
            "uid": st.column_config.TextColumn("Aluno (uid)"),
            "cursoId": st.column_config.NumberColumn("Curso", format="%d"),
            "nota": st.column_config.NumberColumn("Nota", format="%d"),
            "certificadoId": st.column_config.TextColumn("Certificado"),
            "concluidoEm": st.column_config.DatetimeColumn(
                "Concluído em", format="DD/MM/YYYY HH:mm"
            ),
        },
    )
    ui.aviso_truncamento(len(conclusoes), 1000)
