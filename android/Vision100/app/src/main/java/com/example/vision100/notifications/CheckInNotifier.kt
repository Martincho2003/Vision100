package com.example.vision100.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vision100.MainActivity
import com.example.vision100.R
import com.example.vision100.data.CheckInResponse

private const val CHECK_IN_CHANNEL_ID = "check_in_results"
private const val CHECK_IN_NOTIFICATION_ID = 1001

object CheckInNotifier {
    fun showResult(context: Context, result: CheckInResponse) {
        val title = if (result.verified) {
            context.getString(R.string.checkin_success_title)
        } else {
            context.getString(R.string.checkin_fail_title)
        }
        val body = if (result.verified) {
            context.getString(
                R.string.checkin_notification_success_body,
                result.touristObject?.name ?: context.getString(R.string.unknown_object),
                result.pointsAwarded
            )
        } else {
            result.reason
        }

        show(context, title, body)
    }

    fun showProblem(context: Context, message: String) {
        show(
            context = context,
            title = context.getString(R.string.checkin_notification_problem_title),
            body = message
        )
    }

    @SuppressLint("MissingPermission")
    private fun show(context: Context, title: String, body: String) {
        if (!canPostNotifications(context)) return

        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHECK_IN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(CHECK_IN_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHECK_IN_CHANNEL_ID,
            context.getString(R.string.checkin_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.checkin_notification_channel_description)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
