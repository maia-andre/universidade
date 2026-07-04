# Painel RH — Universidade do Servidor

Ferramenta local (Streamlit + Firebase Admin SDK) para a **equipe de treinamento** gerir
o acesso aos cursos, **editar/publicar o conteúdo** e acompanhar conclusões. É o lado
privilegiado da plataforma descrita em
[`../docs/arquitetura_plataforma_v6.md`](../docs/arquitetura_plataforma_v6.md).

Fluxo de mão dupla:
- **Push** (RH → Firestore → App): cadastra o servidor (conta no Auth + perfil), **libera
  um curso** (o app lê a matrícula e define o curso ativo) e **publica o catálogo de
  conteúdo** (`config/conteudo`) — o app atualiza cursos/módulos/aulas **sem novo APK**.
- **Pull** (App → Firestore → RH): o app grava as conclusões → o painel monta os relatórios.

## Estrutura

```
painel-rh/
├── app.py                 # entry do Streamlit (home: KPIs + atalhos)
├── pages/                 # 1 Alunos · 2 Matrículas · 3 Relatórios · 4 Conteúdo · 5 Operadores
├── services/
│   ├── alunos.py          # Auth + perfil (validações de email/matrícula)
│   ├── matriculas.py      # liberar/encerrar (contrato com o app: {uid}_{cursoId}, status)
│   ├── relatorios.py      # KPIs (aggregation count) e cruzamentos
│   ├── conteudo.py        # catálogo publicado: validação de schema, diff, publish
│   │                      #   transacional, histórico de versões e rascunho persistente
│   ├── operadores.py      # autenticação do painel (hash PBKDF2, rate-limit)
│   ├── validacao.py       # email, senha temporária aleatória, dry-run de planilha
│   └── cache.py           # TODAS as leituras das páginas passam por aqui (TTL +
│                          #   invalidação explícita após cada escrita)
├── ui/                    # identidade visual SJC + diálogo de confirmação destrutiva
│   └── conteudo/          # página Conteúdo: estado do rascunho, editor, quiz,
│                          #   publicação (diff) e histórico (restauração)
├── erros.py               # tradução central de erros p/ PT-BR (traceback no terminal)
├── firebase_client.py     # init do Admin SDK (lê a chave de service account)
├── auth_operador.py       # login do operador + require_admin (re-checa no Firestore)
├── config.py              # caminho da chave, coleções, CURSOS_FALLBACK
├── assets/                # brasão + logo (embarcados; o deploy não depende de ../docs)
├── tests/                 # pytest das funções puras de conteúdo
├── .streamlit/config.toml # tema SJC (sidebar azul/dourado)
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
streamlit run app.py
```

Abre em `http://localhost:8501`. Login **por operador** (coleção `operadores`, senha em hash
PBKDF2): o seed inicial vem de `config.OPERADORES` na primeira autenticação, com troca de
senha obrigatória no 1º acesso. Administradores criam/resetam/desativam operadores na
página **Operadores**.

Testes das funções puras: `python -m pytest tests/ -q`.

## Deploy na rede interna (padrão da equipe)

1. Copie esta pasta para o compartilhamento **protegido** da rede (oculto, somente leitura/execução
   para o grupo da equipe de treinamento), junto com a `secrets/service-account.json`.
2. Em cada máquina, rode `instalar_atalho.bat` **uma vez** — cria o atalho "Painel RH".
3. O operador clica no atalho → `run.bat` sobe o Streamlit → abre o painel no navegador.

> Fora do repositório, o bootstrap do conteúdo a partir de `../app/src/main/assets` não
> existe — o catálogo vem do Firestore; `CURSOS_FALLBACK` (config.py) é o último recurso
> para a página Matrículas nunca quebrar.

## Conteúdo dinâmico (página Conteúdo)

- **Rascunho automático:** tudo que se digita é salvo no rascunho (sessão + doc
  `rascunhos/conteudo`) — sobrevive a fechar o navegador; outro operador vê o aviso
  "rascunho em andamento" e abre em modo visualização até **Assumir**.
- **Publicar** valida o catálogo contra o contrato exato do app (campos obrigatórios,
  resposta do quiz dentro da faixa, IDs únicos), mostra o **diff** vs a versão no ar e
  exige confirmação extra quando algo que está no ar seria **removido** (o progresso dos
  alunos nas aulas removidas fica órfão). A publicação é transacional e versionada.
- **Histórico:** cada publish guarda uma cópia em `config/conteudo/historico/{versao}`;
  restaurar = publicar o snapshot antigo como versão nova.
- ⚠️ IDs de curso/módulo/aula são **imutáveis** (progresso do aluno é keyed por `aulaId`).

## Segurança & LGPD

- A chave de service account é o ativo mais sensível — proteja pela rede; nunca no git.
- Há **dados pessoais de servidores** (nome, matrícula, e-mail, lotação) → tratamento sob a
  LGPD; ver `../docs/politica_de_privacidade.md`.
- **Senhas temporárias são geradas aleatórias** (uma por aluno/operador) e exibidas **uma
  única vez**; o CSV de credenciais da importação deve ser **apagado após a entrega** (LGPD).
- O nome do operador é registrado em `criadoPor`/`liberadoPor`/`publicadoPor` (rastreabilidade).
- Login com **rate-limit** (5 falhas/5 min, em memória do processo — zera no restart;
  suficiente para rede interna). **Sem timeout de sessão** por decisão: painel de rede
  interna, a sessão morre ao fechar a aba; timeout só adicionaria fricção.
- Proteções administrativas: ninguém desativa a si próprio nem o último admin ativo;
  a página Operadores re-checa o papel **no Firestore** a cada visita.

## Desempenho

- As páginas leem via `services/cache.py` (`st.cache_data`, TTL 60–300 s); toda escrita
  invalida o grupo correspondente, e o botão **"↻ Atualizar dados"** limpa tudo.
- KPIs usam **aggregation count()** do Firestore (o servidor conta; nada de full scan).
- Listagens têm teto (500/1000) com aviso visível quando atingido — paginação real fica
  para quando o volume justificar.

## Modelo de dados (Firestore)

| Coleção | Doc | Campos |
|---|---|---|
| `servidores` | `{uid}` | nome, email, matricula, lotacao, role, criadoEm, criadoPor |
| `matriculas` | `{uid}_{cursoId}` | uid, cursoId, cursoTitulo, status, liberadoPor, liberadoEm |
| `conclusoes` | `{uid}_{cursoId}` | uid, cursoId, nota, concluidoEm, certificadoId *(gravado pelo app)* |
| `config` | `conteudo` | versao (int monotônico), json (catálogo), publicadoPor, publicadoEm |
| `config/conteudo/historico` | `{versao:06d}` | versao, json, publicadoPor, publicadoEm, resumo |
| `rascunhos` | `conteudo` | json, baseVersao, editadoPor, iniciadoEm, atualizadoEm |
| `operadores` | `{usuario}` | senhaHash (PBKDF2), admin, ativo, precisaTrocar, criadoEm/Por |

As coleções `operadores`, `rascunhos` e a subcoleção `historico` caem no deny-all das
Security Rules — **invisíveis ao app cliente**; só o Admin SDK do painel as acessa.
