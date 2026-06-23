"""Página Conteúdo (V8 Item 1) — editar e publicar o catálogo de cursos para o app.

O que for publicado aqui chega ao app **sem novo APK** (o app sincroniza o doc config/conteudo).
Edição trabalha sobre um **rascunho** em sessão; só "Publicar" grava no Firestore (versão++).

⚠️ IDs de módulo/aula são imutáveis: editar muda texto/título/quiz, nunca o id (preserva o
progresso do aluno). Cursos/módulos/aulas novos recebem id de um contador que só cresce.
"""
import json

import streamlit as st

from auth_operador import require_login
from services import conteudo

operador = require_login()
st.title("📚 Conteúdo dos cursos")
st.caption(
    "Edite e **publique** o catálogo. O app atualiza no próximo login/sync, **sem novo APK**. "
    "As alterações ficam em um rascunho até você clicar em **Publicar**."
)

# --------------------------------------------------------------------------- rascunho em sessão
if "catalogo" not in st.session_state:
    try:
        dados = conteudo.carregar()
        st.session_state.catalogo = dados["cursos"]
        st.session_state.versao_publicada = dados["versao"]
    except Exception as e:  # noqa: BLE001
        st.error(f"Não foi possível carregar o conteúdo: {e}")
        st.stop()

cat = st.session_state.catalogo

col_a, col_b = st.columns([3, 1])
col_a.info(
    f"Versão publicada no app: **v{st.session_state.versao_publicada}** · "
    f"cursos no rascunho: **{len(cat)}**"
)
if col_b.button("↺ Descartar rascunho", help="Recarrega do Firestore, perdendo edições não publicadas."):
    for k in ("catalogo", "versao_publicada"):
        st.session_state.pop(k, None)
    st.rerun()

st.divider()

# --------------------------------------------------------------------------- seleção de curso
top = st.columns([4, 1])
if not cat:
    st.warning("Nenhum curso no rascunho. Adicione o primeiro.")
if top[1].button("➕ Novo curso", width="stretch"):
    novo_id = conteudo.proximo_curso_id(cat)
    cat.append({
        "id": novo_id,
        "titulo": f"Novo curso {novo_id}",
        "descricao": "",
        "cargaHoraria": 0,
        "provaFinal": [],
        "modulos": [],
    })
    st.rerun()

if not cat:
    st.stop()

ic = top[0].selectbox(
    "Curso", range(len(cat)), format_func=lambda i: f"[{cat[i]['id']}] {cat[i]['titulo']}"
)
curso = cat[ic]

with st.form("form_curso"):
    st.subheader(f"Curso #{curso['id']}")
    t = st.text_input("Título", curso["titulo"])
    d = st.text_area("Descrição", curso.get("descricao", ""), height=80)
    ch = st.number_input("Carga horária (h)", min_value=0, value=int(curso.get("cargaHoraria") or 0))
    pf_txt = st.text_area(
        "Prova final (JSON — lista de perguntas)",
        json.dumps(curso.get("provaFinal", []), ensure_ascii=False, indent=2),
        height=200,
        help='Cada item: {"pergunta": "...", "opcoes": ["a","b"], "respostaCorretaIndex": 0}',
    )
    if st.form_submit_button("Salvar curso no rascunho"):
        try:
            pf = json.loads(pf_txt)
            if not isinstance(pf, list):
                raise ValueError("A prova final precisa ser uma lista.")
            curso["titulo"] = t.strip()
            curso["descricao"] = d
            curso["cargaHoraria"] = int(ch)
            curso["provaFinal"] = pf
            st.success("Curso salvo no rascunho.")
        except (ValueError, json.JSONDecodeError) as e:
            st.error(f"Prova final inválida: {e}")

st.divider()

# --------------------------------------------------------------------------- módulos
modulos = curso.setdefault("modulos", [])
mtop = st.columns([4, 1])
mtop[0].subheader("Módulos")
if mtop[1].button("➕ Novo módulo", width="stretch"):
    novo_id = conteudo.proximo_modulo_id(cat)
    modulos.append({"id": novo_id, "titulo": f"Novo módulo {novo_id}", "descricao": "", "aulas": []})
    st.rerun()

if not modulos:
    st.info("Curso sem módulos. Adicione um módulo.")
    st.stop()

im = st.selectbox(
    "Módulo", range(len(modulos)), format_func=lambda i: f"[{modulos[i]['id']}] {modulos[i]['titulo']}"
)
modulo = modulos[im]

with st.form("form_modulo"):
    mt = st.text_input("Título do módulo", modulo["titulo"])
    md = st.text_area("Descrição do módulo", modulo.get("descricao", ""), height=70)
    cols = st.columns(2)
    if cols[0].form_submit_button("Salvar módulo no rascunho"):
        modulo["titulo"] = mt.strip()
        modulo["descricao"] = md
        st.success("Módulo salvo no rascunho.")
    if cols[1].form_submit_button("🗑 Remover módulo", help="Remove o módulo e suas aulas do rascunho."):
        modulos.pop(im)
        st.rerun()

st.divider()

# --------------------------------------------------------------------------- aulas
aulas = modulo.setdefault("aulas", [])
atop = st.columns([4, 1])
atop[0].subheader("Aulas")
if atop[1].button("➕ Nova aula", width="stretch"):
    novo_id = conteudo.proximo_aula_id(cat)
    aulas.append({"id": novo_id, "titulo": f"Nova aula {novo_id}", "conteudo": "", "quiz": []})
    st.rerun()

if not aulas:
    st.info("Módulo sem aulas. Adicione uma aula.")
    st.stop()

ia = st.selectbox(
    "Aula", range(len(aulas)), format_func=lambda i: f"[{aulas[i]['id']}] {aulas[i]['titulo']}"
)
aula = aulas[ia]

with st.form("form_aula"):
    at = st.text_input("Título da aula", aula["titulo"])
    ac = st.text_area("Conteúdo (Markdown)", aula.get("conteudo", ""), height=300)
    quiz_txt = st.text_area(
        "Quiz (JSON — lista de perguntas)",
        json.dumps(aula.get("quiz", []), ensure_ascii=False, indent=2),
        height=200,
        help='Cada item: {"pergunta": "...", "opcoes": ["a","b"], "respostaCorretaIndex": 0}',
    )
    cols = st.columns(2)
    salvar = cols[0].form_submit_button("Salvar aula no rascunho")
    remover = cols[1].form_submit_button("🗑 Remover aula")
    if salvar:
        try:
            quiz = json.loads(quiz_txt)
            if not isinstance(quiz, list):
                raise ValueError("O quiz precisa ser uma lista.")
            aula["titulo"] = at.strip()
            aula["conteudo"] = ac
            aula["quiz"] = quiz
            st.success("Aula salva no rascunho.")
        except (ValueError, json.JSONDecodeError) as e:
            st.error(f"Quiz inválido: {e}")
    if remover:
        aulas.pop(ia)
        st.rerun()

if aula.get("conteudo"):
    with st.expander("👁 Pré-visualizar conteúdo (Markdown)"):
        st.markdown(aula["conteudo"])

# --------------------------------------------------------------------------- publicar
st.divider()
st.subheader("Publicar no app")
st.caption(
    "Grava o rascunho no Firestore e incrementa a versão. Salve as seções editadas no rascunho "
    "antes de publicar."
)
if st.button("🚀 Publicar no app", type="primary"):
    try:
        nova = conteudo.publicar(cat, operador)
        st.session_state.versao_publicada = nova
        st.success(f"Publicado! Nova versão: **v{nova}**. O app atualizará no próximo login/sync.")
        st.balloons()
    except Exception as e:  # noqa: BLE001
        st.error(f"Falha ao publicar: {e}")
