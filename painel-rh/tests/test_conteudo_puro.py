"""Testes das funções puras de services/conteudo.py (validação, diff, estabilidade, limpeza).

Rode com: `.venv/bin/python -m pytest tests/ -q` a partir de painel-rh/.
"""
import copy
import os
import sys

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(__file__), "..")))

from services.conteudo import (  # noqa: E402
    diff_catalogos,
    limpar_chaves_internas,
    validar_catalogo,
    verificar_estabilidade,
)


def _pergunta(texto="Qual?", opcoes=None, idx=0, **extra):
    return {"pergunta": texto, "opcoes": opcoes or ["a", "b"], "respostaCorretaIndex": idx, **extra}


def _catalogo_valido():
    return [{
        "id": 1, "titulo": "Curso A", "descricao": "desc", "cargaHoraria": 8,
        "provaFinal": [_pergunta()],
        "modulos": [{
            "id": 100, "titulo": "Módulo 1", "descricao": "",
            "aulas": [{"id": 101, "titulo": "Aula 1", "conteudo": "# corpo",
                       "quiz": [_pergunta()]}],
        }],
    }]


def _erros(problemas):
    return [p for p in problemas if p.severidade == "erro"]


# --------------------------------------------------------------------------- validar_catalogo

def test_catalogo_valido_nao_tem_erros():
    assert _erros(validar_catalogo(_catalogo_valido())) == []


def test_catalogo_vazio_e_erro():
    assert _erros(validar_catalogo([]))
    assert _erros(validar_catalogo("nao é lista"))


def test_campos_obrigatorios_do_contrato_kotlin():
    # descricao de curso e de módulo são OBRIGATÓRIOS para o parser do app.
    cat = _catalogo_valido()
    del cat[0]["descricao"]
    assert any("descricao" in p.mensagem for p in _erros(validar_catalogo(cat)))

    cat = _catalogo_valido()
    del cat[0]["modulos"][0]["descricao"]
    assert any("descricao" in p.mensagem for p in _erros(validar_catalogo(cat)))

    cat = _catalogo_valido()
    del cat[0]["modulos"][0]["aulas"][0]["titulo"]
    assert any("título da aula" in p.mensagem for p in _erros(validar_catalogo(cat)))


def test_id_nao_inteiro_e_erro():
    cat = _catalogo_valido()
    cat[0]["id"] = "1"
    assert any("id do curso" in p.mensagem for p in _erros(validar_catalogo(cat)))
    cat = _catalogo_valido()
    cat[0]["id"] = True  # bool NÃO vale como int no contrato
    assert any("id do curso" in p.mensagem for p in _erros(validar_catalogo(cat)))


def test_resposta_fora_da_faixa_e_erro():
    cat = _catalogo_valido()
    cat[0]["modulos"][0]["aulas"][0]["quiz"] = [_pergunta(opcoes=["a", "b"], idx=2)]
    assert any("fora da faixa" in p.mensagem for p in _erros(validar_catalogo(cat)))


def test_pergunta_com_uma_opcao_e_erro():
    cat = _catalogo_valido()
    cat[0]["provaFinal"] = [_pergunta(opcoes=["só uma"], idx=0)]
    assert any("2 opções" in p.mensagem for p in _erros(validar_catalogo(cat)))


def test_ids_duplicados_sao_erro():
    cat = _catalogo_valido() + _catalogo_valido()  # duplica tudo
    razoes = [p.mensagem for p in _erros(validar_catalogo(cat))]
    assert any("id de curso duplicado" in r for r in razoes)
    assert any("id de módulo duplicado" in r for r in razoes)
    assert any("id de aula duplicado" in r for r in razoes)


def test_aula_sem_conteudo_e_so_aviso():
    cat = _catalogo_valido()
    cat[0]["modulos"][0]["aulas"][0]["conteudo"] = ""
    problemas = validar_catalogo(cat)
    assert _erros(problemas) == []
    assert any(p.severidade == "aviso" and "sem conteúdo" in p.mensagem for p in problemas)


# --------------------------------------------------------------------------- limpeza de _uid

def test_limpar_chaves_internas_remove_uid_em_qualquer_nivel():
    cat = _catalogo_valido()
    cat[0]["_sujo"] = 1
    cat[0]["modulos"][0]["aulas"][0]["quiz"][0]["_uid"] = "abc"
    limpo = limpar_chaves_internas(cat)
    assert "_sujo" not in limpo[0]
    assert "_uid" not in limpo[0]["modulos"][0]["aulas"][0]["quiz"][0]
    # o original não é mutado
    assert "_uid" in cat[0]["modulos"][0]["aulas"][0]["quiz"][0]


# --------------------------------------------------------------------------- estabilidade

def test_estabilidade_detecta_aula_removida():
    pub = _catalogo_valido()
    ras = copy.deepcopy(pub)
    ras[0]["modulos"][0]["aulas"] = []
    r = verificar_estabilidade(ras, pub)
    assert r["aulas_removidas"] == [(101, "Aula 1")]
    assert r["modulos_removidos"] == [] and r["cursos_removidos"] == []


def test_estabilidade_sem_mudanca_e_vazia():
    pub = _catalogo_valido()
    r = verificar_estabilidade(copy.deepcopy(pub), pub)
    assert not any(r.values())


# --------------------------------------------------------------------------- diff

def test_diff_sem_mudancas():
    pub = _catalogo_valido()
    d = diff_catalogos(pub, copy.deepcopy(pub))
    assert d["total_mudancas"] == 0


def test_diff_ignora_chaves_internas():
    pub = _catalogo_valido()
    ras = copy.deepcopy(pub)
    ras[0]["modulos"][0]["aulas"][0]["quiz"][0]["_uid"] = "x"
    assert diff_catalogos(pub, ras)["total_mudancas"] == 0


def test_diff_detecta_titulo_alterado_e_aula_nova():
    pub = _catalogo_valido()
    ras = copy.deepcopy(pub)
    ras[0]["modulos"][0]["aulas"][0]["titulo"] = "Aula 1 renomeada"
    ras[0]["modulos"][0]["aulas"].append({"id": 102, "titulo": "Aula 2", "conteudo": "x", "quiz": []})
    d = diff_catalogos(pub, ras)
    assert d["aulas"]["alterados"][0]["mudancas"] == ["titulo"]
    assert d["aulas"]["adicionados"] == ["[102] Aula 2"]
    assert d["total_mudancas"] == 2


def test_diff_detecta_quiz_e_prova_com_contagem():
    pub = _catalogo_valido()
    ras = copy.deepcopy(pub)
    ras[0]["provaFinal"].append(_pergunta("Nova?"))
    ras[0]["modulos"][0]["aulas"][0]["quiz"][0]["respostaCorretaIndex"] = 1
    d = diff_catalogos(pub, ras)
    assert any("prova final (1→2" in m for m in d["cursos"]["alterados"][0]["mudancas"])
    assert d["aulas"]["alterados"][0]["mudancas"] == ["quiz"]


def test_diff_detecta_aula_movida_de_modulo():
    pub = _catalogo_valido()
    pub[0]["modulos"].append({"id": 200, "titulo": "Módulo 2", "descricao": "", "aulas": []})
    ras = copy.deepcopy(pub)
    aula = ras[0]["modulos"][0]["aulas"].pop(0)
    ras[0]["modulos"][1]["aulas"].append(aula)
    d = diff_catalogos(pub, ras)
    assert any("movida do módulo 100 → 200" in m
               for m in d["aulas"]["alterados"][0]["mudancas"])
