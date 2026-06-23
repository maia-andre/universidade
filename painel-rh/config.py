"""Configurações do Painel RH — Universidade do Servidor.

Valores sensíveis (caminho da chave, senha do painel) vêm de variáveis de ambiente
em produção; os defaults aqui servem só para desenvolvimento local.
"""
import os

_BASE = os.path.dirname(__file__)

# Caminho da chave de service account (Admin SDK). Em produção, fica na pasta
# protegida da rede interna e é apontada pela env UNISERVIDOR_SA_KEY.
SERVICE_ACCOUNT_PATH = os.environ.get(
    "UNISERVIDOR_SA_KEY",
    os.path.join(_BASE, "secrets", "service-account.json"),
)

# Operadores do painel — SEED ÚNICO (V8 Item 2.2).
# A autenticação real agora vive na coleção Firestore `operadores` (senha em hash PBKDF2; ver
# services/operadores.py). Este dict é usado **uma única vez** por `operadores.garantir_seed()`
# para criar os operadores iniciais quando a coleção está vazia — com `precisaTrocar=True`, então
# a senha abaixo só vale para o 1º acesso e é trocada na hora. Pode ser esvaziado após a migração.
OPERADORES = {
    "andre": "admin",
}

# Coleções do Firestore — espelham docs/arquitetura_plataforma_v6.md §4.2
COL_SERVIDORES = "servidores"
COL_MATRICULAS = "matriculas"
COL_CONCLUSOES = "conclusoes"

# Conteúdo dinâmico (V8 Item 1): catálogo publicado em config/conteudo (doc único, campo `json`
# string versionado por `versao`). O app lê em runtime e atualiza sem novo APK.
COL_CONFIG = "config"
DOC_CONTEUDO = "conteudo"

# Catálogo de cursos — espelha app/src/main/assets/curso_data.json.
# Enquanto o conteúdo vive nos assets do APK, este catálogo é estático.
# Quando o conteúdo virar dinâmico (fase futura), virá do Firestore.
CURSOS = {
    1: "Curso de Supervisores",
    2: "Gestão de Patrimônio",
    3: "Noções de Licitação",
    4: "Almoxarifado e Suprimentos",
}
