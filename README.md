# HologramsFSMP

Плагин Paper 1.21.5 для отображения цветных голограмм и изображений (64x64) с поддержкой RGB-цветов.

Особенности
- Создание/удаление/телепорт к голограммам
- Отображение PNG 64×64 в виде голограммы (каждый пиксель — цветной блок)
- Группировка пикселей по горизонтали (параметр `image-chunk-size` в `config.yml`) для уменьшения количества сущностей
- Сохранение голограмм в `holograms.yml` и автозагрузка изображений при старте (если файл изображений доступен)
- Команды и таб-комплит

Сборка
1. Убедись, что установлен JDK 21 и `JAVA_HOME` указывает на него.
2. Собрать JAR (из корня репозитория):
```powershell
.
cd 'C:\Users\dorof\OneDrive\Desktop\HologramsFSMP'
.\gradlew.bat :lib:jar
```

Установка
1. Скопируй `lib/build/libs/lib-1.0.0.jar` в папку `plugins/` сервера Paper (или переименуй в `HologramsFSMP.jar`).
2. Запусти сервер — в `plugins/HologramsFSMP/` появится `config.yml` и папка `images/`.
3. Положи PNG 64×64 в `plugins/HologramsFSMP/images/`.

Быстрые команды в игре
- `/hd` — показать справку
- `/hd create <name>` — создать голограмму под вами
- `/hd readimage <name> <file.png>` — загрузить изображение из папки `images`
- `/hd addline <name> <index> <text>` — вставить строку
- `/hd save` — сохранить все голограммы

Настройки
- `plugins/HologramsFSMP/config.yml` — содержит `image-chunk-size` (по умолчанию 4). Чем больше — тем меньше сущностей, но меньше точность.

Публикация на GitHub
- Если у тебя установлен GitHub CLI (`gh`) — используй `publish.ps1` в корне проекта, он автоматически создаст репозиторий и запушит код.
- Альтернативно, создай репозиторий вручную на GitHub и выполни команды:
```powershell
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```

Лицензия
Добавь лицензию по желанию.
