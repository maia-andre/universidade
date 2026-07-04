"""Testes de UI da página Relatórios (AppTest, cache mockado — sem Firestore)."""
import os
import sys

import pytest
from streamlit.testing.v1 import AppTest

_RAIZ = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, _RAIZ)

from services import cache  # noqa: E402

_ALUNOS = [
    {"uid": "u1", "nome": "Ana Silva", "email": "ana@sjc.br"},
    {"uid": "u2", "nome": "Beto Souza", "email": "beto@sjc.br"},
]
_SITUACAO = [
    {"aluno": "Ana Silva", "email": "ana@sjc.br", "curso": "Curso Um",
     "matrícula": "ativa", "concluído": True},
    {"aluno": "Beto Souza", "email": "beto@sjc.br", "curso": "Curso Dois",
     "matrícula": "encerrada", "concluído": False},
]
_CONCLUSOES = [
    {"id": "u1_1", "uid": "u1", "cursoId": 1, "nota": 90, "certificadoId": "CERT-1"},
    {"id": "u2_2", "uid": "u2", "cursoId": 2, "nota": 80, "certificadoId": "CERT-2"},
]


@pytest.fixture
def at(monkeypatch):
    monkeypatch.setattr(cache, "resumo_kpis",
                        lambda: {"matriculas": 2, "conclusoes": 2, "taxa": 100.0})
    monkeypatch.setattr(cache, "situacao_alunos", lambda: list(_SITUACAO))
    monkeypatch.setattr(cache, "conclusoes_lista", lambda: list(_CONCLUSOES))
    monkeypatch.setattr(cache, "alunos_lista", lambda: list(_ALUNOS))
    monkeypatch.setattr(cache, "catalogo_cursos",
                        lambda: {1: "Curso Um", 2: "Curso Dois"})
    at = AppTest.from_file(os.path.join(_RAIZ, "pages", "3_Relatorios.py"),
                           default_timeout=15)
    at.session_state["operador"] = "tester"
    at.run()
    assert not at.exception, at.exception
    return at


def test_conclusoes_mostram_nome_e_curso_no_lugar_de_uid(at):
    df = at.dataframe[1].value  # 0 = situação, 1 = conclusões
    assert set(df["aluno"]) == {"Ana Silva", "Beto Souza"}
    assert set(df["curso"]) == {"Curso Um", "Curso Dois"}


def test_filtro_por_aluno_estreita_as_duas_tabelas(at):
    campo = next(t for t in at.text_input if "Buscar aluno" in str(t.label))
    campo.set_value("ana").run()
    assert not at.exception
    situacao, conclusoes = at.dataframe[0].value, at.dataframe[1].value
    assert list(situacao["aluno"]) == ["Ana Silva"]
    assert list(conclusoes["aluno"]) == ["Ana Silva"]

    # filtro por e-mail também funciona; sem resultado → tabelas vazias sem erro
    campo.set_value("beto@sjc").run()
    assert list(at.dataframe[1].value["aluno"]) == ["Beto Souza"]
    campo.set_value("ninguém").run()
    assert at.dataframe[0].value.empty and at.dataframe[1].value.empty
    assert not at.exception
