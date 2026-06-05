# 🎓 Universidade do Servidor

<div align="center">
  <img src="docs/ic_launcher-playstore.png" width="150" alt="Logo Universidade do Servidor">
  <br><br>
  <img src="docs/feature_graphic_sjc.png" alt="Feature Graphic Universidade do Servidor">
</div>

<br>

Plataforma offline de capacitação continuada e desenvolvimento profissional para os servidores da Prefeitura Municipal de São José dos Campos. Atualizado para a **Versão 2** com arquitetura multi-cursos e renderização de aulas em Markdown.

---

## 🛠️ Stack Tecnológica

![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Room Database](https://img.shields.io/badge/Room_Database-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Dagger Hilt](https://img.shields.io/badge/Dagger_Hilt-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Material 3](https://img.shields.io/badge/Material_3-7D5260?style=for-the-badge&logo=materialdesign&logoColor=white)
![KSP](https://img.shields.io/badge/KSP-8A2BE2?style=for-the-badge&logo=kotlin&logoColor=white)

---

## 🎨 Identidade Visual e Identidade Municipal

O aplicativo foi projetado com uma estética premium e totalmente integrada às cores e símbolos oficiais de **São José dos Campos**:
* **Paleta de Cores:** Combinação harmoniosa de Azul Oficial (`#003882`) e Ouro/Dourado (`#FFD700`), alinhada à bandeira municipal.
* **Splash Screen Animada:** O logotipo do aplicativo une o capelo universitário com a palavra `UNIVERSIDADE DO SERVID[O]R`, onde a letra **O** é substituída pelo desenho vetorial da **engrenagem da bandeira de SJC** que rotaciona em 360° de forma contínua usando Canvas nativo do Compose.

---

## 🌟 Funcionalidades (V2 - Arquitetura Escalável)

* **Suporte Multi-Cursos Dinâmico:** Banco de dados refatorado (Room V3) que carrega dinamicamente múltiplos cursos, módulos e aulas de forma escalável.
* **Conteúdo Rico em Markdown:** Aulas formatadas com arquivos `.md` permitindo uma leitura muito mais agradável, com renderização nativa de títulos, listas e negritos no Jetpack Compose.
* **Dashboard de Progresso (Home):** Exibição em tempo real do progresso de leitura do usuário (aulas lidas, porcentagem de conclusão e barra de progresso gráfica) e atalho rápido para as aulas marcadas como favoritas.
* **Catálogo de Cursos:** Tela que gerencia os cursos da plataforma. Cursos indisponíveis aparecem bloqueados visualmente, preparando terreno para gestão via painel do administrador.
* **Menus Aninhados:** Detalhes do curso estruturados com módulos expandíveis que revelam suas respectivas aulas.
* **Quiz de Fixação:** Ao final de cada aula, o servidor deve responder a um quiz interativo. O acerto total valida a aula como concluída, salvando o progresso offline.
* **Bases para o Futuro:** Estruturas criadas para busca global em texto (SearchScreen) e sistema de avaliação de módulos por escala Likert.

---

## 🗄️ Fluxo de Dados e Conteúdo

A Versão 2 introduziu uma separação inteligente de dados para otimizar o banco local:

```mermaid
graph LR
    JSON[curso_data.json<br>Apenas Metadados] -->|Mappers| Room[(Room Database V3<br>Cursos, Módulos, Aulas)]
    MD[Pasta assets/cursos/<br>Arquivos .md] -->|Carregamento sob demanda| Renderer[Markdown Renderer]
    Room --> ViewModel
    Renderer --> ViewModel
    ViewModel --> UI[Jetpack Compose UI]
```

---

## 🔄 Fluxo de Navegação do Aplicativo

O diagrama abaixo ilustra o comportamento do usuário e o fluxo de transições de telas dentro do app:

```mermaid
graph TD
    Splash[Splash Screen SJC Logo Animado] -->|Transição 2.5s| Home[Home Dashboard - Progresso & Favoritos]
    Home -->|Botão Acessar Cursos| Cursos[Cursos Screen - Ativos & Futuros]
    Cursos -->|Clique no Curso de Supervisores| Detail[Curso Detail - Módulos & Sub-módulos expandíveis]
    Detail -->|Clique na Aula| Aula[Leitor de Aula - Conteúdo & Favoritar]
    Aula -->|Responder Perguntas| Quiz{Acertou as 2 questões?}
    Quiz -->|Sim| Completed[Marcar Conclusão no Room & Atualizar Progresso na Home]
    Quiz -->|Não| TryAgain[Feedback de Erro - Botão Tentar Novamente]
```

---

## 📐 Arquitetura e Modularização

O projeto foi construído sobre as diretrizes da **Clean Architecture** aliada ao padrão **MVVM (Model-View-ViewModel)** e com os primeiros passos para uma **modularização por features** (Core, UI, Data, Domain):

```txt
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer (Compose)                    │
│    (Telas modulares, Core Design System, Markdown UI)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observa StateFlow / Envia Eventos
┌──────────────────────────────▼──────────────────────────────┐
│                    ViewModel Layer (Hilt)                   │
│         (Armazena estado da tela, gerencia o fluxo)         │
└──────────────────────────────┬──────────────────────────────┘
                               │ Dispara Ações
┌──────────────────────────────▼──────────────────────────────┐
│                     Domain Layer (UseCases)                 │
│    (Modelos puros Kotlin, Regras de progresso e prova)      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Acessa Contratos (Interfaces)
┌──────────────────────────────▼──────────────────────────────┐
│                      Data Layer (Room)                      │
│ (Repositório unificado, leitura de Assets e banco Room V3)  │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Como Executar o Projeto

1. Certifique-se de que o **Android Studio** esteja atualizado (compatível com Kotlin 2.2.x).
2. Clone este repositório e abra o projeto.
3. Aguarde a sincronização do Gradle (que irá baixar as dependências catalogadas em `libs.versions.toml`).
4. Execute o aplicativo em um Emulador ou dispositivo físico conectado rodando Android 8.0 (API 28) ou superior.

---
