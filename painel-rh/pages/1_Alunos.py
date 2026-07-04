"""Página Alunos — cadastro individual e importação por planilha.

Senhas temporárias são SEMPRE geradas aleatórias (uma por aluno) e exibidas uma única
vez — ficam em `st.session_state` até o operador limpar, para não se perderem num rerun.
"""
import pandas as pd
import streamlit as st

import erros
from auth_operador import require_login
from services import alunos, cache, validacao

operador = require_login()
st.title("👥 Alunos")

tab_novo, tab_planilha, tab_lista = st.tabs(["Novo aluno", "Importar planilha", "Cadastrados"])

# --------------------------------------------------------------------------- novo aluno
with tab_novo:
    with st.form("novo_aluno", clear_on_submit=True):
        nome = st.text_input("Nome completo")
        email = st.text_input("E-mail")
        matricula = st.text_input("Matrícula")
        lotacao = st.text_input("Lotação")
        ok = st.form_submit_button("Cadastrar")
    if ok:
        if not (nome.strip() and email.strip()):
            st.error("Nome e e-mail são obrigatórios.")
        else:
            with erros.protegido("criar aluno"):
                senha = validacao.gerar_senha_temporaria()
                alunos.criar_aluno(nome.strip(), email.strip(), matricula.strip(),
                                   lotacao.strip(), senha, operador)
                cache.invalidar_alunos()
                st.session_state["novo_aluno_resultado"] = {
                    "nome": nome.strip(), "email": email.strip(), "senha": senha,
                }
    if resultado := st.session_state.get("novo_aluno_resultado"):
        with st.container(border=True):
            st.markdown(f"✅ **Aluno criado:** {resultado['nome']} ({resultado['email']})")
            st.markdown("Senha temporária — **anote agora, não será mostrada de novo**:")
            st.code(resultado["senha"])
            st.caption("Entregue ao servidor e oriente a troca no primeiro acesso.")
            if st.button("Limpar", key="limpar_novo"):
                st.session_state.pop("novo_aluno_resultado", None)
                st.rerun()

# --------------------------------------------------------------------------- importar planilha
with tab_planilha:
    st.caption(
        "Planilha com as colunas: **nome, email, matricula, lotacao** "
        "(veja `modelo_importacao.csv`). Cada aluno recebe uma senha temporária "
        "aleatória própria. A importação valida tudo antes — nada é criado sem confirmação."
    )
    if (resultado_import := st.session_state.get("import_resultado")) is not None:
        df_r = pd.DataFrame(resultado_import)
        n_ok = int(df_r["uid"].notna().sum()) if "uid" in df_r.columns else 0
        n_err = len(df_r) - n_ok
        st.success(f"Importação concluída: {n_ok} aluno(s) criado(s)"
                   + (f", {n_err} com erro." if n_err else "."))
        st.dataframe(df_r, width="stretch")
        st.warning(
            "⚠️ As senhas temporárias acima são exibidas **uma única vez**. Baixe o CSV, "
            "entregue as credenciais aos servidores e **apague o arquivo depois** (LGPD)."
        )
        st.download_button(
            "⬇️ Baixar credenciais (CSV)",
            df_r.to_csv(index=False).encode("utf-8-sig"),
            file_name="credenciais_importacao.csv",
            mime="text/csv",
        )
        if st.button("Limpar resultado"):
            st.session_state.pop("import_resultado", None)
            st.rerun()
    else:
        arquivo = st.file_uploader("CSV ou Excel", type=["csv", "xlsx"])
        if arquivo:
            with erros.protegido("validação da planilha"):
                df = (pd.read_csv(arquivo) if arquivo.name.endswith(".csv")
                      else pd.read_excel(arquivo))
                validas, problemas = validacao.validar_planilha(df, cache.alunos_lista())
                c1, c2 = st.columns(2)
                c1.metric("Linhas válidas", len(validas))
                c2.metric("Com problema", len(problemas))
                if problemas:
                    st.error("Linhas que NÃO serão importadas:")
                    st.dataframe(pd.DataFrame(problemas), width="stretch")
                if validas:
                    st.dataframe(pd.DataFrame(validas), width="stretch")
                    if st.button(f"Confirmar importação de {len(validas)} aluno(s)",
                                 type="primary"):
                        with st.spinner("Criando as contas..."):
                            resultado = alunos.importar_planilha(validas, operador)
                        cache.invalidar_alunos()
                        st.session_state["import_resultado"] = resultado
                        st.rerun()

# --------------------------------------------------------------------------- cadastrados
with tab_lista:
    with erros.protegido("lista de alunos", parar=True):
        lista = cache.alunos_lista()
    st.dataframe(pd.DataFrame(lista), width="stretch")
    if len(lista) == 500:
        st.caption("⚠️ Exibindo os primeiros 500 registros.")

    if lista:
        st.divider()
        st.subheader("🔑 Redefinir senha")
        st.caption("Gera uma nova senha temporária para o aluno; a senha atual deixa de valer.")
        opcoes = {f"{a.get('nome', '?')} — {a.get('email', '')}": a["uid"] for a in lista}
        alvo = st.selectbox("Aluno", list(opcoes.keys()))
        if st.button("Redefinir senha"):
            with erros.protegido("redefinir senha do aluno"):
                senha = validacao.gerar_senha_temporaria()
                alunos.redefinir_senha(opcoes[alvo], senha, operador)
                cache.invalidar_alunos()
                st.session_state["reset_senha_resultado"] = {"alvo": alvo, "senha": senha}
        if r := st.session_state.get("reset_senha_resultado"):
            with st.container(border=True):
                st.markdown(f"**Nova senha temporária de {r['alvo']}** — anote agora:")
                st.code(r["senha"])
                st.caption("Entregue ao servidor; oriente a troca no primeiro acesso.")
                if st.button("Limpar", key="limpar_reset"):
                    st.session_state.pop("reset_senha_resultado", None)
                    st.rerun()
