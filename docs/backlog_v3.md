# Backlog V3 — Plataforma: Autenticação, Gestão (RH) e Refinamento

Data: 16 de junho de 2026
Status: planejamento (a iniciar após o merge da `features_v5`).

> **Onde estamos:** V3 (`new_features`) e V4 (`features_v4`) já estão na `main`; a V5 (`features_v5` — prova final + refinamento de UI do issue #3) entra em seguida. Este backlog organiza a **próxima fase**, a maior até aqui: a virada de um app offline-autônomo para uma **plataforma institucional com login, perfil e gestão pelo RH**. Próxima branch sugerida: **`features_v6`**.
>
> **Regra de banco mantida:** migrações sempre **aditivas e versionadas** (a partir de **v7→v8**), nunca destrutivas em upgrade. Hoje ainda não há progresso real de usuários (só o dev), mas a produção real está próxima — mantemos a disciplina.
>
> **LGPD:** a partir do momento em que houver login e dados pessoais de servidores (nome, matrícula, e-mail), entra em cena tratamento de dados pessoais. A `docs/politica_de_privacidade.md` precisará ser revisada junto com a fase de autenticação.

---

## Ordem recomendada

1. **Refinamento de UI/UX** (Item 1) — independente, alto valor percebido, pode começar já enquanto a infra de backend é decidida.
2. **Aumentar o catálogo de cursos** (Item 2) — independente de backend; valida de novo a arquitetura multi-cursos.
3. **Fundação de Autenticação + Backend** (Item 3) — o grande habilitador; destrava os itens 4–6.
4. **Perfil do aluno: curso ativo único + concluídos** (Item 4) — depende do Item 3.
5. **Gestão pelo RH (admin)** (Item 5) — depende do Item 3; começa como descoberta/PoC.
6. **Certificado institucional** (Item 6) — depende do Item 3 (nome do aluno logado) + parte visual independente.
7. **Itens herdados e oportunidades** (Item 7) — encaixar conforme couber.

A fase de backend (3) é grande o suficiente para, sozinha, ser uma ou mais branches. Itens 1, 2 e a parte visual do 6 podem ser entregues antes, sem esperar o login.

---

## Item 1 — Refinamento de UI/UX

**Objetivo:** subir o acabamento visual do app. A V5 deixou tudo funcional e legível (corrigiu a legibilidade do tema claro), mas ainda há espaço para refino — confirmado em teste manual.

**Escopo (a detalhar com o usuário):**
- **1.1 Polish geral de telas:** espaçamentos, hierarquia tipográfica, tons de superfície (avaliar definir os papéis `surfaceContainer*` nos esquemas para cards com elevação tonal coerente, em vez de só `surface`), consistência de ícones e estados.
- **1.2 Identidade visual mais forte:** uso mais consistente do azul/ouro SJC, do logo da Universidade do Servidor (engrenagem) e de ilustrações/empty states.
- **1.3 Microinterações:** respeitando a opção de **redução de movimento** já existente.
- **1.4 Revisão de acessibilidade visual:** revalidar alto contraste e alvos de toque em todas as telas (a V5 desbloqueou esse teste ao corrigir o tema).

**Critério de pronto:** revisão visual aprovada em aparelho, sem regressões de legibilidade/contraste.

---

## Item 2 — Aumentar o catálogo de cursos

**Objetivo:** sair de um curso completo (Supervisores) + três "esqueleto" (Patrimônio, Licitação, Almoxarifado, cada um com 1 módulo/1 aula) para um catálogo real, cumprindo o critério da V2 de **pelo menos 3 cursos completos** sem alterar código.

**Estado atual:** o conteúdo vive em `assets/curso_data.json` + Markdown por aula; a arquitetura multi-cursos já é genérica. Cada curso novo precisa de: módulos, aulas (`.md`), quizzes por aula e **prova final** (campo `provaFinal`, novidade da V5).

**Escopo:**
- **2.1 Completar** os cursos de Patrimônio, Licitação e Almoxarifado (módulos, aulas, quizzes, prova final).
- **2.2 Novos cursos** prioritários do RH (ex.: Integração de Novos Servidores, Ética no Serviço Público).
- **2.3 Lembrete operacional:** como o seed só roda na criação do banco, conteúdo novo só aparece em reinstalação/limpeza de dados — reforça a necessidade do **Item 7.1 (sincronização de conteúdo)** para atualizar catálogo sem republicar APK.

**Critério de pronto:** ao menos 3 cursos completos navegáveis de ponta a ponta (aulas + quizzes + prova final + certificado), sem mudança estrutural de código.

---

## Item 3 — Fundação de Autenticação + Backend (Firebase)

**Objetivo:** introduzir login e um backend centralizado, transformando o app numa plataforma institucional. É o habilitador dos itens 4, 5 e 6.

**Estado atual:** já existe o contrato `AuthRepository` + `AnonymousAuthRepositoryImpl` como preparação (intocados desde a V2). Nada conectado a backend.

**Escopo (a refinar — exige decisões de produto e infra):**
- **3.1 Provedor de identidade:** definir o método de login adequado ao servidor municipal (e-mail corporativo / conta institucional Google Workspace ou Microsoft / matrícula + senha). Firebase Authentication como base.
- **3.2 Backend de dados:** Firebase/Firestore para **perfis de aluno**, **matrículas/liberações de curso** e **conclusões/certificados**. Definir modelo de dados e regras de segurança.
- **3.3 Migração da camada Data:** trocar `AnonymousAuthRepositoryImpl` por implementação real, mantendo UI/ViewModels praticamente inalterados (o contrato já isola isso).
- **3.4 Estratégia offline-first:** o app é usado offline; definir o que é cacheado localmente (Room) e como sincroniza quando há rede (progresso, conclusões).
- **3.5 LGPD:** consentimento, finalidade, retenção e política de privacidade atualizada.

**Dependências:** nenhuma técnica interna, mas exige **backend e decisões institucionais** (TI da Prefeitura). Provavelmente a maior fase do projeto.

**Critério de pronto:** aluno faz login com identidade institucional; perfil e matrículas vêm do backend; progresso sincroniza; funciona offline após o primeiro login.

---

## Item 4 — Perfil do aluno: curso ativo único + concluídos

**Objetivo:** corrigir a regra de "curso ativo", hoje **heurística e local** ("entrar num curso o torna ativo", via DataStore), para algo **controlado pelo perfil**: o aluno tem **um curso ativo por vez** (o que o RH liberou) e **acesso de leitura aos cursos que já concluiu**.

**Estado atual:** `UserPreferencesRepository.cursoAtivo` no DataStore; qualquer curso aberto vira ativo. Não há noção de matrícula nem de histórico de concluídos com acesso vitalício.

**Escopo:**
- **4.1 Curso ativo = matrícula** vinda do perfil (Item 3), não da navegação.
- **4.2 Cursos concluídos** ficam acessíveis (somente leitura / revisão e re-emissão de certificado).
- **4.3 Bloqueio coerente** de cursos não liberados (já existe `UnavailableCursoCard`; passar a refletir a liberação do RH).

**Dependências:** Item 3 (perfil/matrícula).

**Critério de pronto:** o aluno vê e cursa apenas o curso ativo liberado, mais os concluídos em modo leitura; a troca de curso ativo é decisão do RH, não do app.

---

## Item 5 — Gestão pelo RH (painel do administrador)

**Objetivo:** começar a desenhar como o **administrador do RH cadastra alunos e libera o acesso aos cursos**. Nesta fase, foco em **descoberta e PoC** (não necessariamente um painel completo já).

**Pontos a definir:**
- **5.1 Onde mora o admin:** painel web separado, área administrativa dentro do app, ou console/ferramenta interna sobre o Firebase? (Web é o mais provável para o RH.)
- **5.2 Cadastro de alunos:** individual e/ou em lote (importação de matrículas/servidores).
- **5.3 Liberação de cursos:** matricular um aluno num curso (define o "curso ativo" do Item 4), acompanhar conclusões.
- **5.4 Papéis e permissões:** aluno vs. admin RH (e possivelmente gestor/visualizador), refletidos nas regras de segurança do backend.
- **5.5 Relatórios (semente do dashboard gerencial herdado da V2):** quem concluiu, aproveitamento, certificados emitidos.

**Dependências:** Item 3 (backend, papéis, regras de segurança).

**Critério de pronto (desta fase):** documento de decisão + PoC mínima do fluxo "RH cadastra aluno → libera curso → aluno vê o curso liberado".

---

## Item 6 — Certificado institucional

**Objetivo:** tornar o certificado um documento institucional confiável — emitido em nome do **aluno logado e cadastrado**, e visualmente mais rico.

**Estado atual (V3/V5):** `CertificadoPdfGenerator` gera PDF A4 paisagem (Canvas, identidade SJC), com **nome digitado manualmente** na hora de gerar e aproveitamento = nota da prova final.

**Escopo:**
- **6.1 Nome do aluno automático:** preencher com o nome do perfil logado/cadastrado, removendo o campo manual (que era paliativo). → depende do Item 3.
- **6.2 Dar mais "vida" ao certificado (parte visual, pode ir antes do login):**
  - **Marca d'água** com o **selo "Cidade Inteligente" de São José dos Campos**, ou
  - o **símbolo da Universidade do Servidor** (engrenagem no lugar do "O") em marca d'água/cabeçalho.
  - Avaliar: moldura/ornamento, assinatura/cargo, número/código de validação do certificado, QR de verificação (futuro, depende do backend).
- **6.3 Re-emissão** de certificado para cursos concluídos (liga com o Item 4.2).

**Dependências:** 6.1 e 6.3 dependem do Item 3/4; 6.2 (visual) é independente e pode ser entregue junto do Item 1.

**Critério de pronto:** certificado sai automaticamente no nome do aluno logado, com identidade visual reforçada (marca d'água do selo/engrenagem).

---

## Item 7 — Itens herdados (v1/v2) e oportunidades

Trazidos dos backlogs/relatórios anteriores e ainda em aberto:

- **7.1 Sincronização de conteúdo (`version.json` + API):** separar o ciclo de vida do app do conteúdo, atualizando cursos **sem republicar o APK** (planejado na V2, nunca implementado). Casa diretamente com o Item 2 (catálogo) e o Item 3 (backend).
- **7.2 Acessibilidade — recursos avançados** (planejados na Central de Acessibilidade do `backlog_v1`, ainda não feitos): **leitura em voz alta (TTS)** das aulas, **fonte para dislexia** (OpenDyslexic), **espaçamento de linha/parágrafo** ajustável, **paletas para daltonismo** (deuteranopia/protanopia/tritanopia) além do alto contraste já existente.
- **7.3 Busca — FTS4:** otimização de relevância/snippets quando o volume de conteúdo crescer (hoje é `LIKE`, suficiente para o catálogo atual).
- **7.4 Histórico de tentativas de quiz/prova:** evolução temporal do desempenho (hoje guardamos só o último resultado). Exigiria tabela própria (migração aditiva).
- **7.5 Dashboard gerencial:** consolidado para o RH — depende do backend (Item 3/5).
- **7.6 Gamificação:** reavaliar após adoção institucional (selos, trilhas, ranking) — baixa prioridade.
- **7.7 Camada `core` completa:** `designsystem`, `analytics`, `extensions` ainda são esqueleto; consolidar conforme a UI/UX (Item 1) amadurece.

---

## Riscos e Coordenação

- **Backend é uma virada de fase, não uma feature:** autenticação + Firestore + regras de segurança + LGPD + offline-sync exigem decisões institucionais (TI/RH da Prefeitura). Tratar o Item 3 como projeto próprio, com PoC antes do compromisso amplo.
- **Numeração de migração:** qualquer tabela nova (perfil cacheado, histórico de tentativas) segue a sequência **v7→v8, v8→v9…**, aditiva.
- **Dependências:** Itens 4, 5 e 6.1 ficam bloqueados até o Item 3. Por isso a ordem recomendada começa pelo que rende valor sem backend (UI/UX, catálogo, visual do certificado).
- **Offline x online:** o diferencial atual do app é funcionar 100% offline. A introdução do backend não pode quebrar isso para quem está em campo sem rede.
