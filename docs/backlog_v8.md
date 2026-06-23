# Backlog V8 — Conteúdo dinâmico, LGPD e publicação na Play Store

Data: 21 de junho de 2026 (status atualizado em 22 de junho de 2026)
Status: **parcialmente concluído.** Itens **1.1, 1.2, 2.2 e 3 entregues**; faltam **1.3** (conteúdo) e **2.1** (LGPD). Relatório em `docs/progresso_v8.md`.

### Placar (22/06/2026)
| Item | Status |
|---|---|
| 1.1 Sincronização de conteúdo | ✅ feito (build verde) |
| 1.2 Página de Conteúdo no painel | ✅ feito |
| 1.3 Completar cursos esqueleto + novos | ❌ pendente (via painel já pronto) |
| 2.1 LGPD (política) | 🔄 em andamento (rascunho do dev) |
| 2.2 Auth real de operador | ✅ feito (hash PBKDF2) |
| 2.3 Aparelho compartilhado | ⏸ congelado |
| 3 Publicação da V2 (teste interno) | ✅ publicada na Play |

**Verificações ainda pendentes:** smoke test no device do sync de conteúdo (publicar edição → app atualiza sem reinstalar); backup seguro da upload key (`.jks` + credenciais).

> **Onde estamos:** a V7 entregou e deixou build-verde os Itens 1–5 (controle de **acesso por matrícula**, **trocar senha** no app, **certificado** com brasão + carga horária, **auto-nome**, e o **painel** com desmatricular + situação por aluno). Restaram dois itens grandes/de processo — **conteúdo dinâmico** e **LGPD** — deliberadamente adiados, e entra um terceiro: **publicar a V2 no teste interno** da Play quando tudo estiver validado. Esta é a fase de **escalar conteúdo e levar à loja**.
>
> **Pré-requisitos herdados:** migrações de banco **aditivas/versionadas** (v7→v8…), toolchain bleeding-edge (`docs/guia_compatibilidade_agp9.md`), e conteúdo novo **lido em runtime** (não assado no seed).

---

## Os 3 itens da V8

1. **Item 1 (legado da V7) — Conteúdo dinâmico + catálogo.**
2. **Item 2 (legado da V7) — LGPD** (+ débitos de segurança herdados).
3. **Item 3 (novo) — Publicação da V2 no teste interno da Play Store.**

---

## Item 1 — Conteúdo dinâmico + ampliação do catálogo  *(legado: V7 Item 6)* — 🔄 parcial (1.1/1.2 ✅, 1.3 pendente)

**Objetivo:** permitir que o RH atualize o conteúdo dos cursos **sem republicar APK** e ampliar o catálogo (pendência #7 da V6; itens 2 e 7.1 do `backlog_v3`). É a **maior fase** desta versão — merece design dedicado.

**Escopo (a detalhar):**
- ✅ **1.1 — Sincronização de conteúdo** (`version.json` + leitura em runtime), seguindo o padrão "ler de assets/remoto em runtime" já usado para prova final e carga horária. Decidir a origem (Firestore `config/` — já previsto nas rules — ou Storage).
- ✅ **1.2 — Página de Conteúdo no painel** (gerenciar cursos/módulos/aulas).
- ❌ **1.3 — Completar os cursos** esqueleto (Patrimônio, Licitação, Almoxarifado) e novos cursos prioritários do RH. *(pendente — agora possível pelo painel já entregue)*

**Critério de pronto:** RH publica/edita um curso pelo painel e ele aparece/atualiza no app **sem novo APK**.

---

## Item 2 — LGPD + débitos de segurança  *(legado: V7 Item 7.1)* — 🔄 parcial (2.2 ✅, 2.1 em andamento)

**Escopo:**
- 🔄 **2.1 — LGPD (obrigatório):** revisar `docs/politica_de_privacidade.md` agora que há login + dados pessoais de servidores em produção (nome, matrícula, e-mail, lotação). **Jurídico-sensível** — revisar com o RH/jurídico, não redigir unilateralmente.
- ✅ **2.2 — Auth real por operador no painel** *(carryover da V7, 5.4):* hoje é dict de credencial de teste (`OPERADORES`). Evoluir para autenticação real, troca de senha no 1º acesso do operador e reset. Pendência #6 da V6.
- **2.3 — Aparelho compartilhado** (progresso por `uid`): segue **congelado** em `docs/backlog_congelado.md` (C1) — reavaliar quando houver demanda.

**Critério de pronto:** política revisada e publicada; acesso ao painel por operador real.

---

## Item 3 — Publicação da V2 no teste interno da Play Store  *(novo; absorve V7 Itens 7.2/7.3)* — ✅ publicada

**Objetivo:** subir a **V2** (com os ganhos da V7) para o **track de teste interno** do Play Console, instalável pelo RH/beta **sem o alerta do Play Protect** (que é do sideload, não da assinatura). **Só depois da V7 validada no device.**

**Pré-requisito crítico — a chave (migração da máquina de build):**
- A **app signing key** (Google, via Play App Signing) **nunca muda** e não está em máquina nenhuma — updates aos testers são preservados.
- A **upload key** (que assina o `.aab`) é o que precisa migrar para o PC pessoal. Subir a V2 com **chave nova** é **rejeitado** pela Play.
  - **Caminho A (preferido):** copiar o `.jks` + as 3 credenciais (senha do keystore, alias, senha da chave) do PC do trabalho para o pessoal e fazer backup. Fazer **antes** de perder acesso à máquina do trabalho.
  - **Caminho B (fallback):** **reset da upload key** no Play Console (Integridade do app → Assinatura do app) — gera-se uma nova no PC pessoal; a app signing key não muda. Só existe se o **Play App Signing estiver ativado** (confirmar).

**Escopo:**
- **3.1 — Migrar a upload key** para o PC pessoal (Caminho A) e fazer backup seguro.
- **3.2 — `versionCode`/`versionName`** da V2 (incrementar; conferir `build.gradle.kts`).
- **3.3 — Gerar o `.aab` assinado** (release) no PC pessoal.
- **3.4 — Subir no track de teste interno** e validar a instalação via Play (sem alerta de "desenvolvedor desconhecido").
- **Atenção:** debug↔release não atualizam um sobre o outro (assinaturas distintas) e **desinstalar apaga o Room local** — manter a **mesma chave** para preservar o progresso dos testers.

**Critério de pronto:** V2 disponível no teste interno e instalável pelo RH/beta sem alerta, com update limpo sobre a V1.

---

## Lembrete de disciplina (herdado)

- Migrações **aditivas e versionadas** (v7→v8…), nunca destrutivas em upgrade.
- Toolchain bleeding-edge: consultar `docs/guia_compatibilidade_agp9.md` antes de mexer no build.
- Conteúdo novo que precisa chegar a instalações já publicadas: **ler de assets/remoto em runtime**, não assar no seed.
- Cores de card/superfície sempre via `MaterialTheme.colorScheme` (nunca `isSystemInDarkTheme()`).
