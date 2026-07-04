"""Operadores do painel (V8 Item 2.2) — autenticação real com senha em hash.

Substitui o dict de credencial em texto puro (`config.OPERADORES`) por uma coleção Firestore
`operadores/{usuario}`, com senha guardada como **hash PBKDF2-HMAC-SHA256** (stdlib — sem
dependência nova). Suporta troca de senha no 1º acesso, reset por administrador e ativar/desativar.

A coleção `operadores` é acessada **só pelo Admin SDK** (este painel). As Security Rules do app
negam tudo que não é explicitamente liberado, então o cliente Android nunca lê este conteúdo
sensível — não é preciso (nem desejável) criar regra para `operadores`.

⚠️ Nunca guardamos a senha em texto. O hash inclui salt aleatório por operador.
"""
from __future__ import annotations

import base64
import hashlib
import hmac
import os
import time

from firebase_admin import firestore

from config import OPERADORES
from firebase_client import get_db

COL_OPERADORES = "operadores"
_ITERACOES = 200_000
MIN_SENHA = 6

# Rate-limit de login em memória do processo (some no restart — aceitável em rede interna).
_FALHAS: dict[str, list[float]] = {}
MAX_TENTATIVAS = 5
JANELA_SEG = 300


def _col():
    return get_db().collection(COL_OPERADORES)


# --------------------------------------------------------------------------- hashing (stdlib)

def hash_senha(senha: str) -> str:
    """Gera `pbkdf2_sha256$iter$salt_b64$hash_b64` com salt aleatório."""
    salt = os.urandom(16)
    dk = hashlib.pbkdf2_hmac("sha256", senha.encode("utf-8"), salt, _ITERACOES)
    return "pbkdf2_sha256${}${}${}".format(
        _ITERACOES,
        base64.b64encode(salt).decode("ascii"),
        base64.b64encode(dk).decode("ascii"),
    )


def verificar_senha(senha: str, codificado: str) -> bool:
    """Compara a senha com o hash guardado (tempo constante). False se o formato for inválido."""
    try:
        _algo, iteracoes, salt_b64, hash_b64 = codificado.split("$")
        salt = base64.b64decode(salt_b64)
        esperado = base64.b64decode(hash_b64)
        dk = hashlib.pbkdf2_hmac("sha256", senha.encode("utf-8"), salt, int(iteracoes))
        return hmac.compare_digest(dk, esperado)
    except (ValueError, AttributeError):
        return False


# --------------------------------------------------------------------------- seed / migração

def garantir_seed() -> None:
    """Bootstrap único: se a coleção está vazia, migra os operadores do dict `config.OPERADORES`
    com `precisaTrocar=True` (trocam a senha no 1º acesso) e `admin=True` (eram os de confiança)."""
    if next(_col().limit(1).stream(), None) is not None:
        return
    for usuario, senha in OPERADORES.items():
        _col().document(usuario).set({
            "senhaHash": hash_senha(senha),
            "precisaTrocar": True,
            "ativo": True,
            "admin": True,
            "criadoEm": firestore.SERVER_TIMESTAMP,
            "criadoPor": "seed",
        })


# --------------------------------------------------------------------------- operações

def _registrar_falha(usuario: str, agora: float) -> None:
    _FALHAS.setdefault(usuario, []).append(agora)


def autenticar(usuario: str, senha: str) -> dict | None:
    """Valida usuário/senha. Retorna {usuario, precisaTrocar, admin} ou None se inválido/inativo.

    Levanta ValueError se o usuário estourou o limite de tentativas na janela.
    """
    garantir_seed()
    agora = time.time()
    _FALHAS[usuario] = [t for t in _FALHAS.get(usuario, []) if agora - t < JANELA_SEG]
    if len(_FALHAS[usuario]) >= MAX_TENTATIVAS:
        raise ValueError("Muitas tentativas de login. Aguarde alguns minutos e tente novamente.")

    snap = _col().document(usuario).get()
    if not snap.exists:
        _registrar_falha(usuario, agora)
        return None
    dados = snap.to_dict() or {}
    if not dados.get("ativo", True):
        _registrar_falha(usuario, agora)
        return None
    if not verificar_senha(senha, dados.get("senhaHash", "")):
        _registrar_falha(usuario, agora)
        return None
    _FALHAS.pop(usuario, None)
    return {
        "usuario": usuario,
        "precisaTrocar": bool(dados.get("precisaTrocar")),
        "admin": bool(dados.get("admin")),
    }


def criar_operador(usuario: str, senha_temporaria: str, operador: str, admin: bool = False) -> str:
    usuario = (usuario or "").strip()
    if not usuario:
        raise ValueError("Informe o nome de usuário.")
    if len(senha_temporaria or "") < MIN_SENHA:
        raise ValueError(f"A senha temporária precisa de ao menos {MIN_SENHA} caracteres.")
    ref = _col().document(usuario)
    if ref.get().exists:
        raise ValueError(f"O operador '{usuario}' já existe.")
    ref.set({
        "senhaHash": hash_senha(senha_temporaria),
        "precisaTrocar": True,
        "ativo": True,
        "admin": admin,
        "criadoEm": firestore.SERVER_TIMESTAMP,
        "criadoPor": operador,
    })
    return usuario


def trocar_senha(usuario: str, nova_senha: str) -> None:
    if len(nova_senha or "") < MIN_SENHA:
        raise ValueError(f"A senha precisa de ao menos {MIN_SENHA} caracteres.")
    _col().document(usuario).update({
        "senhaHash": hash_senha(nova_senha),
        "precisaTrocar": False,
        "senhaAlteradaEm": firestore.SERVER_TIMESTAMP,
    })


def redefinir_senha(usuario: str, nova_temporaria: str, operador: str) -> None:
    """Reset por administrador: define uma senha temporária e força troca no próximo acesso."""
    if len(nova_temporaria or "") < MIN_SENHA:
        raise ValueError(f"A senha temporária precisa de ao menos {MIN_SENHA} caracteres.")
    _col().document(usuario).update({
        "senhaHash": hash_senha(nova_temporaria),
        "precisaTrocar": True,
        "senhaRedefinidaPor": operador,
        "senhaRedefinidaEm": firestore.SERVER_TIMESTAMP,
    })


def definir_ativo(usuario: str, ativo: bool, operador: str) -> None:
    """Ativa/desativa um operador, com proteções contra perder o acesso ao painel:
    ninguém se desativa a si próprio nem desativa o último administrador ativo."""
    if not ativo:
        if usuario == operador:
            raise ValueError("Você não pode desativar o próprio usuário.")
        ops = listar()
        alvo = next((o for o in ops if o["usuario"] == usuario), None)
        admins_ativos = [o for o in ops if o["admin"] and o["ativo"]]
        if alvo and alvo["admin"] and len(admins_ativos) <= 1:
            raise ValueError("Não é possível desativar o último administrador ativo.")
    _col().document(usuario).update({"ativo": ativo})


def listar() -> list:
    """Lista os operadores (sem expor o hash da senha)."""
    out = []
    for doc in _col().stream():
        d = doc.to_dict() or {}
        out.append({
            "usuario": doc.id,
            "admin": bool(d.get("admin")),
            "ativo": d.get("ativo", True),
            "precisaTrocar": bool(d.get("precisaTrocar")),
        })
    return out


def eh_admin(usuario: str) -> bool:
    snap = _col().document(usuario).get()
    return bool(snap.exists and (snap.to_dict() or {}).get("admin"))
