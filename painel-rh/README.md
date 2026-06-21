# Painel RH — Universidade do Servidor

Ferramenta local (Streamlit + Firebase Admin SDK) para a **equipe de treinamento** gerir
o acesso aos cursos e acompanhar conclusões. É o lado privilegiado da plataforma descrita em
[`../docs/arquitetura_plataforma_v6.md`](../docs/arquitetura_plataforma_v6.md).

Fluxo de mão dupla:
- **Push** (RH → Firestore → App): cadastra o servidor (cria a conta no Auth + perfil) e
  **libera um curso** → o app lê a matrícula e define o **curso ativo**.
- **Pull** (App → Firestore → RH): o app grava as conclusões → o painel monta os relatórios.

> ⚠️ Este é um **scaffold** funcional. Os fluxos centrais já operam contra o Firestore real;
> faltam polimentos (validações, paginação, per-operador). Veja "Status" no fim.

## Estrutura

```
painel-rh/
├── app.py                 # entry do Streamlit (home/dashboard)
├── pages/                 # Alunos, Matrículas, Relatórios
├── services/              # alunos / matriculas / relatorios (lógica de Firestore + Auth)
├── firebase_client.py     # init do Admin SDK (lê a chave de service account)
├── auth_operador.py       # login simples do operador (auditoria)
├── config.py              # caminho da chave, senha do painel, coleções, catálogo de cursos
├── .streamlit/config.toml # tema SJC
├── run.bat                # sobe o painel (chamado pelo atalho)
├── instalar_atalho.bat    # cria o atalho na área de trabalho do operador
└── modelo_importacao.csv  # modelo da planilha de importação de alunos
```

## Pré-requisitos

- **Python 3.10+** (na máquina do operador ou um Python portátil na pasta da rede).
- Dependências: `pip install -r requirements.txt`.

## A chave de service account (Admin SDK) 🔑

O painel **não** usa o `google-services.json` do app — ele usa a **chave de service account**:

1. Firebase Console → ⚙️ **Configurações do projeto** → **Contas de serviço** → **Gerar nova chave privada**.
2. Salve o JSON como `painel-rh/secrets/service-account.json` **(nunca versionar — já está no `.gitignore`)**,
   ou aponte o caminho pela variável de ambiente `UNISERVIDOR_SA_KEY`.

> A chave dá **acesso total** ao projeto (ignora as Security Rules). Em produção ela vive
> **só na pasta protegida da rede interna** (oculta, escrita bloqueada, permissão pela equipe
> de treinamento) — uma cópia, nunca espalhada pelas máquinas.

## Rodar (desenvolvimento)

```bash
cd painel-rh
pip install -r requirements.txt
export UNISERVIDOR_PANEL_PASSWORD="uma-senha-forte"   # opcional; default é fraco
streamlit run app.py
```

Abre em `http://localhost:8501`. Login: seu nome + a senha do painel.

## Deploy na rede interna (padrão da equipe)

1. Copie esta pasta para o compartilhamento **protegido** da rede (oculto, somente leitura/execução
   para o grupo da equipe de treinamento), junto com a `secrets/service-account.json`.
2. Em cada máquina (gestora + supervisor), rode `instalar_atalho.bat` **uma vez** — cria o atalho
   "Painel RH" na área de trabalho (use `painel.ico` para o ícone personalizado, se quiser).
3. O operador clica no atalho → `run.bat` sobe o Streamlit → abre o painel no navegador.

## Segurança & LGPD

- A chave de service account é o ativo mais sensível — proteja pela rede; nunca no git.
- Há **dados pessoais de servidores** (nome, matrícula, e-mail, lotação) → tratamento sob a LGPD;
  rever `../docs/politica_de_privacidade.md` junto da fase de autenticação.
- O nome do operador é registrado em `criadoPor`/`liberadoPor` (rastreabilidade).
- Senha do painel: defina `UNISERVIDOR_PANEL_PASSWORD` (não use o default).

## Modelo de dados (Firestore)

| Coleção | Doc | Campos |
|---|---|---|
| `servidores` | `{uid}` | nome, email, matricula, lotacao, role, criadoEm, criadoPor |
| `matriculas` | `{uid}_{cursoId}` | uid, cursoId, cursoTitulo, status, liberadoPor, liberadoEm |
| `conclusoes` | `{uid}_{cursoId}` | uid, cursoId, nota, concluidoEm, certificadoId *(gravado pelo app)* |

## Status (scaffold) e próximos passos

- ✅ Login do operador, cadastro de aluno (manual + planilha), liberar curso, relatórios/KPIs.
- ⬜ Forçar troca de senha no 1º acesso; reset de senha.
- ⬜ Per-operador (em vez de senha compartilhada) + papéis.
- ⬜ Página de **Conteúdo** (quando o conteúdo dos cursos virar dinâmico).
- ⬜ Catálogo de cursos vindo do Firestore (hoje é estático em `config.py`, espelhando os assets).
