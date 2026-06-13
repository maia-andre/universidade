# Progresso V2 - Refatoração Arquitetural e Multi-Cursos

## Motivação
A principal motivação para a versão 2 foi criar uma arquitetura escalável que suportasse múltiplos cursos e conteúdos ricos (Markdown). Na versão inicial, os dados do curso eram estáticos, embutidos na camada de dados e o texto das aulas era um grande bloco de String não formatado dentro do arquivo JSON, o que causava um peso excessivo no banco e falta de flexibilidade. Além disso, as builds falhavam frequentemente devido à incompatibilidade do Gradle e do Kotlin.

## Decisões e Arquitetura
1. **Conteúdo Híbrido (Markdown + JSON)**:
   - Extraímos o conteúdo denso das aulas do arquivo `curso_data.json` e o transformamos em arquivos `.md` armazenados na pasta `assets/cursos/`.
   - Adotamos a biblioteca `multiplatform-markdown-renderer-m3` para renderizar o Markdown nativamente no Jetpack Compose, suportando formatação rica (negrito, títulos, listas).
   - O JSON agora serve estritamente como um índice de metadados, contendo o `contentPath` para carregar o Markdown correspondente sob demanda via `DatabaseCallback`.

2. **Suporte Multi-Cursos**:
   - Refatoramos o banco de dados Room (agora na versão 3, sob o nome `universidade_database_v3`).
   - Adicionamos a entidade `CursoEntity` e o `CursoDao`, e incluímos a chave estrangeira indireta `cursoId` na tabela de módulos (`ModuloEntity`).
   - O `CursoRepositoryImpl` foi refatorado para remover o hardcode e passar a puxar todos os cursos, módulos e aulas dinamicamente do Room através do `combine` do Kotlin Flow.

3. **Camada Core (Modularização base)**:
   - Criamos a base de modularização com os pacotes `core/navigation`, `core/designsystem` e `core/utils`.
   - Adicionamos os contratos e mocks para a futura integração de login (`AuthRepository` e `AnonymousAuthRepositoryImpl`).

4. **Nova Funcionalidade - Avaliação e Buscas**:
   - Foram preparadas as bases de interface para a `AvaliacaoScreen` (avaliação de módulo com escala Likert) e `SearchScreen` (busca global).
   - O UseCase `VerificarAprovacaoCursoUseCase` foi estruturado para a futura validação de prova final (nota >= 70% e leitura de 100%).

## Problemas Enfrentados
- **Gradle e Configurações de Build**: Encontramos extrema dificuldade inicial para realizar o sync do Gradle. Foi necessário atualizar as bibliotecas `core-ktx`, `lifecycle`, e introduzir a flag experimental `android.disallowKotlinSourceSets=false` para resolver conflitos entre Kotlin 2.2.10 e as bibliotecas antigas. Habilitamos também o `Configuration Cache` para melhoria de velocidade.
- **Migração do Room (Destructive Migration)**: Tivemos perda de dados silenciosa quando incrementamos a versão do Room de 1 para 2. Como migrações destrutivas não acionam novamente o `onCreate`, as tabelas eram recriadas vazias. A solução foi renomear o arquivo do banco para `universidade_database_v3` forçando uma recriação completa.
- **Acoplamento Hardcoded**: Descobrimos que grande parte do Repositório original continha listas engessadas. Tivemos que desmontar a lógica hardcoded de "Curso de Supervisores" para suportar dinamicamente os 4 cursos de teste.

## Débitos Técnicos
- **Persistência do Quiz (URGENTE)**: O estado da resposta e pontuação do quiz de cada aula não está persistindo adequadamente. Ao sair de uma aula preenchida e retornar, o quiz aparece em branco como se não tivesse sido feito, mesmo que o sistema de persistência identifique a aula como concluída. Necessário implementar a leitura do Room para remontar o estado preenchido e "somente leitura" do Quiz.
- **Tela de Busca Ausente**: A `SearchScreen` foi criada, mas ainda não existe um botão ou ponto de acesso na UI para acessá-la.
- **Card de Progresso Geral Engessado**: O componente de progresso na `HomeScreen` está atualmente buscando dados apenas para o curso de Supervisores (ID 1). Precisa ser refatorado para refletir apenas o "Curso Ativo" do aluno.
- **Limpeza de Bancos Antigos**: Os arquivos físicos dos bancos de dados antigos (`universidade_database` e `universidade_database_v2`) ainda podem estar salvos no emulador/dispositivo. O ideal é implementar uma rotina de limpeza ou simplesmente apagá-los manualmente para não deixar lixo legado, garantindo uma fonte de verdade única.
- **Avisos de Depreciação (Warnings da Build)**: Durante a compilação de release (AAB), surgiram 5 avisos de código depreciado que devem ser atualizados futuramente:
  - `fallbackToDestructiveMigration()` no `AppDatabase.kt` (substituir pela versão sobrecarregada).
  - `Icons.Filled.ArrowBack` no `CursoDetailScreen.kt` e `CursosScreen.kt` (substituir por `Icons.AutoMirrored.Filled.ArrowBack`).
  - `Divider` no `CursoDetailScreen.kt` (substituir por `HorizontalDivider`).
  - `statusBarColor` no `Theme.kt` (depreciado no Java).

## Definição de Papéis e Regras de Negócio
- **Administrador**: Terá acesso a um painel dentro do próprio aplicativo onde poderá cadastrar alunos e liberar o acesso a qual curso eles podem fazer.
- **Aluno**: Só poderá ter **1 (um) curso ativo por vez**. Após a conclusão do curso e aprovação na prova final (emissão do certificado), o curso passa para um histórico de "Concluídos" (acesso vitalício), e o Administrador ou o sistema poderá liberar o próximo curso ativo.

## Lições Aprendidas V2

- Separar conteúdo do banco reduz drasticamente o peso dos dados.
- Markdown é mais sustentável que grandes blocos de texto em JSON.
- Room exige planejamento cuidadoso de migrações.
- Configuração de versões Kotlin/Gradle é um fator crítico de estabilidade.
- Registrar bugs recorrentes acelera projetos futuros.
- Preparar contratos (interfaces) antes da implementação reduz refatorações.

## Sugestões e Próximos Passos (V3)
1. **Resolver o Débito do Quiz**: Na próxima sessão, conectar adequadamente o estado do quiz persistido na `AulaEntity/ProgressoEntity` e refleti-lo na `AulaViewModel` como "Somente Leitura" com os acertos/erros visíveis.
2. **Implementar Prova Final**: Finalizar a lógica para o módulo de avaliação/prova e integração com a tela de emissão de certificado.
3. **Busca Funcional**: Adicionar o atalho da Busca na navegação e ligar a `SearchScreen` com uma query Full-Text Search (FTS) no Room para pesquisar conteúdo nas aulas.
4. **Login e Firebase (Gestão de Papéis)**: Iniciar a transição dos mocks de autenticação para Firebase/Firestore. Implementar a tabela de alunos e o painel in-app do administrador, além da lógica do "Curso Ativo" único por aluno.
5. **Módulo de Ferramentas Práticas**: Adicionar um novo card/painel chamado "Ferramentas" na tela inicial, abaixo dos cursos. Ele conterá utilitários interativos (ex: Matriz SWOT, 5W2H) para o aluno aplicar na prática os conceitos vistos durante os cursos.
