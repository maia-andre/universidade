"""Aba Editor da página Conteúdo — navegação por ID (nunca por índice) e campos ligados
ao rascunho via `estado.campo_*` (autosave; nada digitado se perde ao navegar)."""
import streamlit as st

import ui
from services import conteudo
from ui.conteudo import estado, quiz


def _selecionar(rotulo: str, itens: list, key: str, container=st):
    """Selectbox que guarda o ID selecionado (índices quebram ao remover itens)."""
    ids = [i["id"] for i in itens]
    if st.session_state.get(key) not in ids:
        st.session_state.pop(key, None)  # id sumiu (removido/troca de nível) → volta ao 1º
    escolhido = container.selectbox(
        rotulo, ids, key=key,
        format_func=lambda i: f"[{i}] {next(x.get('titulo', '?') for x in itens if x['id'] == i)}",
    )
    return next(x for x in itens if x["id"] == escolhido)


def render(operador: str) -> None:
    ro = st.session_state.get("somente_leitura", False)
    cat = st.session_state.rascunho

    # ------------------------------------------------------------------- curso
    topo = st.columns([4, 1], vertical_alignment="bottom")
    if not ro and topo[1].button("➕ Novo curso", width="stretch"):
        novo_id = conteudo.proximo_curso_id(cat)
        cat.append({"id": novo_id, "titulo": f"Novo curso {novo_id}", "descricao": "",
                    "cargaHoraria": 0, "provaFinal": [], "modulos": []})
        estado.registrar_mutacao(operador)
        st.rerun()
    if not cat:
        st.warning("Nenhum curso no rascunho. Adicione o primeiro.")
        return
    curso = _selecionar("Curso", cat, "sel_curso_id", topo[0])
    cid = curso["id"]

    with st.container(border=True):
        cab = st.columns([5, 1], vertical_alignment="center")
        cab[0].subheader(f"Curso #{cid}")
        if not ro and cab[1].button("🗑 Remover curso", key=f"rm_curso_{cid}", width="stretch"):
            n_aulas = sum(len(m.get("aulas", [])) for m in curso.get("modulos", []))

            def _remover_curso(cid=cid):
                st.session_state.rascunho[:] = [
                    c for c in st.session_state.rascunho if c["id"] != cid
                ]
                estado.registrar_mutacao(operador)

            ui.confirmar_acao(
                "Remover curso",
                f"Remover o curso **[{cid}] {curso.get('titulo', '')}** e suas "
                f"**{n_aulas} aula(s)** do rascunho? Se publicar depois, o progresso dos "
                "alunos nessas aulas ficará órfão.",
                _remover_curso,
            )
        estado.campo_texto("Título", f"c_{cid}_titulo", ("curso", cid), "titulo", operador)
        estado.campo_texto("Descrição", f"c_{cid}_descricao", ("curso", cid), "descricao",
                           operador, widget=st.text_area, height=80)
        estado.campo_inteiro("Carga horária (h)", f"c_{cid}_ch", ("curso", cid),
                             "cargaHoraria", operador)
        st.divider()
        quiz.render_lista("🏁 Prova final do curso", ("curso", cid), "provaFinal", operador)

    # ------------------------------------------------------------------- módulos
    st.divider()
    modulos = curso.setdefault("modulos", [])
    mtopo = st.columns([4, 1], vertical_alignment="bottom")
    if not ro and mtopo[1].button("➕ Novo módulo", width="stretch"):
        novo_id = conteudo.proximo_modulo_id(cat)
        modulos.append({"id": novo_id, "titulo": f"Novo módulo {novo_id}",
                        "descricao": "", "aulas": []})
        estado.registrar_mutacao(operador)
        st.rerun()
    if not modulos:
        st.info("Curso sem módulos. Adicione um módulo.")
        return
    modulo = _selecionar("Módulo", modulos, "sel_modulo_id", mtopo[0])
    mid = modulo["id"]

    with st.container(border=True):
        mcab = st.columns([5, 1], vertical_alignment="center")
        mcab[0].markdown(f"**Módulo #{mid}**")
        if not ro and mcab[1].button("🗑 Remover módulo", key=f"rm_mod_{mid}", width="stretch"):
            n_aulas = len(modulo.get("aulas", []))

            def _remover_modulo(cid=cid, mid=mid):
                curso_ = estado._resolver(("curso", cid))
                if curso_ is not None:
                    curso_["modulos"] = [m for m in curso_["modulos"] if m["id"] != mid]
                estado.registrar_mutacao(operador)

            ui.confirmar_acao(
                "Remover módulo",
                f"Remover o módulo **[{mid}] {modulo.get('titulo', '')}** e suas "
                f"**{n_aulas} aula(s)** do rascunho? Se publicar depois, o progresso dos "
                "alunos nessas aulas ficará órfão.",
                _remover_modulo,
            )
        estado.campo_texto("Título do módulo", f"m_{mid}_titulo",
                           ("modulo", cid, mid), "titulo", operador)
        estado.campo_texto("Descrição do módulo", f"m_{mid}_descricao",
                           ("modulo", cid, mid), "descricao", operador,
                           widget=st.text_area, height=70)

    # ------------------------------------------------------------------- aulas
    st.divider()
    aulas = modulo.setdefault("aulas", [])
    atopo = st.columns([4, 1], vertical_alignment="bottom")
    if not ro and atopo[1].button("➕ Nova aula", width="stretch"):
        novo_id = conteudo.proximo_aula_id(cat)
        aulas.append({"id": novo_id, "titulo": f"Nova aula {novo_id}",
                      "conteudo": "", "quiz": []})
        estado.registrar_mutacao(operador)
        st.rerun()
    if not aulas:
        st.info("Módulo sem aulas. Adicione uma aula.")
        return
    aula = _selecionar("Aula", aulas, "sel_aula_id", atopo[0])
    aid = aula["id"]

    with st.container(border=True):
        acab = st.columns([5, 1], vertical_alignment="center")
        acab[0].markdown(f"**Aula #{aid}**")
        if not ro and acab[1].button("🗑 Remover aula", key=f"rm_aula_{aid}", width="stretch"):
            def _remover_aula(cid=cid, mid=mid, aid=aid):
                modulo_ = estado._resolver(("modulo", cid, mid))
                if modulo_ is not None:
                    modulo_["aulas"] = [a for a in modulo_["aulas"] if a["id"] != aid]
                estado.registrar_mutacao(operador)

            ui.confirmar_acao(
                "Remover aula",
                f"Remover a aula **[{aid}] {aula.get('titulo', '')}** do rascunho? "
                "Se publicar depois, o progresso dos alunos nela ficará órfão "
                "(reversível se a mesma aula voltar a ser publicada).",
                _remover_aula,
            )
        estado.campo_texto("Título da aula", f"a_{aid}_titulo",
                           ("aula", cid, mid, aid), "titulo", operador)
        estado.campo_texto("Conteúdo (Markdown)", f"a_{aid}_conteudo",
                           ("aula", cid, mid, aid), "conteudo", operador,
                           widget=st.text_area, height=300)
        if (aula.get("conteudo") or "").strip():
            with st.expander("👁 Pré-visualizar conteúdo (Markdown)"):
                st.markdown(aula["conteudo"])
        st.divider()
        quiz.render_lista("🧩 Quiz da aula", ("aula", cid, mid, aid), "quiz", operador)
