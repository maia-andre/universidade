# Relatório de Progresso — Versão 7 (Branch `features_v7`)

Data: 21 de junho de 2026
Status: **implementação concluída e validada no device.** Fase de **correção e maturidade** pós-smoke test da V6 — fecha o bug de controle de acesso, dá ao aluno autonomia de senha, aproxima o certificado do padrão do RH e amadurece o Painel RH.

Branch saída da `main` (já com V1–V6). Planejamento em `docs/backlog_v7.md`; os itens 6 e 7 foram movidos para `docs/backlog_v8.md`. Execução incremental com **build verde a cada etapa**.

---

## 1. O Que Foi Entregue

### Item 1 — 🔴 Controle de acesso por matrícula (bug crítico da V6)
O smoke test da V6 revelou que **todos os cursos ficavam acessíveis**, independentemente de matrícula — o aluno concluía e certificava curso não liberado. Causa raiz: o catálogo listava todos os cursos `isAvailable`, "curso ativo" era só um destaque (não um portão), e a heurística "entrar no curso → vira ativo" reforçava a falsa liberação.

- **Regra (definida com o RH):** `acessível = matriculado OU concluído`. O catálogo mostra **todos** os cursos, cada um com um estado: **Ativo** (matriculado, em andamento), **Concluído** (acessível para sempre) ou **Bloqueado** (cadeado, não navegável).
- **Camada de dados:** `UserPreferencesRepository` ganhou os conjuntos `cursosMatriculados`, `cursosConcluidos` e o derivado `cursosAcessiveis`. `PlataformaRepository` ganhou `getCursosMatriculados`/`getCursosConcluidos`. O sync na `HomeViewModel.init` (ampliação do D3) popula ambos.
- **Concluído durável:** sobrevive à reinstalação (que apaga o Room local) — alimentado tanto na emissão do certificado (`adicionarCursoConcluido`) quanto pelo sync downstream das `conclusoes` do Firestore.
- **Gate em profundidade:** catálogo renderiza os 3 estados; `CursoDetailViewModel` nega entrada a curso bloqueado; `Home`/`Desempenho` só escolhem curso acessível. A **heurística** foi removida (encerra a pendência #5 da V6).
- **`firestore.rules`:** gravar `conclusoes/{uid}_{cursoId}` agora **exige a matrícula correspondente** — defesa em profundidade contra cliente adulterado.

### Item 2 — Trocar a própria senha no app
- `AuthRepository.trocarSenha`: reautentica com a senha atual (`EmailAuthProvider`) antes do `updatePassword` (o Firebase exige login recente).
- UI em **Configurações → Conta** (diálogo: senha atual + nova + confirmação), validação de ≥6 caracteres e mensagens de erro mapeadas.
- **Ajuste pós-device:** a tela de Configurações virou rolável — a nova seção empurrava o botão "Sair da conta" para fora da viewport.

### Item 3 — Refino do certificado
- **Brasão de SJC** embutido no rodapé, em linha central: logo Universidade do Servidor · URL · brasão (`res/drawable-nodpi/brasao_sjc.png`).
- **Carga horária real:** Curso de Supervisores **33h** (lida em runtime, chega sem re-seed); cursos esqueleto mockados em 4h.
- **Cabeçalho "capelo":** ornamento do topo simétrico (banner com dip central + acentos ouro), aprovado no device. Faixa cinza do rodapé removida.
- Geometria do Canvas afinada **por screenshot** (não renderizável localmente).

### Item 4 — Auto-nome no certificado
- O nome vem do cadastro do RH via `ObterNomeServidorUseCase` (`getNomeServidor`), mantendo o campo digitável como fallback offline. Encerra a pendência #4 da V6.

### Item 5 — Painel RH
- **Desmatricular** (encerrar matrícula, `status="encerrada"` + auditoria): o curso deixa de ser acessível no app, salvo se já concluído.
- **Situação por aluno** nos Relatórios: cruza `servidores` × `matriculas` × `conclusoes` (estado da matrícula + flag de concluído).
- Múltiplas matrículas por aluno já eram suportadas (cada liberação é um doc).
- Veio junto na branch o trabalho de painel que ficou **fora do PR #5 da V6**: autenticação por operador (`OPERADORES`) + redefinir senha via Admin SDK.

---

## 2. Decisões Técnicas

- **Acesso = matriculado OU concluído.** "Curso ativo" voltou a ser apenas o destaque da Home/Desempenho, não o portão de acesso.
- **Concluído durável.** Como o Room local é apagado na reinstalação, o estado "concluído" é reconstituído pelo sync das `conclusoes` do Firestore — senão um curso concluído voltaria a aparecer bloqueado.
- **Defesa em profundidade.** O acesso é gateado na UI **e** nas `firestore.rules` (conclusão exige matrícula), para um cliente adulterado não burlar.
- **Certificado iterado por screenshot.** O gerador usa `Canvas`/`PdfDocument` (Android), não renderizável no ambiente de dev — a geometria foi ajustada com base nas imagens do device.
- **Build verde incremental.** `assembleDebug` a cada etapa (Itens 1; 2/4; 3; ajustes), no padrão da V6; painel validado por `py_compile`.

---

## 3. Débitos Técnicos e Pendências (→ V8)

1. **Conteúdo dinâmico + catálogo** (ex-Item 6): maior fase, movida para a **V8 Item 1**.
2. **LGPD** (ex-Item 7.1) + **auth real de operador** no painel (ex-V6 #6 / V7 5.4): **V8 Item 2**.
3. **Publicação da V2 no teste interno da Play** (distribuição + migração da upload key): **V8 Item 3** — pré-requisito é a chave no PC pessoal.
4. **Certificado:** o "capelo" foi aprovado, mas **não é idêntico** ao `docs/Modelo.pdf`; a carga horária dos cursos esqueleto é **mockada** (4h) até virem os valores oficiais.
5. **Re-emitir certificado** de curso sem matrícula falha com as rules novas — comportamento esperado (o acesso de leitura/concluído permanece).
6. **Aparelho compartilhado** (progresso por `uid`): segue congelado (`backlog_congelado.md`, C1).

---

## 4. Verificação

- **Build de debug verde** em todas as etapas (`:app:assembleDebug`); painel `py_compile` verde.
- **Smoke test no device (validado):** estados do catálogo (Ativo/Concluído/Bloqueado) com o comportamento esperado; conclusão de curso refletindo no painel; desmatricular; certificado (cabeçalho "capelo" aprovado, nome do cadastro); **troca de senha** e o fix do botão "Sair".
- **`firestore.rules` publicadas** no console (a regra de conclusão→matrícula passou a valer).

---

## 5. Próximos Passos

1. **Merge da `features_v7`** na `main` (via PR).
2. **V8:** começar pela **publicação da V2** no teste interno da Play assim que a upload key estiver no PC pessoal (Caminho A), depois conteúdo dinâmico e LGPD.
3. **Certificado:** afinar a geometria contra o `Modelo.pdf` se o RH pedir; inserir cargas horárias oficiais dos demais cursos.

---

## 6. Lições Aprendidas V7

- **Estado de acesso precisa ser durável.** Atrelar "concluído" só ao progresso local quebraria na reinstalação; o registro no Firestore é a fonte durável.
- **Decidir a regra antes de codar.** Fechar `acessível = matriculado OU concluído` com o usuário antes da implementação evitou retrabalho no gate.
- **Adicionar conteúdo a uma `Column` não-rolável empurra o fim para fora da tela.** O botão "Sair" sumiu por isso — `verticalScroll` é o padrão para telas de formulário/lista de opções que podem crescer.
- **Geometria de Canvas/PDF se afina no device.** Sem render local, as mudanças visuais são primeira aproximação até o screenshot — vale entregar primeiro o que é de alta confiança (brasão presente, carga horária) e iterar o posicionamento depois.
- **Trabalho não commitado não entra no merge.** A feature de painel da V6 ficou fora do PR #5 por estar só no working tree — recuperada e commitada no arranque da V7.
