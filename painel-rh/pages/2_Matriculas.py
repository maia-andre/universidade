"""Página Matrículas — liberar um curso a um aluno (define o curso ativo no app)."""
import pandas as pd
import streamlit as st

import erros
from auth_operador import require_login
from services import cache, matriculas

operador = require_login()
st.title("🎓 Matrículas")
st.caption("Liberar um curso define o **curso ativo** do aluno no app (lido no login/sync).")

with erros.protegido("lista de alunos", parar=True):
    lista = cache.alunos_lista()

# Catálogo dinâmico: vem do conteúdo publicado (config/conteudo) — cursos novos
# criados na página Conteúdo aparecem aqui sem mexer em código.
with erros.protegido("catálogo de cursos", parar=True):
    catalogo = cache.catalogo_cursos()

if lista:
    opcoes = {f"{a.get('nome', '?')} — {a.get('email', '')}": a["uid"] for a in lista}
    with st.form("liberar"):
        aluno_label = st.selectbox("Aluno", list(opcoes.keys()))
        curso_id = st.selectbox("Curso", sorted(catalogo), format_func=lambda i: catalogo[i])
        ok = st.form_submit_button("Liberar curso")
    if st.button("↻ Recarregar cursos", help="Busca cursos publicados há pouco na página Conteúdo."):
        cache.invalidar_catalogo()
        st.rerun()
    if ok:
        with erros.protegido("liberar curso"):
            matriculas.liberar_curso(opcoes[aluno_label], curso_id, catalogo[curso_id], operador)
            cache.invalidar_matriculas()
            st.success("Curso liberado. O app o definirá como curso ativo no próximo login/sync.")
else:
    st.info("Cadastre alunos primeiro (página **Alunos**).")

st.divider()
st.subheader("Matrículas existentes")
with erros.protegido("lista de matrículas"):
    lista_matriculas = cache.matriculas_lista()
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
        with erros.protegido("encerrar matrícula"):
            matriculas.encerrar_matricula(m["uid"], m["cursoId"], operador)
            cache.invalidar_matriculas()
            st.toast("Matrícula encerrada.", icon="✅")  # sobrevive ao rerun (st.success não)
            st.rerun()
