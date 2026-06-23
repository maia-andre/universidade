# Design — Conteúdo Dinâmico (V8 Item 1)

Data: 22 de junho de 2026
Status: **implementado (Fases 1.1a + 1.2) — `:app:assembleDebug` verde, painel `py_compile` verde.** Ver §10 (as-built). Falta o smoke test no device/console (§8).
Planejamento de origem: `docs/backlog_v8.md` Item 1 (legado V7 Item 6).

> **Objetivo (do backlog):** permitir que o RH atualize o conteúdo dos cursos (cursos / módulos / aulas) **sem republicar APK**, lendo o conteúdo de uma origem remota em runtime em vez de assá-lo no Room no seed.
>
> **Critério de pronto (do backlog):** o RH publica/edita um curso pelo painel e ele aparece/atualiza no app **sem novo APK**.

---

## 1. Contexto medido (o que decide o design)

- **O conteúdo é minúsculo.** `curso_data.json` = **30 KB**; os 21 Markdowns = **11 KB**. Catálogo + conteúdo ≈ **42 KB**, contra o limite de **1 MiB por documento** do Firestore (~24× de folga).
  → **O catálogo inteiro cabe num único documento Firestore.** Não precisamos de Storage nem de coleção fragmentada agora.
- **As rules já reservam a porta.** `firestore.rules` já tem `match /config/{doc}` — leitura por autenticados, escrita negada ao cliente (só Admin SDK / painel). Zero mudança de rules para o caminho feliz.
- **O padrão de leitura-runtime já existe.** `CursoRepositoryImpl.getCargaHoraria()` e `ProvaFinalRepositoryImpl.getPerguntas()` usam `Mutex` + `@Volatile` cache + DTO `@Serializable`. É o molde.
- **Offline-first é requisito.** O app é offline; o conteúdo precisa estar disponível **sem rede**. Logo, qualquer origem remota precisa de **cache local** — e o cache local natural já existe: o **Room**.
- **Progresso é separado e durável.** `ProgressoEntity` é keyed por `aulaId`, numa tabela à parte, sobreposto reativamente via `combine()`. Atualizar o conteúdo **não pode** apagar progresso.

---

## 2. Decisão central — "Sync para o Room, com versionamento"

**Abordagem escolhida (A): a origem remota alimenta o Room por upsert; o grafo de domínio continua sendo lido do Room.**

O `CursoRepositoryImpl.getCursos()` já reconstrói o grafo combinando `cursoDao`/`moduloDao`/`aulaDao` com `progressoDao`. **Não mexemos nesse grafo.** Mudamos só a *origem* do conteúdo: em vez de "assar uma vez no `onCreate`", passamos a ter um **passo de sync** que faz upsert de cursos/módulos/aulas no Room quando há versão nova — preservando `ProgressoEntity` (keyed por `aulaId`).

Por que A (e não "ler do Firestore a cada sessão"):
- **Mantém offline-first de graça** — o conteúdo fica no Room, disponível sem rede.
- **Reusa todo o grafo `combine()` existente** — o overlay de progresso, busca (`SearchDao`), favoritos etc. continuam funcionando sem refactor.
- **É o mesmo princípio do V7** (estado durável reconstituído do Firestore), agora aplicado a conteúdo.

**Origem:** **um único documento Firestore `config/conteudo`**, versionado, contendo o catálogo no **mesmo formato do `curso_data.json`** (cursos → módulos → aulas → quiz/provaFinal, com `conteudo` inline em vez de `contentPath` — a fonte remota carrega o Markdown embutido).

```
config/conteudo
  versao: number          // monotônico; o app sincroniza quando remoto > local
  publicadoEm: timestamp
  publicadoPor: string    // operador (auditoria, como matriculas.liberadoPor)
  cursos: [ { id, titulo, descricao, cargaHoraria, provaFinal:[...],
              modulos:[ { id, titulo, descricao,
                          aulas:[ { id, titulo, conteudo, quiz:[...] } ] } ] } ]
```

> **Escape hatch (não agora):** se o conteúdo um dia se aproximar de 1 MiB (ex.: muitos cursos com Markdown longo/imagens em base64), fragmentar para `config/conteudo_index` + `conteudo/curso_{id}`. Com 42 KB hoje, doc único é o certo; registrar o limite e medir a cada publicação.

---

## 3. Fluxo de sync

```
App abre (logado)  ──►  SyncConteudoUseCase
                          │
                          ├─ lê versaoLocal (DataStore)
                          ├─ lê config/conteudo.versao (Firestore, 1 get)
                          │
                          ├─ remoto > local ?
                          │      ├─ não → no-op (usa o que está no Room)
                          │      └─ sim → baixa o doc, faz UPSERT no Room
                          │               (cursos/modulos/aulas), grava versaoLocal
                          │
                          └─ sem rede / erro → no-op silencioso (offline-first)
```

- **Gatilho:** no boot já-logado (junto do sync de matrículas/conclusões que já roda na `HomeViewModel.init`). Custo: **1 leitura Firestore** quando não há novidade (só lê `versao`); o doc inteiro só quando há atualização. (Refinável: ler `versao` de um doc leve `config/conteudo_meta` se quisermos evitar baixar 42 KB só para comparar — provavelmente desnecessário nessa escala.)
- **Onde mora a versão local:** **DataStore** (`UserPreferencesRepository`), não uma coluna nova no Room — **evita migração de schema** e segue o padrão de "preferências/estado fora do DB de conteúdo".
- **Upsert preservando progresso:** `INSERT ... ON CONFLICT REPLACE` nas tabelas `cursos`/`modulos`/`aulas` (que **não** têm progresso). `ProgressoEntity` (keyed por `aulaId`) **não é tocada** → progresso sobrevive. Aulas removidas no remoto: decidir política (ver §6, risco de IDs órfãos).
- **Seed inicial continua dos assets.** Numa instalação nova **sem rede**, o `onCreate` semeia do `curso_data.json` (como hoje) → o app nunca abre vazio. O remoto **sobrepõe** quando `versao` remota > a versão semeada. O `curso_data.json` ganha um campo `versao` (= versão de baseline assada no APK).

---

## 4. Contrato de IDs estáveis — o ponto crítico

Todo o esquema depende de **IDs inteiros estáveis** para curso/módulo/aula, porque:
- o **progresso** é keyed por `aulaId` — mudar o ID de uma aula = perder o progresso dela;
- **matrículas/conclusões** (Firestore) são keyed por `cursoId`;
- o upsert usa o ID como chave de conflito.

**Regra:** o painel **nunca reusa nem renumera IDs**. IDs são alocados uma vez e imutáveis; "editar" muda título/descrição/conteúdo/quiz, **não** o ID. Aulas/cursos novos recebem IDs novos de um contador que só cresce. **Isso precisa ser garantido no painel (Item 1.2), não no app.**

---

## 5. Camadas a tocar

### App Android
| Camada | Mudança | Arquivo |
|---|---|---|
| `domain/repository` | `ConteudoRemotoRepository` (interface): `getVersaoRemota()`, `baixarConteudo(): ConteudoRemoto?` | novo |
| `data/repository` | `ConteudoRemotoRepositoryImpl` (Firestore `config/conteudo`, padrão `await()` como `PlataformaRepositoryImpl`) | novo |
| `domain/usecase` | `SyncConteudoUseCase` (compara versão, dispara upsert) | novo |
| `data/.../dao` | `upsertCursos/Modulos/Aulas` (`@Insert(onConflict = REPLACE)`) | DAOs existentes |
| `core/.../preferences` | `versaoConteudo` em DataStore (get/set) | `UserPreferencesRepository` |
| `ui/home` | disparar `SyncConteudoUseCase` no `init` (junto do sync atual) | `HomeViewModel` |
| assets | `versao` de baseline no `curso_data.json` | `curso_data.json` |

**Sem migração de DB** se a versão ficar no DataStore e não criarmos colunas novas. (Se preferirmos uma `meta` table no Room, aí sim v7→v8 + schema export — proposta é evitar.)

### Painel RH (Python) — Item 1.2
- Nova página `pages/4_Conteudo.py`: editar cursos/módulos/aulas (Markdown por aula + metadados + quiz/provaFinal).
- `services/conteudo.py`: ler/gravar `config/conteudo`; **publicar = `versao += 1` + gravar doc + carimbar `publicadoPor/publicadoEm`** (espelha `matriculas.liberar_curso`).
- Alocação de IDs estáveis (contador persistido, ex.: `config/conteudo_seq`).
- `config.py`: `CURSOS` hardcoded passa a derivar do conteúdo publicado (encerra o débito do catálogo duplicado).

---

## 6. Riscos e decisões em aberto

1. **IDs estáveis (§4)** — maior risco. Mitigação: alocação central no painel; nunca renumerar. Aula deletada no remoto → **manter a entidade no Room mas marcar oculta**, ou remover entidade e **deixar o `ProgressoEntity` órfão** (inócuo, keyed por `aulaId`). Proposta: marcar oculta (não destrutivo, reversível).
2. **Comparar versão sem baixar 42 KB** — opcional `config/conteudo_meta` só com `versao`. Provavelmente desnecessário nessa escala; decidir na implementação.
3. **provaFinal/cargaHorária hoje vêm dos assets em runtime** — passam a vir do mesmo doc remoto. Manter os leitores de assets como **fallback** quando não há doc remoto, ou migrá-los para ler do Room pós-sync. Proposta: pós-sync, ler tudo do Room (uma fonte só), com assets como baseline de seed.
4. **Markdown com imagens** — hoje os Markdowns são texto puro. Se o RH inserir imagens, voltamos ao tema Storage. Fora de escopo agora; sinalizar limite.
5. **Autoria concorrente no painel** — dois operadores publicando. Improvável (RH pequeno); usar `versao` como trava otimista se necessário. Fora de escopo agora.

---

## 7. Faseamento proposto (incrementos com build verde)

- **Fase 1.1a — Pipeline de sync (app), sem painel novo:**
  popular `config/conteudo` **uma vez** com o catálogo atual (script/painel pontual) → implementar `ConteudoRemotoRepository` + `SyncConteudoUseCase` + DataStore + upsert DAOs + gatilho no `HomeViewModel`. **Critério:** editar o doc no console e ver o app atualizar sem APK novo.
- **Fase 1.2 — Página de Conteúdo no painel:**
  CRUD + alocação de IDs estáveis + publicar (versão++). **Critério:** RH edita pelo painel e o app reflete.
- **Fase 1.3 — Completar cursos esqueleto** (Patrimônio, Licitação, Almoxarifado) — trabalho de conteúdo, via painel já pronto.

Ordem recomendada para hoje: **Fase 1.1a** (a espinha dorsal de eng., 100% verificável no device/console), depois 1.2.

---

## 8. Verificação

- `:app:assembleDebug` verde a cada etapa (padrão V6/V7); painel `py_compile` verde.
- **Smoke test:** publicar uma edição (mudar título de uma aula / corrigir um texto) → reabrir o app logado → conteúdo atualizado **sem reinstalar**; progresso/favoritos **preservados**; modo avião → conteúdo (última versão) ainda disponível.
- `firestore.rules`: confirmar `config/conteudo` legível pelo cliente e não-gravável (já coberto).

---

## 9. Decisões que preciso confirmar antes de codar

1. **Origem = Firestore `config/conteudo` doc único** (vs Storage / coleção fragmentada). Recomendado pelos 42 KB + rules prontas.
2. **Versão local no DataStore** (sem migração de DB) vs `meta` table no Room (com migração v7→v8). Recomendo DataStore.
3. **Escopo de hoje = Fase 1.1a** (pipeline de sync ponta-a-ponta, conteúdo inicial carregado por script/console) antes de construir a página de autoria do painel (1.2).
4. **Aula removida no remoto = marcar oculta** (não destrutivo) vs remover entidade.

> **Aprovado (2026-06-22):** arquitetura central + escopo **1.1a + 1.2** na mesma sessão.

---

## 10. As-built (o que foi efetivamente implementado)

Implementado conforme o design, com **5 refinamentos** decididos durante a execução:

1. **Documento Firestore com campo `json` string.** `config/conteudo` guarda `{ versao:int, json:string, publicadoPor, publicadoEm }`, onde `json` é o catálogo inteiro serializado (mesmo formato do `curso_data.json`). Mais simples que mapear arrays/maps aninhados do Firestore nos dois lados; o app lê a string e reusa os DTOs `@Serializable`.
2. **"Aula removida = não destrutivo" via full-replace em transação** (não via coluna `oculta`). O sync apaga `cursos/modulos/aulas` e reinsere o catálogo publicado dentro de `appDatabase.withTransaction { }`; **`ProgressoEntity` (keyed por `aulaId`) não é tocada**. Resultado: conteúdo removido some da UI, progresso intacto, e **reversível** — republicar a aula com o mesmo `aulaId` reanexa o progresso. **Sem migração de schema** (DB segue v7).
3. **Versão de baseline implícita (0) no DataStore.** O `curso_data.json` é um array JSON; adicionar um campo `versao` quebraria os parsers. Em vez disso, `UserPreferencesRepository.versaoConteudo` começa em 0; o 1º doc publicado (versão ≥1) sobrepõe. Sem mudança estrutural no asset.
4. **🔴 Bug crítico corrigido — colisão de IDs entre cursos.** Os cursos esqueleto reusavam IDs de módulo/aula do Curso 1 (módulos 200/300/400; aulas 201/301/401). Como `id` é PrimaryKey, um seed/sync limpo faria o esqueleto **sobrescrever** módulos do Curso 1. Renumerados para faixas únicas: Curso 2 → 2000/2001, Curso 3 → 3000/3001, Curso 4 → 4000/4001. O Curso 1 ficou **intocado** (progresso preservado). Validado: todos os IDs de módulo/aula agora são globalmente únicos.
5. **`ConteudoLocalSource` como fonte única.** Lê do arquivo `filesDir/conteudo_remoto.json` (sincronizado) ou do asset baseline. `getCargaHoraria` e `getPerguntas` (prova final) foram **refatorados para ler dele** — uma fonte só, e ambos passam a refletir conteúdo publicado offline-persistente.

**Arquivos (app):** `data/local/ConteudoLocalSource.kt` (novo), `domain/repository/ConteudoRemotoRepository.kt` (novo), `data/repository/ConteudoRemotoRepositoryImpl.kt` (novo), `domain/usecase/SincronizarConteudoUseCase.kt` (novo); editados `CursoRepository(+Impl)`, `ProvaFinalRepositoryImpl`, `Curso/Modulo/AulaDao` (+`deleteAll`), `UserPreferencesRepository` (+`versaoConteudo`), `AppModule` (bind), `HomeViewModel` (gatilho no `init`), `curso_data.json` (renumeração).

**Arquivos (painel):** `services/conteudo.py` (novo: carregar/bootstrap dos assets/publicar + IDs estáveis), `pages/4_Conteudo.py` (novo: autoria + publicar), `config.py` (+`COL_CONFIG`/`DOC_CONTEUDO`).

**Rules:** **nenhuma mudança necessária** — `firestore.rules` já tem `match /config/{doc}` (leitura por autenticados, escrita negada ao cliente; o painel grava via Admin SDK, que ignora rules). Confirmado.

**Pendente:** smoke test no device/console (§8) — publicar uma edição pelo painel e ver o app atualizar sem reinstalar, com progresso preservado.
