"""Aba Histórico da página Conteúdo — versões publicadas e restauração (rollback).

Restaurar = publicar o snapshot antigo como uma versão NOVA (a versão sempre avança —
o app só sincroniza versões maiores). Passa pelas mesmas validações e confirmações de
estabilidade da publicação normal."""
import streamlit as st

import erros
from services import conteudo
from ui.conteudo import publicacao


def _fmt_ts(ts) -> str:
    try:
        return ts.astimezone().strftime("%d/%m/%Y %H:%M")
    except (AttributeError, ValueError):
        return "?"


def _dialogo_restaurar(operador: str, versao: int) -> None:
    @st.dialog(f"🔄 Restaurar v{versao}", width="large")
    def _dlg():
        with erros.protegido("carregar versão do histórico", parar=True):
            snapshot = conteudo.carregar_versao_historico(versao)
            pub = conteudo.carregar_publicado()
            ha_rascunho = conteudo.carregar_rascunho() is not None

        problemas = [p for p in conteudo.validar_catalogo(snapshot)
                     if p.severidade == "erro"]
        if problemas:
            st.error("Esta versão não passa na validação atual e não pode ser restaurada:")
            for p in problemas[:10]:
                st.markdown(f"- **{p.caminho}** — {p.mensagem}")
            return

        st.caption(f"O conteúdo da **v{versao}** será publicado como "
                   f"**v{pub['versao'] + 1}** (a versão sempre avança).")
        if ha_rascunho:
            st.warning("✏️ Há um rascunho em andamento — restaurar **apaga o rascunho**.")
        diff = conteudo.diff_catalogos(pub["cursos"], snapshot)
        publicacao.render_diff(diff)
        est = conteudo.verificar_estabilidade(snapshot, pub["cursos"])
        pode = publicacao.confirmar_estabilidade(est, f"rest_confirmo_{versao}")

        c1, c2 = st.columns(2)
        if c1.button("Confirmar restauração", type="primary", disabled=not pode,
                     width="stretch"):
            publicacao._publicar_e_fechar(snapshot, operador, pub["versao"],
                                          keys_limpar=(f"rest_confirmo_{versao}",))
        if c2.button("Cancelar", width="stretch"):
            st.session_state.pop(f"rest_confirmo_{versao}", None)
            st.rerun()

    _dlg()


def render(operador: str) -> None:
    ro = st.session_state.get("somente_leitura", False)
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
        col_info, col_btn = st.columns([4, 1], vertical_alignment="center")
        marcador = " · 📡 **no ar**" if v.get("versao") == atual else ""
        col_info.markdown(
            f"**v{v.get('versao')}**{marcador} — {v.get('resumo', '')}  \n"
            f"por {v.get('publicadoPor', '?')} em {_fmt_ts(v.get('publicadoEm'))}"
        )
        if not ro and v.get("versao") != atual and col_btn.button(
            "🔄 Restaurar…", key=f"rest_{v.get('versao')}", width="stretch"
        ):
            _dialogo_restaurar(operador, int(v["versao"]))
        st.divider()
