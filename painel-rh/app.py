"""Painel RH — Universidade do Servidor (home / dashboard).

Entry point do Streamlit. Rode com: `streamlit run app.py` (ou o atalho run.bat).
"""
import streamlit as st

import erros
from auth_operador import require_login
from services import cache

st.set_page_config(
    page_title="Painel RH — Universidade do Servidor",
    page_icon="🎓",
    layout="wide",
)

require_login()

st.title("🎓 Painel RH — Universidade do Servidor")
st.caption("Gestão de acesso aos cursos e acompanhamento de conclusões.")

with erros.protegido("resumo de indicadores"):
    r = cache.resumo_kpis()
    c1, c2, c3 = st.columns(3)
    c1.metric("Matrículas", r["matriculas"])
    c2.metric("Conclusões", r["conclusoes"])
    c3.metric("Taxa de conclusão", f"{r['taxa']:.0f}%")

st.divider()
st.markdown(
    "Use o menu lateral:\n"
    "- **Alunos** — cadastrar servidores (manual ou por planilha)\n"
    "- **Matrículas** — liberar um curso (define o curso ativo do aluno no app)\n"
    "- **Relatórios** — conclusões enviadas pelos alunos"
)
