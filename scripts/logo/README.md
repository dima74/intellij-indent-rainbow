# Инструкция

1. Создаём предварительную версию лого с помощью create.py (icon.svg)
2. Скачиваем шрифт Ubuntu Mono (Bold): https://fonts.google.com/specimen/Ubuntu+Mono (в формате woff2)
3. Преобразовываем шрифт в формат ttf командой woff2_decompress
4. Устанавливаем шрифт в систему (копируем ttf файл в папку /usr/share/fonts/TTF затем запускаем fc-cache). Подробнее: https://wiki.archlinux.org/index.php/Fonts#Manual_installation
5. Открываем `icon.svg` в inkscape, меняем шрифт на Ubuntu Mono (это нужно в частности чтобы проверить что inkscape видит шрифт), сохраняем файл как `icon_inkscape.svg`
6. Преобразуем text в svg командой `inkscape icon_inkscape.svg --export-text-to-path --export-plain-svg shapes.svg`
7. Заменяем `<text>` в icon.svg на `<path>` из shapes.svg
8. Удаляем ненужные атрибуты (`id`, `style` в `<path>`)
8. Копируем icon.svg как pluginIcon.svg и pluginIcon_dark.svg
9. В pluginIcon_dark.svg добавляем `fill="white"` для текста
