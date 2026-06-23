# Relatório de Progresso — Versão 8 (sessão 2026-06-22)

Data: 22 de junho de 2026
Status: **Itens 1, 2.2 e 3 concluídos** — V2 (versionCode 2 / versionName 1.02.1) **publicada no teste interno da Play**. Item 2.1 (LGPD) em andamento à parte (rascunho do dev).

Planejamento: `docs/backlog_v8.md`. Design do Item 1: `docs/design_conteudo_dinamico_v8.md`. Publicação: `docs/publicacao_v2_play.md`.

---

## 1. O que foi entregue

### Item 1 — Conteúdo dinâmico (Fases 1.1a + 1.2) ✅
RH atualiza o conteúdo dos cursos **sem republicar APK**.

- **Origem:** doc único Firestore `config/conteudo` (`{versao, json, publicadoPor, publicadoEm}`, `json` = catálogo inteiro como string, ~32 KB << 1 MiB). Rules já cobriam `config/{doc}` — **sem mudança de rules**.
- **App (pipeline de sync):** `ConteudoLocalSource` (lê do arquivo `filesDir/conteudo_remoto.json` sincronizado, senão do asset baseline) → `ConteudoRemotoRepository` (Firestore) → `SincronizarConteudoUseCase` compara versão (DataStore `versaoConteudo`, baseline = 0) e, se maior, faz **full-replace do Room em transação preservando `ProgressoEntity`** (keyed por `aulaId`). Disparado no `HomeViewModel.init`, offline-safe. `getCargaHoraria` e a prova final passaram a ler da fonte unificada.
- **Painel (autoria):** `services/conteudo.py` (carregar / bootstrap do `curso_data.json` com `.md` inline / publicar com versão++ e IDs estáveis) + `pages/4_Conteudo.py` (editar curso/módulo/aula/quiz/prova e **Publicar**).
- **🔴 Bug crítico corrigido:** os cursos esqueleto (Patrimônio/Licitação/Almoxarifado) reusavam IDs de módulo/aula do Curso 1 — num seed/sync limpo, sobrescreveriam módulos do Curso 1 (PrimaryKey). Renumerados (2000/3000/4000…); Curso 1 intocado. IDs agora globalmente únicos (validado).
- **Decisão "não destrutivo":** aula removida no remoto some via full-replace mas o progresso fica preservado e **reversível** (reanexa pelo `aulaId` ao republicar). Sem migração de schema (DB segue v7).

### Item 2.2 — Auth real de operador no painel ✅
Substitui o dict de credencial em texto puro.

- `services/operadores.py`: coleção `operadores/{usuario}` com senha em **hash PBKDF2-HMAC-SHA256** (stdlib, sem dependência nova; salt por operador, verificação em tempo constante). Seed único migra do `config.OPERADORES` com `precisaTrocar=True`.
- `auth_operador.py` reescrito: login real, **troca de senha obrigatória no 1º acesso**, troca voluntária na sidebar.
- `pages/5_Operadores.py` (restrita a admin): criar operador, redefinir senha (reset força troca), ativar/desativar.
- Coleção `operadores` fica fora das rules do cliente (default-deny) — só o Admin SDK acessa.

### Item 3 — Publicação da V2 ✅ publicada no teste interno
- `build.gradle.kts`: `versionCode = 2`, `versionName = "1.02.1"`; `signingConfigs.release` lê `keystore.properties` (gitignored, condicional — sem ele o release sai unsigned e o build não quebra). `keystore.properties` adicionado ao `.gitignore`; modelo `keystore.properties.example` criado.
- **`.aab` assinado gerado e validado:** `:app:bundleRelease` verde, 17 MB, `jarsigner -verify` → "jar verified" (alias `UNISERVI…`). Primeiro release assinado na máquina Linux — a Play **aceitou**, confirmando que a upload key migrada é a registrada.
- **Publicado no teste interno** (Play Console). Resolvido o aviso de "APK oculto" removendo o pacote antigo (versionCode 1) da versão. Demais avisos (tamanho/R8, mapping, símbolos nativos) são **não bloqueantes**.
- **Acesso ao app:** criada conta de **revisor** dedicada pelo painel, **matriculada no Curso de Supervisores** (senão o gate da V7 mostraria tudo bloqueado ao revisor); credenciais informadas em *Acesso ao app*.
- Passo a passo e disciplina de release em `docs/publicacao_v2_play.md`.

---

## 2. Verificação

- **`:app:assembleDebug` verde** após o Item 1 (pipeline de sync).
- **`:app:bundleRelease` verde** após o Item 3 (release, versão e signingConfig).
- **Painel `py_compile` verde** (todos os módulos novos/alterados); **self-test do hash PBKDF2** ok (salt aleatório, rejeita senha errada e hash inválido).
- **Bootstrap do conteúdo testado** contra os assets reais: 21 markdowns resolvidos, nenhum `contentPath` remanescente, IDs únicos, payload 32 KB.

## 3. Smoke tests pendentes (device/console)

1. **Conteúdo dinâmico:** publicar uma edição no painel → app logado atualiza **sem reinstalar**; progresso preservado; offline mantém a última versão.
2. **Auth de operador:** 1º acesso força troca; admin cria/reseta/desativa operador.
3. **Release:** instalar o `.aab` assinado via Play (teste interno), sem alerta do Play Protect, update limpo sobre a V1.

## 4. Pendências → próximos passos

1. **Smoke test da V2 instalada via Play** (teste interno): login, conteúdo do curso liberado, certificado; **sync de conteúdo dinâmico** (publicar uma edição no painel e ver o app atualizar sem reinstalar); update limpo sobre a versão anterior, sem alerta do Play Protect.
2. **Item 2.1 — LGPD:** revisar a política de privacidade (rascunho do dev) para publicar no Notion — **jurídico-sensível**, revisar com RH/jurídico. Inventário de dados real fornecido nesta sessão.
3. **Item 1.3:** completar os cursos esqueleto pelo painel já pronto.
4. **Backup seguro** da upload key (`.jks` + 3 credenciais) — ainda pendente.
5. **Opcional:** ligar o R8/minify (reduz tamanho + resolve avisos da Play) como tarefa própria com re-teste; enviar símbolos de depuração nativos.
