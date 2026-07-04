"""Painel RH — Universidade do Servidor (home / dashboard).

Entry point do Streamlit. Rode com: `streamlit run app.py` (ou o atalho run.bat).
"""
import streamlit as st

import erros
import ui
from auth_operador import require_login
from services import cache

ui.configurar_pagina("Painel RH — Universidade do Servidor")
require_login()
ui.pagina_header(
    "Painel RH — Universidade do Servidor", "🎓",
    "Gestão de acesso aos cursos e acompanhamento de conclusões.",
)

with erros.protegido("resumo de indicadores"):
    r = cache.resumo_kpis()
    c1, c2, c3 = st.columns(3)
    c1.container(border=True).metric("📋 Matrículas", r["matriculas"])
    c2.container(border=True).metric("🏆 Conclusões", r["conclusoes"])
    c3.container(border=True).metric("📈 Taxa de conclusão", f"{r['taxa']:.0f}%")

st.divider()
st.subheader("Acesso rápido")
cols = st.columns(5)
cols[0].page_link("pages/1_Alunos.py", label="Alunos", icon="👥")
cols[1].page_link("pages/2_Matriculas.py", label="Matrículas", icon="🎓")
cols[2].page_link("pages/3_Relatorios.py", label="Relatórios", icon="📊")
cols[3].page_link("pages/4_Conteudo.py", label="Conteúdo", icon="📚")
if st.session_state.get("operador_admin"):
    cols[4].page_link("pages/5_Operadores.py", label="Operadores", icon="🛠")

with st.expander("Como usar o painel"):
    st.markdown(
        "- **Alunos** — cadastrar servidores (manual ou por planilha); cada aluno recebe "
        "uma senha temporária própria\n"
        "- **Matrículas** — liberar um curso (define o curso ativo do aluno no app)\n"
        "- **Relatórios** — situação por aluno e conclusões enviadas pelo app\n"
        "- **Conteúdo** — editar e publicar os cursos (o app atualiza sem novo APK)\n"
        "- **Operadores** — gestão de acesso ao painel (só administradores)"
    )
