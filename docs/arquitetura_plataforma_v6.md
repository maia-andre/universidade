# Arquitetura da Plataforma — V6 (Autenticação + Painel RH)

Data: 20 de junho de 2026
Status: **modelagem aprovada** — detalha o "como" do Item 3 (backend) e do Item 5 (gestão RH) do `backlog_v3.md`. A iniciar na branch sugerida **`features_v6`**.

> Este documento é o aprofundamento arquitetural da maior virada do projeto: de app offline-autônomo para **plataforma institucional** com login, perfil e gestão pelo RH. O `backlog_v3.md` continua sendo o "o quê" (itens 1–7); aqui está o "como" do núcleo de backend + painel.

---

## 1. Decisões travadas (nesta sessão)

| Tema | Decisão |
|---|---|
| **Painel do RH** | Ferramenta local em **Python + Streamlit + `firebase-admin`** (Admin SDK). É o padrão de ETL/KPI que o time já opera, com cara de produto. |
| **Deploy do painel** | Pasta **fechada/oculta na rede interna** (escrita bloqueada, permissão por grupo da equipe de treinamento) contendo código + chave de serviço. `.bat` instalador cria um **atalho personalizado** (favicon + nome) na área de trabalho; clique → `streamlit run` → `localhost`. A chave **nunca é copiada** para as máquinas. |
| **Login do aluno** | **E-mail + senha provisionados pelo RH** — a criação da conta é feita **pelo próprio painel** (Admin SDK). Funciona offline após o 1º login. |
| **Cadastro de alunos** | **Manual** (formulário) **e** por **import de planilha** (CSV/Excel de servidores). |
| **Conteúdo dos cursos** | Permanece **nos assets do APK** no V6 (dev-authored). Conteúdo dinâmico (editável pela web) é fase posterior — encaixa como mais uma página do painel sem reprojeto. |
| **Custo de infra** | Plano **Spark (gratuito)** atende: Admin SDK local **não exige Cloud Functions/Blaze**; Auth e Firestore têm cota gratuita folgada para uma divisão. *(Verificar cotas conforme o volume real.)* |

---

## 2. Visão geral — três componentes e o fluxo de mão dupla

```
   ┌──────────────────────────┐                      ┌──────────────────────────┐
   │   PAINEL RH (Streamlit)   │   push: cadastro/    │   FIREBASE (plano Spark) │
   │   pasta fechada na rede   │   matrícula          │   • Auth   (e-mail/senha)│
   │   interna + atalho .bat   │ ───────────────────► │   • Firestore (dados)    │
   │   gestora + supervisor    │                      │   • Storage (opcional)   │
   │   [Admin SDK / chave]     │ ◄─────────────────── │   • Security Rules        │
   └──────────────────────────┘   pull: conclusões/   └──────────────────────────┘
                                   relatórios                  ▲   │
                                                    matrícula  │   │  conclusões
                                                               │   ▼
                                                  ┌──────────────────────────┐
                                                  │   APP ANDROID (aluno)     │
                                                  │   Firebase SDK cliente    │
                                                  │   offline-first (Room)    │
                                                  └──────────────────────────┘
```

- **Push (RH → Firestore → App):** RH cadastra o servidor (cria conta no Auth + doc em `servidores`) e **libera um curso** (doc em `matriculas`). No login/sync, o app lê as matrículas → isso passa a **definir o "curso ativo"** (substitui a heurística "abriu = ativo").
- **Pull (App → Firestore → RH):** ao concluir (prova final ≥ 70%), o app grava em `conclusoes`. O painel lê e monta o relatório *"quem concluiu o que eu liberei"*.

---

## 3. O Painel RH (centro do V6)

### 3.1 Tecnologia e topologia
- **Streamlit** (UI amigável, sem o RH ver script) + **`firebase-admin`** (push/pull privilegiado no Firestore e no Auth).
- **Origem dos dados de entrada** = uma **planilha de servidores** que o RH sobe (paralelo direto ao "banco local de origem" de um ETL): a ferramenta valida → transforma → cria contas + matrículas.
- **Execução:** o atalho roda `streamlit run` a partir da pasta da rede; o servidor sobe em `localhost` na sessão do operador, lendo a chave de serviço **da pasta protegida** (sem cópia local).
- **Evolução possível:** rodar o Streamlit num único host sempre-ligado da rede e os demais só abrirem o navegador no `host:porta` — fica para quando/se houver mais operadores.

### 3.2 Páginas
1. **Alunos** — cadastro manual (nome, matrícula, e-mail, lotação) **+** import de planilha (validação, prévia, criação em lote).
2. **Matrículas** — liberar um curso para um aluno (= define o curso ativo do app); acompanhar status (ativa/concluída).
3. **Relatórios** — conclusões, aproveitamento, certificados emitidos (os KPIs lidos do Firestore).
4. **Conteúdo** — *(fase futura)* criar/editar cursos → escreve docs de curso; o app sincroniza.

### 3.3 Operações
| Direção | Ação | Chamada (Admin SDK) |
|---|---|---|
| Push | criar conta de aluno | `auth.create_user(email, senha_temp)` + grava `servidores/{uid}` |
| Push | liberar curso | grava `matriculas/{uid}_{cursoId}` (`status: ativa`, `liberadoPor`) |
| Pull | relatório de conclusões | lê `conclusoes` + `matriculas` → tabelas/KPIs |

### 3.4 Segurança e auditoria
- O **Admin SDK bypassa as Security Rules** — por isso a proteção é da **rede** (pasta oculta, escrita bloqueada, permissão por grupo) e da **máquina** (acesso físico).
- **Login simples do operador** no painel + carimbo `liberadoPor` / `criadoPor` nos documentos → rastreabilidade de "quem fez o quê" (importante em contexto de governo).
- Senha temporária no provisionamento; **forçar troca no 1º login** do app (boa prática).

---

## 4. Backend Firebase

### 4.1 Autenticação
- **Firebase Authentication** com provedor **e-mail/senha**. Contas criadas **pelo painel** (Admin SDK), não por auto-cadastro — o RH controla quem entra.
- Papel (`role`) guardado no doc `servidores/{uid}` (aluno/admin). *(Custom claims podem vir depois se for preciso diferenciar regras por papel no cliente.)*

### 4.2 Modelo de dados (Firestore)
```
servidores/{uid}
  nome: string
  matricula: string
  email: string
  lotacao: string
  role: "aluno" | "admin"
  criadoEm: timestamp
  criadoPor: string            // operador do painel

matriculas/{uid}_{cursoId}
  uid: string
  cursoId: string
  cursoTitulo: string          // denormalizado p/ relatório
  status: "ativa" | "concluida"
  liberadoPor: string
  liberadoEm: timestamp

conclusoes/{uid}_{cursoId}
  uid: string
  cursoId: string
  nota: number                 // aproveitamento da prova final
  concluidoEm: timestamp
  certificadoId: string        // código de validação do certificado

config/conteudo                // (fase futura — sync de conteúdo)
  versao: number
```

### 4.3 Security Rules (para o app cliente)
- **Deny by default.**
- `servidores/{uid}`: o aluno só lê/escreve o **próprio** doc (`request.auth.uid == uid`).
- `matriculas`: o aluno **lê** as suas (`uid == request.auth.uid`); **não escreve** (só Admin SDK).
- `conclusoes`: o aluno **cria/atualiza** a sua (`uid == request.auth.uid`); o RH lê via Admin SDK.
- O painel ignora as rules (Admin SDK) — as rules existem para blindar o **cliente**.

### 4.4 Storage
- O certificado é gerado **no device** (Canvas/PDF) — não precisa de Storage no V6.
- Storage entra junto do **conteúdo dinâmico** (fase futura: arquivos `.md`/imagens).

---

## 5. Integração no app Android

### 5.1 Dependências e toolchain ✅ (validado por probe em 20/06/2026)
- Adicionar **Firebase BoM + `firebase-auth` + `firebase-firestore`** (via `libs.versions.toml`), o plugin **`com.google.gms.google-services`** e o `google-services.json` (real, do projeto Firebase).
- **Apólice executada:** um probe isolado (deps + plugin + `google-services.json` dummy) **buildou com sucesso** (`:app:assembleDebug` → APK gerado, task `processDebugGoogleServices` rodou) — **sem incompatibilidade** com AGP 9.2.1 / Gradle 9 / KSP2 / Kotlin 2.2.10 / Hilt 2.59.2 / Room 2.8.4 / Compose BOM 2025.02.00. Probe revertido após a verificação.
- **Combo provado:** `com.google.gms.google-services` **4.5.0** + `firebase-bom` **34.15.0**. → registrar no `guia_compatibilidade_agp9.md` quando a integração entrar (Fase D).
- *Ainda não testado* (e **não** é risco de toolchain): a **inicialização em runtime** do Firebase, que exige o `google-services.json` real do projeto — fica para a integração de fato.

### 5.2 Camadas
- **`FirebaseAuthRepositoryImpl : AuthRepository`** — estender o contrato (hoje só anônimo) para `loginComEmailSenha`, `logout`, observar sessão e expor o perfil. UI/ViewModels ficam quase intactos (o contrato isola o mecanismo — propósito da preparação da V2).
- **Tela de Login** + **gate no NavHost**: `Splash → Login` (se não autenticado) → `Home`.
- **Camada de sincronização:**
  - No login: puxa `servidores/{uid}` + `matriculas` → grava o **curso ativo pelo perfil** (mata a heurística no `UserPreferencesRepository`).
  - Na conclusão: grava `conclusoes/{uid}_{cursoId}` (com a nota da prova final e o `certificadoId`).

### 5.3 Offline-first (o diferencial não pode quebrar)
- Ligar a **persistência offline do Firestore**; **Room** continua sendo o cache local de conteúdo/progresso.
- Em campo sem rede → o app funciona como hoje; sincroniza ao reconectar.
- **Vínculo do progresso ↔ conta:** o progresso local (Room) hoje é anônimo. No 1º login, mantém-se o progresso local existente e passa-se a carimbar a conclusão com o `uid`. *(Se surgir cenário de aparelho compartilhado, namespacear o progresso por `uid` — ver §7.)*

---

## 6. Faseamento do V6 (ordem por isolamento de risco)

> **Princípio (decidido em 20/06):** deixar o **spike de toolchain (`google-services` em AGP 9)** por **último**, isolado. A fundação Firebase e o painel **não tocam no build Android** — então os construímos primeiro para **validar o modelo de dados ponta a ponta** antes de a integração Android consumi-lo. Assim, no ponto arriscado, há **uma só variável nova** em jogo.

- **Fase A — Android sem backend (paralela, momentum, risco ~zero):** certificado novo no padrão do `docs/Modelo.pdf` (faixas azul/ouro, "CERTIFICADO", corpo da Escola de Governo, rodapé com `logouniservidor.jpeg` + brasão + `www.SJC.sp.gov.br`; → Item 6.2), refino de UI/UX (Item 1), completar catálogo (Item 2). **Não adiciona dependências ao Gradle.**
- **Fase B — Fundação Firebase (baixo risco, base compartilhada):** criar o projeto, habilitar Auth (e-mail/senha), Firestore, **Security Rules** e o **modelo de dados** (§4). Tudo em console + Python; **não toca no Gradle**.
- **Fase C — Painel RH (Streamlit/Python):** construir contra a fundação (cadastro + import + matrícula + relatórios). **Valida o modelo de dados** pelo lado privilegiado, end-to-end. Independe do toolchain Android. → Item 5.
- **Fase D — Spike de toolchain + integração Android (o ponto arriscado, por último):** `google-services` no AGP 9 como **spike isolado e documentado** → `FirebaseAuthRepositoryImpl` + Login + sync `matrículas → curso ativo` + `conclusões` + offline-first. → Itens 3 e 4. Entra com o backend **já provado** pelo painel.
- **Fase E — conteúdo dinâmico (provável fora do V6):** mover conteúdo p/ backend + sync (`version.json`). → Item 7.1.

> **Apólice — executada e ✅ aprovada (20/06/2026):** o *probe* de build (BoM + plugin + json dummy → `assembleDebug` → revertido) passou com `google-services 4.5.0` + `firebase-bom 34.15.0`. O maior risco do AGP 9 está, em grande parte, descartado já antes da Fase D.
> A Fase A é independente de tudo e roda em paralelo, mantendo o app evoluindo sem encostar no risco.

---

## 7. Riscos e questões em aberto

1. **Toolchain (google-services em AGP 9):** **mitigado** — probe de build aprovado em 20/06 (`google-services 4.5.0` + `firebase-bom 34.15.0` buildam no AGP 9.2.1 / Gradle 9 / KSP2). Resta só a integração de runtime na Fase D (não é risco de toolchain) e documentar no `guia_compatibilidade_agp9.md`.
2. **LGPD:** a partir do login há **dados pessoais de servidores** (nome, matrícula, e-mail, lotação). Revisar `docs/politica_de_privacidade.md` junto da Fase B (consentimento, finalidade, retenção, base legal).
3. **Identidade institucional futura:** começamos com e-mail/senha provisionado. Se a Prefeitura padronizar Google Workspace / Microsoft 365, dá pra migrar o provedor depois sem reescrever a UI (contrato isola).
4. **Aparelho compartilhado:** progresso por dispositivo (hoje) vs por `uid` (namespacing). **→ congelado** (`backlog_congelado.md`) — não é risco para o momento; revisitar se o cenário surgir.
5. **Senha temporária / recuperação:** definir fluxo de troca no 1º login e "esqueci a senha" (e-mail de reset do Firebase ou via RH).
6. **Cotas do plano Spark:** validar reads/writes do Firestore contra o volume real da divisão; só viraria Blaze se adotarmos Cloud Functions ou estourarmos cota.

---

## 8. Próximos passos imediatos

1. Criar a branch **`features_v6`** a partir da `main`.
2. **Fundação Firebase (Fase B):** criar o projeto (console), habilitar Auth e-mail/senha + Firestore, aplicar as Security Rules e o modelo de dados do §4. *(Depende do console — passos detalhados à parte.)*
3. **Painel RH (Fase C):** esqueleto Streamlit + `firebase-admin` reusando o padrão de deploy de rede interna já validado pelo time; valida o modelo de dados ponta a ponta.
4. **Em paralelo (Fase A):** reimplementar o `CertificadoPdfGenerator` no padrão do `Modelo.pdf` — independe de backend, alto valor, sem risco de toolchain.
5. **Por último (Fase D):** spike do `google-services` no AGP 9 + integração Android, com o backend já provado.
