"""Cadastro de servidores (alunos): cria a conta no Auth + o perfil no Firestore (push).

Esta é a operação privilegiada central — só o Admin SDK cria contas de autenticação.
"""
from firebase_admin import firestore

from config import COL_SERVIDORES
from firebase_client import get_auth, get_db


def criar_aluno(nome, email, matricula, lotacao, senha_temporaria, operador):
    """Cria a conta de autenticação e o doc do servidor. Retorna o uid."""
    user = get_auth().create_user(
        email=email,
        password=senha_temporaria,
        display_name=nome,
    )
    get_db().collection(COL_SERVIDORES).document(user.uid).set({
        "nome": nome,
        "email": email,
        "matricula": matricula,
        "lotacao": lotacao,
        "role": "aluno",
        "criadoEm": firestore.SERVER_TIMESTAMP,
        "criadoPor": operador,
    })
    return user.uid


def listar_alunos(limite=500):
    docs = get_db().collection(COL_SERVIDORES).limit(limite).stream()
    return [{"uid": d.id, **d.to_dict()} for d in docs]


def importar_planilha(df, senha_padrao, operador):
    """Importa servidores de um DataFrame (colunas: nome, email, matricula, lotacao).

    Retorna (criados, erros) — processa linha a linha para não abortar tudo num erro.
    """
    criados, erros = [], []
    for i, row in df.iterrows():
        email = str(row.get("email", "")).strip()
        try:
            uid = criar_aluno(
                nome=str(row.get("nome", "")).strip(),
                email=email,
                matricula=str(row.get("matricula", "")).strip(),
                lotacao=str(row.get("lotacao", "")).strip(),
                senha_temporaria=senha_padrao,
                operador=operador,
            )
            criados.append((email, uid))
        except Exception as e:  # noqa: BLE001 — reporta erro por linha
            erros.append((email or f"linha {i + 2}", str(e)))
    return criados, erros
