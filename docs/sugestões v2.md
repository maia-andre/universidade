# Universidade do Servidor

## Planejamento de Evolução — Escopo V2

### Objetivo

A Versão 2 tem como foco consolidar a arquitetura do aplicativo, aumentar sua capacidade de manutenção e preparar a plataforma para crescimento institucional.

O objetivo não é adicionar funcionalidades corporativas complexas, mas fortalecer a fundação tecnológica criada no MVP para permitir evolução sustentável nas próximas versões.

---

# Diretrizes da V2

## 1. Criação da Camada Core

Criar uma camada compartilhada para centralizar recursos reutilizáveis da aplicação.

Estrutura sugerida:

```txt
core/
├── navigation/
├── designsystem/
├── components/
├── extensions/
├── constants/
├── analytics/
└── utils/
```

### Objetivos

* Eliminar duplicação de código.
* Padronizar componentes visuais.
* Centralizar constantes e comportamentos compartilhados.
* Reduzir acoplamento entre módulos.

### Benefícios

* Maior organização do projeto.
* Evolução mais simples.
* Menor risco de inconsistências visuais.
* Melhor experiência para futuros desenvolvedores.

---

## 2. Migração do Conteúdo para Markdown

Substituir gradualmente o armazenamento de conteúdo em texto simples por arquivos Markdown.

Estrutura sugerida:

```txt
assets/

curso-supervisores/
├── modulo-01/
│   ├── aula-01.md
│   ├── aula-02.md
│   └── quiz.json

curso-patrimonio/
├── modulo-01/
│   ├── aula-01.md
│   └── quiz.json
```

### Objetivos

* Facilitar manutenção do conteúdo.
* Permitir formatação rica.
* Simplificar produção de novas aulas.
* Facilitar revisão e geração de conteúdo por IA.

### Benefícios

* Conteúdo mais agradável para leitura.
* Menor dependência de alterações no código.
* Facilidade para expansão institucional.

---

## 3. Validação da Arquitetura Multi-Cursos

Considerando que a modelagem já suporta múltiplos cursos, a V2 deve validar esse comportamento em ambiente real.

### Objetivos

Verificar se:

* Novos cursos são carregados sem alteração de código.
* Navegação funciona independentemente do curso.
* Módulos e aulas são tratados de forma totalmente genérica.
* O progresso continua funcionando corretamente em todos os cursos.

### Critério de sucesso

Adicionar ao menos três cursos completos utilizando exclusivamente a estrutura já existente.

Exemplos:

* Formação de Supervisores.
* Patrimônio.
* Licitações.
* Almoxarifado.
* Integração de Novos Servidores.

Nenhuma alteração estrutural deve ser necessária para inclusão desses cursos.

---

## 4. Implementação da Busca Global

Criar mecanismo de pesquisa local.

Escopo da busca:

* Cursos.
* Módulos.
* Aulas.
* Conteúdo textual.

Exemplos de uso:

```txt
inventário
licitação
patrimônio
estoque
sindicância
```

### Objetivos

* Melhorar experiência do usuário.
* Reduzir tempo de navegação.
* Facilitar localização de conhecimento.

### Benefícios

* Maior produtividade.
* Melhor aproveitamento dos conteúdos.

---

## 5. Estrutura para Atualização de Conteúdo

Preparar a arquitetura para sincronização futura.

Modelo inicial:

```txt
version.json
```

Exemplo:

```json
{
  "version": 2
}
```

### Objetivos

* Separar ciclo de vida do aplicativo e do conteúdo.
* Permitir atualizações futuras sem republicação do APK.

### Benefícios

* Redução de esforço operacional.
* Maior autonomia para manutenção dos cursos.

---

## 6. Preparação Arquitetural para Autenticação Futura

A autenticação não será implementada nesta versão.

Entretanto, a arquitetura deverá ser preparada para recebê-la futuramente sem necessidade de refatoração ampla.

### Estrutura sugerida

```kotlin
interface AuthRepository
```

Implementação atual:

```txt
AnonymousAuthRepository
```

Implementação futura:

```txt
ApiAuthRepository
```

ou

```txt
MicrosoftAuthRepository
```

ou

```txt
GoogleWorkspaceAuthRepository
```

### Objetivos

* Evitar dependência direta da UI com mecanismos de autenticação.
* Facilitar futura integração com sistemas corporativos.
* Reduzir impacto da futura implantação de login.

### Benefícios

Quando a autenticação for implementada:

* telas permanecerão praticamente inalteradas;
* ViewModels permanecerão praticamente inalterados;
* a troca ocorrerá principalmente na camada Data.

---

# O que NÃO será implementado na V2

## Autenticação de Usuários

Motivos:

* Exige backend.
* Exige gestão de identidade.
* Exige controles adicionais de LGPD.
* Não agrega valor imediato ao MVP.

Previsão:

Avaliação para V3.

---

## Banco Centralizado

Motivos:

* Não há necessidade operacional imediata.
* O foco atual é conteúdo e experiência do usuário.

Previsão:

Implementação conjunta com autenticação.

---

## Dashboard Gerencial

Motivos:

* Depende de coleta centralizada de dados.
* Depende de autenticação confiável.

Previsão:

Após implantação da infraestrutura corporativa.

---

## Certificados Automáticos

Motivos:

* Requer identificação inequívoca do usuário.
* Requer persistência centralizada.

Previsão:

Após autenticação.

---

## Gamificação

Motivos:

* Não é prioridade de negócio.
* Pode gerar complexidade desnecessária neste estágio.

Previsão:

Reavaliar após adoção institucional.

---

# Resultado Esperado da V2

Ao final da V2, o aplicativo deverá possuir uma arquitetura mais madura, modular e preparada para expansão institucional, mantendo simplicidade operacional.

O produto deverá estar pronto para suportar múltiplos cursos, conteúdo estruturado em Markdown, busca integrada e futura sincronização de conteúdo, além de possuir uma fundação arquitetural preparada para receber autenticação corporativa e serviços online nas próximas fases.
