"""Login do operador do RH — autenticação real + auditoria (V8 Item 2.2).

As credenciais ficam em `operadores/{usuario}` (senha em hash PBKDF2, ver `services/operadores.py`),
não mais em texto puro. Fluxo: login → troca obrigatória no 1º acesso → painel. O nome do operador
é carimbado em `criadoPor`/`liberadoPor`/`publicadoPor` nas ações (rastreabilidade).
"""
from __future__ import annotations

import streamlit as st

from services import operadores
from services.operadores import MIN_SENHA


def _form_nova_senha(form_id: str, titulo: str) -> tuple[bool, str]:
    """Renderiza um formulário de nova senha (com confirmação). Retorna (válido, nova_senha)."""
    with st.form(form_id):
        if titulo:
            st.markdown(f"**{titulo}**")
        nova = st.text_input("Nova senha", type="password")
        conf = st.text_input("Confirmar nova senha", type="password")
        ok = st.form_submit_button("Salvar nova senha")
    if not ok:
        return False, ""
    if len(nova) < MIN_SENHA:
        st.error(f"A senha precisa de ao menos {MIN_SENHA} caracteres.")
        return False, ""
    if nova != conf:
        st.error("As senhas não conferem.")
        return False, ""
    return True, nova


def require_login() -> str:
    """Bloqueia a página até o operador se autenticar. Retorna o usuário do operador."""
    # 1º acesso (ou após reset): troca de senha obrigatória antes de liberar o painel.
    pendente = st.session_state.get("operador_pendente")
    if pendente and not st.session_state.get("operador"):
        st.title("🔑 Primeiro acesso — defina sua senha")
        st.caption("Por segurança, escolha uma nova senha para continuar.")
        valido, nova = _form_nova_senha("trocar_forcado", "")
        if valido:
            try:
                operadores.trocar_senha(pendente, nova)
                st.session_state["operador"] = pendente
                st.session_state.pop("operador_pendente", None)
                st.rerun()
            except Exception as e:  # noqa: BLE001
                st.error(f"Falha ao trocar a senha: {e}")
        st.stop()

    # Já autenticado: barra lateral com troca voluntária de senha e sair.
    if st.session_state.get("operador"):
        usuario = st.session_state["operador"]
        with st.sidebar:
            st.caption(f"Operador: **{usuario}**")
            with st.expander("🔑 Trocar minha senha"):
                valido, nova = _form_nova_senha("trocar_voluntario", "")
                if valido:
                    try:
                        operadores.trocar_senha(usuario, nova)
                        st.success("Senha atualizada.")
                    except Exception as e:  # noqa: BLE001
                        st.error(str(e))
            if st.button("Sair", width="stretch"):
                st.session_state.clear()
                st.rerun()
        return usuario

    # Não autenticado: tela de login.
    st.title("🔐 Painel RH — Universidade do Servidor")
    st.caption("Acesso restrito à equipe de treinamento.")
    with st.form("login"):
        usuario = st.text_input("Usuário")
        senha = st.text_input("Senha", type="password")
        entrar = st.form_submit_button("Entrar")
    if entrar:
        try:
            res = operadores.autenticar(usuario.strip(), senha)
        except Exception as e:  # noqa: BLE001
            st.error(f"Erro ao autenticar: {e}")
            st.stop()
        if res is None:
            st.error("Usuário ou senha inválidos.")
        else:
            st.session_state["operador_admin"] = res["admin"]
            if res["precisaTrocar"]:
                st.session_state["operador_pendente"] = res["usuario"]
            else:
                st.session_state["operador"] = res["usuario"]
            st.rerun()
    st.stop()
