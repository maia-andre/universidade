"""Tratamento padronizado de erros do painel.

Uso: `with erros.protegido("criar aluno"): ...` — exceções viram mensagem amigável em
português na tela (`st.error`), e o traceback completo vai para o terminal do painel.
`ValueError` é tratado como mensagem de negócio (os services já levantam em português)
e não gera log de exceção.
"""
import contextlib
import logging

import requests.exceptions
import streamlit as st
from firebase_admin import auth as fb_auth
from firebase_admin import exceptions as fb_exceptions
from google.api_core import exceptions as gcp_exceptions
from google.auth import exceptions as gauth_exceptions

from firebase_client import ChaveAusenteError

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("painel")

_ERROS_DE_REDE = (
    gcp_exceptions.ServiceUnavailable,
    gcp_exceptions.DeadlineExceeded,
    gcp_exceptions.RetryError,
    gauth_exceptions.TransportError,
    requests.exceptions.ConnectionError,
)


def mensagem_amigavel(e: Exception) -> str:
    """Traduz exceções conhecidas para uma mensagem exibível ao operador."""
    if isinstance(e, ValueError):
        return str(e)
    if isinstance(e, fb_auth.EmailAlreadyExistsError):
        return "Já existe uma conta com este e-mail."
    if isinstance(e, gcp_exceptions.PermissionDenied):
        return "A chave de service account não tem permissão para esta operação."
    if isinstance(e, _ERROS_DE_REDE):
        return "Sem conexão com o Firebase. Verifique a rede e tente novamente."
    if isinstance(e, fb_exceptions.FirebaseError):
        return f"Erro do Firebase ({e.code}). Detalhes no terminal do painel."
    return "Erro inesperado. Detalhes registrados no terminal do painel."


@contextlib.contextmanager
def protegido(contexto: str = "", parar: bool = False):
    """Executa o bloco traduzindo erros; `parar=True` interrompe a página após o erro."""
    try:
        yield
    except ChaveAusenteError as e:
        st.warning(str(e))
        st.stop()
    except Exception as e:  # noqa: BLE001 — fronteira única de tradução de erros
        # st.stop()/st.rerun() sinalizam controle de fluxo via exceção — nunca engolir.
        if type(e).__module__.startswith("streamlit"):
            raise
        if not isinstance(e, ValueError):
            log.exception("Falha em %s", contexto or "operação do painel")
        st.error(mensagem_amigavel(e))
        if parar:
            st.stop()
