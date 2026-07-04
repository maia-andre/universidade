"""Matrículas: liberar um curso a um aluno (define o curso ativo no app) e acompanhar status.

Regra de negócio: **no máximo UMA matrícula ativa por aluno.** No app, o acesso é
`matriculado OU concluído` — cursos concluídos permanecem acessíveis via `conclusoes`
mesmo depois de a matrícula ser encerrada, então trocar de curso não tira acesso ao
que o aluno já terminou.
"""
from firebase_admin import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

from config import COL_MATRICULAS
from firebase_client import get_db


def matriculas_ativas(uid):
    """Matrículas com status 'ativa' do aluno (filtro de status em Python — a query por
    uid usa índice automático; um aluno tem poucas matrículas)."""
    docs = (get_db().collection(COL_MATRICULAS)
            .where(filter=FieldFilter("uid", "==", uid)).stream())
    linhas = [{"id": d.id, **(d.to_dict() or {})} for d in docs]
    return [m for m in linhas if m.get("status") == "ativa"]


def liberar_curso(uid, curso_id, curso_titulo, operador, encerrar_outras=False):
    """Cria/atualiza a matrícula. O app lê isto no login/sync e define o curso ativo.

    Se o aluno já tem OUTRA matrícula ativa, levanta ValueError — a menos que
    `encerrar_outras=True` (fluxo confirmado pelo operador no diálogo de troca),
    que encerra as demais antes de liberar.

    ⚠️ Contrato com o app Android: doc id `{uid}_{cursoId}`, campos `uid`, `cursoId` (int)
    e `status == "ativa"` — não renomear. O título vem do catálogo publicado (dinâmico).
    """
    outras = [m for m in matriculas_ativas(uid) if m.get("cursoId") != curso_id]
    if outras and not encerrar_outras:
        titulos = ", ".join(str(m.get("cursoTitulo") or m.get("cursoId")) for m in outras)
        raise ValueError(
            f"O aluno já tem matrícula ativa em: {titulos}. Regra: um curso ativo por "
            "aluno — encerre a matrícula anterior ou confirme a troca."
        )
    for m in outras:
        encerrar_matricula(uid, m["cursoId"], operador)

    doc_id = f"{uid}_{curso_id}"
    get_db().collection(COL_MATRICULAS).document(doc_id).set({
        "uid": uid,
        "cursoId": curso_id,
        "cursoTitulo": curso_titulo,
        "status": "ativa",
        "liberadoPor": operador,
        "liberadoEm": firestore.SERVER_TIMESTAMP,
    })
    return doc_id


def encerrar_matricula(uid, curso_id, operador):
    """Encerra (desmatricula) uma matrícula: status='encerrada'. O curso deixa de ser
    acessível no app — salvo se já concluído, que permanece acessível (v7, Item 5)."""
    doc_id = f"{uid}_{curso_id}"
    get_db().collection(COL_MATRICULAS).document(doc_id).update({
        "status": "encerrada",
        "encerradoPor": operador,
        "encerradoEm": firestore.SERVER_TIMESTAMP,
    })
    return doc_id


def listar_matriculas(limite=500):
    docs = get_db().collection(COL_MATRICULAS).limit(limite).stream()
    return [{"id": d.id, **d.to_dict()} for d in docs]
