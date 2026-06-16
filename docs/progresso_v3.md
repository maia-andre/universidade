# Relatório de Progresso — Versão 3 (Branch `new_features`)

Data: 13 de junho de 2026
Status: Sprint de refinamento concluída (Itens 1–4 do `backlog_v1.md`)

Esta sessão foi conduzida em uma branch dedicada (`new_features`), separada da `main`, pois a `main` já está com **beta ativo na Play Store** (teste interno). Todo o trabalho foi planejado em `docs/backlog_v1.md` e executado de forma incremental, com um commit por sub-item e build de validação a cada etapa.

---

## 1. Contexto e Diretriz Central

Com usuários reais já instalados, a diretriz que guiou toda a sprint foi: **nunca destruir o progresso do usuário**. Isso mudou uma decisão herdada da V2 (uso de `fallbackToDestructiveMigration()` + renomear o banco), que apagava dados a cada incremento de versão. A partir desta branch, **toda alteração de schema exige migração versionada explícita**.

---

## 2. O Que Foi Entregue

### Item 1 — Sprint de Fundação (estabilização)

1. **Migração não-destrutiva do Room (1.1):**
   - Removido `fallbackToDestructiveMigration()`; adotado `addMigrations(*ALL_MIGRATIONS)` + destrutivo apenas em downgrade.
   - `exportSchema = true` com schemas versionados em `app/schemas/` (baseline 3.json).
   - Arquivo `Migrations.kt` como ponto único, com a regra do projeto documentada.

2. **Persistência completa do Quiz (1.2):**
   - Resolvido o débito da V2: o quiz voltava em branco ao reabrir a aula.
   - `ProgressoEntity` ganhou `quizSubmitted`, `quizAcertos`, `quizRespostasJson` (migração **v3→v4**).
   - `AulaViewModel` restaura o estado salvo: o quiz reabre preenchido, travado e com o gabarito visível. A nota (`quizAcertos`) alimenta a prova final do Item 4.

3. **Progresso da Home dinâmico (1.3):**
   - Removido o `getCursoDetailUseCase(1)` fixo.
   - Introduzido o conceito de **curso ativo** via DataStore (`UserPreferencesRepository`). Entrar em um curso o define como ativo (regra de 1 curso por vez).
   - Home reflete o curso ativo e trata o estado "nenhum curso iniciado".

4. **Limpeza de depreciações (1.4):**
   - `ArrowBack` → `AutoMirrored`, `Divider` → `HorizontalDivider`, remoção de `statusBarColor` (edge-to-edge cuida das barras). Build sem warnings.

### Item 2 — Features de UX

- **2.1 Tema claro/escuro manual:** nova tela de **Configurações** com seletor (Sistema/Claro/Escuro), aplicado na raiz via `MainViewModel` e persistido no DataStore.
- **2.2 Continuar de onde parou:** `ProgressoEntity.ultimoAcessoEm` (migração **v4→v5**); card na Home apontando para a última aula acessada e não concluída do curso ativo.
- **2.3 Tamanho de fonte no leitor (acessibilidade):** botões A−/A+ que escalam toda a tipografia (inclusive o Markdown) via `LocalDensity`, mantendo o layout em `dp`; persistido (0.85x–1.5x).
- **2.4 Progresso por curso e módulo:** card de progresso do curso no detalhe, barra por módulo e percentual/contagem em cada card da lista de cursos.
- **2.5 Polish:** transições slide+fade entre telas no `NavHost`; componentes reutilizáveis `LoadingBox`/`EmptyMessage` em `core/components`.

### Item 3 — Busca Global Funcional

- A `SearchScreen` (antes órfã) foi ligada a uma busca real: `SearchDao` com JOIN cursos→módulos→aulas usando `LIKE`, restrita a cursos disponíveis, com debounce de 300 ms e trecho do conteúdo ao redor do termo.
- Ponto de acesso (ícone na Home) + rota; resultados navegam direto para a aula.
- **Decisão técnica:** optou-se por `LIKE` em vez de FTS4 — o catálogo é pequeno, não exige migração nem re-seed (funciona já para os betas) e evita manter uma tabela FTS sincronizada. FTS4 fica como otimização futura.

### Item 4 — Prova Final + Certificado PDF

- `VerificarAprovacaoCursoUseCase` **implementado de verdade** (antes era mock `return true`): aprovado quando 100% das aulas concluídas **e** aproveitamento ≥ 70% (`Constants.MINIMUM_PASSING_SCORE`).
- `CertificadoPdfGenerator`: gera um PDF A4 paisagem via `Canvas` nativo com a identidade SJC (azul/ouro), nome do aluno, curso, aproveitamento e data.
- `CertificadoScreen`: valida a aprovação, captura o nome e gera/compartilha o PDF (via `FileProvider`).
- Botão "Emitir Certificado" no detalhe do curso quando ele é 100% concluído.

---

## 3. Decisões Técnicas e Arquitetura

- **Migrações versionadas como regra:** duas migrações reais nesta sprint (v3→v4 e v4→v5), ambas aditivas (`ALTER TABLE ... ADD COLUMN`), preservando o progresso dos betas.
- **DataStore para preferências locais:** `UserPreferencesRepository` centraliza curso ativo, tema e escala de fonte — fora do banco de conteúdo.
- **Reaproveitamento da arquitetura existente:** todas as features novas seguiram o padrão UseCase → Repository → Room/DataStore e `@HiltViewModel` + `StateFlow`, sem quebrar a Clean Architecture.
- **Acessibilidade via `LocalDensity`:** escala a tipografia sem mexer no layout, e cobre bibliotecas de terceiros (renderizador de Markdown) automaticamente.

---

## 4. Débitos Técnicos e Pendências

1. **Smoke test em dispositivo pendente:** a sprint foi validada por **compilação e lint** (build de debug verde, zero warnings em todas as etapas). As telas novas (Configurações, Busca, Certificado) e a geração/compartilhamento do PDF ainda **não foram testadas manualmente em emulador/dispositivo** — recomenda-se um teste visual antes de promover para a `main`.
2. **Curso ativo é heurístico:** hoje "entrar no curso = curso ativo". Com a chegada de autenticação/admin (V3), isso passará a ser controlado pelo perfil do aluno.
3. **`AvaliacaoScreen` (Likert) ainda não integrada:** permanece como base; pode ser incorporada à prova final futuramente.
4. **FTS4** como otimização de busca para quando o volume de conteúdo crescer.

---

## 5. Próximos Passos Sugeridos

1. **Central de Acessibilidade** (planejada em `backlog_v1.md`): fonte global responsiva, modo daltônico/alto contraste, suporte a TalkBack, redução de movimento, TTS e fonte para dislexia.
2. **V3 — Autenticação e Gestão (Firebase):** transição dos mocks de `AuthRepository` para Firebase/Firestore, painel do administrador in-app e a regra de "1 curso ativo por vez" controlada pelo perfil.
3. **Sincronização de conteúdo** (`version.json` + API) para atualizar cursos sem republicar o APK.
4. **Suíte de testes automatizados** (hoje inexistente): use cases de progresso/aprovação e migrações do Room são bons primeiros alvos.

---

## 6. Lições Aprendidas V3

- Com produto em produção, a estratégia de migração deixa de ser detalhe e vira **pré-requisito** de qualquer mudança de dados.
- Persistir estado de UI (quiz, último acesso) exige planejar o schema antes — agrupar campos numa única migração evita idas e vindas.
- `LocalDensity` é a alavanca certa para acessibilidade tipográfica em Compose.
- Builds com o toolchain AGP 9 são caras (até ~20 min em cache frio); validar em lotes coesos e usar o `Configuration Cache` foi essencial para a produtividade.
