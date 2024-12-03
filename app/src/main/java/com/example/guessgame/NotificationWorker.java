package com.example.guessgame;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {

    public static final String TASK_TITLE_KEY = "taskTitle";
    public static final String TASK_DEADLINE_KEY = "taskDeadline";

    public NotificationWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        // Логируем, что работа началась
        Log.d("NotificationWorker", "Notification worker started.");

        // Получаем данные из inputData
        String taskTitle = getInputData().getString(TASK_TITLE_KEY);
        String taskDeadline = getInputData().getString(TASK_DEADLINE_KEY);

        // Проверка, что данные не пустые
        if (taskTitle == null || taskDeadline == null) {
            Log.e("NotificationWorker", "Task title or deadline is null. Cannot send notification.");
            return Result.failure();
        }

        // Логируем полученные данные
        Log.d("NotificationWorker", "Task Title: " + taskTitle + ", Task Deadline: " + taskDeadline);

        // Отправляем уведомление
        sendNotification(taskTitle, taskDeadline);

        // Логируем, что уведомление отправлено
        Log.d("NotificationWorker", "Notification sent for task: " + taskTitle);

        return Result.success();
    }

    private void sendNotification(String taskTitle, String taskDeadline) {
        // Логируем, что уведомление будет отправлено
        Log.d("NotificationWorker", "Sending notification for task: " + taskTitle);

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "task_deadline_channel";
        NotificationChannel channel = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    channelId,
                    "Task Deadline Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Создаем уведомление
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle("Task Added")
                .setContentText("Task: " + taskTitle + ". Deadline: " + taskDeadline)
                .setSmallIcon(R.drawable.notification)
                .build();

        // Отправляем уведомление
        notificationManager.notify(1, notification);
    }
}
