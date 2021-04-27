package com.enrollmentlist.alembic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static com.enrollmentlist.alembic.MainActivity.CHANNEL_ID;
import static com.enrollmentlist.alembic.MainActivity.BaseUrl;

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
         * In case the data for title is empty but a notification is sent set default event title
         */
        String title = remoteMessage.getData().get("title");
        if (title == null)
            title = "New event available";

        /*
         * Get the event description
         * The notification push action does not work when getNotification().getBody() is used
         * for the same reason as for the title.
         * In case the data for the description is empty set default event description
         */
        String body = remoteMessage.getData().get("body");
        if (body == null)
            body = "The event description should be here, but due to a mistake there is no description available";

        String eventUrl = remoteMessage.getData().get("eventURL");
        Integer eventID = EventIdExtractor(eventUrl);

        sendNotification(title, body, eventUrl, eventID);
    }

    /*
     * Make the notification
     */
    private void sendNotification(String title, String body, String Url, Integer eventID) {

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
         * Clean up link if notification creator uses the www.alembic.utwente.nl/inschrijflijst/
         */
        if (Url != null) {
            if (Url.contains(BaseUrl))
                intent.putExtra("URL", Url);
            else if (eventID != -1)
                intent.putExtra("URL", BaseUrl + "events/" + eventID);
            else {
                intent.putExtra("URL", BaseUrl + "events/");
                eventID = (int) (Math.random() * 1000);
            }
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        /*
         * Set push notification on click action with specifier of the eventID to prevent
         * overwriting of the event URLs
         */
        PendingIntent pendingIntent = PendingIntent.getActivity(this, eventID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
         * Launch the notification with eventID to create separate notifications for each event
         */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(eventID, notificationBuilder.build());
    }

    /*
     * For extraction of the event ID
     * e.g. for https://alembic.utwente.nl/inschrijflijst/events/145 it should extract 145
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
         * Return eventId integer value after conversion of the string value and return the
         * first value
         * Maximum value that can be reached is 2 billion (Ask google for max integer value)
         * In the special case that either no link is supplied or the link can not be found
         * return -1, which is handled later on to random variable after fixing the URL to default.
         */
        if (matcher.find())
            return Integer.parseInt(matcher.group(1));
        else
            return -1;
    }
}