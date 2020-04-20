/*
 * Chat.onion - P2P Instant Messenger
 *
 * http://play.google.com/store/apps/details?id=onion.chat
 * http://onionapps.github.io/Chat.onion/
 * http://github.com/onionApps/Chat.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package com.ivor.coatex.tor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.ivor.coatex.MainActivity;
import com.ivor.coatex.R;
import com.ivor.coatex.RequestActivity;
import com.ivor.coatex.db.Database;
import com.ivor.coatex.utils.Settings;
import com.ivor.coatex.utils.Util;

import java.io.File;

public class Notifier {

    private static Notifier instance;
    private Context context;
    private int activities = 0;

    private static final String MESSAGE_CHANNEL_ID = "coatex_message_01";// The id of the channel.

    private Notifier(Context context) {
        context = context.getApplicationContext();
        this.context = context;
    }

    public static Notifier getInstance(Context context) {
        context = context.getApplicationContext();
        if (instance == null) {
            instance = new Notifier(context);
        }
        return instance;
    }

    private void log(String s) {
        Log.i("Notifier", s);
    }

    public synchronized void onMessage() {
        log("onMessage");
        if (activities <= 0) {
            Database.getInstance(context).addNotification();
            update();
        } else {
            if (Settings.getPrefs(context).getBoolean("sound", true)) {
                try {
                    File toneDir = new File(context.getFilesDir(), "tones");
                    if (!toneDir.exists()) toneDir.mkdir();
                    File toneFile = new File(toneDir, "tone.ogg");
                    Uri uri = Uri.fromFile(toneFile);
                    if (!toneFile.exists())
                        Util.copyAsset(context, "tones/tone.ogg", toneFile.getAbsolutePath());
                    Ringtone r = RingtoneManager.getRingtone(context, uri);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void onResumeActivity() {
        Database.getInstance(context).clearNotifications();
        activities++;
        update();
    }

    public synchronized void onPauseActivity() {
        activities--;
    }

    private void update() {
        log("update");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int messageId = 5;
        int requestId = 6;
        int messages = Database.getInstance(context).getNotifications();
        if (messages <= 0 || !Settings.getPrefs(context).getBoolean("notify", true)) {
            log("cancel");
            notificationManager.cancel(messageId);
            notificationManager.cancel(requestId);
        } else {
            log("notify");
            showNotification(context,
                    context.getResources().getString(R.string.app_name),
                    context.getResources().getQuantityString(R.plurals.notification_new_messages, messages, messages),
                    new Intent(context, MainActivity.class));
        }
    }

    public void showNotification(Context context, String title, String body, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 5;
        String channelId = "coatex_message_01";
        String channelName = "Coatex Message";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        String notificationTone = Settings.getPrefs(context).getString("ringtone", "DEFAULT_SOUND");

        log("Notification tone : " + notificationTone);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

//            NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);
//            if (existingChannel != null) {
//                notificationManager.deleteNotificationChannel(channelId);
//            }

            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);

            if (Settings.getPrefs(context).getBoolean("sound", true)) {
                AudioAttributes att = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                mChannel.setSound(Uri.parse(notificationTone), att);
            }
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_chat)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentTitle(title)
                .setContentText(body);


        if (Settings.getPrefs(context).getBoolean("sound", true)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                mBuilder.setSound(Uri.parse(notificationTone));
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }

    public void showRequestNotification(String sender, String description) {
        Intent intent = new Intent(context, RequestActivity.class);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 6;
        String channelId = "coatex_request_01";
        String channelName = "Coatex Request";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_chat)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentTitle(sender)
                .setContentText(description);

        if (Settings.getPrefs(context).getBoolean("sound", true)) {
            mBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
            mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }
}
