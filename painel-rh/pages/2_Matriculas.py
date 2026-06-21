"""Página Matrículas — liberar um curso a um aluno (define o curso ativo no app)."""
import pandas as pd
import streamlit as st

from auth_operador import require_login
from config import CURSOS
from services import alunos, matriculas

operador = require_login()
st.title("🎓 Matrículas")
st.caption("Liberar um curso define o **curso ativo** do aluno no app (lido no login/sync).")

try:
    lista = alunos.listar_alunos()
except Exception as e:  # noqa: BLE001
    st.error(str(e))
    lista = []

if lista:
    opcoes = {f"{a.get('nome', '?')} — {a.get('email', '')}": a["uid"] for a in lista}
    with st.form("liberar"):
        aluno_label = st.selectbox("Aluno", list(opcoes.keys()))
        curso_id = st.selectbox("Curso", list(CURSOS.keys()), format_func=lambda i: CURSOS[i])
        ok = st.form_submit_button("Liberar curso")
    if ok:
        try:
            matriculas.liberar_curso(opcoes[aluno_label], curso_id, operador)
            st.success("Curso liberado. O app o definirá como curso ativo no próximo login/sync.")
        except Exception as e:  # noqa: BLE001
            st.error(str(e))
else:
    st.info("Cadastre alunos primeiro (página **Alunos**).")

st.divider()
st.subheader("Matrículas existentes")
try:
    lista_matriculas = matriculas.listar_matriculas()
except Exception as e:  # noqa: BLE001
    st.error(str(e))
    lista_matriculas = []
st.dataframe(pd.DataFrame(lista_matriculas), width="stretch")

# Encerrar matrícula (desmatricular) — v7, Item 5.
ativas = [m for m in lista_matriculas if m.get("status") == "ativa"]
if ativas:
    st.divider()
    st.subheader("Encerrar matrícula")
    st.caption("Encerra o acesso ao curso no app (cursos já concluídos seguem acessíveis).")
    nomes = {a["uid"]: a.get("nome", a["uid"]) for a in lista}
    rotulos = {
        f"{nomes.get(m['uid'], m['uid'])} — {m.get('cursoTitulo', m.get('cursoId'))}": m
        for m in ativas
    }
    with st.form("encerrar"):
        alvo = st.selectbox("Matrícula ativa", list(rotulos.keys()))
        enc = st.form_submit_button("Encerrar matrícula")
    if enc:
        m = rotulos[alvo]
        try:
            matriculas.encerrar_matricula(m["uid"], m["cursoId"], operador)
            st.success("Matrícula encerrada.")
            st.rerun()
        except Exception as e:  # noqa: BLE001
            st.error(str(e))
