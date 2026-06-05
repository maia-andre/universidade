# Progresso V2a - Guia de Publicação na Play Store

Este documento registra o passo a passo para gerar a versão de "Teste Interno / Alfa Fechado" do aplicativo e publicá-lo no Google Play Console.

## 1. Política de Privacidade
- Foi criado o texto base em `docs/politica_de_privacidade.md`.
- A política foi hospedada no Notion e transformada em uma página pública.
- **Link Oficial:** [Política de Privacidade (Notion)](https://notas-psjc.notion.site/Pol-tica-de-Privacidade-3754489ba0e88085a522c8af46ae3c27?source=copy_link)
- Esse link será exigido pelo Google Play Console na seção "Conteúdo do App".

## 2. Ícone do Aplicativo
- Foi gerado um ícone flat minimalista com gradiente azul, o capelo amarelo e as iniciais **SJC**.
- **Como aplicar no projeto:**
  1. No Android Studio, clique com o botão direito na pasta `app` > **New** > **Image Asset**.
  2. Em "Asset Type", escolha **Image**.
  3. No "Path", selecione o arquivo de imagem do ícone gerado.
  4. Ajuste a escala (Scaling) para caber no círculo de segurança.
  5. Avance (Next) e conclua (Finish) para gerar os recursos `mipmap`.

## 3. Gerar Keystore (Assinatura) e Arquivo .AAB
Para subir na loja, o aplicativo precisa ser assinado digitalmente e convertido no formato `.aab`.
- **Como gerar no Android Studio:**
  1. Vá no menu **Build** > **Generate Signed Bundle / APK...**
  2. Escolha **Android App Bundle**.
  3. Em "Key store path", clique em **Create new...** para gerar uma nova chave de segurança (`.jks`). Guarde o arquivo e as senhas com extrema segurança.
  4. Preencha o Alias (ex: `universidade_key`), senhas e os dados do certificado (ex: SJC).
  5. Avance, escolha a versão **release** e clique em **Finish**.
  6. O Android Studio criará o arquivo final na pasta `app/release/app-release.aab`.

## 4. Google Play Console
Com o `.aab` e o link da Política de Privacidade em mãos, as etapas finais na loja são:
1. Criar o App no [Play Console](https://play.google.com/apps/publish).
2. **Conteúdo do App:** Preencher questionários de classificação, público alvo (+18) e colar o link da Política de Privacidade.
3. **Presença na Loja:** Adicionar título, descrições, o ícone recém-gerado, capturas de tela e o Gráfico de Recursos (Feature Graphic 1024x500).
4. **Testes Fechados / Internos:** Criar uma nova versão, subir o arquivo `.aab` e enviar para a revisão do Google.

## Próximos Passos
Após enviar para teste, a equipe poderá instalar o app através do link de convite do Google Play e testar a estabilidade e usabilidade antes da migração para Firebase na V3.

Build successful - 5 warning

> Task :app:compileReleaseKotlin
w: file:///D:/Users/andre.maia/AndroidStudioProjects/UniversidadedoServidor/app/src/main/java/com/sgaf/universidadedoservidor/data/local/database/AppDatabase.kt:50:18 'fun fallbackToDestructiveMigration(): RoomDatabase.Builder<AppDatabase>' is deprecated. Replace by overloaded version with parameter to indicate if all tables should be dropped or not.
w: file:///D:/Users/andre.maia/AndroidStudioProjects/UniversidadedoServidor/app/src/main/java/com/sgaf/universidadedoservidor/ui/screens/curso_detail/CursoDetailScreen.kt:53:57 'val Icons.Filled.ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.ArrowBack.
w: file:///D:/Users/andre.maia/AndroidStudioProjects/UniversidadedoServidor/app/src/main/java/com/sgaf/universidadedoservidor/ui/screens/curso_detail/CursoDetailScreen.kt:229:25 'fun Divider(modifier: Modifier = ..., thickness: Dp = ..., color: Color = ...): Unit' is deprecated. Renamed to HorizontalDivider.
w: file:///D:/Users/andre.maia/AndroidStudioProjects/UniversidadedoServidor/app/src/main/java/com/sgaf/universidadedoservidor/ui/screens/cursos/CursosScreen.kt:53:57 'val Icons.Filled.ArrowBack: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.ArrowBack.
w: file:///D:/Users/andre.maia/AndroidStudioProjects/UniversidadedoServidor/app/src/main/java/com/sgaf/universidadedoservidor/ui/theme/Theme.kt:51:20 'var statusBarColor: Int' is deprecated. Deprecated in Java.
