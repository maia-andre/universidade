# Arquitetura Base — App de Conteúdo de Curso (Kotlin + Android)

## Objetivo

Aplicativo simples para:

* visualizar módulos do curso;
* abrir aulas/conteúdo;
* marcar progresso;
* salvar favoritos;
* funcionar offline;
* servir como laboratório para publicação na Play Store.

---

# Stack Recomendada

* Kotlin
* Jetpack Compose
* MVVM
* Room Database
* Coroutines + Flow
* Hilt (injeção de dependência)
* Navigation Compose
* Material 3

---

# Estrutura de Pastas

```txt
com.seuapp.treinamento
│
├── ui/
│   ├── screens/
│   │   ├── home/
│   │   ├── modulo/
│   │   ├── aula/
│   │   ├── quiz/
│   │   └── favoritos/
│   │
│   ├── components/
│   └── theme/
│
├── viewmodel/
│   ├── HomeViewModel.kt
│   ├── ModuloViewModel.kt
│   ├── AulaViewModel.kt
│   └── QuizViewModel.kt
│
├── domain/
│   ├── model/
│   │   ├── Modulo.kt
│   │   ├── Aula.kt
│   │   └── Progresso.kt
│   │
│   ├── repository/
│   │   ├── ModuloRepository.kt
│   │   └── AulaRepository.kt
│   │
│   └── usecase/
│       ├── GetModulosUseCase.kt
│       ├── GetAulasUseCase.kt
│       └── MarcarConclusaoUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   └── AppDatabase.kt
│   │   │
│   │   ├── dao/
│   │   │   ├── ModuloDao.kt
│   │   │   ├── AulaDao.kt
│   │   │   └── ProgressoDao.kt
│   │   │
│   │   └── entities/
│   │       ├── ModuloEntity.kt
│   │       ├── AulaEntity.kt
│   │       └── ProgressoEntity.kt
│   │
│   ├── mapper/
│   │   ├── ModuloMapper.kt
│   │   └── AulaMapper.kt
│   │
│   └── repository/
│       ├── ModuloRepositoryImpl.kt
│       └── AulaRepositoryImpl.kt
│
├── di/
│   └── AppModule.kt
│
└── MainActivity.kt
```

---

# Fluxo da Aplicação

```txt
Usuário toca na interface
        ↓
Compose Screen
        ↓
ViewModel
        ↓
UseCase
        ↓
Repository
        ↓
Room Database
        ↓
Dados retornam via Flow
        ↓
UI atualiza automaticamente
```

---

# Camadas

## 1. UI Layer (Compose)

Responsável por:

* telas;
* componentes;
* navegação;
* renderização visual.

Exemplo:

* HomeScreen
* AulaScreen
* QuizScreen

A UI:

* NÃO acessa banco diretamente;
* NÃO contém regra de negócio;
* observa estados do ViewModel.

---

## 2. ViewModel

Responsável por:

* controlar estado da tela;
* receber eventos da UI;
* chamar UseCases;
* expor StateFlow.

Exemplo:

```kotlin
class HomeViewModel(
    private val getModulosUseCase: GetModulosUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun carregar() {
        viewModelScope.launch {
            getModulosUseCase().collect {
                _state.value = HomeState(modulos = it)
            }
        }
    }
}
```

---

## 3. Domain Layer

Camada central da regra de negócio.

Contém:

* models;
* use cases;
* contratos de repository.

Não depende do Android.

Exemplo:

```kotlin
class GetModulosUseCase(
    private val repository: ModuloRepository
) {
    operator fun invoke() =
        repository.getModulos()
}
```

---

## 4. Data Layer

Responsável por:

* persistência;
* cache;
* integração futura;
* banco local.

Implementa os repositories.

---

# Room Database

## Entity

```kotlin
@Entity(tableName = "modulos")
data class ModuloEntity(
    @PrimaryKey
    val id: Int,
    val titulo: String,
    val descricao: String
)
```

---

## DAO

```kotlin
@Dao
interface ModuloDao {

    @Query("SELECT * FROM modulos")
    fun listar(): Flow<List<ModuloEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(lista: List<ModuloEntity>)
}
```

---

# Navegação

```txt
Home
 ├── Lista de módulos
 │
 └── Módulo
      ├── Lista de aulas
      │
      └── Aula
           ├── Conteúdo
           ├── PDF
           ├── Quiz
           └── Favoritar
```

---

# Funcionalidades Simples para MVP

## Essenciais

* lista de módulos;
* lista de aulas;
* marcar concluído;
* favoritos;
* pesquisa;
* modo offline.

## Extras

* quiz;
* exportar certificado PDF;
* progresso percentual;
* tema escuro;
* notificações locais.

---

# Banco de Dados Inicial

## Tabelas

### modulos

```txt
id
titulo
descricao
```

### aulas

```txt
id
moduloId
titulo
conteudo
pdfPath
```

### progresso

```txt
id
aulaId
concluida
dataConclusao
```

---

# Dependências Gradle

```kotlin
implementation("androidx.core:core-ktx:1.12.0")

implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

implementation("androidx.compose.ui:ui:1.6.0")

implementation("androidx.compose.material3:material3:1.2.0")

implementation("androidx.room:room-runtime:2.6.1")

kapt("androidx.room:room-compiler:2.6.1")

implementation("androidx.room:room-ktx:2.6.1")

implementation("com.google.dagger:hilt-android:2.50")

kapt("com.google.dagger:hilt-compiler:2.50")
```

---

# Arquitetura Recomendada para Evolução

## Fase 1 — MVP Local

* Room
* Compose
* Offline
* Conteúdo fixo

## Fase 2 — Sincronização

* API REST
* Retrofit
* Sync online/offline

## Fase 3 — Conta/Login

* Firebase Auth
* JWT
* Controle de acesso

## Fase 4 — Produção Real

* Analytics
* Crashlytics
* Logs
* Criptografia
* Backup
* Play Integrity

---

# Estrutura Ideal de Conteúdo

```txt
Módulo
 ├── Aula 1
 ├── Aula 2
 ├── PDF
 ├── Links
 └── Quiz
```
