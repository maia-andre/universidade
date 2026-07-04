"""Conteúdo dinâmico (V8 Item 1 / reforma V8.1) — catálogo de cursos publicado para o app.

O catálogo (cursos → módulos → aulas → quiz/provaFinal) é publicado em **um único doc**
`config/conteudo` do Firestore, no formato:

    config/conteudo {
        versao: int,        # monotônico — o app só sincroniza quando aumenta
        json: str,          # catálogo inteiro como string JSON (mesmo formato do curso_data.json)
        publicadoPor: str,  # operador (auditoria)
        publicadoEm: ts,
    }

O app lê isso em runtime e reconstrói o conteúdo local **sem novo APK**. As regras do Firestore já
liberam leitura por autenticados e negam escrita ao cliente (só o Admin SDK do painel grava).

V8.1 acrescenta, invisíveis ao app (deny-all nas rules):
- `rascunhos/conteudo` — rascunho persistente de edição (sobrevive à sessão do navegador);
- `config/conteudo/historico/{versao:06d}` — cópia de cada versão publicada (rollback).

⚠️ **IDs estáveis:** módulos e aulas têm id **globalmente único** e **imutável**. Editar muda
título/descrição/conteúdo/quiz, nunca o id — senão o progresso do aluno (keyed por aulaId) quebra.
IDs novos saem sempre de um contador que só cresce ([proximo_*_id]).

⚠️ **Validação pré-publish:** o parser do app (kotlinx) descarta o catálogo INTEIRO se faltar
campo obrigatório — [validar_catalogo] espelha exatamente esse contrato e bloqueia o publish.

Este módulo NÃO importa streamlit (funções puras testáveis + acesso Firestore).
"""
import json as _json
import os
from dataclasses import dataclass
from typing import Literal

from firebase_admin import firestore

from config import COL_CONFIG, COL_RASCUNHOS, DOC_CONTEUDO, DOC_RASCUNHO_CONTEUDO, SUBCOL_HISTORICO
from firebase_client import get_db

# Assets do app (baseline) — o painel roda a partir do repositório, então alcança o JSON e os .md.
_ASSETS_DIR = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets")
)
_BASELINE_JSON = os.path.join(_ASSETS_DIR, "curso_data.json")

# Limites do doc único (Firestore aceita 1 MiB por doc).
_LIMITE_AVISO_BYTES = 700_000
_LIMITE_ERRO_BYTES = 950_000


class ConflitoVersaoError(ValueError):
    """Outro operador publicou entre a revisão do diff e a confirmação.
    Subclasse de ValueError: é erro de negócio — a mensagem vai direto ao operador."""


def _doc_ref():
    return get_db().collection(COL_CONFIG).document(DOC_CONTEUDO)


def _rascunho_ref():
    return get_db().collection(COL_RASCUNHOS).document(DOC_RASCUNHO_CONTEUDO)


def _historico_col():
    return _doc_ref().collection(SUBCOL_HISTORICO)


# --------------------------------------------------------------------------- leitura

def carregar() -> dict:
    """Retorna {"versao": int, "cursos": list} do doc publicado; se ausente, faz o bootstrap
    do baseline embarcado nos assets (resolvendo contentPath → Markdown inline)."""
    snap = _doc_ref().get()
    if snap.exists:
        dados = snap.to_dict() or {}
        cursos = _json.loads(dados.get("json") or "[]")
        return {"versao": int(dados.get("versao") or 0), "cursos": cursos}
    return {"versao": 0, "cursos": _bootstrap_dos_assets()}


carregar_publicado = carregar  # alias semântico (contraparte de carregar_rascunho)


def versao_atual() -> int:
    snap = _doc_ref().get()
    if snap.exists:
        return int((snap.to_dict() or {}).get("versao") or 0)
    return 0


# --------------------------------------------------------------------------- rascunho persistente

def carregar_rascunho() -> dict | None:
    """Rascunho em andamento, ou None. {"cursos", "baseVersao", "editadoPor", "iniciadoEm",
    "atualizadoEm"}."""
    snap = _rascunho_ref().get()
    if not snap.exists:
        return None
    d = snap.to_dict() or {}
    return {
        "cursos": _json.loads(d.get("json") or "[]"),
        "baseVersao": int(d.get("baseVersao") or 0),
        "editadoPor": d.get("editadoPor", "?"),
        "iniciadoEm": d.get("iniciadoEm"),
        "atualizadoEm": d.get("atualizadoEm"),
    }


def salvar_rascunho(cursos: list, operador: str, base_versao: int) -> None:
    """Grava (write-through) o rascunho — sempre sem chaves internas `_*`."""
    ref = _rascunho_ref()
    dados = {
        "json": _json.dumps(limpar_chaves_internas(cursos), ensure_ascii=False),
        "baseVersao": int(base_versao),
        "editadoPor": operador,
        "atualizadoEm": firestore.SERVER_TIMESTAMP,
    }
    if not ref.get().exists:
        dados["iniciadoEm"] = firestore.SERVER_TIMESTAMP
    ref.set(dados, merge=True)


def assumir_rascunho(operador: str) -> None:
    _rascunho_ref().set({"editadoPor": operador}, merge=True)


def apagar_rascunho() -> None:
    _rascunho_ref().delete()


# --------------------------------------------------------------------------- publicação + histórico

def publicar(cursos: list, operador: str, versao_base: int | None = None) -> int:
    """Publica o catálogo em transação. Retorna a nova versão.

    - Valida o schema completo (erros ⇒ ValueError): um payload que o parser do app não
      aceita jamais deve ser publicado (nunca chegaria aos alunos).
    - `versao_base`: concorrência otimista — se o publicado avançou desde que o operador
      revisou o diff, aborta com ConflitoVersaoError (nada é gravado).
    - Backfill idempotente: a versão vigente vai para o histórico antes de ser substituída
      (cobre versões publicadas antes do histórico existir).
    - Apaga o rascunho ao publicar (o publicado vira a nova base de edição).
    """
    erros_validacao = [p for p in validar_catalogo(cursos) if p.severidade == "erro"]
    if erros_validacao:
        detalhes = "; ".join(f"{p.caminho}: {p.mensagem}" for p in erros_validacao[:5])
        extra = f" (+{len(erros_validacao) - 5} erros)" if len(erros_validacao) > 5 else ""
        raise ValueError(f"Catálogo com erros — corrija antes de publicar: {detalhes}{extra}")

    cursos_limpos = limpar_chaves_internas(cursos)
    payload = _json.dumps(cursos_limpos, ensure_ascii=False)
    doc_ref, rascunho_ref, hist = _doc_ref(), _rascunho_ref(), _historico_col()

    @firestore.transactional
    def _tx(tx):
        snap = doc_ref.get(transaction=tx)
        atual = (snap.to_dict() or {}) if snap.exists else {}
        versao_lida = int(atual.get("versao") or 0)
        if versao_base is not None and versao_base != versao_lida:
            raise ConflitoVersaoError(
                f"O conteúdo publicado avançou para v{versao_lida} enquanto você revisava "
                "— feche e revise o diff novamente."
            )
        nova = versao_lida + 1
        if versao_lida > 0 and atual.get("json"):
            tx.set(hist.document(f"{versao_lida:06d}"), {
                "versao": versao_lida,
                "json": atual["json"],
                "publicadoPor": atual.get("publicadoPor", "?"),
                "publicadoEm": atual.get("publicadoEm"),
                "resumo": _resumo_catalogo(_json.loads(atual["json"])),
            })
        novo = {
            "versao": nova,
            "json": payload,
            "publicadoPor": operador,
            "publicadoEm": firestore.SERVER_TIMESTAMP,
        }
        tx.set(doc_ref, novo)
        tx.set(hist.document(f"{nova:06d}"), {**novo, "resumo": _resumo_catalogo(cursos_limpos)})
        tx.delete(rascunho_ref)
        return nova

    return _tx(get_db().transaction())


def listar_historico(limite: int = 20) -> list[dict]:
    """Versões publicadas, mais recente primeiro — projeção leve (sem o campo json)."""
    docs = (_historico_col()
            .order_by("versao", direction=firestore.Query.DESCENDING)
            .select(["versao", "publicadoPor", "publicadoEm", "resumo"])
            .limit(limite).stream())
    return [d.to_dict() for d in docs]


def carregar_versao_historico(versao: int) -> list:
    """Catálogo (lista de cursos) de uma versão do histórico."""
    snap = _historico_col().document(f"{int(versao):06d}").get()
    if not snap.exists:
        raise ValueError(f"A versão {versao} não está no histórico.")
    return _json.loads((snap.to_dict() or {}).get("json") or "[]")


def _resumo_catalogo(cursos: list) -> str:
    n_mod = sum(len(c.get("modulos", [])) for c in cursos)
    n_aulas = sum(len(m.get("aulas", [])) for c in cursos for m in c.get("modulos", []))
    return f"{len(cursos)} curso(s), {n_mod} módulo(s), {n_aulas} aula(s)"


# --------------------------------------------------------------------------- funções puras

def limpar_chaves_internas(obj):
    """Remove recursivamente chaves `_*` (ex.: `_uid` das perguntas) — o que sai do painel
    (rascunho, diff, publish) é sempre o formato limpo do contrato."""
    if isinstance(obj, dict):
        return {k: limpar_chaves_internas(v) for k, v in obj.items() if not str(k).startswith("_")}
    if isinstance(obj, list):
        return [limpar_chaves_internas(x) for x in obj]
    return obj


@dataclass
class Problema:
    severidade: Literal["erro", "aviso"]
    caminho: str
    mensagem: str


def _e_int(v) -> bool:
    return isinstance(v, int) and not isinstance(v, bool)


def _texto(v) -> bool:
    return isinstance(v, str)


def _validar_pergunta(p, caminho: str, problemas: list) -> None:
    if not isinstance(p, dict):
        problemas.append(Problema("erro", caminho, "pergunta não é um objeto"))
        return
    if not (_texto(p.get("pergunta")) and p.get("pergunta", "").strip()):
        problemas.append(Problema("erro", caminho, "texto da pergunta vazio ou ausente"))
    opcoes = p.get("opcoes")
    if not isinstance(opcoes, list) or len(opcoes) < 2:
        problemas.append(Problema("erro", caminho, "precisa de ao menos 2 opções"))
    elif not all(_texto(o) and o.strip() for o in opcoes):
        problemas.append(Problema("erro", caminho, "há opção vazia ou não-textual"))
    idx = p.get("respostaCorretaIndex")
    if not _e_int(idx):
        problemas.append(Problema("erro", caminho, "respostaCorretaIndex ausente ou não-inteiro"))
    elif isinstance(opcoes, list) and not (0 <= idx < len(opcoes)):
        # O app NÃO faz bounds-check — índice fora da faixa quebra a correção em runtime.
        problemas.append(Problema(
            "erro", caminho, f"respostaCorretaIndex={idx} fora da faixa (0..{len(opcoes) - 1})"
        ))


def validar_catalogo(cursos) -> list[Problema]:
    """Valida o catálogo contra o contrato EXATO do parser do app (kotlinx serialization):
    campos obrigatórios curso{id,titulo,descricao} / módulo{id,titulo,descricao} /
    aula{id,titulo} / pergunta{pergunta,opcoes,respostaCorretaIndex}; ids únicos; índice de
    resposta na faixa; tamanho do doc. Qualquer "erro" deve bloquear o publish."""
    problemas: list[Problema] = []
    if not isinstance(cursos, list) or not cursos:
        problemas.append(Problema(
            "erro", "catálogo",
            "catálogo vazio — o app trata como inválido e não aplica (publicação bloqueada)",
        ))
        return problemas

    ids_curso, ids_modulo, ids_aula = {}, {}, {}
    for c in cursos:
        if not isinstance(c, dict):
            problemas.append(Problema("erro", "catálogo", "curso não é um objeto"))
            continue
        cam_c = f"curso [{c.get('id', '?')}] '{c.get('titulo', '')}'"
        if not _e_int(c.get("id")):
            problemas.append(Problema("erro", cam_c, "id do curso ausente ou não-inteiro"))
        else:
            ids_curso.setdefault(c["id"], []).append(cam_c)
        if not (_texto(c.get("titulo")) and c.get("titulo", "").strip()):
            problemas.append(Problema("erro", cam_c, "título do curso vazio ou ausente"))
        if not _texto(c.get("descricao")):
            problemas.append(Problema(
                "erro", cam_c, "campo 'descricao' ausente (obrigatório para o app; pode ser vazio)"
            ))
        ch = c.get("cargaHoraria")
        if ch is not None and not _e_int(ch):
            problemas.append(Problema("erro", cam_c, "cargaHoraria não-inteira"))
        if not isinstance(c.get("provaFinal", []), list):
            problemas.append(Problema("erro", cam_c, "provaFinal não é uma lista"))
        else:
            for i, p in enumerate(c.get("provaFinal", []), start=1):
                _validar_pergunta(p, f"{cam_c} > prova final, pergunta {i}", problemas)

        if not isinstance(c.get("modulos", []), list):
            problemas.append(Problema("erro", cam_c, "modulos não é uma lista"))
            continue
        for m in c.get("modulos", []):
            if not isinstance(m, dict):
                problemas.append(Problema("erro", cam_c, "módulo não é um objeto"))
                continue
            cam_m = f"{cam_c} > módulo [{m.get('id', '?')}] '{m.get('titulo', '')}'"
            if not _e_int(m.get("id")):
                problemas.append(Problema("erro", cam_m, "id do módulo ausente ou não-inteiro"))
            else:
                ids_modulo.setdefault(m["id"], []).append(cam_m)
            if not (_texto(m.get("titulo")) and m.get("titulo", "").strip()):
                problemas.append(Problema("erro", cam_m, "título do módulo vazio ou ausente"))
            if not _texto(m.get("descricao")):
                problemas.append(Problema(
                    "erro", cam_m,
                    "campo 'descricao' ausente (obrigatório para o app; pode ser vazio)",
                ))
            if not isinstance(m.get("aulas", []), list):
                problemas.append(Problema("erro", cam_m, "aulas não é uma lista"))
                continue
            for a in m.get("aulas", []):
                if not isinstance(a, dict):
                    problemas.append(Problema("erro", cam_m, "aula não é um objeto"))
                    continue
                cam_a = f"{cam_m} > aula [{a.get('id', '?')}] '{a.get('titulo', '')}'"
                if not _e_int(a.get("id")):
                    problemas.append(Problema("erro", cam_a, "id da aula ausente ou não-inteiro"))
                else:
                    ids_aula.setdefault(a["id"], []).append(cam_a)
                if not (_texto(a.get("titulo")) and a.get("titulo", "").strip()):
                    problemas.append(Problema("erro", cam_a, "título da aula vazio ou ausente"))
                if a.get("conteudo") is not None and not _texto(a.get("conteudo")):
                    problemas.append(Problema("erro", cam_a, "conteúdo não-textual"))
                elif not (a.get("conteudo") or "").strip():
                    problemas.append(Problema("aviso", cam_a, "aula sem conteúdo (corpo vazio)"))
                if not isinstance(a.get("quiz", []), list):
                    problemas.append(Problema("erro", cam_a, "quiz não é uma lista"))
                else:
                    for i, p in enumerate(a.get("quiz", []), start=1):
                        _validar_pergunta(p, f"{cam_a} > quiz, pergunta {i}", problemas)

    for nome, ids in (("curso", ids_curso), ("módulo", ids_modulo), ("aula", ids_aula)):
        for id_, usos in ids.items():
            if len(usos) > 1:
                problemas.append(Problema(
                    "erro", usos[1],
                    f"id de {nome} duplicado ({id_}) — ids são globais e imutáveis",
                ))

    tamanho = len(_json.dumps(limpar_chaves_internas(cursos), ensure_ascii=False).encode("utf-8"))
    if tamanho > _LIMITE_ERRO_BYTES:
        problemas.append(Problema(
            "erro", "catálogo",
            f"payload com {tamanho // 1024} KB — excede o limite seguro do doc (1 MiB)",
        ))
    elif tamanho > _LIMITE_AVISO_BYTES:
        problemas.append(Problema(
            "aviso", "catálogo",
            f"payload com {tamanho // 1024} KB — aproximando do limite de 1 MiB do doc",
        ))
    return problemas


def verificar_estabilidade(rascunho: list, publicado: list) -> dict:
    """Entidades que EXISTEM no publicado e sumiram do rascunho — remover uma aula deixa
    órfão o progresso dos alunos nela (reversível se o mesmo aulaId voltar). Não bloqueia:
    exige confirmação explícita no diálogo de publicação."""
    def mapa(cursos):
        cs, ms, as_ = {}, {}, {}
        for c in cursos:
            cs[c.get("id")] = c.get("titulo", "?")
            for m in c.get("modulos", []):
                ms[m.get("id")] = m.get("titulo", "?")
                for a in m.get("aulas", []):
                    as_[a.get("id")] = a.get("titulo", "?")
        return cs, ms, as_

    pc, pm, pa = mapa(publicado)
    rc, rm, ra = mapa(rascunho)
    return {
        "cursos_removidos": sorted((i, t) for i, t in pc.items() if i not in rc),
        "modulos_removidos": sorted((i, t) for i, t in pm.items() if i not in rm),
        "aulas_removidas": sorted((i, t) for i, t in pa.items() if i not in ra),
    }


def diff_catalogos(publicado: list, rascunho: list) -> dict:
    """Diff estruturado por id (para o preview de publicação). Compara os catálogos LIMPOS.
    Saída por nível: adicionados / removidos / alterados (com a lista de campos mudados)."""
    publicado = limpar_chaves_internas(publicado)
    rascunho = limpar_chaves_internas(rascunho)

    def achatar(cursos):
        cs, ms, as_ = {}, {}, {}
        for c in cursos:
            cs[c.get("id")] = c
            for m in c.get("modulos", []):
                ms[m.get("id")] = (c, m)
                for a in m.get("aulas", []):
                    as_[a.get("id")] = (c, m, a)
        return cs, ms, as_

    pc, pm, pa = achatar(publicado)
    rc, rm, ra = achatar(rascunho)

    def resumo_curso(c):
        return f"[{c.get('id')}] {c.get('titulo', '?')}"

    def mudancas_curso(antes, depois):
        muda = [campo for campo in ("titulo", "descricao", "cargaHoraria")
                if antes.get(campo) != depois.get(campo)]
        pf_a, pf_d = antes.get("provaFinal", []), depois.get("provaFinal", [])
        if pf_a != pf_d:
            muda.append(f"prova final ({len(pf_a)}→{len(pf_d)} perguntas)"
                        if len(pf_a) != len(pf_d) else "prova final")
        return muda

    def mudancas_modulo(antes, depois):
        return [campo for campo in ("titulo", "descricao")
                if antes.get(campo) != depois.get(campo)]

    def mudancas_aula(antes_t, depois_t):
        (_, m_a, a_a), (_, m_d, a_d) = antes_t, depois_t
        muda = [campo for campo in ("titulo", "conteudo")
                if a_a.get(campo) != a_d.get(campo)]
        qz_a, qz_d = a_a.get("quiz", []), a_d.get("quiz", [])
        if qz_a != qz_d:
            muda.append(f"quiz ({len(qz_a)}→{len(qz_d)} perguntas)"
                        if len(qz_a) != len(qz_d) else "quiz")
        if m_a.get("id") != m_d.get("id"):
            muda.append(f"movida do módulo {m_a.get('id')} → {m_d.get('id')}")
        return muda

    def nivel(pub_map, ras_map, rotulo, fn_mudancas, fn_res):
        adicionados = [fn_res(ras_map[i]) for i in ras_map if i not in pub_map]
        removidos = [fn_res(pub_map[i]) for i in pub_map if i not in ras_map]
        alterados = []
        for i in ras_map:
            if i in pub_map:
                muda = fn_mudancas(pub_map[i], ras_map[i])
                if muda:
                    alterados.append({"resumo": fn_res(ras_map[i]), "mudancas": muda})
        return {"adicionados": adicionados, "removidos": removidos, "alterados": alterados}

    def res_mod(par):
        _, m = par
        return f"[{m.get('id')}] {m.get('titulo', '?')}"

    def res_aula(tripla):
        _, _, a = tripla
        return f"[{a.get('id')}] {a.get('titulo', '?')}"

    d = {
        "cursos": nivel(pc, rc, "curso", mudancas_curso, resumo_curso),
        "modulos": nivel(pm, rm, "módulo",
                         lambda a, b: mudancas_modulo(a[1], b[1]), res_mod),
        "aulas": nivel(pa, ra, "aula", mudancas_aula, res_aula),
    }
    d["total_mudancas"] = sum(
        len(d[n][k]) for n in ("cursos", "modulos", "aulas")
        for k in ("adicionados", "removidos", "alterados")
    )
    return d


# --------------------------------------------------------------------------- IDs estáveis

def _todos_modulo_ids(cursos: list) -> list:
    return [m["id"] for c in cursos for m in c.get("modulos", [])]


def _todos_aula_ids(cursos: list) -> list:
    return [a["id"] for c in cursos for m in c.get("modulos", []) for a in m.get("aulas", [])]


def proximo_curso_id(cursos: list) -> int:
    return (max((c["id"] for c in cursos), default=0)) + 1


def proximo_modulo_id(cursos: list) -> int:
    return (max(_todos_modulo_ids(cursos), default=0)) + 1


def proximo_aula_id(cursos: list) -> int:
    return (max(_todos_aula_ids(cursos), default=0)) + 1


# --------------------------------------------------------------------------- bootstrap

def _bootstrap_dos_assets() -> list:
    """Lê o curso_data.json do APK e inlina o Markdown de cada aula (resolve contentPath),
    para que o conteúdo publicado seja autocontido (o app não tem acesso aos .md remotamente)."""
    with open(_BASELINE_JSON, encoding="utf-8") as f:
        cursos = _json.load(f)
    for curso in cursos:
        for modulo in curso.get("modulos", []):
            for aula in modulo.get("aulas", []):
                if not aula.get("conteudo") and aula.get("contentPath"):
                    aula["conteudo"] = _ler_markdown(aula["contentPath"])
                aula.pop("contentPath", None)
    return cursos


def _ler_markdown(content_path: str) -> str:
    caminho = os.path.join(_ASSETS_DIR, content_path)
    try:
        with open(caminho, encoding="utf-8") as f:
            return f.read()
    except OSError:
        return ""
