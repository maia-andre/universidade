# Relatório de Progresso — Versão 6 (Branch `features_v6`)

Data: 21 de junho de 2026
Status: **implementação concluída; verificação manual no device pendente.** A maior virada do projeto — de app offline-autônomo para **plataforma institucional** (login + painel do RH + sincronização de mão dupla).

Branch saída da `main` (já com V1–V5). 12 commits, execução incremental com build verde a cada etapa. Planejamento em `docs/backlog_v3.md` (o "o quê") e `docs/arquitetura_plataforma_v6.md` (o "como" do backend/painel). Convenção de commits herdada das versões anteriores.

---

## 1. O Que Foi Entregue

### Planejamento e arquitetura
- **`docs/arquitetura_plataforma_v6.md`**: modela a plataforma (app Android + Firebase + painel RH), o fluxo de mão dupla, o modelo de dados do Firestore, a integração Android e o **faseamento por isolamento de risco**.
- **`docs/backlog_congelado.md`**: itens deliberadamente adiados (C1 — progresso por `uid` em aparelho compartilhado).
- **`CLAUDE.md`** atualizado ao estado real do código (DB v7, prova final, telas, regra do tema via `colorScheme`).

### Apólice de toolchain (Firebase no AGP 9)
- Antes de comprometer a integração, um **probe isolado** (deps + plugin `google-services` + `google-services.json` dummy → `assembleDebug` → revertido) confirmou que o ecossistema Firebase **builda sem incompatibilidade** no toolchain bleeding-edge.
- **Combo validado:** `google-services` **4.5.0** + `firebase-bom` **34.15.0** com AGP 9.2.1 / Gradle 9 / KSP2 / Kotlin 2.2.10 / Hilt 2.59.2 / Room 2.8.4. Documentado no `guia_compatibilidade_agp9.md` (seção 10).

### Fase A — Certificado institucional + carga horária
- `CertificadoPdfGenerator` reescrito no padrão do `docs/Modelo.pdf`: ornamentos angulares azul/ouro, título "CERTIFICADO", texto da Escola de Governo – Universidade do Servidor, rodapé com o **logo `logouniservidor`** (embutido) + Prefeitura + `www.SJC.sp.gov.br`.
- **Carga horária por curso** end-to-end: campo no `curso_data.json`, **lido em runtime** (`CursoRepository.getCargaHoraria` → `ObterCargaHorariaUseCase` → `CertificadoViewModel`), chegando a instalações já publicadas sem re-seed — mesmo padrão da prova final.
- Aproveitamento (%) removido do PDF (mantido na tela), seguindo o modelo de participação.

### Fundação Firebase
- Plugin `google-services` + Firebase BoM + **Auth** + **Firestore** via o **version catalog** (regra do projeto — não hardcoded como o console sugere). `google-services.json` real, **gitignored**.

### Fase C — Painel RH (Streamlit + Firebase Admin SDK)
- Ferramenta local em `painel-rh/`: **Alunos** (cadastro manual + import de planilha), **Matrículas** (liberar curso = define o curso ativo no app), **Relatórios** (conclusões/KPIs).
- **Deploy** pelo padrão da equipe: pasta protegida na rede interna + atalho `.bat`. A **chave de service account** vive só nessa pasta.
- Login do operador (auditoria via `criadoPor`/`liberadoPor`).
- **Validado end-to-end no lado de escrita:** cadastro de aluno (Auth `create_user` + doc) e liberação de curso gravando no Firestore real.

### Fase D — Integração Android (login + sincronização de mão dupla)
- **D1 — Auth:** `AuthRepository` real (login e-mail/senha, reset, logout, estado reativo) + `FirebaseAuthRepositoryImpl`; bind no DI; mock anônimo removido.
- **D2 — Login + gate:** `LoginScreen`/`ViewModel` com identidade SJC; gate **Splash → (logado?) Home : Login**; logout em Configurações.
- **D3 — downstream:** `PlataformaRepository` (Firestore) lê a matrícula liberada pelo RH; `SincronizarCursoAtivoUseCase` roda no `HomeViewModel.init` e define o **curso ativo** (cobre login e arranque já-logado; o estado reage via `cursoAtivoId`).
- **D4 — upstream:** `RegistrarConclusaoUseCase` grava em `conclusoes/{uid}_{cursoId}` na emissão do certificado, com código de validação determinístico → aparece nos Relatórios do painel.

### Segurança e documentação
- **`firestore.rules`** (raiz): regras do **cliente** (aluno lê seu perfil/matrículas, grava suas conclusões; escrita administrativa só via Admin SDK).
- **Incidente de segurança tratado:** a chave do Admin SDK, colada com o nome padrão do console (`firebase-adminsdk-*.json`), **não casava** com o `.gitignore` e ficou exposta (untracked, não vazou). Corrigido: chave movida para `painel-rh/secrets/`, `.gitignore` endurecido.

---

## 2. Decisões Técnicas

- **Painel = ferramenta local Python (Streamlit + Admin SDK)**, não Cloud Functions. Escolhido por simplicidade, custo (plano Spark, sem Blaze) e por reaproveitar o padrão de ETL/KPI que o time já opera na rede interna. A credencial privilegiada fica protegida pelos controles de acesso da própria rede.
- **Faseamento por isolamento de risco:** fundação Firebase + painel **antes** do spike de toolchain e da integração Android, validando o modelo de dados pelo lado privilegiado primeiro.
- **Login provisionado pelo RH** (e-mail/senha): o RH controla quem entra; o app só autentica.
- **Curso ativo sincronizado no `HomeViewModel.init`** (não no login): um único ponto reativo cobre todos os caminhos de entrada.
- **Conclusão best-effort/offline-safe:** a persistência offline do Firestore (ligada por padrão no Android) enfileira a escrita e sobe ao reconectar.
- **`await()` de `Task`** via `suspendCancellableCoroutine`, sem a dependência `play-services-coroutines`.
- **Apólice de toolchain** antes de comprometer a fase arriscada — barata e decisiva.

---

## 3. Débitos Técnicos e Pendências

1. **Verificação manual no device pendente (tudo):** login, sync curso ativo, conclusões e o visual do certificado foram validados por **compilação** (não há emulador no ambiente atual). Recomenda-se o smoke test completo no aparelho.
2. **🔴 Publicar o `firestore.rules` no console** antes do teste no device — em modo produção, as regras padrão negam o cliente, e D3/D4 falham silenciosamente (o painel segue funcionando, pois usa Admin SDK).
3. **Certificado:** a geometria do Canvas é uma **primeira aproximação** (não renderizada localmente) — precisa de ajuste por screenshot; a **carga horária** está com **valores placeholder** (Supervisores 20h, esqueletos 4h — faltam os oficiais do RH); o **brasão da Prefeitura** está ausente (rodapé usa fallback em texto).
4. **Nome no certificado** ainda é digitado (paliativo). `getNomeServidor(uid)` já existe no repositório — auto-preenchimento (Item 6.1) é um próximo passo barato.
5. **Heurística do curso ativo** ("entrar no curso → ativo", em `CursoDetailViewModel`) **coexiste** com o sync da matrícula — remover quando o sync for validado.
6. **Painel — autenticação:** hoje é uma senha de teste compartilhada (`andre/admin`, **não commitada**). Falta autenticação real por operador, forçar troca de senha no 1º acesso e fluxo de reset.
7. **Conteúdo dinâmico** (painel gerenciar o conteúdo dos cursos): fase futura — página de Conteúdo no painel + sync (`version.json`).
8. **LGPD:** com login e dados pessoais de servidores (nome, matrícula, e-mail, lotação), entra o tratamento sob a LGPD; revisar `docs/politica_de_privacidade.md`.
9. **Branch `features_v6` ainda local** (sem push/PR).
10. **Aparelho compartilhado** (progresso por `uid`): congelado em `backlog_congelado.md` (C1).

---

## 4. Verificação

- **Build de debug verde** em todas as etapas (Fase A, fundação Firebase, D1/D2, D3/D4) — `:app:assembleDebug` com o `google-services.json` real.
- **Painel validado contra o Firestore real** no lado de escrita (cadastro + matrícula).
- **Roteiro de smoke test (device):** publicar as rules → instalar o APK → login com o aluno cadastrado no painel (e-mail + senha temporária) → conferir o curso ativo (D3) → concluir o curso + prova final + certificado → conferir a conclusão nos Relatórios do painel (D4) → testar o logout.

---

## 5. Próximos Passos

1. **Smoke test no device** (acima) — fechar a validação de ponta a ponta.
2. **Fechar a Fase A** com base no screenshot: ajustar a geometria do certificado, inserir a carga horária real e o brasão.
3. **Auto-nome no certificado** (Item 6.1) via `getNomeServidor`.
4. **Remover a heurística** do curso ativo, deixando a matrícula como única fonte.
5. **Refino de UI/UX** (`backlog_v3` Item 1) e **aumentar o catálogo** de cursos (Item 2).
6. **Painel:** autenticação por operador, troca de senha no 1º acesso, página de Conteúdo.
7. **Conteúdo dinâmico** + sync (`backlog_v3` Item 7.1).
8. **Push/PR** da `features_v6` quando a validação manual passar.

---

## 6. Lições Aprendidas V6

- **Apólice barata antes de fase arriscada:** o probe do `google-services` no AGP 9 custou um build e desarmou o maior risco da fase — acabou indolor, mas só soubemos por tê-lo feito.
- **`.gitignore` de segredos precisa cobrir os nomes reais**, não só convenções: a chave do Admin SDK com o nome padrão do console quase escapou. Defense-in-depth (`*firebase-adminsdk*.json`) e pasta `secrets/` dedicada.
- **Construir o lado privilegiado (painel) primeiro** validou o modelo de dados end-to-end antes de a integração Android consumi-lo — isolou os riscos em uma variável por vez.
- **Ambiente de dev sem SDK** trava todo o trabalho Android logo na configuração do AGP; instalar o SDK foi pré-requisito de tudo. `local.properties` é por-máquina (gitignored), nunca sincronizado.
- **Sincronizar no ponto reativo certo** (`HomeViewModel.init`) cobre todos os caminhos de entrada sem acoplar o fluxo de login.
