# Camera Overhall

Клиентский мод для Fabric и Minecraft 26.3. Текущая реализация обнаруживает камеры по пользовательским именам голов: на экипированных головах сущностей и на загруженных блоках-черепах в ближайших загруженных чанках.

## Сборка в IntelliJ IDEA

Требуется:

- IntelliJ IDEA 2025.3 или новее;
- JDK 25;
- Minecraft 26.3;
- Fabric Loader 0.19.5;
- Fabric API 0.164.0+26.3;
- Gradle 9.8.0 (проект использует Gradle Wrapper).

1. Распакуйте архив в отдельную папку.
2. В IntelliJ IDEA выберите **File → Open** и откройте именно корневую папку `Camera Overhall`, где находятся `settings.gradle` и `build.gradle`.
3. В **File → Project Structure → Project SDK** выберите JDK 25.
4. В **Settings → Build, Execution, Deployment → Build Tools → Gradle** проверьте, что Gradle JVM — JDK 25, а Build and run using — **Gradle**.
5. Нажмите **Reload All Gradle Projects** в окне Gradle.
6. Для сборки откройте окно **Gradle → Tasks → build → build**. Не переключайте Build and run using на IntelliJ IDEA: проект использует Fabric Loom, поэтому задачи сборки должны выполняться Gradle.

Первая синхронизация требует доступа к интернету, чтобы IDEA/Gradle загрузили Gradle, Loom, JDK toolchain (если он отсутствует) и зависимости Fabric/Minecraft. Отключите Gradle Offline mode на время первой синхронизации.

**Важно про Gradle Wrapper:** в исходном архиве не было файла `gradle/wrapper/gradle-wrapper.jar`. IntelliJ IDEA может импортировать проект через Gradle Tooling API по `gradle-wrapper.properties`. После успешной синхронизации один раз запустите задачу **Gradle → Tasks → build setup → wrapper** — она создаст недостающий Wrapper JAR для запуска `gradlew`/`gradlew.bat` из терминала.

## Сборка из терминала

Windows:

```bat
gradlew.bat build
```

Linux/macOS:

```sh
./gradlew build
```

Готовый JAR появится в `build/libs/`.

## Запуск клиента из IDEA

В окне Gradle откройте **Tasks → fabric → runClient** (или используйте конфигурацию запуска **Minecraft Client**). В первый запуск Loom подготовит клиентскую среду разработки.

## Формат имени камеры

Сканер распознаёт пользовательские имена в форматах:

- `CAM:<network-id>:<camera-index>`;
- `CAM_<network-id>_<camera-index>`;
- `CAMERA <network-id> <camera-index>`.

Пример: `CAM:warehouse:2` — камера с индексом `2` в сети `warehouse`. Идентификатор сети может содержать латинские буквы, цифры, точку, дефис и подчёркивание.

## Примечание по Minecraft 26.x

Начиная с Minecraft 26.1 игра распространяется без обфускации. Проект использует официальные имена Minecraft и не подключает Yarn mappings. Клиентские классы находятся в `src/client`, общие модели и реестр — в `src/main`.
