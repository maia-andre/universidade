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


def _filtrar_por_aluno(df: pd.DataFrame, termo: str) -> pd.DataFrame:
    """Filtra por nome OU e-mail (case-insensitive, contém)."""
    if not termo.strip() or df.empty:
        return df
    t = termo.strip().lower()
    colunas = [c for c in ("aluno", "email") if c in df.columns]
    mascara = pd.Series(False, index=df.index)
    for c in colunas:
        mascara |= df[c].fillna("").astype(str).str.lower().str.contains(t, regex=False)
    return df[mascara]


st.divider()
filtro = st.text_input(
    "🔎 Buscar aluno", placeholder="Filtra as tabelas abaixo por nome ou e-mail…"
)

# ------------------------------------------------------------------ situação por aluno
st.subheader("Situação por aluno")
st.caption("Cruzamento de matrículas × conclusões.")
with erros.protegido("situação por aluno"):
    df_s = pd.DataFrame(cache.situacao_alunos())
    if not df_s.empty:
        status = st.segmented_control(
            "Filtrar matrículas", ["Todas", "Ativas", "Encerradas"], default="Todas"
        )
        if status == "Ativas":
            df_s = df_s[df_s["matrícula"] == "ativa"]
        elif status == "Encerradas":
            df_s = df_s[df_s["matrícula"] == "encerrada"]
        df_s = _filtrar_por_aluno(df_s, filtro)
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
    if filtro.strip() and df_s.empty:
        st.caption("Nenhum aluno corresponde ao filtro.")

# ------------------------------------------------------------------ conclusões
st.divider()
st.subheader("Conclusões")
with erros.protegido("lista de conclusões"):
    conclusoes = cache.conclusoes_lista()
    df_c = pd.DataFrame(conclusoes)
    if not df_c.empty:
        # Enriquece os docs crus (uid/cursoId) com nome, e-mail e título do curso.
        alunos_por_uid = {a["uid"]: a for a in cache.alunos_lista()}
        catalogo = cache.catalogo_cursos()
        df_c["aluno"] = df_c["uid"].map(
            lambda u: alunos_por_uid.get(u, {}).get("nome") or u)
        df_c["email"] = df_c["uid"].map(
            lambda u: alunos_por_uid.get(u, {}).get("email") or "")
        df_c["curso"] = df_c["cursoId"].map(lambda c: catalogo.get(c) or str(c))
        if "concluidoEm" in df_c.columns:
            df_c = df_c.sort_values("concluidoEm", ascending=False, na_position="last")
        df_c = _filtrar_por_aluno(df_c, filtro)
    st.dataframe(
        df_c,
        width="stretch",
        column_order=("aluno", "email", "curso", "nota", "concluidoEm", "certificadoId"),
        column_config={
            "aluno": st.column_config.TextColumn("Aluno"),
            "email": st.column_config.TextColumn("E-mail"),
            "curso": st.column_config.TextColumn("Curso"),
            "nota": st.column_config.NumberColumn("Nota", format="%d"),
            "concluidoEm": st.column_config.DatetimeColumn(
                "Concluído em", format="DD/MM/YYYY HH:mm"
            ),
            "certificadoId": st.column_config.TextColumn("Certificado"),
        },
    )
    if filtro.strip() and df_c.empty:
        st.caption("Nenhuma conclusão corresponde ao filtro.")
    ui.aviso_truncamento(len(conclusoes), 1000)
