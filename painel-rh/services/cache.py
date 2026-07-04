"""Camada de cache do painel — as páginas leem SEMPRE por aqui, nunca dos services direto.

O Streamlit re-executa o script inteiro a cada interação; sem cache, cada clique refaz
todas as leituras do Firestore. Aqui cada leitura ganha um TTL curto e uma função de
invalidação explícita — **regra fixa: toda ação que grava chama a `invalidar_*()`
correspondente na mesma sequência**, para o rerun seguinte já vir com dados frescos.
"""
import logging

import streamlit as st

from config import CURSOS_FALLBACK
from services import alunos, conteudo, matriculas, relatorios

log = logging.getLogger("painel.cache")

TTL_LISTAS = 60      # listagens operacionais (alunos, matrículas)
TTL_KPIS = 120       # contagens/cruzamentos de relatório
TTL_CATALOGO = 300   # catálogo publicado (muda raramente; botão de recarga manual)


@st.cache_data(ttl=TTL_LISTAS, show_spinner="Carregando alunos...")
def alunos_lista() -> list[dict]:
    return alunos.listar_alunos()


@st.cache_data(ttl=TTL_LISTAS, show_spinner="Carregando matrículas...")
def matriculas_lista() -> list[dict]:
    return matriculas.listar_matriculas()


@st.cache_data(ttl=TTL_KPIS, show_spinner="Calculando indicadores...")
def resumo_kpis() -> dict:
    return relatorios.resumo()


@st.cache_data(ttl=TTL_KPIS, show_spinner="Cruzando dados...")
def situacao_alunos() -> list[dict]:
    return relatorios.situacao_por_aluno()


@st.cache_data(ttl=TTL_KPIS, show_spinner="Carregando conclusões...")
def conclusoes_lista() -> list[dict]:
    return relatorios.listar_conclusoes()


@st.cache_data(ttl=TTL_CATALOGO, show_spinner="Carregando catálogo de cursos...")
def catalogo_cursos() -> dict[int, str]:
    """{cursoId → título} do conteúdo publicado — inclui cursos criados pelo painel.

    Fallback para o dict estático só quando o Firestore E o bootstrap dos assets
    falham (ex.: painel em produção sem rede), para Matrículas nunca quebrar.
    """
    try:
        mapa = {int(c["id"]): str(c.get("titulo") or c["id"])
                for c in conteudo.carregar()["cursos"]}
        if mapa:
            return mapa
    except Exception:  # noqa: BLE001 — fallback consciente, com log
        log.exception("Falha ao carregar o catálogo publicado; usando o fallback estático.")
    return dict(CURSOS_FALLBACK)


# --------------------------------------------------------------------------- invalidação

def invalidar_alunos() -> None:
    """Após criar/importar aluno ou redefinir senha."""
    alunos_lista.clear()
    situacao_alunos.clear()


def invalidar_matriculas() -> None:
    """Após liberar curso ou encerrar matrícula."""
    matriculas_lista.clear()
    resumo_kpis.clear()
    situacao_alunos.clear()


def invalidar_catalogo() -> None:
    """Após publicar conteúdo (a página Conteúdo DEVE chamar isto no publish)."""
    catalogo_cursos.clear()


def invalidar_tudo() -> None:
    """Botão "Atualizar dados" do cabeçalho."""
    st.cache_data.clear()
