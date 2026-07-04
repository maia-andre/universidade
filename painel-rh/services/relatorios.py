"""Relatórios: lê as conclusões gravadas pelo app (pull) e consolida KPIs."""
from config import COL_CONCLUSOES, COL_MATRICULAS, COL_SERVIDORES, CURSOS
from firebase_client import get_db


def listar_conclusoes(limite=1000):
    docs = get_db().collection(COL_CONCLUSOES).limit(limite).stream()
    return [{"id": d.id, **d.to_dict()} for d in docs]


def situacao_por_aluno(limite=1000):
    """Cruza servidores × matrículas × conclusões: uma linha por matrícula, com o
    estado da matrícula e se o curso já foi concluído (v7, Item 5)."""
    db = get_db()
    servidores = {d.id: d.to_dict() for d in db.collection(COL_SERVIDORES).limit(limite).stream()}
    concluidos = {
        (c.get("uid"), c.get("cursoId"))
        for c in (d.to_dict() for d in db.collection(COL_CONCLUSOES).limit(limite).stream())
    }
    linhas = []
    for d in db.collection(COL_MATRICULAS).limit(limite).stream():
        m = d.to_dict()
        uid, curso_id = m.get("uid"), m.get("cursoId")
        serv = servidores.get(uid, {})
        linhas.append({
            "aluno": serv.get("nome", uid),
            "email": serv.get("email", ""),
            "curso": CURSOS.get(curso_id, str(curso_id)),
            "matrícula": m.get("status", "?"),
            "concluído": "✅" if (uid, curso_id) in concluidos else "—",
        })
    return linhas


def _contar(colecao):
    """Conta os docs via aggregation query — o servidor conta, sem baixar a coleção."""
    resultado = get_db().collection(colecao).count(alias="total").get()
    return int(resultado[0][0].value)


def resumo():
    """KPIs simples: total de matrículas, de conclusões e a taxa de conclusão."""
    total_m = _contar(COL_MATRICULAS)
    total_c = _contar(COL_CONCLUSOES)
    taxa = (total_c / total_m * 100) if total_m else 0.0
    return {"matriculas": total_m, "conclusoes": total_c, "taxa": taxa}
