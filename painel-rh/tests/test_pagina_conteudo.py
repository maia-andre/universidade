"""Testes de UI da página Conteúdo (streamlit.testing.AppTest) — sem tocar o Firestore.

Regressões cobertas (relatadas pelo operador em 04/07/2026):
1. Adicionar pergunta na prova final jogava a tela de volta ao curso 1 (purge de sel_*).
2. O dropdown de aulas não acompanhava a troca de módulo (key compartilhada).
"""
import copy
import os
import sys

import pytest
from streamlit.testing.v1 import AppTest

_RAIZ = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, _RAIZ)

from services import conteudo  # noqa: E402


def _pergunta(texto="Qual?"):
    return {"pergunta": texto, "opcoes": ["a", "b"], "respostaCorretaIndex": 0}


CATALOGO = [
    {
        "id": 1, "titulo": "Curso Um", "descricao": "d", "cargaHoraria": 8,
        # A 2ª pergunta reproduz o rascunho real que quebrava a página: opcoes []
        # (lista vazia vira coluna float64 no pandas → TextColumn estourava).
        "provaFinal": [_pergunta(),
                       {"pergunta": "Sem opções", "opcoes": [], "respostaCorretaIndex": 0}],
        "modulos": [
            {"id": 100, "titulo": "Módulo 100", "descricao": "", "aulas": [
                {"id": 101, "titulo": "Aula 101", "conteudo": "c", "quiz": []},
                {"id": 102, "titulo": "Aula 102", "conteudo": "c", "quiz": []},
            ]},
            {"id": 200, "titulo": "Módulo 200", "descricao": "", "aulas": [
                {"id": 201, "titulo": "Aula 201", "conteudo": "c", "quiz": []},
            ]},
        ],
    },
    {
        "id": 2, "titulo": "Curso Dois", "descricao": "d", "cargaHoraria": 4,
        "provaFinal": [],
        "modulos": [
            {"id": 2000, "titulo": "Módulo 2000", "descricao": "", "aulas": [
                {"id": 2001, "titulo": "Aula 2001", "conteudo": "c", "quiz": []},
            ]},
        ],
    },
]


@pytest.fixture
def at(monkeypatch):
    """Página logada com os services de conteúdo substituídos por fakes em memória."""
    salvos = []
    monkeypatch.setattr(conteudo, "carregar_publicado",
                        lambda: {"versao": 1, "cursos": copy.deepcopy(CATALOGO)})
    monkeypatch.setattr(conteudo, "carregar", conteudo.carregar_publicado)
    monkeypatch.setattr(conteudo, "carregar_rascunho", lambda: None)
    monkeypatch.setattr(conteudo, "salvar_rascunho",
                        lambda cursos, op, base: salvos.append(copy.deepcopy(cursos)))
    monkeypatch.setattr(conteudo, "apagar_rascunho", lambda: None)
    monkeypatch.setattr(conteudo, "assumir_rascunho", lambda op: None)
    monkeypatch.setattr(conteudo, "listar_historico", lambda limite=20: [])

    at = AppTest.from_file(os.path.join(_RAIZ, "pages", "4_Conteudo.py"),
                           default_timeout=15)
    at.session_state["operador"] = "tester"
    at.session_state["operador_admin"] = True
    at.run()
    assert not at.exception, at.exception
    at._salvos = salvos  # inspecionável nos testes
    return at


def _sel(at, key):
    caixa = next((s for s in at.selectbox if s.key == key), None)
    assert caixa is not None, f"selectbox {key} não encontrado " \
                              f"(existem: {[s.key for s in at.selectbox]})"
    return caixa


def _botao(at, key):
    b = next((b for b in at.button if b.key == key), None)
    assert b is not None, f"botão {key} não encontrado"
    return b


# ------------------------------------------------------------------ regressão bug 1

def test_pergunta_sem_opcoes_nao_quebra_a_pagina(at):
    """Regressão: StreamlitAPIException (TextColumn × coluna FLOAT) com opcoes == []."""
    assert not at.exception  # a fixture já renderizou o curso 1, que contém o caso


def test_linha_nao_preenchida_do_data_editor_nao_vira_opcao_nan():
    """NaN é truthy em Python — linha adicionada e não digitada virava a opção 'nan'."""
    import numpy as np
    import pandas as pd

    from ui.conteudo import quiz

    p = {"pergunta": "Q?", "opcoes": ["a"], "respostaCorretaIndex": 0, "_uid": "t1"}
    df = pd.DataFrame({"opcao": ["a", np.nan], "correta": [True, np.nan]})
    opcoes, marcadas = quiz._sincronizar_opcoes(p, df, "tester")
    assert opcoes == ["a"], f"linha NaN virou opção: {opcoes}"
    assert marcadas == [0]
    assert p["opcoes"] == ["a"]


def test_adicionar_pergunta_na_prova_mantem_o_curso_selecionado(at):
    # vai para o curso 2 e adiciona pergunta na prova final
    _sel(at, "sel_curso").select(2).run()
    assert not at.exception
    _botao(at, "add_curso_2_provaFinal").click().run()
    assert not at.exception

    # ANTES do fix: purge de sel_* devolvia a tela ao curso 1
    assert _sel(at, "sel_curso").value == 2, "a tela voltou para o primeiro curso!"
    curso2 = next(c for c in at.session_state["rascunho"] if c["id"] == 2)
    assert len(curso2["provaFinal"]) == 1, "a pergunta não entrou no curso selecionado"


def test_adicionar_aula_mantem_navegacao_e_seleciona_a_nova(at):
    _sel(at, "sel_curso").select(2).run()
    # o botão "Nova aula" não tem key própria: localiza pelo rótulo
    alvo = next(b for b in at.button if "Nova aula" in str(b.label))
    alvo.click().run()
    assert not at.exception
    assert _sel(at, "sel_curso").value == 2
    curso2 = next(c for c in at.session_state["rascunho"] if c["id"] == 2)
    nova_id = curso2["modulos"][0]["aulas"][-1]["id"]
    assert _sel(at, "sel_aula_2_2000").value == nova_id, \
        "a aula recém-criada deveria ficar selecionada"


# ------------------------------------------------------------------ regressão bug 2

def test_trocar_de_modulo_atualiza_o_dropdown_de_aulas(at):
    # módulo 100 selecionado por padrão → aula 101
    assert _sel(at, "sel_modulo_1").value == 100
    assert _sel(at, "sel_aula_1_100").value == 101

    # troca para o módulo 200 → o dropdown de aulas deve mostrar as aulas do 200
    _sel(at, "sel_modulo_1").select(200).run()
    assert not at.exception
    caixa_aulas = _sel(at, "sel_aula_1_200")
    assert caixa_aulas.options == ["[201] Aula 201"], \
        f"dropdown de aulas não acompanhou o módulo: {caixa_aulas.options}"
    assert caixa_aulas.value == 201

    # volta ao módulo 100 → aulas do 100 de novo
    _sel(at, "sel_modulo_1").select(100).run()
    assert int(_sel(at, "sel_aula_1_100").value) in (101, 102)


def test_trocar_de_curso_nao_arrasta_selecao_de_modulo(at):
    _sel(at, "sel_modulo_1").select(200).run()
    _sel(at, "sel_curso").select(2).run()
    assert not at.exception
    assert _sel(at, "sel_modulo_2").value == 2000
    assert int(_sel(at, "sel_aula_2_2000").value) == 2001


# ------------------------------------------------------------------ edição sem perda

def test_edicao_de_titulo_sobrevive_a_navegacao(at):
    campo = next(t for t in at.text_input if t.key == "a_101_titulo")
    campo.set_value("Aula 101 renomeada").run()
    assert not at.exception

    # navega para outro módulo e volta
    _sel(at, "sel_modulo_1").select(200).run()
    _sel(at, "sel_modulo_1").select(100).run()

    curso1 = next(c for c in at.session_state["rascunho"] if c["id"] == 1)
    aula = curso1["modulos"][0]["aulas"][0]
    assert aula["titulo"] == "Aula 101 renomeada", "edição se perdeu na navegação!"
    campo = next(t for t in at.text_input if t.key == "a_101_titulo")
    assert campo.value == "Aula 101 renomeada"
    # e o autosave gravou o rascunho (fake registrou ao menos 1 save)
    assert at._salvos, "autosave não gravou o rascunho"
