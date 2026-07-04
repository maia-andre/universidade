"""Página Relatórios — conclusões enviadas pelos alunos (pull) e KPIs."""
import pandas as pd
import streamlit as st

import erros
from auth_operador import require_login
from services import cache

require_login()
st.title("📊 Relatórios")

with erros.protegido("resumo de indicadores", parar=True):
    r = cache.resumo_kpis()
    c1, c2, c3 = st.columns(3)
    c1.metric("Matrículas", r["matriculas"])
    c2.metric("Conclusões", r["conclusoes"])
    c3.metric("Taxa de conclusão", f"{r['taxa']:.0f}%")

st.divider()
st.subheader("Situação por aluno")
st.caption("Cruzamento de matrículas × conclusões.")
with erros.protegido("situação por aluno"):
    st.dataframe(pd.DataFrame(cache.situacao_alunos()), width="stretch")

st.divider()
st.subheader("Conclusões")
with erros.protegido("lista de conclusões"):
    st.dataframe(pd.DataFrame(cache.conclusoes_lista()), width="stretch")
