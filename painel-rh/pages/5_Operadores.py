"""Página Operadores (V8 Item 2.2) — gestão de quem acessa o painel (restrita a administradores)."""
import pandas as pd
import streamlit as st

import erros
import ui
from auth_operador import require_admin, require_login
from services import operadores, validacao

ui.configurar_pagina("Operadores — Painel RH", "🛠")
operador = require_login()
require_admin(operador)  # re-checa no Firestore — revogação de admin vale imediatamente

ui.pagina_header(
    "Operadores do painel", "🛠",
    "As senhas são guardadas com **hash PBKDF2** (nunca em texto); operadores novos e os "
    "que tiveram a senha redefinida **trocam no próximo acesso**.",
)

with erros.protegido("lista de operadores", parar=True):
    lista = operadores.listar()

if lista:
    st.dataframe(
        pd.DataFrame(lista),
        width="stretch",
        column_config={
            "usuario": st.column_config.TextColumn("Usuário"),
            "admin": st.column_config.CheckboxColumn("Administrador"),
            "ativo": st.column_config.CheckboxColumn("Ativo"),
            "precisaTrocar": st.column_config.CheckboxColumn("Troca pendente"),
        },
    )

# --------------------------------------------------------------------------- novo operador
st.divider()
st.subheader("Novo operador")
st.caption("A senha temporária é gerada automaticamente e exibida uma única vez.")
with st.form("novo_operador", clear_on_submit=True):
    novo_usuario = st.text_input("Usuário")
    is_admin = st.checkbox("Administrador (pode gerenciar operadores)")
    criar = st.form_submit_button("Criar operador")
if criar:
    with erros.protegido("criar operador"):
        senha = validacao.gerar_senha_temporaria()
        operadores.criar_operador(novo_usuario, senha, operador, admin=is_admin)
        st.session_state["novo_operador_resultado"] = {
            "usuario": novo_usuario.strip(), "senha": senha,
        }
if r := st.session_state.get("novo_operador_resultado"):
    with st.container(border=True):
        st.markdown(f"✅ **Operador `{r['usuario']}` criado.** Senha temporária — **anote agora**:")
        st.code(r["senha"])
        st.caption("Ele define a própria senha no primeiro acesso.")
        if st.button("Limpar", key="limpar_novo_op"):
            st.session_state.pop("novo_operador_resultado", None)
            st.rerun()

# --------------------------------------------------------------------------- reset / ativar
if lista:
    st.divider()
    st.subheader("Redefinir senha / ativar / desativar")
    alvo = st.selectbox("Operador", [o["usuario"] for o in lista])
    atual = next((o for o in lista if o["usuario"] == alvo), {})
    c1, c2 = st.columns(2)

    if c1.button("🔑 Redefinir senha", width="stretch"):
        def _redefinir(alvo=alvo):
            senha = validacao.gerar_senha_temporaria()
            operadores.redefinir_senha(alvo, senha, operador)
            return senha

        ui.confirmar_acao(
            "Redefinir senha do operador",
            f"Gerar nova senha temporária para **{alvo}**? A senha atual deixará de valer "
            "e ele trocará no próximo acesso.",
            _redefinir,
            render_resultado=lambda s: (
                st.markdown(f"Nova senha temporária de **{alvo}** — anote agora:"),
                st.code(s),
            ),
        )

    if atual.get("ativo", True):
        if c2.button("🚫 Desativar", width="stretch"):
            def _desativar(alvo=alvo):
                operadores.definir_ativo(alvo, False, operador)

            ui.confirmar_acao(
                "Desativar operador",
                f"**{alvo}** perderá o acesso ao painel imediatamente.",
                _desativar,
            )
    elif c2.button("✅ Ativar", width="stretch"):
        with erros.protegido("ativar operador"):
            operadores.definir_ativo(alvo, True, operador)
            ui.toast_ok(f"Operador '{alvo}' reativado.")
            st.rerun()
