"""Relatórios: lê as conclusões gravadas pelo app (pull) e consolida KPIs."""
from config import COL_CONCLUSOES, COL_MATRICULAS
from firebase_client import get_db


def listar_conclusoes(limite=1000):
    docs = get_db().collection(COL_CONCLUSOES).limit(limite).stream()
    return [{"id": d.id, **d.to_dict()} for d in docs]


def resumo():
    """KPIs simples: total de matrículas, de conclusões e a taxa de conclusão."""
    db = get_db()
    total_m = sum(1 for _ in db.collection(COL_MATRICULAS).stream())
    total_c = sum(1 for _ in db.collection(COL_CONCLUSOES).stream())
    taxa = (total_c / total_m * 100) if total_m else 0.0
    return {"matriculas": total_m, "conclusoes": total_c, "taxa": taxa}
