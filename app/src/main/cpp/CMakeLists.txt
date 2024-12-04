cmake_minimum_required(VERSION 3.18.1)

project("completion_rate")

# Добавляем ваш C/C++ файл в сборку
add_library(
    completion_rate         # Имя создаваемой библиотеки
    SHARED                  # Тип библиотеки (общая)
    completion_rate.c       # Список исходных файлов
)

# Указываем системные библиотеки, которые нужны для компоновки
find_library(
    log-lib
    log
)

# Связываем вашу библиотеку с системными
target_link_libraries(
    completion_rate
    ${log-lib}
)
