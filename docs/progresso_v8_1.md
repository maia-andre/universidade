# Relatório de Progresso — V8.1: Reforma do Painel RH (sessão 2026-07-04)

Data: 4 de julho de 2026
Status: **concluída** (11 fases, commits F1–F11 na `features_v8`). Plano aprovado pelo dev
antes da execução; contrato painel↔app verificado nos fontes e preservado.

> **Adendo (mesma data, pós-teste do dev):** dois bugs do editor corrigidos —
> (1) adicionar pergunta/aula purgava as keys de navegação e jogava a tela de volta ao
> curso 1 (o rascunho do curso novo ficou intacto); (2) seleção de aula com key
> compartilhada entre módulos. Agora a navegação usa keys por contexto e criar item
> navega para ele. Cobertos por **testes de UI (streamlit.testing.AppTest)** com
> services mockados. Sidebar: home renomeada para "Início" via `st.navigation`; brasão
> da sidebar sobre badge branco arredondado. **Teste fim-a-fim real executado:** curso 6
> completo publicado (v2), backfill da v1 no histórico, rollback pelo histórico (v3 = v1)
> e rascunho do operador (curso 5) preservado/devolvido com base v3.

Motivação: o operador perdeu edições de conteúdo duas vezes (o rascunho antigo descartava
texto digitado ao navegar/publicar) e a V8 Item 1.3 (completar cursos) dependia de um
painel confiável. A reforma cobre o painel inteiro + 1 correção no app.

---

## 1. O que foi entregue

### Correção crítica no app (F2) — "versão queimada"
`SincronizarConteudoUseCase` gravava a versão local **mesmo quando o catálogo remoto era
inválido** — o payload era descartado e aquela versão nunca mais era tentada.
`aplicarConteudoRemoto` agora retorna `Boolean` e a versão só é gravada quando o catálogo
foi aplicado. Teste unitário novo (4 casos); consertado também o
`CursoRepositoryImplBuscaTest` (quebrado desde 064d36a). **Chega aos usuários no próximo
release do APK** — até lá, a validação do painel é a proteção.

### Página Conteúdo reescrita (F7–F10)
- **Rascunho que não perde nada:** campos com keys por id + `on_change` (commit antes do
  rerun); autosave no Firestore (`rascunhos/conteudo`) — sobrevive a fechar o navegador;
  aviso de "rascunho em andamento por X" com **Assumir** (outro operador abre visualizando).
- **Editor estruturado de quiz/prova final:** sem JSON cru; opções em tabela com checkbox
  "Resposta correta" na linha (índice derivado — imune a desalinhamento).
- **Validação pré-publish** espelhando o contrato exato do parser kotlinx do app (campos
  obrigatórios, `respostaCorretaIndex` na faixa, IDs únicos, limite de 1 MiB) — erro
  bloqueia a publicação.
- **Publicar com diff + confirmação:** diálogo relê o publicado, mostra o que
  muda (➕/➖/✏️ por nível), exige checkbox quando remove conteúdo no ar (progresso órfão)
  e publica em **transação** com `versao_base` (publicação concorrente aborta com
  mensagem amigável).
- **Histórico + rollback:** cada publish copia a versão para
  `config/conteudo/historico/{v}`; restaurar publica o snapshot como versão nova.
- 16 testes pytest das funções puras (`painel-rh/tests/`).

### Plataforma do painel (F1, F3–F5)
- **Cache** (`services/cache.py`): todas as leituras com TTL + invalidação após escrita;
  KPIs por **aggregation count()** (acabaram os full scans a cada clique).
- **Catálogo dinâmico em Matrículas:** o selectbox vem do conteúdo publicado — curso novo
  criado no painel aparece para matrícula (pré-requisito do Item 1.3). `config.CURSOS`
  virou `CURSOS_FALLBACK`.
- **Validações:** email, matrícula duplicada, importação de planilha em 2 passos
  (dry-run com preview → confirmar) com **senha aleatória por aluno** e CSV de credenciais
  (nota LGPD: apagar após entrega). Fim do "mudar@123" hardcoded.
- **Confirmações destrutivas** (`ui.confirmar_acao`, st.dialog): encerrar matrícula,
  redefinir senhas, desativar operador.
- **Endurecimento:** `require_admin` re-checa no Firestore; ninguém desativa a si próprio
  nem o último admin; rate-limit de login (5/5min). Timeout de sessão descartado
  (decisão documentada no README).
- **Erros amigáveis** (`erros.py`): mensagens PT-BR na tela, traceback no terminal.

### Visual (F6)
Tema SJC completo (sidebar azul #003882 com dourado #FFD700), brasão na sidebar, logo no
login, cabeçalho padrão com "↻ Atualizar dados", home com cards de KPI e atalhos,
tabelas com rótulos PT e datas DD/MM/YYYY.

## 2. Verificação

- `pytest` (16 testes) e `py_compile` de todos os módulos: **verdes**.
- App Android: `testDebugUnitTest` e `assembleDebug` **verdes**.
- Contra o Firestore real (sem efeitos destrutivos): aggregation count OK; catálogo
  dinâmico OK; rascunho CRUD OK; transação de publish **aborta sem gravar** em conflito
  de versão; rate-limit e proteções de operador OK.
- Firestore rules: `rascunhos/` e `config/conteudo/historico/*` caem no deny-all
  (invisíveis ao app) — **nenhuma mudança de rules**.

## 3. Pendências (herdadas + novas)

1. **Smoke test fim-a-fim do conteúdo dinâmico** (pendência da V8): publicar uma edição
   pelo painel novo → app no device atualiza sem reinstalar; progresso preservado.
2. **Item 1.3** — completar os cursos esqueleto pelo painel (agora com editor decente).
3. **Item 2.1 — LGPD** — revisar a política com RH/jurídico.
4. **Backup seguro da upload key** (`.jks` + 3 credenciais).
5. **Próximo release do APK** leva o fix da versão queimada (F2).
6. Opcional: R8/minify; símbolos nativos na Play.
