"""Página Operadores (V8 Item 2.2) — gestão de quem acessa o painel (restrita a administradores)."""
import pandas as pd
import streamlit as st

from auth_operador import require_login
from services import operadores
from services.operadores import MIN_SENHA

operador = require_login()
st.title("🛠 Operadores do painel")

if not st.session_state.get("operador_admin"):
    st.error("Acesso restrito a administradores.")
    st.stop()

st.caption(
    "Gerencie quem acessa o painel. As senhas são guardadas com **hash PBKDF2** (nunca em texto); "
    "operadores novos e os que tiveram a senha redefinida **trocam no próximo acesso**."
)

try:
    lista = operadores.listar()
except Exception as e:  # noqa: BLE001
    st.error(str(e))
    lista = []

if lista:
    st.dataframe(pd.DataFrame(lista), width="stretch")

# --------------------------------------------------------------------------- novo operador
st.divider()
st.subheader("Novo operador")
with st.form("novo_operador"):
    novo_usuario = st.text_input("Usuário")
    nova_senha = st.text_input("Senha temporária", type="password",
                               help=f"Mínimo {MIN_SENHA} caracteres. O operador troca no 1º acesso.")
    is_admin = st.checkbox("Administrador (pode gerenciar operadores)")
    criar = st.form_submit_button("Criar operador")
if criar:
    try:
        operadores.criar_operador(novo_usuario, nova_senha, operador, admin=is_admin)
        st.success(f"Operador '{novo_usuario.strip()}' criado. Ele definirá a senha no 1º acesso.")
        st.rerun()
    except Exception as e:  # noqa: BLE001
        st.error(str(e))

# --------------------------------------------------------------------------- reset / ativar
if lista:
    st.divider()
    st.subheader("Redefinir senha / ativar")
    alvo = st.selectbox("Operador", [o["usuario"] for o in lista])
    atual = next((o for o in lista if o["usuario"] == alvo), {})
    with st.form("gerir_operador"):
        senha_reset = st.text_input("Nova senha temporária", type="password",
                                    help=f"Mínimo {MIN_SENHA} caracteres.")
        col1, col2 = st.columns(2)
        redefinir = col1.form_submit_button("Redefinir senha")
        rotulo_toggle = "Desativar" if atual.get("ativo", True) else "Ativar"
        alternar = col2.form_submit_button(rotulo_toggle)
    if redefinir:
        try:
            operadores.redefinir_senha(alvo, senha_reset, operador)
            st.success(f"Senha de '{alvo}' redefinida. Ele troca no próximo acesso.")
            st.rerun()
        except Exception as e:  # noqa: BLE001
            st.error(str(e))
    if alternar:
        try:
            operadores.definir_ativo(alvo, not atual.get("ativo", True))
            st.success(f"Operador '{alvo}' {rotulo_toggle.lower()}do.")
            st.rerun()
        except Exception as e:  # noqa: BLE001
            st.error(str(e))
