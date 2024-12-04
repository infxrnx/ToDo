#include <jni.h>

int calculateCompletionRate(int completedTasks, int totalTasks) {
    if (totalTasks > 0) {
        // Вычисляем процент выполнения
        return (int)((float)completedTasks / totalTasks * 100);
    } else {
        // Если нет задач, возвращаем 0
        return 0;
    }
}

// JNI-функция для вызова из Java/Kotlin
JNIEXPORT jint JNICALL
Java_com_example_guessgame_CompletionRateCalculator_calculateCompletionRate(
        JNIEnv *env, jobject obj, jint completedTasks, jint totalTasks) {
    return calculateCompletionRate(completedTasks, totalTasks);
}
