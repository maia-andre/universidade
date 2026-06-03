# Relatório de Progresso — Versão 1

Data: 3 de junho de 2026  
Status: Fase 1 (MVP Local) Finalizado

Este documento relata as entregas realizadas na primeira sessão de desenvolvimento do aplicativo **Universidade do Servidor**, detalhando as decisões de arquitetura, escolhas técnicas, débitos técnicos identificados e os próximos passos sugeridos para a evolução da plataforma.

---

## 1. O Que Foi Entregue (Fase 1)

Implementamos a fundação completa do aplicativo no padrão **Single Activity** com **Jetpack Compose**, contendo:
* **Splash Screen:** Animação customizada em Canvas da engrenagem do brasão da Prefeitura de São José dos Campos (SJC) rotacionando em loop e logotipo combinando o capelo de formatura com o nome do app.
* **Home Dashboard:** Tela inicial com resumo de progresso reativo (aulas concluídas, percentual de conclusão e barra de progresso) e atalho para a listagem das aulas marcadas como favoritas.
* **Plataforma de Cursos:** Tela listando o "Curso de Supervisores" ativo e outros 7 cursos marcados visualmente como indisponíveis (em itálico, opacidade reduzida e com ícone de cadeado).
* **Grade de Módulos (Menu Aninhado):** Visualização do currículo do curso por meio de cards expandíveis que revelam as aulas (sub-módulos). Módulos de aula única direta (Estatuto e Acessibilidade) navegam diretamente ao clique, sem necessidade de expansão.
* **Leitor de Aulas e Favoritos:** Tela de leitura limpa com controle de favoritamento (coração) reativo que persiste no banco de dados local.
* **Quiz de Fixação Integrado:** Duas perguntas de múltipla escolha ao final de cada aula. O acerto de ambas as questões concede feedback de sucesso em verde e marca a aula como concluída no Room, recalculando o progresso da Home. Respostas erradas geram feedback em vermelho e possibilitam tentar novamente.

---

## 2. Decisões Técnicas e Arquitetura

### MVVM + Clean Architecture
Optamos por dividir o código-fonte em camadas bem definidas (`ui`, `viewmodel`, `domain`, `data` e `di`), mantendo a seguinte responsabilidade:
1. **Domain Layer:** Contém os modelos puros de Kotlin (`Curso`, `Modulo`, `Aula`, `QuizPergunta`) e as regras de negócio cruas via `UseCases`. Esta camada não possui dependências do framework do Android, facilitando testes unitários rápidos.
2. **Data Layer:** Responsável pelo gerenciamento do banco de dados Room (`entities`, `dao`, `AppDatabase`) e pela leitura do arquivo JSON semente. Implementa o `CursoRepository` convertendo entidades do banco em modelos do domínio por meio de mappers.
3. **UI/ViewModel Layer:** Controla o estado de tela exposto por meio de `StateFlow` reativos e intercepta eventos do usuário usando Jetpack Compose e Material 3.

* **Por que essa arquitetura?**  
  Garante alta testabilidade e escalabilidade. Se na Fase 2 decidirmos ler as aulas de uma API externa (Retrofit) ao invés do arquivo JSON local, alteramos **apenas** a implementação na camada de dados (`CursoRepositoryImpl`), sem tocar em uma única linha de código das telas ou ViewModels.

### Kotlin Symbol Processing (KSP) em vez de Kapt
A arquitetura inicial sugeria `kapt` para processamento das anotações do Hilt e do Room. Substituímos por **KSP**.
* **Motivo:** O KSP é até 2x mais rápido que o Kapt durante as compilações e oferece suporte nativo e otimizado para o Kotlin 2.x e o novo compilador K2, evitando a geração de stubs Java redundantes.

### Navegação Segura (Type-Safe Navigation)
Implementamos a nova navegação fortemente tipada do Navigation Compose (2.8+).
* **Motivo:** Em vez de definir rotas como strings (ex: `"aula/{aulaId}"`) que podem conter erros de digitação e exigem tratamento manual de argumentos, definimos as rotas como classes `@Serializable` (ex: `data class Aula(val aulaId: Int)`). O próprio framework realiza o parse de forma transparente e segura em tempo de compilação.

### Separação de Dados Estáticos e Dinâmicos (Tabela de Progresso)
As tabelas `modulos` e `aulas` guardam o conteúdo carregado do JSON. Criamos a tabela `progresso` separada para mapear apenas o `aulaId`, se a aula foi lida (`isCompleted`) e se foi favoritada (`isFavorite`).
* **Motivo:** Evita operações de atualização de linhas (UPDATE) gigantes nas tabelas de conteúdo estático. A tabela de progresso atua de forma isolada e otimizada para escrita.

---

## 3. Débitos Técnicos

1. **Leitura Assíncrona do JSON no Database Callback:**  
   No primeiro acesso do app, o callback do Room lê o arquivo `curso_data.json` da pasta assets e insere os dados no banco. Para o MVP isso funciona perfeitamente, mas se a base de dados de aulas crescer para gigabytes, pode haver um atraso na primeira abertura.
2. **Conteúdo em String Plana:**  
   O texto das aulas é armazenado no banco como String crua. Não há suporte nativo para formatação avançada de textos (como negrito, itálicos e títulos no corpo do texto) a não ser que usemos strings formatadas em HTML ou Markdown.
3. **Escopo dos ViewModels:**  
   Todos os ViewModels e componentes de dados usam os escopos padrão do Hilt. Em fases futuras de produção, escopos customizados podem ajudar na limpeza de memória.

---

## 4. Próximos Passos (Sugestões de Melhoria)

1. **Suporte a Markdown no Leitor de Aulas:**  
   Adicionar a biblioteca `compose-markdown` para interpretar o conteúdo em formato Markdown. Isso permitirá estruturar as aulas com subtítulos, links clicáveis e até imagens remotas ou locais de forma simples e dinâmica.
2. **Histórico e Estatísticas de Quizzes:**  
   Criar uma tabela no Room para salvar o histórico de tentativas de quiz do usuário. Isso permitiria exibir um dashboard de pontuação na Home, informando quais temas o servidor possui maior domínio.
3. **Modo Escuro / Light Toggle:**  
   Embora o tema já responda ao padrão do sistema, seria excelente adicionar um interruptor (Switch) de tema manual nas configurações do app para maior conforto visual.
4. **Fase 2 - Sincronização Rest API:**  
   Preparar a criação de um serviço Web básico (ex: Node.js ou Spring Boot) para expor o JSON de aulas. Implementar no aplicativo o Retrofit + WorkManager para baixar atualizações de conteúdo em segundo plano.
