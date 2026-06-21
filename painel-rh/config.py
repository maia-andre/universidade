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

# Senha de acesso ao painel (equipe de treinamento). Sobrescreva por env em produção.
PANEL_PASSWORD = os.environ.get("UNISERVIDOR_PANEL_PASSWORD", "trocar-esta-senha")

# Coleções do Firestore — espelham docs/arquitetura_plataforma_v6.md §4.2
COL_SERVIDORES = "servidores"
COL_MATRICULAS = "matriculas"
COL_CONCLUSOES = "conclusoes"

# Catálogo de cursos — espelha app/src/main/assets/curso_data.json.
# Enquanto o conteúdo vive nos assets do APK, este catálogo é estático.
# Quando o conteúdo virar dinâmico (fase futura), virá do Firestore.
CURSOS = {
    1: "Curso de Supervisores",
    2: "Gestão de Patrimônio",
    3: "Noções de Licitação",
    4: "Almoxarifado e Suprimentos",
}
