# Publicação da V2 no teste interno da Play (V8 Item 3)

Data: 22 de junho de 2026
Status: **build de release configurado e verificado** (versionCode 2, versionName 1.02.1, `signingConfig` via `keystore.properties`). Falta o passo manual: preencher as credenciais, gerar o `.aab` assinado e subir no Play Console.

> **Contexto:** a *app signing key* fica no Google (Play App Signing) e **nunca muda**. O que assina o `.aab` localmente é a *upload key* (a `.jks` que você trouxe do PC do trabalho para o Linux). Subir um `.aab` com chave nova é rejeitado — por isso usamos a mesma upload key.

---

## 1. Pré-requisito — `keystore.properties` (uma vez por máquina)

1. Copie o modelo: `keystore.properties.example` → `keystore.properties` (na raiz do projeto).
2. Preencha os 4 valores com os dados da sua upload key:
   - `storeFile` = caminho **absoluto** da `.jks`
   - `storePassword`, `keyAlias`, `keyPassword`
3. Não versione — `keystore.properties` já está no `.gitignore`.

O `app/build.gradle.kts` lê esse arquivo e assina o release automaticamente. **Sem** ele, o release sai **não assinado** (e o build não quebra).

> **Backup:** mantenha a `.jks` + as 3 credenciais em local seguro (ex.: gerenciador de senhas + cópia offline). Perder a upload key obriga um reset no Play Console (Integridade do app → Assinatura do app), que só existe se o Play App Signing estiver ativado.

---

## 2. Gerar o `.aab` assinado

```bash
./gradlew :app:bundleRelease
```

Saída: `app/build/outputs/bundle/release/app-release.aab`.

### Conferir que está assinado
```bash
# Deve listar META-INF/*.RSA e *.SF (blocos de assinatura):
unzip -l app/build/outputs/bundle/release/app-release.aab | grep -iE "META-INF/.*\.(RSA|SF)"
# Ou, com o JDK:
jarsigner -verify -verbose app/build/outputs/bundle/release/app-release.aab | head -3   # "jar verified"
```

> Se aparecer "sem bloco de assinatura", o `keystore.properties` não foi lido — confira o caminho e os valores.

---

## 3. Subir no teste interno (Play Console)

1. **Play Console** → app *Universidade do Servidor* → **Testes → Teste interno**.
2. **Criar nova versão** → enviar o `app-release.aab`.
3. Preencher as notas da versão → **Revisar** → **Iniciar lançamento para o teste interno**.
4. Compartilhar o **link de adesão** do teste interno com o RH/beta.

A instalação **via Play** (não sideload) **não** mostra o alerta de "desenvolvedor desconhecido" do Play Protect.

---

## 4. Disciplina de release (não esquecer)

- **`versionCode` sempre crescente.** Esta versão é **2**. O próximo upload precisa ser **> 2** — confirme no Console o maior já enviado antes de bumpar.
- **`versionName`** é cosmético (mostrado ao usuário). Esta versão: **`1.02.1`**.
- **debug ↔ release não se atualizam** entre si (assinaturas distintas). Para testar o fluxo de update real, instale sempre o release.
- **Desinstalar apaga o Room local** (progresso). Manter a **mesma upload key** preserva o update limpo sobre a versão anterior, sem desinstalar.
- O `google-services.json` é necessário no build e é gitignored — garanta que ele está presente na máquina de build.

---

## 5. Checklist

- [ ] `keystore.properties` preenchido (não versionado)
- [ ] `./gradlew bundleRelease` verde
- [ ] `.aab` verificado como assinado
- [ ] `versionCode` (2) maior que o último no Console
- [ ] `.aab` enviado ao track de teste interno
- [ ] Instalação testada via link do Play (sem alerta de Play Protect), update limpo sobre a V1
