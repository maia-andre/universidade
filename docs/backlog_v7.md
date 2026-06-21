# Backlog V7 — Correções da plataforma + maturidade (pós-smoke test V6)

Data: 21 de junho de 2026
Status: **Itens 1–5 implementados e build-verde** na `features_v7` (validação no device em curso). Itens 6 e 7 **movidos para a V8** (`docs/backlog_v8.md`).

> **Onde estamos:** a V6 entregou a virada para **plataforma institucional** (login Firebase + Painel RH + sync de mão dupla) e foi **validada por smoke test no device** (ver §1). O teste confirmou que o eixo central funciona — login, reset por e-mail, sync da matrícula, conclusão chegando ao painel, desempenho e "continuar de onde parou" — mas expôs **um bug de controle de acesso** e listou ajustes de acabamento. Esta é a fase de **endurecer o que já existe** antes de avançar para conteúdo dinâmico e novos cursos.
>
> **Regra de banco mantida:** migrações sempre **aditivas e versionadas** (próxima é **v7→v8**), nunca destrutivas em upgrade — agora com usuários reais próximos, a disciplina é inegociável.
>
> **LGPD:** com login + dados pessoais de servidores já em produção, revisar `docs/politica_de_privacidade.md` deixou de ser preventivo e passou a ser obrigatório (Item 7).

---

## 1. O que o smoke test V6 confirmou (em 21/06/2026, device real, rules publicadas)

**Funcionando:**
- **Login + gate** Splash → (logado?) Home : Login.
- **Reset de senha por e-mail** ("Esqueci minha senha" na `LoginScreen` → e-mail do Firebase) — testado, funcionou.
- **D4 — conclusão upstream:** concluir curso + prova final + certificado gravou `conclusoes/{uid}_{cursoId}` e apareceu nos **Relatórios** do painel.
- **Desempenho:** a aba trouxe corretamente os dados do curso matriculado.
- **"Continuar de onde parou":** funcionou ao retornar ao app.
- **Certificado:** gera no padrão institucional (carece de ajuste fino — Item 3).
- **Painel:** reset de senha por e-mail validado em conjunto (a função de reset do RH foi adicionada no fim da V6).

**Bug encontrado:** controle de acesso por matrícula ausente (Item 1).

---

## Ordem recomendada

1. **🔴 Controle de acesso por matrícula** (Item 1) — bug crítico; a matrícula precisa ser um portão, não só um destaque. Bloqueia a confiança no produto.
2. **Trocar a própria senha no app** (Item 2) — barato, fecha o ciclo de credenciais (hoje o aluno só reseta por e-mail).
3. **Refino do certificado** (Item 3) — aproximar do modelo oficial do RH; baixo risco.
4. **Auto-nome no certificado** (Item 4) — barato, `getNomeServidor` já existe.
5. **Painel: múltiplas matrículas / desmatricular** (Item 5) — acompanha o Item 1.
6. **Conteúdo dinâmico + catálogo** (Item 6) — fase maior, destravada pelo resto.
7. **LGPD + débitos herdados** (Item 7).

---

## Item 1 — 🔴 Controle de acesso por matrícula (bug crítico)

**Sintoma (smoke test):** o aluno recém-criado apareceu com **os 5 cursos acessíveis de uma vez**, mesmo matriculado em um só. Foi possível entrar em **Gestão de Patrimônio** (sem matrícula nele), concluir, gerar certificado e gravar a conclusão — o id no painel "mudou de 1 para 2" porque a conclusão do curso não matriculado subiu para o Firestore.

**Causa raiz (confirmada no código):**
- O catálogo (`CursosViewModel` → `GetCursosUseCase`) lista **todos** os cursos com `isAvailable = true`. **Não há filtro por matrícula.**
- "Curso ativo" é apenas um `cursoAtivoId` no `UserPreferencesRepository` (DataStore) — um **destaque** da Home/Desempenho, **não** um controle de acesso.
- `SincronizarCursoAtivoUseCase` (D3) só define o curso ativo **inicial** a partir da matrícula; não restringe acesso.
- A **heurística** `CursoDetailViewModel:38` (`setCursoAtivo(cursoId)` ao entrar em qualquer curso) **ainda coexiste** com o sync (pendência #5 da V6) — entrar em qualquer curso o torna "ativo", reforçando a falsa sensação de acesso liberado.
- `PlataformaRepository.getCursoAtivoMatriculado(uid)` retorna **uma** matrícula; o modelo de dados (`matriculas/{uid}_{cursoId}`) já suporta **várias**, mas o app só lê uma.

**Regra de acesso (definida com o RH em 21/06/2026):** o catálogo mostra **todos** os cursos disponíveis, cada um com um **estado**:
- **Ativo / em andamento** — matriculado pelo RH e ainda não concluído → **acessível**.
- **Concluído** — já finalizado (100% das aulas + prova final aprovada) → **acessível para sempre** (revisar aulas e rever o certificado quando quiser; o selo muda para "concluído").
- **Bloqueado** (cadeado) — nem matriculado nem concluído → **sem acesso**.

Em resumo: **`acessível = matriculado OU concluído`**. "Curso ativo" volta a ser apenas o **destaque** (o matriculado em andamento) na Home/Desempenho — **não** o portão de acesso.

**Escopo:**
- **1.1 — Fonte de verdade: matrícula.** Novo `PlataformaRepository.getCursosMatriculados(uid): List<Int>` (status "ativa"), sincronizado para um conjunto local (análogo ao D3). Hoje só se lê **uma** matrícula (`getCursoAtivoMatriculado`).
- **1.2 — Fonte de verdade: conclusão (durável).** O estado "concluído" precisa **sobreviver a reinstalação** (que apaga o Room local). Sincronizar `conclusoes/{uid}_{cursoId}` do Firestore **para baixo** (downstream) e usar isso como base do acesso/selo — senão reinstalar tornaria um curso concluído "bloqueado" de novo. Hoje só se grava conclusão (upstream, D4); falta o caminho de leitura.
- **1.3 — Gatear catálogo e navegação:** `CursosViewModel`/catálogo renderiza os três estados (cadeado no bloqueado, selo no concluído). `CursoDetailViewModel` **nega entrada** a curso bloqueado (e prova final / certificado idem) — defesa em profundidade, não só esconder visualmente.
- **1.4 — Reforço no Firestore:** as `firestore.rules` devem **rejeitar** `conclusoes/{uid}_{cursoId}` quando não existir matrícula correspondente — para que mesmo um cliente adulterado não certifique curso não liberado. (Concluir pressupõe ter sido matriculado; após concluir, o acesso persiste pelo estado "concluído" mesmo que o RH encerre a matrícula.)
- **1.5 — Remover a heurística** (`CursoDetailViewModel:38`, `setCursoAtivo` ao entrar): com o acesso vindo da matrícula, "entrar → vira ativo" deixa de fazer sentido. Encerra a pendência #5 da V6.

**Critério de pronto:** um aluno matriculado em um curso vê o catálogo inteiro, mas **só abre/conclui/certifica** o matriculado; cursos concluídos seguem acessíveis (inclusive após reinstalar); cursos nem matriculados nem concluídos ficam **bloqueados** — verificado no device **e** tentando burlar pelo Firestore (rules negam).

---

## Item 2 — Trocar a própria senha no app (aluno logado)

**Objetivo:** o RH orienta a troca de senha no primeiro acesso, mas **o app não oferece onde trocar** — só há reset por e-mail. Adicionar a opção em **Configurações**.

**Escopo:**
- **2.1 — UI em Configurações:** seção "Conta" com "Trocar senha" (senha atual + nova + confirmação).
- **2.2 — `AuthRepository.trocarSenha(...)`:** Firebase `updatePassword`; tratar `requires-recent-login` com **reautenticação** (pedir a senha atual e `reauthenticate` antes do `updatePassword`).
- **2.3 — Mensagens claras:** mínimo de 6 caracteres (regra do Auth), senha atual incorreta, sucesso.
- **2.4 — (opcional) Sinalizar "senha temporária"**: marcar no perfil quando a senha foi definida/redefinida pelo RH e **sugerir** a troca no primeiro acesso (não bloquear de início).

**Critério de pronto:** aluno logado troca a própria senha em Configurações e faz novo login com a nova senha.

---

## Item 3 — Refino do certificado (aproximar do modelo oficial do RH)

**Objetivo:** o certificado "ficou bom, mas ligeiramente diferente" do `docs/Modelo.pdf`. Fechar a aproximação a partir do comparativo `docs/certificado smoke test 1.pdf` × `docs/Modelo.pdf`. Geometria atual em `CertificadoPdfGenerator.kt`.

**Diferenças observadas (gerado × modelo):**
- **3.1 — Ornamento do topo:** o gerado tem o cluster amarelo/azul **no canto superior esquerdo** (`desenharOrnamentos`); o modelo usa uma **faixa centralizada no topo** — fina faixa azul na borda superior + triângulos amarelo/azul/azul-claro apontando para baixo, mais simétricos e ao centro. Reposicionar/reformar para o padrão do modelo.
- **3.2 — Rodapé:** o gerado separa **logo Universidade do Servidor à esquerda** e **texto "Prefeitura…" + URL à direita**; o modelo **centraliza três elementos em linha**: logo Universidade do Servidor · `www.SJC.sp.gov.br` · **brasão da Prefeitura**. **Inserir o brasão** (hoje é fallback em texto — pendência #3 da V6) e centralizar a linha do rodapé.
- **3.3 — Texto do corpo:** o modelo traz "…– Universidade do Servidor – com N horas de duração, **no período de DD a DD de mês**". Decisão com o RH: nossos cursos são offline/autoguiados, sem período fixo — provavelmente **manter "com N horas de duração" + data de conclusão** (sem período), ou derivar período de matrícula→conclusão. Registrar a decisão.
- **3.4 — Tipografia/cor:** título "CERTIFICADO" do modelo é um azul mais **claro e espaçado**; avaliar aproximar a cor/peso da fonte.
- **3.5 — Carga horária real:** substituir os **placeholders** (Supervisores 20h, esqueletos 4h) pelos valores **oficiais do RH** no `curso_data.json` (lido em runtime — chega sem re-seed). Pendência #3 da V6.

**Critério de pronto:** certificado gerado no device lado a lado com o `Modelo.pdf` aprovado pelo RH; brasão presente; carga horária oficial.

---

## Item 4 — Auto-nome no certificado

**Objetivo:** hoje o nome no certificado é **digitado** (paliativo). `PlataformaRepository.getNomeServidor(uid)` já existe e foi validado.

**Escopo:** preencher o nome a partir do cadastro (uid logado) no `CertificadoViewModel`, com o campo digitável como fallback offline. Encerra a pendência #4 da V6.

**Critério de pronto:** certificado sai com o nome do cadastro sem digitação.

---

## Item 5 — Painel RH: múltiplas matrículas e desmatricular

**Objetivo:** acompanhar o Item 1 do lado privilegiado.

**Escopo:**
- **5.1 — Desmatricular / encerrar matrícula** (status "encerrada") na página de Matrículas — hoje só há liberar.
- **5.2 — Visão por aluno:** quais cursos estão ativos, concluídos (cruzar `matriculas` × `conclusoes`).
- **5.3 — (se Item 1 opção b) liberar vários cursos** ao mesmo aluno de forma clara.
- **5.4 — Autenticação real por operador:** hoje é dict de credencial de teste (`OPERADORES`, não commitado de verdade) — evoluir para autenticação real, troca de senha no 1º acesso do operador e reset. Pendência #6 da V6.

**Critério de pronto:** RH consegue matricular, desmatricular e ver o estado de cada aluno; acesso ao painel por operador real.

---

## Item 6 — Conteúdo dinâmico + ampliação do catálogo  → **movido para a V8**

Adiado para a V8 (Item 1). Ver `docs/backlog_v8.md`.

## Item 7 — LGPD + distribuição  → **movido para a V8**

Adiado/redistribuído na V8: LGPD + auth real de operador no Item 2; distribuição + migração da chave + a publicação propriamente dita no **Item 3 (Publicação da V2 no teste interno)**. Ver `docs/backlog_v8.md`. (Aparelho compartilhado segue congelado em `docs/backlog_congelado.md`, C1.)

---

## Lembrete de disciplina (herdado)

- Migrações **aditivas e versionadas** (v7→v8), nunca destrutivas em upgrade.
- Toolchain bleeding-edge: consultar `docs/guia_compatibilidade_agp9.md` antes de mexer no build.
- Conteúdo novo que precisa chegar a instalações já publicadas: **ler de assets/remoto em runtime**, não assar no seed.
- Cores de card/superfície sempre via `MaterialTheme.colorScheme` (nunca `isSystemInDarkTheme()`).
