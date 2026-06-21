"""Matrículas: liberar um curso a um aluno (define o curso ativo no app) e acompanhar status."""
from firebase_admin import firestore

from config import COL_MATRICULAS, CURSOS
from firebase_client import get_db


def liberar_curso(uid, curso_id, operador):
    """Cria/atualiza a matrícula. O app lê isto no login/sync e define o curso ativo."""
    doc_id = f"{uid}_{curso_id}"
    get_db().collection(COL_MATRICULAS).document(doc_id).set({
        "uid": uid,
        "cursoId": curso_id,
        "cursoTitulo": CURSOS.get(curso_id, str(curso_id)),
        "status": "ativa",
        "liberadoPor": operador,
        "liberadoEm": firestore.SERVER_TIMESTAMP,
    })
    return doc_id


def listar_matriculas(limite=500):
    docs = get_db().collection(COL_MATRICULAS).limit(limite).stream()
    return [{"id": d.id, **d.to_dict()} for d in docs]
