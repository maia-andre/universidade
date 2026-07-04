"""Aba Publicar da página Conteúdo — validação, diff vs versão no ar e diálogo de publicação.

O diálogo relê o publicado (pega publicação concorrente), mostra o diff agrupado, exige
confirmação explícita quando a publicação REMOVE conteúdo que está no ar (progresso órfão)
e publica com `versao_base` (concorrência otimista — aborta com mensagem amigável)."""
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


def render_diff(diff: dict) -> None:
    """Diff agrupado por nível: ➕ adicionados, ➖ removidos, ✏️ alterados."""
    if diff["total_mudancas"] == 0:
        st.info("Nenhuma diferença em relação à versão no ar.")
        return
    for chave, rotulo in (("cursos", "Cursos"), ("modulos", "Módulos"), ("aulas", "Aulas")):
        bloco = diff[chave]
        if not any(bloco.values()):
            continue
        st.markdown(f"**{rotulo}**")
        for r in bloco["adicionados"]:
            st.markdown(f"- ➕ {r}")
        for r in bloco["removidos"]:
            st.markdown(f"- ➖ ~~{r}~~")
        for alt in bloco["alterados"]:
            st.markdown(f"- ✏️ {alt['resumo']} — {', '.join(alt['mudancas'])}")


def confirmar_estabilidade(est: dict, key: str) -> bool:
    """Bloco de aviso quando a publicação remove conteúdo no ar; retorna se pode seguir."""
    if not (est["cursos_removidos"] or est["modulos_removidos"] or est["aulas_removidas"]):
        return True
    ids_aulas = ", ".join(str(i) for i, _ in est["aulas_removidas"]) or "—"
    st.error(
        "⚠️ Esta publicação **remove conteúdo que está no ar**: "
        f"{len(est['cursos_removidos'])} curso(s), {len(est['modulos_removidos'])} módulo(s), "
        f"{len(est['aulas_removidas'])} aula(s). O progresso dos alunos nas aulas removidas "
        f"(ids: {ids_aulas}) ficará órfão — reversível apenas se as mesmas aulas voltarem "
        "a ser publicadas."
    )
    return st.checkbox("Entendo e quero publicar mesmo assim", key=key)


def _publicar_e_fechar(cursos: list, operador: str, versao_base: int,
                       keys_limpar: tuple = ()) -> None:
    sucesso, nova = False, None
    with erros.protegido("publicar conteúdo"):
        nova = conteudo.publicar(cursos, operador, versao_base=versao_base)
        sucesso = True
    if sucesso:
        cache.invalidar_catalogo()
        for k in keys_limpar:
            st.session_state.pop(k, None)
        estado.resetar()
        ui.toast_ok(f"Publicado! v{nova} — o app atualiza no próximo login/sync.")
        st.rerun()


def _dialogo_publicar(operador: str) -> None:
    @st.dialog("🚀 Publicar no app", width="large")
    def _dlg():
        # Releitura FRESCA dentro do diálogo — pega publicação concorrente feita
        # depois que a página carregou.
        with erros.protegido("leitura do conteúdo publicado", parar=True):
            pub = conteudo.carregar_publicado()
        rascunho_limpo = conteudo.limpar_chaves_internas(st.session_state.rascunho)
        diff = conteudo.diff_catalogos(pub["cursos"], rascunho_limpo)
        est = conteudo.verificar_estabilidade(rascunho_limpo, pub["cursos"])

        st.caption(f"No ar: **v{pub['versao']}** → esta publicação sairá como "
                   f"**v{pub['versao'] + 1}**.")
        render_diff(diff)
        if diff["total_mudancas"] == 0:
            st.caption("Publicar sem mudanças só incrementa a versão (força re-sync no app).")
        pode = confirmar_estabilidade(est, "pub_confirmo_remocao")

        c1, c2 = st.columns(2)
        if c1.button("Confirmar publicação", type="primary", disabled=not pode,
                     width="stretch"):
            _publicar_e_fechar(st.session_state.rascunho, operador, pub["versao"],
                               keys_limpar=("pub_confirmo_remocao",))
        if c2.button("Cancelar", width="stretch"):
            st.session_state.pop("pub_confirmo_remocao", None)
            st.rerun()

    _dlg()


def render(operador: str) -> None:
    ro = st.session_state.get("somente_leitura", False)
    cat = st.session_state.rascunho
    problemas = conteudo.validar_catalogo(conteudo.limpar_chaves_internas(cat))
    bloqueado = _render_validacao(problemas)

    st.divider()
    if st.button("🚀 Publicar no app…", type="primary", disabled=bloqueado or ro):
        _dialogo_publicar(operador)
