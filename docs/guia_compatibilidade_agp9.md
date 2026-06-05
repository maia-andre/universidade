# Guia de Engenharia: Compatibilidade e Resolução de Builds com AGP 9.x (Android Gradle Plugin)

Este documento serve como base de conhecimento técnico e de referência rápida para engenheiros e agentes de desenvolvimento de software. O objetivo é evitar retrabalho ao iniciar novos projetos ou atualizar setups legados que utilizem o **Android Gradle Plugin (AGP) 9.0 ou superior** com **Gradle 9.x** e **Kotlin 2.x**.

---

## 1. Arquivos de Plugin Corrompidos (ZipException / CEN header)

### Sintoma
Durante a sincronização (sync) ou build do projeto, o Gradle falha prematuramente com mensagens de classe não encontrada ou erro de leitura de zip:
```text
java.io.UncheckedIOException: java.util.zip.ZipException: invalid CEN header (bad entry name or comment)
    at org.gradle.internal.classloader.TransformReplacer$JarLoader.getJarFileLocked(...)
Caused by: java.lang.ClassNotFoundException: Failed to instrument class org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
```

### Causa Raiz
Downloads parciais de dependências ou cancelamentos bruscos de sincronização geram arquivos JAR incompletos/corrompidos no cache global do Gradle. Como o AGP 9.0+ valida agressivamente os plugins do Kotlin durante a fase de configuração, qualquer corrupção impede a inicialização do projeto.

### Solução de Engenharia
Forçar a reconstrução do cache e o re-download completo das dependências de plugins do Kotlin e do Android via CLI:
1. **Comando de atualização de dependências:**
   ```bash
   ./gradlew help --refresh-dependencies
   ```
2. **Resolução Manual (Se o comando acima falhar):**
   Delete a pasta do Kotlin no cache do Gradle local para forçar o re-download físico:
   * **Windows:** `%USERPROFILE%\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin`
   * **macOS/Linux:** `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin`

---

## 2. Incompatibilidade do Dagger Hilt (`BaseExtension not found`)

### Sintoma
A sincronização do Gradle falha na aplicação do plugin do Hilt com o seguinte erro:
```text
An exception occurred applying plugin request [id: 'com.google.dagger.hilt.android', version: '2.51.1']
> Failed to apply plugin 'com.google.dagger.hilt.android'.
   > Android BaseExtension not found.
```

### Causa Raiz
O AGP 9.0 removeu por completo APIs e classes legadas de DSL interna (como a clássica classe `BaseExtension`) para dar lugar a novas interfaces limpas de compilação (ex: `ApplicationExtension` e `AndroidComponentsExtension`). Versões antigas do Dagger Hilt (como a `2.51.1`) que faziam referência a essa classe falham ao tentar rodar em ambientes AGP 9.x.

### Solução de Engenharia
Atualizar a dependência do Hilt e KSP para versões compatíveis com o novo ecossistema do AGP 9.0:
1. No arquivo central de dependências (`gradle/libs.versions.toml`), defina o Hilt para a versão **`2.59.2`** (ou superior):
   ```toml
   [versions]
   hilt = "2.59.2"
   ```
2. Mantenha os compiladores (`hilt-android`, `hilt-compiler`) referenciando essa versão catalogada.

---

## 3. Uso do Plugin Kotlin Manual e `kotlinOptions`

### Sintoma
Sintaxe do Gradle aponta referências não encontradas ou conflitos de nomenclatura de extensões ao aplicar o Kotlin:
```text
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```
Ou no `build.gradle.kts`:
```text
Line 36: kotlinOptions {
         ^ Unresolved reference: kotlinOptions
```

### Causa Raiz
No AGP 9.0+, o suporte ao Kotlin é **nativo e embutido** através do plugin principal `com.android.application`.
1. **Conflito de Plugin:** Se você declarar manualmente `alias(libs.plugins.kotlin.android)` no bloco de `plugins {}`, haverá conflito de namespaces com o registro automático do AGP.
2. **DSL Removida:** A antiga estrutura `kotlinOptions { jvmTarget = "17" }` de dentro do bloco `android { ... }` foi descontinuada e removida em favor da DSL oficial do compilador Kotlin.

### Solução de Engenharia
1. **Remova o plugin manual:** Retire o plugin `kotlin-android` (ou `org.jetbrains.kotlin.android`) do `build.gradle.kts` raiz e do módulo `:app`.
2. **Substitua a DSL de Compilação:** Remova o bloco `kotlinOptions` do bloco `android {}` e declare a nova sintaxe moderna de compilação no escopo raiz do `app/build.gradle.kts`:
   ```kotlin
   // Fora do bloco android { ... }
   kotlin {
       compilerOptions {
           jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
       }
   }
   ```

---

## 4. Conflito de Geração de Fontes do KSP / Hilt (`disallowKotlinSourceSets`)

### Sintoma
Ao tentar sincronizar o projeto contendo geradores de código como o KSP ou Hilt, ocorre o erro:
```text
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
  Kotlin source set 'debug' contains: [.../build/generated/ksp/debug/kotlin]
  Solution: Use android.sourceSets DSL instead.
```

### Causa Raiz
Como o AGP 9.0+ isolou o gerenciamento de código Kotlin de forma embutida, o uso de `kotlin.sourceSets` está bloqueado por padrão. No entanto, plugins externos de terceiros (como versões correntes do KSP e Hilt) ainda utilizam a API antiga por baixo dos panos para registrar as pastas contendo as classes auto-geradas.

### Solução de Engenharia
Para permitir que o KSP registre corretamente as pastas contendo códigos gerados durante a compilação no AGP 9.x, ative a seguinte flag de compatibilidade temporária:

Adicione no arquivo **`gradle.properties`**:
```properties
# Permite que plugins de terceiros (KSP, Hilt) registrem código gerado no Kotlin SourceSets
android.disallowKotlinSourceSets=false
```

> [!IMPORTANT]
> **Nota de Manutenibilidade:** Esta flag é um "escape hatch" temporário e experimental exposto pelo AGP 9.x. Ela deve ser utilizada até que os mantenedores do KSP atualizem suas integrações internas para usar a nova API `android.sourceSets` nativamente.

---

## 5. Falha de Resolução do Foojay Toolchains Resolver

### Sintoma
```text
Plugin [id: 'org.gradle.toolchains.foojay-resolver-convention', version: '1.0.0'] was not found in any of the following sources
```

### Causa Raiz
O plugin `foojay-resolver-convention` é usado para baixar JDKs de repositórios externos caso a máquina de desenvolvimento não possua um instalado. Em redes corporativas ou sob configurações de proxy estritas, o plugin falha ao tentar resolver a versão `"1.0.0"` nos portais.

### Solução de Engenharia
* **Se você já possui o JDK correto localmente:** (ex: Java 17 ou 21 configurado no Android Studio), a aplicação deste plugin é opcional. **Remova** o bloco do plugin do arquivo `settings.gradle.kts`:
  ```kotlin
  // Remova isso do settings.gradle.kts
  plugins {
      id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  }
  ```
* **Caso precise de compatibilidade de download:** Reduza a versão para uma amplamente testada e disponível localmente no cache, como a `"0.8.0"`.

---

## 6. Erro de Compilação do Room com KSP2 (`unexpected jvm signature V`)

### Sintoma
Durante a compilação do projeto (tarefas como `:app:kspDebugKotlin`), o build falha com uma exceção interna do KSP/Room:
```text
Execution failed for task ':app:kspDebugKotlin'.
> A failure occurred while executing com.google.devtools.ksp.gradle.KspAAWorkerAction
   > unexpected jvm signature V
Caused by: java.lang.IllegalStateException: unexpected jvm signature V
    at androidx.room.compiler.processing.javac.kotlin.JvmDescriptorUtilsKt.typeNameFromJvmSignature(...)
    at androidx.room.compiler.processing.ksp.KSTypeJavaPoetExtKt.asJTypeName(...)
```

### Causa Raiz
Ao utilizar o Kotlin 2.x e o KSP2 (habilitado por padrão no KSP 2.x), o processador de anotações do Room em versões legadas (como `2.6.1`) falha ao processar assinaturas de funções suspendíveis no bytecode que retornam `Unit` (representado pela assinatura `V` de void no JVM).

### Solução de Engenharia
Atualizar o Room Database para uma versão que ofereça suporte nativo estável ao Kotlin 2.x e KSP2.

No seu arquivo `gradle/libs.versions.toml`, atualize o Room para a versão **`2.8.4`** (ou superior):
```toml
[versions]
room = "2.8.4"
```
Isso garante a compatibilidade com o KSP2 e resolve o processamento de assinaturas do compilador Kotlin 2.2+.

---

## 7. Dependência de Icons Faltando ou Não Declarada no Compose (`Unresolved reference 'icons'`)

### Sintoma
Ao tentar compilar o projeto, o compilador do Kotlin acusa classe ou pacote não encontrado ao tentar importar ícones:
```text
e: .../CursosScreen.kt:9:34 Unresolved reference 'icons'.
e: .../HomeScreen.kt:9:34 Unresolved reference 'icons'.
e: .../HomeScreen.kt:167:44 Unresolved reference 'Icons'.
```

### Causa Raiz
No Compose Material3 moderno ou com as novas especificações de compilação modulares do AGP 9.0+, as bibliotecas de ícones clássicos do Material (como `material-icons-core` ou `material-icons-extended`) não são mais inclusas de forma implícita e automática pelas dependências transitivas do Material3. Elas exigem a declaração explícita no script de dependências.

### Solução de Engenharia
1. **Adicionar ao Catálogo (`libs.versions.toml`):**
   Declare a biblioteca de ícones principais do Material (gerenciada via Compose BOM):
   ```toml
   [libraries]
   androidx-compose-material-icons-core = { group = "androidx.compose.material", name = "material-icons-core" }
   ```
2. **Adicionar ao script de build (`app/build.gradle.kts`):**
   Declare a dependência dentro do bloco de `dependencies {}`:
   ```kotlin
   implementation(libs.androidx.compose.material.icons.core)
   ```

---

## 8. Chamada Qualificada de Funções de Extensão no Kotlin (`Unresolved reference: border`)

### Sintoma
O compilador aponta erro ao tentar resolver referências de modificadores de borda (ou outras funções de extensão do Compose):
```text
e: .../AulaScreen.kt:366:37 Unresolved reference 'border'.
```

### Causa Raiz
No Kotlin, funções de extensão (como `Modifier.border(...)` do Compose Foundation) são top-level ou declaradas com tipo receptor. Tentar chamá-las usando a sintaxe clássica de chamada qualificada por pacote (`androidx.compose.foundation.border(border, shape)`) falha porque funções de extensão não se comportam como métodos estáticos tradicionais chamados via escopo de pacote.

### Solução de Engenharia
1. **Importar a Extensão:**
   Declare o import explícito do modificador no topo do arquivo `.kt`:
   ```kotlin
   import androidx.compose.foundation.border
   ```
2. **Invocação Direta no Receptor:**
   Invoque a extensão diretamente sobre a instância do receptor (como `Modifier` ou `this`), nunca pelo caminho qualificado do pacote:
   ```kotlin
   // Correto:
   Modifier.border(borderStroke, shape)
   // Ou dentro de um bloco com receptor implicit/explicit:
   this.border(borderStroke, shape)
   ```

---

## 9. Incompatibilidade Binária do Compose (`Cannot access class ComposableFunction0`)

### Sintoma
O IDE (Android Studio) destaca praticamente todo o código Compose (como o `Scaffold` ou `Text`) com um erro de compilação ou warning persistente:
```text
Cannot access class 'kotlin.jvm.functions.Function0' (ou 'ComposableFunction0'). 
Check your module classpath for missing or conflicting dependencies.
```

### Causa Raiz
Este erro ocorre devido a uma **incompatibilidade binária** entre a versão do compilador Kotlin e a versão das bibliotecas do Jetpack Compose (geralmente via Compose BOM). O uso de versões " bleeding edge" ou futuras do BOM (ex: `2026.x.x`) com uma versão estável/atual do Kotlin (ex: `2.2.10`) causa uma falha na resolução dos metadados das funções `@Composable`.

### Solução de Engenharia
Alinhar a versão do Compose BOM para uma versão estável e compatível com a sua versão do plugin do Kotlin:

1. No arquivo `gradle/libs.versions.toml`, ajuste a versão do BOM para uma versão estável atual (ex: `2025.02.00`):
   ```toml
   [versions]
   composeBom = "2025.02.00"
   ```
2. Realize o **Sync Project with Gradle Files**. Caso o erro persista, certifique-se de que a versão do Kotlin no bloco `plugins` coincide com a versão do plugin `kotlin-compose`.
