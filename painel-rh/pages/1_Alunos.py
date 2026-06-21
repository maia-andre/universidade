"""Página Alunos — cadastro individual e importação por planilha."""
import pandas as pd
import streamlit as st

from auth_operador import require_login
from services import alunos

operador = require_login()
st.title("👥 Alunos")

tab_novo, tab_planilha, tab_lista = st.tabs(["Novo aluno", "Importar planilha", "Cadastrados"])

with tab_novo:
    with st.form("novo_aluno"):
        nome = st.text_input("Nome completo")
        email = st.text_input("E-mail")
        matricula = st.text_input("Matrícula")
        lotacao = st.text_input("Lotação")
        senha = st.text_input("Senha temporária", value="mudar@123")
        ok = st.form_submit_button("Cadastrar")
    if ok:
        if not (nome.strip() and email.strip()):
            st.error("Nome e e-mail são obrigatórios.")
        else:
            try:
                uid = alunos.criar_aluno(nome, email, matricula, lotacao, senha, operador)
                st.success(
                    f"Aluno criado (uid `{uid}`). Entregue a senha temporária ao servidor "
                    "e oriente a troca no primeiro acesso."
                )
            except Exception as e:  # noqa: BLE001
                st.error(f"Falha ao criar: {e}")

with tab_planilha:
    st.caption("Planilha com as colunas: **nome, email, matricula, lotacao** "
               "(veja `modelo_importacao.csv`).")
    arquivo = st.file_uploader("CSV ou Excel", type=["csv", "xlsx"])
    senha_padrao = st.text_input("Senha temporária padrão", value="mudar@123")
    if arquivo and st.button("Importar"):
        df = pd.read_csv(arquivo) if arquivo.name.endswith(".csv") else pd.read_excel(arquivo)
        criados, erros = alunos.importar_planilha(df, senha_padrao, operador)
        st.success(f"{len(criados)} aluno(s) criado(s).")
        if erros:
            st.error(f"{len(erros)} com erro:")
            st.dataframe(pd.DataFrame(erros, columns=["email", "erro"]), width="stretch")

with tab_lista:
    try:
        st.dataframe(pd.DataFrame(alunos.listar_alunos()), width="stretch")
    except Exception as e:  # noqa: BLE001
        st.error(str(e))
