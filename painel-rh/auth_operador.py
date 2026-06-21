"""Login simples do operador do RH — controle de acesso ao painel + auditoria.

O nome do operador é carimbado em `criadoPor`/`liberadoPor` nas ações (rastreabilidade).
Para o scaffold é uma senha compartilhada; per-operador real pode vir depois.
"""
import streamlit as st

from config import OPERADORES


def require_login() -> str:
    """Bloqueia a página até o operador se autenticar. Retorna o usuário do operador."""
    if st.session_state.get("operador"):
        with st.sidebar:
            st.caption(f"Operador: **{st.session_state['operador']}**")
            if st.button("Sair", width="stretch"):
                st.session_state.clear()
                st.rerun()
        return st.session_state["operador"]

    st.title("🔐 Painel RH — Universidade do Servidor")
    st.caption("Acesso restrito à equipe de treinamento.")
    with st.form("login"):
        usuario = st.text_input("Usuário")
        senha = st.text_input("Senha", type="password")
        entrar = st.form_submit_button("Entrar")
    if entrar:
        usuario = usuario.strip()
        if usuario and OPERADORES.get(usuario) == senha:
            st.session_state["operador"] = usuario  # carimbado em criadoPor/liberadoPor
            st.rerun()
        else:
            st.error("Usuário ou senha inválidos.")
    st.stop()
