# Relatório de Progresso — Versão 4 (Branch `features_v4`)

Data: 14 de junho de 2026
Status: Itens 1–4 do `backlog_v2.md` concluídos.

Branch empilhada sobre a v3 (`new_features`, PR #1). PR da v4: #2 (base `new_features`). Planejamento em `docs/backlog_v2.md`. Execução incremental, um commit por sub-item, com build/`test` verde a cada etapa. Convenção de commits herdada da v3.

---

## 1. O Que Foi Entregue

### Item 4 — Testes Automatizados (primeira rede de segurança)
- Infra: `kotlinx-coroutines-test`, `turbine`, `mockk`, `room-testing`; schemas exportados expostos como assets de `androidTest`.
- Unitários (JVM): `VerificarAprovacaoCursoUseCase` (regra de aprovação), `CursoRepositoryImpl.buscar` (guarda/trecho), `CalcularEstatisticasCursoUseCase` (agregação por módulo).
- Instrumentado: `MigracaoRoomTest` valida **v3→v4→v5→v6** preservando o progresso (executar com `connectedAndroidTest`).

### Item 1 — Central de Acessibilidade
- Tela dedicada (a partir de Configurações), tudo persistido no DataStore.
- **Fonte global responsiva**: `fontScale` agora escala todo o app via `LocalDensity` no `MainActivity` (removido o wrapper duplicado do leitor).
- **Alto contraste**: esquemas de cor dedicados (claro/escuro).
- **Redução de movimento**: desliga transições de tela e a engrenagem do splash.
- **Quiz daltônico-seguro**: correção também por ícone ✓/✗, não só por cor.

### Item 3 — Estatísticas + Avaliação
- **Dashboard "Meu Desempenho"**: conclusão e aproveitamento por módulo e no total do curso ativo, com destaques (mais forte / a reforçar). Acesso por card na Home.
- **Avaliação Likert**: integra a `AvaliacaoScreen` (antes mock) à tabela `avaliacoes` já existente (**sem migração**). 5 perguntas Likert + 2 campos de texto, com prefill/atualização; acesso por "Avaliar curso" no curso concluído.

### Item 2 — Ferramentas Práticas
- Tabela `ferramentas` (**migração v5→v6**, aditiva) + `FerramentaRepository` (CRUD, campos serializados em JSON).
- **Matriz SWOT** e **5W2H** com editor de campos dinâmicos por tipo; criar, salvar, reabrir, excluir.
- **Exportação em PDF** (reaproveita o `FileProvider`/padrão do certificado). Acesso por card na Home.

---

## 2. Decisões Técnicas

- **Migração v5→v6** apenas cria uma tabela nova (não toca dados existentes), seguindo a regra do projeto de nunca destruir progresso com beta ativo. Schemas `3..6.json` versionados.
- **Avaliação sem migração**: a tabela `avaliacoes` já existia desde a V2; bastou ligar o `AvaliacaoDao` ao repositório e construir a UI.
- **Fonte global** centralizada no `MainActivity` via `LocalDensity` (escala sp, preserva dp), cobrindo inclusive bibliotecas de terceiros (Markdown).
- **Repositório dedicado** para ferramentas (`FerramentaRepository`), evitando inchar o `CursoRepository`.
- **Cultura de testes**: novas regras de negócio (estatísticas, aprovação, busca) acompanhadas de testes unitários.

---

## 3. Débitos e Pendências

1. **Smoke test em dispositivo**: validado por build + `./gradlew test` + lint; falta teste manual das telas novas (Acessibilidade, Desempenho, Avaliação, Ferramentas) e dos fluxos de PDF (certificado e ferramentas) em emulador/aparelho.
2. **Histórico de tentativas de quiz** (Item 3.2): adiado para evitar mais uma migração; o dashboard usa o estado atual. Fica como evolução.
3. **TTS de aulas** (Item 1.6, opcional): não implementado nesta fase.
4. **FTS4** na busca permanece otimização futura.

---

## 4. Próximos Passos

1. Revisão manual da v3 (PR #1) → merge na `main` → trocar a base do PR #2 (v4) para `main`.
2. Smoke test do que entrou na v4.
3. Fase de Autenticação/Firebase (papéis admin/aluno, curso ativo pelo perfil) — fora da v4, como combinado.

---

## 5. Lições Aprendidas V4

- Verificar o schema existente antes de planejar migração evita trabalho (a avaliação não precisou de migração).
- Centralizar acessibilidade (fonte/contraste/movimento) numa única tela + DataStore mantém o app coerente e fácil de evoluir.
- Testar as migrações com `MigrationTestHelper` contra os schemas exportados é a salvaguarda certa para um app já em produção.
