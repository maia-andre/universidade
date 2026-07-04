"""Camada de cache do painel — as páginas leem SEMPRE por aqui, nunca dos services direto.

O Streamlit re-executa o script inteiro a cada interação; sem cache, cada clique refaz
todas as leituras do Firestore. Aqui cada leitura ganha um TTL curto e uma função de
invalidação explícita — **regra fixa: toda ação que grava chama a `invalidar_*()`
correspondente na mesma sequência**, para o rerun seguinte já vir com dados frescos.
"""
import streamlit as st

from services import alunos, matriculas, relatorios

TTL_LISTAS = 60      # listagens operacionais (alunos, matrículas)
TTL_KPIS = 120       # contagens/cruzamentos de relatório


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


def invalidar_tudo() -> None:
    """Botão "Atualizar dados" do cabeçalho."""
    st.cache_data.clear()
