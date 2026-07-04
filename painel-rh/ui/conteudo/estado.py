"""Estado do rascunho da página Conteúdo — o mecanismo que garante que NADA digitado se perde.

Como funciona (modelo de execução do Streamlit):
- Cada widget de texto usa uma key derivada do id da entidade (`a_101_titulo`) e um
  `on_change` que grava o valor direto no rascunho. O blur/Enter que precede qualquer
  clique (inclusive trocar de curso/módulo/aula no selectbox) commita ANTES do rerun.
- O rascunho vive em `st.session_state["rascunho"]` e é persistido write-through no
  Firestore (`rascunhos/conteudo`, com throttle) — sobrevive a fechar o navegador.
- Mutações estruturais (adicionar/remover) chamam [registrar_mutacao] (persiste + purga
  as keys de widget) e então `st.rerun()`.
- Perguntas de quiz/prova ganham um `_uid` efêmero para as keys de widget (índices
  quebrariam ao remover do meio); o `_uid` é removido em toda serialização.
"""
import json
import time
import uuid

import streamlit as st

from services import conteudo

# Keys de CAMPO (reseedam do rascunho sem perda) vs keys de NAVEGAÇÃO (sel_*).
# Mutação estrutural purga só os campos — purgar sel_* jogava o operador de volta
# ao primeiro curso a cada "➕ Pergunta"/"➕ Aula" (bug do curso 5 → curso 1).
_PREFIXOS_CAMPOS = ("c_", "m_", "a_", "p_")
_PREFIXOS = _PREFIXOS_CAMPOS + ("sel_",)
_THROTTLE_SEG = 2.0

_CHAVES_SESSAO = (
    "rascunho", "versao_publicada", "base_versao", "rascunho_meta", "somente_leitura",
    "hash_salvo", "ultimo_save", "save_pendente",
)


# --------------------------------------------------------------------------- ciclo de vida

def inicializar(operador: str) -> None:
    """Carrega o estado uma vez por sessão: rascunho remoto se existir, senão o publicado."""
    if "rascunho" in st.session_state:
        return
    pub = conteudo.carregar_publicado()
    st.session_state.versao_publicada = pub["versao"]
    rascunho = conteudo.carregar_rascunho()
    if rascunho is not None:
        st.session_state.rascunho = _anexar_uids(rascunho["cursos"])
        st.session_state.base_versao = rascunho["baseVersao"]
        st.session_state.rascunho_meta = {
            "editadoPor": rascunho["editadoPor"],
            "iniciadoEm": rascunho["iniciadoEm"],
            "atualizadoEm": rascunho["atualizadoEm"],
        }
        # Rascunho de outro operador: abre em modo visualização até "Assumir rascunho".
        st.session_state.somente_leitura = rascunho["editadoPor"] != operador
    else:
        st.session_state.rascunho = _anexar_uids(pub["cursos"])
        st.session_state.base_versao = pub["versao"]
        st.session_state.rascunho_meta = None
        st.session_state.somente_leitura = False
    st.session_state.hash_salvo = _hash_atual()
    st.session_state.ultimo_save = 0.0
    st.session_state.save_pendente = False


def resetar() -> None:
    """Esquece o estado da sessão (recarrega do Firestore no próximo run)."""
    purgar_keys()
    for k in _CHAVES_SESSAO:
        st.session_state.pop(k, None)


def descartar(operador: str) -> None:  # noqa: ARG001 — assinatura uniforme para o diálogo
    """Apaga o rascunho remoto e reseta a sessão (volta ao publicado)."""
    conteudo.apagar_rascunho()
    resetar()


# --------------------------------------------------------------------------- persistência

def _hash_atual() -> int:
    return hash(json.dumps(
        conteudo.limpar_chaves_internas(st.session_state.rascunho),
        ensure_ascii=False, sort_keys=True,
    ))


def persistir_rascunho(operador: str, forcar: bool = False) -> None:
    """Write-through com throttle: grava só se mudou; adia (save_pendente) se salvou há <2s.
    Chamar também no topo da página (flush do save adiado)."""
    if st.session_state.get("somente_leitura") or "rascunho" not in st.session_state:
        return
    h = _hash_atual()
    if h == st.session_state.get("hash_salvo") and not st.session_state.get("save_pendente"):
        return
    agora = time.time()
    if not forcar and agora - st.session_state.get("ultimo_save", 0.0) < _THROTTLE_SEG:
        st.session_state.save_pendente = True
        return
    conteudo.salvar_rascunho(
        st.session_state.rascunho, operador, st.session_state.base_versao
    )
    st.session_state.hash_salvo = h
    st.session_state.ultimo_save = agora
    st.session_state.save_pendente = False
    meta = st.session_state.get("rascunho_meta") or {"iniciadoEm": None, "atualizadoEm": None}
    meta["editadoPor"] = operador
    st.session_state.rascunho_meta = meta


def registrar_mutacao(operador: str) -> None:
    """Após mutação estrutural: persiste (forçado) + purga as keys de CAMPO (os ids podem
    ser reutilizados — ex.: remover a aula de maior id e criar outra — e a key antiga
    ressuscitaria o valor velho). A navegação (sel_*) é preservada.
    NÃO faz rerun — o chamador decide (botão → st.rerun; diálogo → confirmar_acao)."""
    persistir_rascunho(operador, forcar=True)
    purgar_keys(_PREFIXOS_CAMPOS)


def purgar_keys(prefixos: tuple = _PREFIXOS) -> None:
    """Limpa as keys de widget do editor — obrigatório após mutação estrutural, descarte
    ou restauração (senão widgets ressuscitam valores de entidades que mudaram)."""
    for k in [k for k in st.session_state
              if isinstance(k, str) and k.startswith(prefixos)]:
        del st.session_state[k]


# --------------------------------------------------------------------------- widgets ligados

def _resolver(caminho: tuple):
    """Localiza a entidade POR ID no rascunho no momento do callback (nunca guardar a
    referência do dict — ela fica órfã após descartar/restaurar)."""
    tipo, *ids = caminho
    curso = next((c for c in st.session_state.rascunho if c.get("id") == ids[0]), None)
    if tipo == "curso" or curso is None:
        return curso
    modulo = next((m for m in curso.get("modulos", []) if m.get("id") == ids[1]), None)
    if tipo == "modulo" or modulo is None:
        return modulo
    return next((a for a in modulo.get("aulas", []) if a.get("id") == ids[2]), None)


def _commit(caminho: tuple, campo: str, key: str, operador: str, cast) -> None:
    entidade = _resolver(caminho)
    if entidade is None:
        return
    valor = st.session_state.get(key)
    if cast is not None:
        try:
            valor = cast(valor)
        except (TypeError, ValueError):
            return
    entidade[campo] = valor
    persistir_rascunho(operador)


def campo_texto(rotulo: str, key: str, caminho: tuple, campo: str, operador: str,
                widget=st.text_input, **kw) -> None:
    """Widget de texto ligado ao rascunho: seed na 1ª renderização, commit no on_change."""
    entidade = _resolver(caminho)
    if entidade is None:
        return
    if key not in st.session_state:
        st.session_state[key] = str(entidade.get(campo) or "")
    widget(rotulo, key=key, on_change=_commit, args=(caminho, campo, key, operador, None),
           disabled=st.session_state.get("somente_leitura", False), **kw)


def campo_inteiro(rotulo: str, key: str, caminho: tuple, campo: str, operador: str,
                  **kw) -> None:
    entidade = _resolver(caminho)
    if entidade is None:
        return
    if key not in st.session_state:
        st.session_state[key] = int(entidade.get(campo) or 0)
    st.number_input(rotulo, key=key, min_value=0, step=1,
                    on_change=_commit, args=(caminho, campo, key, operador, int),
                    disabled=st.session_state.get("somente_leitura", False), **kw)


# --------------------------------------------------------------------------- uids efêmeros

def _anexar_uids(cursos: list) -> list:
    for c in cursos:
        for p in c.get("provaFinal", []) or []:
            p.setdefault("_uid", uuid.uuid4().hex[:8])
        for m in c.get("modulos", []) or []:
            for a in m.get("aulas", []) or []:
                for p in a.get("quiz", []) or []:
                    p.setdefault("_uid", uuid.uuid4().hex[:8])
    return cursos


def nova_pergunta() -> dict:
    return {"pergunta": "", "opcoes": ["", ""], "respostaCorretaIndex": 0,
            "_uid": uuid.uuid4().hex[:8]}


# --------------------------------------------------------------------------- banners e status

def _fmt_ts(ts) -> str:
    try:
        return ts.astimezone().strftime("%d/%m %H:%M")
    except (AttributeError, ValueError):
        return "?"


def renderizar_banners(operador: str) -> None:
    meta = st.session_state.get("rascunho_meta")
    if st.session_state.get("somente_leitura") and meta and meta["editadoPor"] != operador:
        st.warning(
            f"✏️ Há um rascunho em andamento de **{meta['editadoPor']}** "
            f"(desde {_fmt_ts(meta['iniciadoEm'])}, último save {_fmt_ts(meta['atualizadoEm'])}). "
            "Você está **só visualizando** — assumir o rascunho passa a edição para você."
        )
        if st.button("✋ Assumir rascunho"):
            conteudo.assumir_rascunho(operador)
            meta["editadoPor"] = operador
            st.session_state.somente_leitura = False
            st.rerun()
    if st.session_state.base_versao < st.session_state.versao_publicada:
        st.warning(
            f"📡 O catálogo publicado avançou para **v{st.session_state.versao_publicada}** "
            f"enquanto este rascunho (base v{st.session_state.base_versao}) existia — "
            "o diff antes de publicar é calculado contra a versão atual no ar."
        )


def renderizar_status(operador: str) -> None:
    ultimo = st.session_state.get("ultimo_save", 0.0)
    salvo = (time.strftime("%H:%M:%S", time.localtime(ultimo)) if ultimo
             else "sem alterações nesta sessão")
    col_info, col_btn = st.columns([5, 1], vertical_alignment="center")
    col_info.caption(
        f"📡 No ar: **v{st.session_state.versao_publicada}** · "
        f"rascunho: base v{st.session_state.base_versao} · 💾 salvo: {salvo}"
    )
    if not st.session_state.get("somente_leitura") and col_btn.button(
        "↺ Descartar rascunho", help="Apaga TODAS as edições não publicadas.", width="stretch"
    ):
        import ui
        ui.confirmar_acao(
            "Descartar rascunho",
            "Descartar **todas** as edições não publicadas e voltar ao conteúdo no ar?",
            lambda: descartar(operador),
        )
