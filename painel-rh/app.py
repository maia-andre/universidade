"""Painel RH — Universidade do Servidor (home / dashboard).

Entry point do Streamlit. Rode com: `streamlit run app.py` (ou o atalho run.bat).
"""
import streamlit as st

from auth_operador import require_login
from firebase_client import ChaveAusenteError
from services import relatorios

st.set_page_config(
    page_title="Painel RH — Universidade do Servidor",
    page_icon="🎓",
    layout="wide",
)

require_login()

st.title("🎓 Painel RH — Universidade do Servidor")
st.caption("Gestão de acesso aos cursos e acompanhamento de conclusões.")

try:
    r = relatorios.resumo()
    c1, c2, c3 = st.columns(3)
    c1.metric("Matrículas", r["matriculas"])
    c2.metric("Conclusões", r["conclusoes"])
    c3.metric("Taxa de conclusão", f"{r['taxa']:.0f}%")
except ChaveAusenteError as e:
    st.warning(str(e))
except Exception as e:  # noqa: BLE001
    st.error(f"Erro ao acessar o Firestore: {e}")

st.divider()
st.markdown(
    "Use o menu lateral:\n"
    "- **Alunos** — cadastrar servidores (manual ou por planilha)\n"
    "- **Matrículas** — liberar um curso (define o curso ativo do aluno no app)\n"
    "- **Relatórios** — conclusões enviadas pelos alunos"
)
