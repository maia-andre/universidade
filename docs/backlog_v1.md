# Backlog V1 — Branch `new_features`

Data: 13 de junho de 2026
Status: planejamento aprovado — execução incremental, item 1 → 4.

> **Contexto crítico:** a branch `main` já está com **beta ativo na Play Store** (teste interno/fechado). Existem usuários reais com progresso salvo no dispositivo. Toda esta branch (`new_features`) deve ser desenvolvida sem quebrar esses dados. A consequência prática mais importante está detalhada no Item 1 (migração não-destrutiva do Room).

Este documento detalha as 4 frentes de trabalho desta sessão, na ordem em que serão atacadas. Cada item traz: motivação, estado atual (com evidência no código), escopo proposto e critério de pronto.

---

## Item 1 — Sprint de Fundação (estabilização)

**Objetivo:** corrigir os débitos técnicos que ameaçam a base já publicada e que travam a evolução das próximas features. É a fundação sobre a qual os itens 2–4 serão construídos.

### 1.1 Migração não-destrutiva do Room 🔴 (pré-requisito de tudo)

**Estado atual:**
- `AppDatabase.kt` usa `.fallbackToDestructiveMigration()` e o banco foi versionado renomeando o arquivo (`universidade_database_v3`).
- Isso significa: **qualquer incremento de `version` do `@Database` apaga todo o progresso do usuário** (aulas concluídas, favoritos).

**Por que mudou:** isso era inofensivo no MVP (sem usuários). Agora, com beta ativo, precisamos adicionar colunas (ex.: persistência do quiz no Item 1.2) **sem destruir** o progresso dos testadores.

**Escopo:**
- Substituir `fallbackToDestructiveMigration()` por objetos `Migration(from, to)` explícitos.
- Estabelecer o padrão de migração versionada como regra do projeto daqui pra frente.
- Reativar `exportSchema = true` + configurar o diretório de schemas para diffs versionados (opcional, mas recomendado).

**Critério de pronto:** subir a versão do banco e instalar por cima de uma versão anterior preserva aulas concluídas e favoritos.

### 1.2 Persistência completa do Quiz 🔴 (urgente — herdado da V2)

**Estado atual (confirmado no código):**
- `AulaViewModel.kt:38` mantém `_quizState` como `MutableStateFlow` **apenas em memória**. Ao sair da aula e voltar, o ViewModel é recriado e o quiz volta em branco.
- `ProgressoEntity.kt` só guarda `isCompleted` e `isFavorite` — não há registro de respostas nem nota.
- Resultado: mesmo uma aula marcada como concluída reabre com o quiz vazio e editável, o que confunde o usuário e permite "refazer" algo já validado.

**Escopo:**
- Estender `ProgressoEntity` com os campos do quiz (respostas selecionadas serializadas, nota/acertos, flag de submissão). → depende do Item 1.1.
- No `CursoRepositoryImpl`, ler esse estado e remontá-lo no domínio.
- No `AulaViewModel`, ao abrir uma aula já concluída, restaurar as respostas e exibir o quiz em **modo somente-leitura** com gabarito (acertos/erros visíveis).
- Persistir a nota no momento da submissão (base para a prova final do Item 4).

**Critério de pronto:** responder o quiz, sair, voltar → o quiz aparece preenchido, travado e com o resultado visível.

### 1.3 Progresso da Home dinâmico 🟠

**Estado atual:** `HomeViewModel.kt:27` chama `getCursoDetailUseCase(1)` — **fixo no curso ID 1** ("Supervisores"). Qualquer outro curso ativo não aparece no card de progresso.

**Escopo:**
- Introduzir o conceito de "curso ativo" do aluno (por enquanto persistido localmente via DataStore; futuramente virá do perfil/admin na V3).
- Refatorar a Home para refletir o curso ativo, não um ID hardcoded.
- Tratar o estado "nenhum curso ativo ainda".

**Critério de pronto:** trocar o curso ativo muda o card de progresso da Home corretamente.

### 1.4 Limpeza de warnings de depreciação 🟡

**Estado atual (listado na V2):**
- `Icons.Filled.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack` (`CursoDetailScreen`, `CursosScreen`).
- `Divider` → `HorizontalDivider` (`CursoDetailScreen`).
- `statusBarColor` em `Theme.kt` (depreciado).
- `fallbackToDestructiveMigration()` (resolvido pelo Item 1.1).

**Critério de pronto:** build de release sem esses warnings.

---

## Item 2 — Features de UX visíveis

**Objetivo:** ganhos rápidos e perceptíveis para os testadores do beta, melhorando conforto e retenção. Baixo custo, alto valor percebido.

### 2.1 Tema claro/escuro com toggle manual
- Hoje o tema só segue o sistema (`Theme.kt`). Adicionar um interruptor manual nas configurações.
- Persistir a preferência com **DataStore** (mesma infra do "curso ativo" do Item 1.3).

### 2.2 "Continuar de onde parou"
- Card na Home apontando para a última aula aberta/não concluída do curso ativo.
- Requer registrar timestamp de último acesso por aula (mais um campo de progresso — aproveita a migração do Item 1.1).

### 2.3 Controle de tamanho de fonte no leitor (acessibilidade)
- Público-alvo é o servidor municipal (faixa etária ampla, classificação +18). Ajuste de fonte é acessibilidade real, não enfeite.
- Slider/botões A-/A+ no leitor de aula; preferência persistida.

### 2.4 Progresso por módulo e por curso
- Hoje só existe o percentual global. Adicionar barras de progresso por módulo (no `CursoDetailScreen`) e por curso (na lista de cursos).

### 2.5 Polish de navegação e estados
- Transições animadas entre telas (Navigation Compose).
- Estados de loading / vazio / erro consistentes e reutilizáveis (candidatos a `core/components`).

---

## Item 3 — Busca global funcional

**Objetivo:** ligar a `SearchScreen` (que já existe na UI mas está órfã) a uma busca real, reduzindo o tempo de navegação até o conhecimento.

**Estado atual:** a `SearchScreen` foi criada na V2, mas **não há ponto de acesso na navegação** e a busca não está conectada a nenhuma query.

**Escopo:**
- Adicionar ponto de acesso à busca (ícone na Home / barra superior) e registrar a rota no `NavHost`.
- Implementar busca local sobre cursos, módulos, aulas e **conteúdo textual** das aulas usando **FTS4** no Room (tabela virtual indexando o conteúdo já assado no banco).
- Resultados navegáveis direto para a aula correspondente, com destaque do termo.
- Tratar estados: vazio, sem resultados, digitando.

**Critério de pronto:** buscar termos como "inventário", "licitação", "patrimônio" retorna aulas relevantes e leva direto ao conteúdo.

---

## Item 4 — Prova final + certificado PDF offline

**Objetivo:** fechar o ciclo de aprendizado com avaliação final e emissão de certificado — **sem depender de login** (gerado localmente com o nome digitado pelo usuário).

**Estado atual:** `VerificarAprovacaoCursoUseCase.kt` existe mas é **mock** (`return true`). A `AvaliacaoScreen` (escala Likert) tem base de UI. A regra de negócio definida na V2: aprovação exige **nota ≥ 70% e 100% das aulas lidas**.

**Escopo:**
- Implementar de verdade o `VerificarAprovacaoCursoUseCase` lendo o progresso real (concluídas + notas dos quizzes persistidos no Item 1.2).
- Tela de prova final do curso (agregando questões / ou avaliação Likert dos módulos, a definir).
- Ao aprovar: capturar o nome do aluno e **gerar certificado em PDF offline** (via `PdfDocument`/Canvas nativo, com a identidade visual SJC — azul `#003882` e ouro `#FFD700`).
- Mover o curso aprovado para um estado "Concluído" (histórico, acesso vitalício) — alinhado à regra de "1 curso ativo por vez".
- Compartilhar/salvar o PDF.

**Critério de pronto:** concluir 100% de um curso + passar na prova → gerar e abrir/compartilhar um certificado em PDF com o nome do aluno.

---

## Notas de dependência entre itens

- **1.1 (migração)** é pré-requisito de 1.2, 2.2 e 2.3 (todos adicionam colunas de progresso).
- **1.2 (nota do quiz)** é pré-requisito de **Item 4** (cálculo de aprovação).
- **1.3 / 2.1 (DataStore)** compartilham a mesma infra de preferências locais.

## Fora de escopo nesta branch (fica para V3)

Autenticação (Firebase), banco centralizado, painel do administrador in-app, sync de conteúdo via API e dashboard gerencial — conforme já delimitado em `sugestões v2.md`. A camada `core/AuthRepository` permanece como contrato preparatório.
