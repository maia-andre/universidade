# Backlog V2 — Branch `features_v4`

Data: 14 de junho de 2026
Status: planejamento (escopo aprovado) — implementação ainda não iniciada.

> **Encadeamento:** esta branch sai de `new_features` (a v3, em PR #1 aguardando revisão). A v4 está **empilhada na v3** — depende dela, inclusive porque as migrações de banco são sequenciais. O PR da v4 deve usar **base `new_features`** enquanto a v3 não for mergeada; depois que a v3 entrar na `main`, a base do PR da v4 é trocada para `main`.
>
> **Continuidade de migrações:** a v3 chegou na versão **5** do Room (v3→v4→v5). Qualquer migração nova da v4 começa em **v5→v6** e segue a regra do projeto: **nunca destrutiva em upgrade** (beta ativo).
>
> **Fora de escopo (fase própria):** Autenticação/Firebase (login, papéis, curso ativo pelo perfil). Exige backend e LGPD; os contratos `AuthRepository`/`AnonymousAuthRepositoryImpl` permanecem intactos como preparação.

---

## Ordem recomendada

1. **Testes automatizados** (Item 4) — primeiro, para criar a rede de segurança antes de mexer no banco/UI.
2. **Central de Acessibilidade** (Item 1) — alto valor para o público-alvo, baixo acoplamento.
3. **Estatísticas + Avaliação** (Item 3) — aproveita dados/estruturas existentes.
4. **Ferramentas Práticas** (Item 2) — feature mais autocontida, fecha a v4.

A ordem é uma sugestão; cada item é independente o suficiente para ser reordenado.

---

## Item 1 — Central de Acessibilidade

**Objetivo:** consolidar a acessibilidade numa tela dedicada (a partir de Configurações), expandindo o controle de fonte do leitor (v3, Item 2.3) para o app inteiro e cobrindo daltonismo, leitor de tela e movimento. Atende diretamente o público servidor (faixa etária ampla, uso prolongado).

**Estado atual:** existe apenas o ajuste de fonte **local ao leitor** (`AulaScreen`, via `LocalDensity` + `UserPreferencesRepository.fontScale`). Não há tela de acessibilidade, nem tratamento de cor/movimento.

**Escopo:**
- **1.1 Tela "Acessibilidade"** acessível a partir de Configurações; todos os ajustes persistidos no DataStore (infra já existe).
- **1.2 Fonte global responsiva:** promover o `fontScale` para escala global do app (aplicar o `LocalDensity` no `MainActivity`, não só no leitor), garantindo layouts que não quebram em fontes grandes.
- **1.3 Modo daltônico / alto contraste:** não depender só de cor para informação. Concreto: no quiz, exibir ícones ✓/✗ e texto além do verde/vermelho; oferecer um tema de **alto contraste** como opção.
- **1.4 Auditoria de leitor de tela (TalkBack):** revisar `contentDescription` de ícones/imagens, ordem de foco e semântica; garantir alvos de toque ≥ 48dp.
- **1.5 Redução de movimento:** opção para desabilitar animações (engrenagem do splash, transições de tela do Item 2.5).
- **1.6 (opcional) Leitura em voz alta (TTS)** do conteúdo das aulas via `TextToSpeech` nativo.

**Dependências:** DataStore (pronto). Não exige migração de Room.

**Critério de pronto:** a tela "Acessibilidade" reúne os ajustes, todos persistidos e refletidos em todo o app; o quiz comunica acerto/erro sem depender só de cor.

---

## Item 2 — Ferramentas Práticas

**Objetivo:** adicionar um módulo de utilitários interativos para o aluno **aplicar na prática** os conceitos dos cursos (gestão/administração pública). Ideia originada na V2.

**Estado atual:** não existe. Há espaço natural na Home (abaixo do acesso aos cursos).

**Escopo:**
- **2.1 Card "Ferramentas"** na Home + tela de listagem das ferramentas disponíveis.
- **2.2 Matriz SWOT:** grade 2x2 editável (Forças/Fraquezas/Oportunidades/Ameaças).
- **2.3 5W2H:** formulário guiado (What/Why/Where/When/Who/How/How much).
- **2.4 Persistência das instâncias preenchidas:** o aluno pode salvar/retomar várias análises. → **nova tabela Room** (migração **v5→v6**) ou armazenamento em arquivos; a decidir na implementação.
- **2.5 Exportar/compartilhar:** reaproveitar o padrão de PDF + FileProvider do certificado (v3, Item 4) para exportar a ferramenta preenchida.

**Dependências:** se houver persistência em Room, migração v5→v6. Reaproveita `CertificadoPdfGenerator`/FileProvider como referência.

**Critério de pronto:** criar, salvar, reabrir e exportar pelo menos a SWOT e o 5W2H.

---

## Item 3 — Estatísticas + Avaliação

**Objetivo:** dar visibilidade ao desempenho do aluno e coletar a percepção dele sobre os módulos.

**Estado atual:**
- O progresso guarda o **último** resultado do quiz por aula (`quizAcertos`), mas não há histórico temporal de tentativas nem um painel consolidado.
- A `AvaliacaoScreen` (escala Likert) e `AvaliacaoEntity`/`AvaliacaoDao` existem como **base**, mas não estão integradas à navegação nem persistindo.

**Escopo:**
- **3.1 Dashboard de desempenho:** painel (na Home ou em tela própria) com aproveitamento por curso/módulo a partir do estado atual (reaproveita `VerificarAprovacaoCursoUseCase`/desempenho). Temas com maior/menor domínio.
- **3.2 (opcional) Histórico de tentativas:** para evolução ao longo do tempo, criar tabela de tentativas de quiz (migração **v5→v6**). Avaliar se vale o custo nesta fase.
- **3.3 Avaliação Likert dos módulos:** integrar a `AvaliacaoScreen` — ponto de acesso ao concluir um módulo/curso, persistindo a avaliação via `AvaliacaoDao`.

**Dependências:** 3.2 exige migração v5→v6 (coordenar numeração com o Item 2.4 se ambos criarem tabelas). 3.3 usa estruturas já existentes.

**Critério de pronto:** o aluno vê seu desempenho consolidado e consegue avaliar um módulo (Likert) com a resposta persistida.

---

## Item 4 — Testes Automatizados

**Objetivo:** criar a primeira rede de segurança real do projeto (hoje só há stubs gerados), priorizando regras de negócio e migrações — o que mais pode quebrar dados em produção.

**Estado atual:** sem testes reais. Dependências de teste no catálogo limitam-se a JUnit/Espresso padrão.

**Escopo:**
- **4.1 Infra de teste:** adicionar ao catálogo `kotlinx-coroutines-test`, `turbine` (asserção de Flows) e `mockk`; configurar `room-testing` para testes de migração instrumentados.
- **4.2 Testes de regra de negócio (unitários):**
  - `VerificarAprovacaoCursoUseCase` (aprovação por 100% + nota ≥ 70%).
  - `CursoRepositoryImpl`: cálculo de quiz (acertos), mappers, `salvarResultadoQuiz`/`resetarQuiz`, geração de trecho da busca.
  - Lógica de "curso ativo" e "continuar de onde parou".
- **4.3 Testes de migração do Room (instrumentados):** validar v3→v4→v5 preservando dados, com `MigrationTestHelper` e os schemas já exportados em `app/schemas/`.

**Dependências:** nenhuma (mas blinda os Itens 1–3).

**Critério de pronto:** `./gradlew test` cobre as regras de negócio principais; testes de migração verdes contra os schemas exportados.

---

## Riscos e Coordenação

- **Numeração de migração compartilhada:** se Item 2.4 e Item 3.2 criarem tabelas, coordenar para que as versões sejam sequenciais (v5→v6, v6→v7) e cada uma tenha sua `Migration`.
- **Impacto de correções da v3:** se a revisão da v3 (PR #1) gerar correções, elas entram na `new_features` e são trazidas para `features_v4` via merge/rebase; geramos um doc de "correções v3 → impacto v4" quando isso ocorrer.
- **Acessibilidade x animações:** o Item 1.5 (redução de movimento) deve respeitar as transições adicionadas no Item 2.5 da v3.
