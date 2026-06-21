"""Página Relatórios — conclusões enviadas pelos alunos (pull) e KPIs."""
import pandas as pd
import streamlit as st

from auth_operador import require_login
from services import relatorios

require_login()
st.title("📊 Relatórios")

try:
    r = relatorios.resumo()
    c1, c2, c3 = st.columns(3)
    c1.metric("Matrículas", r["matriculas"])
    c2.metric("Conclusões", r["conclusoes"])
    c3.metric("Taxa de conclusão", f"{r['taxa']:.0f}%")

    st.divider()
    st.subheader("Situação por aluno")
    st.caption("Cruzamento de matrículas × conclusões.")
    st.dataframe(pd.DataFrame(relatorios.situacao_por_aluno()), width="stretch")

    st.divider()
    st.subheader("Conclusões")
    st.dataframe(pd.DataFrame(relatorios.listar_conclusoes()), width="stretch")
except Exception as e:  # noqa: BLE001
    st.error(str(e))
