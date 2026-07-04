"""Painel RH — Universidade do Servidor (entry point).

Roteia as páginas com `st.navigation` para controlar rótulo e ícone na sidebar
(sem isso o Streamlit usa o nome do arquivo — a home aparecia como "app").
Rode com: `streamlit run app.py` (ou o atalho run.bat).
"""
import streamlit as st

st.navigation([
    st.Page("pages/0_Inicio.py", title="Início", icon="🏠", default=True),
    st.Page("pages/1_Alunos.py", title="Alunos", icon="👥"),
    st.Page("pages/2_Matriculas.py", title="Matrículas", icon="🎓"),
    st.Page("pages/3_Relatorios.py", title="Relatórios", icon="📊"),
    st.Page("pages/4_Conteudo.py", title="Conteúdo", icon="📚"),
    st.Page("pages/5_Operadores.py", title="Operadores", icon="🛠"),
]).run()
