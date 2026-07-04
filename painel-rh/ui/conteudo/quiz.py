"""Editor estruturado de perguntas — usado pelo quiz da aula E pela prova final do curso.

Formato do contrato: {"pergunta": str, "opcoes": [str, ...], "respostaCorretaIndex": int}.
A resposta correta é marcada por checkbox NA PRÓPRIA LINHA da opção e o índice é DERIVADO
da linha marcada — imune ao bug clássico de desalinhamento ao adicionar/remover opções.

Nota de implementação: a key do st.data_editor inclui uma assinatura do conteúdo da
pergunta. Sem isso, o delta interno do editor (ex.: linha adicionada) seria reaplicado
sobre o dataframe já sincronizado a cada rerun, duplicando linhas.
"""
import hashlib
import json

import pandas as pd
import streamlit as st

import ui
from ui.conteudo import estado


def _pergunta_por_uid(caminho_container: tuple, campo_lista: str, uid: str):
    ent = estado._resolver(caminho_container)
    if ent is None:
        return None
    return next((p for p in ent.get(campo_lista, []) if p.get("_uid") == uid), None)


def _commit_texto(caminho_container: tuple, campo_lista: str, uid: str, key: str,
                  operador: str) -> None:
    p = _pergunta_por_uid(caminho_container, campo_lista, uid)
    if p is None:
        return
    p["pergunta"] = st.session_state.get(key, "")
    estado.persistir_rascunho(operador)


def _assinatura(p: dict) -> str:
    payload = json.dumps([p.get("opcoes", []), p.get("respostaCorretaIndex")],
                         ensure_ascii=False)
    return hashlib.md5(payload.encode("utf-8")).hexdigest()[:8]


def _celula_texto(valor) -> str:
    """Célula do data_editor → str. Linha recém-adicionada vem como NaN, que é TRUTHY
    em Python (`str(nan or "")` viraria a opção literal "nan")."""
    return "" if pd.isna(valor) else str(valor).strip()


def _celula_bool(valor) -> bool:
    return (not pd.isna(valor)) and bool(valor)  # bool(NaN) é True — nunca usar direto


def _sincronizar_opcoes(p: dict, df: pd.DataFrame, operador: str) -> tuple[list, list]:
    """Escreve o retorno do data_editor no rascunho (roda todo rerun; converge porque a
    key muda junto com o conteúdo). Linhas vazias são ignoradas; o índice da correta só
    é gravado quando exatamente UMA linha está marcada (senão mantém e mostra erro)."""
    linhas = [(_celula_texto(r.get("opcao")), _celula_bool(r.get("correta")))
              for _, r in df.iterrows()]
    linhas = [(t, c) for t, c in linhas if t]
    opcoes = [t for t, _ in linhas]
    marcadas = [i for i, (_, c) in enumerate(linhas) if c]
    mudou = False
    if opcoes != p.get("opcoes"):
        p["opcoes"] = opcoes
        mudou = True
    if len(marcadas) == 1 and marcadas[0] != p.get("respostaCorretaIndex"):
        p["respostaCorretaIndex"] = marcadas[0]
        mudou = True
    if mudou:
        estado.persistir_rascunho(operador)
    return opcoes, marcadas


def render_lista(titulo: str, caminho_container: tuple, campo_lista: str,
                 operador: str) -> None:
    """Lista de perguntas de `campo_lista` ('quiz' da aula ou 'provaFinal' do curso)."""
    ro = st.session_state.get("somente_leitura", False)
    ent = estado._resolver(caminho_container)
    if ent is None:
        return
    perguntas = ent.setdefault(campo_lista, [])
    sufixo = "_".join(str(x) for x in caminho_container) + f"_{campo_lista}"

    cab = st.columns([4, 1], vertical_alignment="center")
    cab[0].markdown(f"**{titulo}** — {len(perguntas)} pergunta(s)")
    if not ro and cab[1].button("➕ Pergunta", key=f"add_{sufixo}", width="stretch"):
        perguntas.append(estado.nova_pergunta())
        estado.registrar_mutacao(operador)
        st.rerun()

    for n, p in enumerate(list(perguntas), start=1):
        uid = p.get("_uid")
        rotulo = (p.get("pergunta") or "").strip()[:60] or "(sem texto)"
        with st.expander(f"Pergunta {n}: {rotulo}"):
            key_txt = f"p_{uid}_texto"
            if key_txt not in st.session_state:
                st.session_state[key_txt] = str(p.get("pergunta") or "")
            st.text_area(
                "Pergunta", key=key_txt, height=70,
                on_change=_commit_texto,
                args=(caminho_container, campo_lista, uid, key_txt, operador),
                disabled=ro,
            )
            # dtypes explícitos: uma lista de opções VAZIA viraria float64 no pandas,
            # e o TextColumn recusa a coluna (StreamlitAPIException em runtime).
            opcoes_atuais = [str(o) for o in p.get("opcoes", [])]
            df = pd.DataFrame({
                "opcao": pd.Series(opcoes_atuais, dtype="object"),
                "correta": pd.Series(
                    [i == p.get("respostaCorretaIndex")
                     for i in range(len(opcoes_atuais))],
                    dtype="bool",
                ),
            })
            editado = st.data_editor(
                df,
                key=f"p_{uid}_ops_{_assinatura(p)}",
                num_rows="dynamic", hide_index=True, width="stretch", disabled=ro,
                column_config={
                    "opcao": st.column_config.TextColumn("Opção", required=True,
                                                         width="large"),
                    "correta": st.column_config.CheckboxColumn("Resposta correta",
                                                               default=False),
                },
            )
            if ro:
                continue
            opcoes, marcadas = _sincronizar_opcoes(p, editado, operador)
            if len(opcoes) < 2:
                st.error("A pergunta precisa de ao menos 2 opções.")
            if len(marcadas) != 1:
                st.error("Marque exatamente **uma** opção correta.")
            if st.button("🗑 Remover pergunta", key=f"p_{uid}_rm"):
                def _remover(uid=uid):
                    ent_ = estado._resolver(caminho_container)
                    if ent_ is not None:
                        ent_[campo_lista] = [q for q in ent_[campo_lista]
                                             if q.get("_uid") != uid]
                    estado.registrar_mutacao(operador)

                ui.confirmar_acao(
                    "Remover pergunta",
                    f'Remover a pergunta {n} ("{rotulo}")?',
                    _remover,
                )
