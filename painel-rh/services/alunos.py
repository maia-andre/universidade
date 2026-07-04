"""Cadastro de servidores (alunos): cria a conta no Auth + o perfil no Firestore (push).

Esta é a operação privilegiada central — só o Admin SDK cria contas de autenticação.
"""
from firebase_admin import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

from config import COL_SERVIDORES
from firebase_client import get_auth, get_db
from services import validacao


def matricula_existente(matricula: str) -> bool:
    """Consulta por igualdade em campo único — índice automático do Firestore."""
    docs = (get_db().collection(COL_SERVIDORES)
            .where(filter=FieldFilter("matricula", "==", matricula)).limit(1).stream())
    return next(docs, None) is not None


def criar_aluno(nome, email, matricula, lotacao, senha_temporaria, operador):
    """Cria a conta de autenticação e o doc do servidor. Retorna o uid."""
    if not validacao.email_valido(email):
        raise ValueError(f"E-mail em formato inválido: '{email}'.")
    if matricula and matricula_existente(matricula):
        raise ValueError(f"A matrícula '{matricula}' já está cadastrada.")
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


def redefinir_senha(uid, nova_senha, operador):
    """Define uma nova senha temporária para o aluno (RH dispara o reset).

    O Admin SDK altera a credencial direto, sem depender de e-mail — o RH entrega
    a nova senha e o servidor a troca no acesso. Carimba quem redefiniu e quando,
    para auditoria, no doc do servidor (que sempre existe para um aluno listado).
    """
    get_auth().update_user(uid, password=nova_senha)
    get_db().collection(COL_SERVIDORES).document(uid).update({
        "senhaRedefinidaPor": operador,
        "senhaRedefinidaEm": firestore.SERVER_TIMESTAMP,
    })
    return uid


def listar_alunos(limite=500):
    docs = get_db().collection(COL_SERVIDORES).order_by("nome").limit(limite).stream()
    return [{"uid": d.id, **d.to_dict()} for d in docs]


def importar_planilha(linhas, operador):
    """Importa alunos já validados por `validacao.validar_planilha` (dry-run na página).

    Gera uma senha temporária aleatória POR aluno. Retorna uma linha de resultado por
    entrada: `{nome, email, senha_temporaria, uid}` em sucesso, ou com `erro` em falha —
    processa linha a linha para não abortar tudo num erro.
    """
    resultado = []
    for linha in linhas:
        senha = validacao.gerar_senha_temporaria()
        try:
            uid = criar_aluno(
                nome=linha["nome"],
                email=linha["email"],
                matricula=linha.get("matricula", ""),
                lotacao=linha.get("lotacao", ""),
                senha_temporaria=senha,
                operador=operador,
            )
            resultado.append({"nome": linha["nome"], "email": linha["email"],
                              "senha_temporaria": senha, "uid": uid})
        except Exception as e:  # noqa: BLE001 — reporta erro por linha
            resultado.append({"nome": linha["nome"], "email": linha["email"],
                              "senha_temporaria": "", "erro": str(e)})
    return resultado
