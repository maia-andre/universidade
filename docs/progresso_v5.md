# Relatório de Progresso — Versão 5 (Branch `features_v5`)

Data: 16 de junho de 2026
Status: Issue #3 (refinamento de UI/UX + prova final) — itens desta fase concluídos.

Branch criada a partir da `main` (já com V3 e V4 mergeadas). Trabalho disparado pelo **issue #3**, aberto após os testes manuais da V3/V4. Execução incremental, build + testes unitários verdes ao final. Convenção de commits herdada da v3/v4.

---

## 1. O Que Foi Entregue

### Correções visuais e de acessibilidade (issue #3, itens 1 e 4)
- **Legibilidade no tema claro (bug principal):** cinco telas (Home, Configurações, Busca, Aula, Detalhe do Curso) escolhiam a cor dos cards via `isSystemInDarkTheme()` — que reflete o tema do **aparelho**, não o `ThemeMode` escolhido no app. Com o aparelho no escuro e "Tema Claro" no app, o card ficava escuro com texto escuro (ilegível); o caso espelhado (branco no tema escuro) também ocorria. Agora usam `MaterialTheme.colorScheme.surface`, seguindo o esquema já resolvido no `MainActivity` (claro/escuro/alto contraste). Constante `CardDarkBg` e imports órfãos removidos.
- **Acessibilidade:** os alvos de toque do Likert da avaliação subiram de 44dp para 48dp. Com o tema corrigido, as ferramentas de acessibilidade (contraste, movimento) voltam a ser testáveis — o item 4 do issue estava bloqueado por este bug.

### Prova Final de verdade (issue #3, item 3a)
- Antes, "concluir o curso" era apenas acertar o quiz de cada aula, e o certificado era liberado só por conclusão — sem avaliação final. Agora o curso tem uma **prova final** que **gate** a emissão do certificado.
- **Fluxo:** 100% das aulas → "Fazer prova final" → aprovado (≥70%) → "Emitir Certificado". `VerificarAprovacaoCursoUseCase` passa a exigir a aprovação na prova; a nota do certificado reflete a prova final.
- **Conteúdo nos assets:** perguntas em `curso_data.json` (campo `provaFinal`), lidas em **runtime** — chegam a qualquer instalação sem re-seed. 6 questões no Curso de Supervisores; 3 em cada curso menor.
- **Persistência:** resultado por curso na nova tabela `prova_final_resultado` (**migração v6→v7**, aditiva). DB na versão 7, schema exportado.
- **UI:** `ProvaFinalScreen`/`ViewModel` + rota `ProvaFinal(cursoId)`, com correção daltônico-segura (ícones ✓/✗) e "Tentar novamente". `ProvaFinalRepository` dedicado + use cases.

---

## 2. Decisões Técnicas

- **Perguntas nos assets, resultado no Room.** Como o seed do Room só roda na criação do banco, conteúdo novo "assado" não chegaria a instalações existentes. Lendo as perguntas dos assets em runtime, elas chegam a todos; só o estado do aluno (resultado/tentativas) é migrado.
- **Migração v6→v7 aditiva** (apenas cria a tabela de resultado), seguindo a regra do projeto de nunca destruir progresso em upgrade. Mantida mesmo sem progresso real de usuários ainda, porque a produção real está próxima.
- **Aprovação = 100% das aulas + prova final.** Antes a aprovação derivava trivialmente dos quizzes por aula (cada aula só conclui com 100% de acerto), então não havia barreira real — exatamente o "fui direto ao certificado" relatado.
- **Repositório dedicado** (`ProvaFinalRepository`), como em Ferramentas, para não inchar o `CursoRepository`.
- **Tema centralizado.** A causa-raiz era telas decidindo a cor por conta própria via `isSystemInDarkTheme()` em vez de confiar no `MaterialTheme.colorScheme` já resolvido. Corrigir na origem conserta claro/escuro e alto contraste de uma vez.

---

## 3. Adiado para a fase de Login/Firebase (issue #3, itens 2 e 3b)

1. **Curso ativo único + acesso aos concluídos** (item 2): depende de perfil/autenticação.
2. **Certificado com dados de cadastro do servidor** (item 3b): hoje o certificado usa o nome digitado manualmente (paliativo); a integração com o cadastro vem com o Firebase.

---

## 4. Verificação

- **Build de debug + testes unitários (JVM) verdes** (`assembleDebug` + `testDebugUnitTest`), após rebuild limpo — a build incremental tropeçou numa falha transitória do daemon Kotlin (AGP 9 + K2).
- `VerificarAprovacaoCursoUseCaseTest` reescrito para a nova regra (mocka `ProvaFinalRepository`).
- `MigracaoRoomTest` estendido para **v3→v7**; rodar em device/emulador com `connectedAndroidTest` (instrumentado).
- **Smoke test manual** recomendado: telas novas (prova final, fluxo até o certificado) e re-teste das ferramentas de acessibilidade no tema claro.

---

## 5. Lições Aprendidas V5

- Bugs de "claro/escuro" quase sempre vêm de telas decidindo cor por conta própria (`isSystemInDarkTheme()`) em vez de confiar no `colorScheme` do tema — centralizar evita a classe inteira de bugs.
- Para conteúdo que precisa chegar a instalações já publicadas, ler dos assets em runtime é mais robusto que depender do seed do banco.
- Builds incrementais no toolchain bleeding-edge (AGP 9 + K2) podem corromper a resolução de símbolos após falha do daemon; um rebuild limpo é o reset confiável.
