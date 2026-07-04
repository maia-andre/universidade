"""Aba Publicar da página Conteúdo — validação sempre visível + publicação transacional."""
import streamlit as st

import erros
import ui
from services import cache, conteudo
from ui.conteudo import estado


def _render_validacao(problemas: list) -> bool:
    """Mostra erros/avisos. Retorna True se há erro (publicação bloqueada)."""
    erros_ = [p for p in problemas if p.severidade == "erro"]
    avisos = [p for p in problemas if p.severidade == "aviso"]
    if not erros_ and not avisos:
        st.success("✅ Catálogo válido — pronto para publicar.")
    if erros_:
        st.error(f"❌ {len(erros_)} erro(s) bloqueiam a publicação:")
        for p in erros_:
            st.markdown(f"- **{p.caminho}** — {p.mensagem}")
    if avisos:
        with st.expander(f"⚠️ {len(avisos)} aviso(s) — não bloqueiam"):
            for p in avisos:
                st.markdown(f"- **{p.caminho}** — {p.mensagem}")
    return bool(erros_)


def render(operador: str) -> None:
    ro = st.session_state.get("somente_leitura", False)
    cat = st.session_state.rascunho
    problemas = conteudo.validar_catalogo(conteudo.limpar_chaves_internas(cat))
    bloqueado = _render_validacao(problemas)

    st.divider()
    if st.button("🚀 Publicar no app", type="primary", disabled=bloqueado or ro):
        with erros.protegido("publicar conteúdo"):
            nova = conteudo.publicar(cat, operador)
            cache.invalidar_catalogo()
            estado.resetar()
            ui.toast_ok(f"Publicado! Versão v{nova} — o app atualiza no próximo login/sync.")
            st.rerun()
