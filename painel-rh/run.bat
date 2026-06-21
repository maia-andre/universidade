@echo off
REM Sobe o Painel RH (Streamlit) a partir desta pasta. Chamado pelo atalho da area de trabalho.
REM Requer Python instalado (ou um Python portatil nesta mesma pasta protegida).
cd /d "%~dp0"
python -m streamlit run app.py
