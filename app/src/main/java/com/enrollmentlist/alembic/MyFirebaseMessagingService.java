package com.enrollmentlist.alembic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static com.enrollmentlist.alembic.MainActivity.CHANNEL_ID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /*
     * When a notification is received
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        /*
         * Get the title of the event.
         * The notification push action does not work when getNotification().getTitle() is used
         * when the app is in the background or killed.
         * This is due to the onMessageReceived not being triggered
         */
        /*
         * Set the variables used in this class
         */
        String title = remoteMessage.getData().get("title");

        /*
         * In case the data for title is empty but a notification is sent set default event title
         */
        if (title == null) {
            title = "New event available";
        }

        /*
         * Get the event description
         * The notification push action does not work when getNotification().getBody() is used
         * for the same reason as for the title.
         */
        String body = remoteMessage.getData().get("body");

        /*
         * In case the data for the description is empty set default event description
         */
        if (body == null) {
            body = "The event description should be here, but due to a mistake there is no description available";
        }

        /*
         * Get the link of the event entry
         */
        String eventURL = remoteMessage.getData().get("eventURL");

        /*
         * Get the eventID
         */
        Integer eventID = EventIdExtractor(eventURL);

        /*
         * Make the notification and notify the app users
         */
        sendNotification(title, body, eventURL, eventID);
    }

    /*
     * Make the notification
     */
    private void sendNotification(String title, String body, String url, Integer eventID) {

        /*
         * Create notification channel for higher versions of Android
         */
        CreateNotificationChannel createChannel = new CreateNotificationChannel(this);
        createChannel.createNotificationChannel();

        /*
         * Poke MainActivity
         */
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        /*
         * Check if there is a link available and add this as extra data of the intent
         */
        if (url != null) {
            intent.putExtra("URL", url);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        /*
         * Set push notification on click action
         */
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         * Set notification parameters with BigTextStyle
         */
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        /*
         * Launch the notification with random channel integer
         */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(eventID, notificationBuilder.build());
    }

    /*
     * For extraction of the event ID
     * e.g. for https://alembic.utwente.nl/inschrijflijst/events/145 it should extract 145
     *
     * This is needed for notification IDs
     */
    private Integer EventIdExtractor(String eventURL) {
        /*
         * Setup regex for identifying the digit sequence "\d+" from a string
         */
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(eventURL);

        /*
         * Check if regex found digit sequence in the eventURL
         */
        if (matcher.find()) {
            /*
             * Return eventId integer value after conversion of the string value and return the
             * first value
             * Maximum value that can be reached is 2 billion (Ask google for max integer value)
             */
            return Integer.parseInt(matcher.group(1));
        }
        else {
            /*
             * In the special case that either no link is supplied or the link can not be found
             * return random integer
             */
            return new Random().nextInt();
        }
    }
}