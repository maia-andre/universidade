@echo off
REM Cria o atalho "Painel RH" na area de trabalho do operador, apontando para run.bat.
REM Rode UMA vez na maquina de cada operador (gestora / supervisor).
setlocal
set "ALVO=%~dp0run.bat"
set "ATALHO=%USERPROFILE%\Desktop\Painel RH - Universidade do Servidor.lnk"
set "ICONE=%~dp0painel.ico"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ws=New-Object -ComObject WScript.Shell; $l=$ws.CreateShortcut($env:ATALHO); $l.TargetPath=$env:ALVO; $l.WorkingDirectory='%~dp0'; if(Test-Path $env:ICONE){$l.IconLocation=$env:ICONE}; $l.Description='Painel RH - Universidade do Servidor'; $l.Save()"
echo Atalho criado na area de trabalho.
pause
