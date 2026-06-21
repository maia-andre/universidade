"""Login simples do operador do RH — controle de acesso ao painel + auditoria.

O nome do operador é carimbado em `criadoPor`/`liberadoPor` nas ações (rastreabilidade).
Para o scaffold é uma senha compartilhada; per-operador real pode vir depois.
"""
import streamlit as st

from config import PANEL_PASSWORD


def require_login() -> str:
    """Bloqueia a página até o operador se identificar. Retorna o nome do operador."""
    if st.session_state.get("operador"):
        with st.sidebar:
            st.caption(f"Operador: **{st.session_state['operador']}**")
            if st.button("Sair", use_container_width=True):
                st.session_state.clear()
                st.rerun()
        return st.session_state["operador"]

    st.title("🔐 Painel RH — Universidade do Servidor")
    st.caption("Acesso restrito à equipe de treinamento.")
    with st.form("login"):
        nome = st.text_input("Seu nome (fica registrado nas ações)")
        senha = st.text_input("Senha do painel", type="password")
        entrar = st.form_submit_button("Entrar")
    if entrar:
        if senha == PANEL_PASSWORD and nome.strip():
            st.session_state["operador"] = nome.strip()
            st.rerun()
        else:
            st.error("Nome ou senha inválidos.")
    st.stop()
