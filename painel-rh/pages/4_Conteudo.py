"""Página Conteúdo (reforma V8.1) — editar e publicar o catálogo de cursos para o app.

O que for publicado chega ao app **sem novo APK** (sync do doc config/conteudo).
Tudo que o operador digita é salvo automaticamente no rascunho (local + Firestore) —
não existe mais "salvar no rascunho" manual, e navegar não perde nada.

⚠️ IDs de curso/módulo/aula são imutáveis (progresso do aluno é keyed por aulaId).
"""
import streamlit as st

import erros
import ui
from auth_operador import require_login
from ui.conteudo import editor, estado, historico, publicacao

ui.configurar_pagina("Conteúdo — Painel RH", "📚")
operador = require_login()
ui.pagina_header(
    "Conteúdo dos cursos", "📚",
    "Edite e **publique** o catálogo — o app atualiza no próximo login/sync, sem novo APK. "
    "As edições são salvas automaticamente no rascunho.",
    botao_atualizar=False,  # o estado do rascunho tem ciclo próprio (não limpar caches no meio)
)

with erros.protegido("carregar o conteúdo", parar=True):
    estado.inicializar(operador)

estado.persistir_rascunho(operador)  # flush de autosave adiado pelo throttle
estado.renderizar_banners(operador)
estado.renderizar_status(operador)

tab_editor, tab_pub, tab_hist = st.tabs(["✏️ Editor", "🚀 Publicar", "🕘 Histórico"])
with tab_editor:
    editor.render(operador)
with tab_pub:
    publicacao.render(operador)
with tab_hist:
    historico.render(operador)
