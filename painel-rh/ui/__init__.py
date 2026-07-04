"""Helpers visuais compartilhados do painel — identidade SJC (azul #003882 / dourado #FFD700).

Regra do diálogo de confirmação: o gatilho deve ser um `st.button` comum, FORA de
`st.form` (um `form_submit_button` dispara rerun do form e o diálogo fecharia sozinho).
"""
import os
from collections.abc import Callable

import streamlit as st

import erros
from services import cache

AZUL = "#003882"
DOURADO = "#FFD700"

_ASSETS = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "assets"))
BRASAO = os.path.join(_ASSETS, "brasao.png")             # brasão puro (RGBA)
BRASAO_BADGE = os.path.join(_ASSETS, "brasao_badge.png")  # brasão sobre badge branco
                                                          # arredondado — p/ a sidebar azul
LOGO = os.path.join(_ASSETS, "logo_uniservidor.jpeg")    # fundo branco — usar só sobre fundo claro

# CSS mínimo e contido: só o filete dourado sob os títulos (o resto vem do tema no config.toml).
_CSS = f"""
<style>
h1 {{ border-bottom: 3px solid {DOURADO}; padding-bottom: 0.25em; }}
</style>
"""


def configurar_pagina(titulo: str, icone: str = "🎓") -> None:
    """PRIMEIRA chamada de toda página: set_page_config + brasão na sidebar + CSS."""
    st.set_page_config(page_title=titulo, page_icon=icone, layout="wide")
    if os.path.exists(BRASAO_BADGE):
        st.logo(BRASAO_BADGE, size="large")
    st.markdown(_CSS, unsafe_allow_html=True)


def pagina_header(titulo: str, icone: str = "", subtitulo: str | None = None,
                  botao_atualizar: bool = True) -> None:
    """Cabeçalho padrão com botão "Atualizar dados" (limpa todo o cache)."""
    col_t, col_b = st.columns([5, 1], vertical_alignment="center")
    with col_t:
        st.title(f"{icone} {titulo}".strip())
        if subtitulo:
            st.caption(subtitulo)
    if botao_atualizar and col_b.button(
        "↻ Atualizar dados", help="Recarrega as informações do Firebase.", width="stretch"
    ):
        cache.invalidar_tudo()
        st.rerun()


def toast_ok(msg: str) -> None:
    """Feedback de sucesso que sobrevive a `st.rerun()` (st.success não sobrevive)."""
    st.toast(msg, icon="✅")


def aviso_truncamento(qtd: int, limite: int) -> None:
    """Alerta quando a listagem bateu no teto — sem isso o corte seria silencioso."""
    if qtd >= limite:
        st.caption(f"⚠️ Exibindo apenas os primeiros {limite} registros.")


def confirmar_acao(
    titulo: str,
    mensagem: str,
    acao: Callable[[], object],
    rotulo: str = "Confirmar",
    render_resultado: Callable[[object], None] | None = None,
) -> None:
    """Abre um diálogo de confirmação; `acao` roda SÓ no clique de Confirmar.

    Interações dentro do diálogo re-executam apenas o diálogo (semântica de fragment
    do `st.dialog`), por isso o padrão `if st.button(...): confirmar_acao(...)` funciona.
    Com `render_resultado`, o resultado da ação (ex.: senha gerada) é exibido no próprio
    diálogo até o operador clicar em "Fechar" — exibição única garantida, sem se perder
    em rerun. A `acao` deve incluir a invalidação de cache correspondente.
    """
    chave = f"_dlg_resultado_{titulo}"
    # Limpa resultado de um diálogo anterior abandonado (fechado no X com resultado na tela).
    st.session_state.pop(chave, None)

    @st.dialog(titulo)
    def _dlg():
        if chave not in st.session_state:
            st.warning(mensagem)
            c1, c2 = st.columns(2)
            if c1.button(rotulo, type="primary", key=f"_dlg_ok_{titulo}", width="stretch"):
                resultado, sucesso = None, False
                with erros.protegido(titulo):
                    resultado = acao()
                    sucesso = True
                if sucesso:
                    if render_resultado is None:
                        st.rerun()  # fecha o diálogo e atualiza a página
                    st.session_state[chave] = resultado
                    st.rerun(scope="fragment")
            if c2.button("Cancelar", key=f"_dlg_no_{titulo}", width="stretch"):
                st.rerun()
        else:
            render_resultado(st.session_state[chave])
            if st.button("Fechar", type="primary", key=f"_dlg_fim_{titulo}", width="stretch"):
                st.session_state.pop(chave, None)
                st.rerun()

    _dlg()
