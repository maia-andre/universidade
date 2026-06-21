"""Inicialização do Firebase Admin SDK (privilegiado) e acesso a Auth/Firestore.

O Admin SDK **ignora as Security Rules** — por isso a chave fica na pasta protegida
da rede interna, e o acesso ao painel é restrito por login do operador.
"""
import os

import firebase_admin
from firebase_admin import auth, credentials, firestore

from config import SERVICE_ACCOUNT_PATH


class ChaveAusenteError(RuntimeError):
    """Levantada quando a chave de service account não está no lugar esperado."""


def _ensure_app() -> None:
    if firebase_admin._apps:  # Streamlit re-executa o script: inicializa só uma vez.
        return
    if not os.path.exists(SERVICE_ACCOUNT_PATH):
        raise ChaveAusenteError(
            "Chave de service account não encontrada em:\n"
            f"  {SERVICE_ACCOUNT_PATH}\n"
            "Gere em: Firebase Console → Configurações do projeto → Contas de serviço "
            "→ Gerar nova chave privada. Veja o README."
        )
    firebase_admin.initialize_app(credentials.Certificate(SERVICE_ACCOUNT_PATH))


def get_auth():
    _ensure_app()
    return auth


def get_db():
    _ensure_app()
    return firestore.client()
