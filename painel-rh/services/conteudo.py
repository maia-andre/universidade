"""Conteúdo dinâmico (V8 Item 1) — gerência do catálogo de cursos publicado para o app.

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

⚠️ **IDs estáveis:** módulos e aulas têm id **globalmente único** e **imutável**. Editar muda
título/descrição/conteúdo/quiz, nunca o id — senão o progresso do aluno (keyed por aulaId) quebra.
IDs novos saem sempre de um contador que só cresce ([proximo_*_id]).
"""
import json as _json
import os

from firebase_admin import firestore

from config import COL_CONFIG, DOC_CONTEUDO
from firebase_client import get_db

# Assets do app (baseline) — o painel roda a partir do repositório, então alcança o JSON e os .md.
_ASSETS_DIR = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets")
)
_BASELINE_JSON = os.path.join(_ASSETS_DIR, "curso_data.json")


def _doc_ref():
    return get_db().collection(COL_CONFIG).document(DOC_CONTEUDO)


def carregar() -> dict:
    """Retorna {"versao": int, "cursos": list}. Lê o doc publicado; se ausente, faz o bootstrap
    do baseline embarcado nos assets (resolvendo contentPath → Markdown inline)."""
    snap = _doc_ref().get()
    if snap.exists:
        dados = snap.to_dict() or {}
        cursos = _json.loads(dados.get("json") or "[]")
        return {"versao": int(dados.get("versao") or 0), "cursos": cursos}
    return {"versao": 0, "cursos": _bootstrap_dos_assets()}


def versao_atual() -> int:
    snap = _doc_ref().get()
    if snap.exists:
        return int((snap.to_dict() or {}).get("versao") or 0)
    return 0


def publicar(cursos: list, operador: str) -> int:
    """Publica o catálogo: incrementa a versão e grava o doc. Retorna a nova versão.
    Valida que cada módulo/aula tem id único antes de publicar."""
    _validar_ids(cursos)
    nova_versao = versao_atual() + 1
    _doc_ref().set({
        "versao": nova_versao,
        "json": _json.dumps(cursos, ensure_ascii=False),
        "publicadoPor": operador,
        "publicadoEm": firestore.SERVER_TIMESTAMP,
    })
    return nova_versao


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


def _validar_ids(cursos: list) -> None:
    mods = _todos_modulo_ids(cursos)
    aulas = _todos_aula_ids(cursos)
    if len(mods) != len(set(mods)):
        raise ValueError("IDs de módulo duplicados — cada módulo precisa de id único.")
    if len(aulas) != len(set(aulas)):
        raise ValueError("IDs de aula duplicados — cada aula precisa de id único.")


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
