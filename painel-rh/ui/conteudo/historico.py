"""Aba Histórico da página Conteúdo — versões publicadas (restauração na V8.1 F10)."""
import streamlit as st

import erros
from services import conteudo


def _fmt_ts(ts) -> str:
    try:
        return ts.astimezone().strftime("%d/%m/%Y %H:%M")
    except (AttributeError, ValueError):
        return "?"


def render(operador: str) -> None:  # noqa: ARG001 — usado na restauração (F10)
    with erros.protegido("histórico de versões", parar=True):
        versoes = conteudo.listar_historico()
    if not versoes:
        st.info(
            "O histórico passa a ser gravado a partir da **próxima publicação** "
            "(a versão atualmente no ar também será preservada nesse momento)."
        )
        return
    atual = st.session_state.get("versao_publicada")
    for v in versoes:
        marcador = " · **📡 no ar**" if v.get("versao") == atual else ""
        st.markdown(
            f"**v{v.get('versao')}**{marcador} — {v.get('resumo', '')}  \n"
            f"por {v.get('publicadoPor', '?')} em {_fmt_ts(v.get('publicadoEm'))}"
        )
        st.divider()
