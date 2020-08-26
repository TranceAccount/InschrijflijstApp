package com.enrollmentlist.alembic;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    /*
     * Set variables
     */
    public static final String CHANNEL_ID = "Alembic";
    private final String DefaultUrl = "https://alembic.utwente.nl/inschrijflijst/events/";
    private String Url = "";
    private boolean BackPressed = false;
    private boolean UrlLoaded = false;

    /*
     * Create the app when the app is open by a notification or by pressing on the app icon
     * In case it is opened by a notification the link of the notification is loaded
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Open activity_main.xml layout
         */
        setContentView(R.layout.activity_main);

        /*
         * Subscribe users to topic all for notifications
         */
        FirebaseMessaging.getInstance().subscribeToTopic("all");

        /*
         * Create notification channel for higher version of Android
         */
        CreateNotificationChannel createChannel = new CreateNotificationChannel(this);
        createChannel.createNotificationChannel();

        /*
         * Open Alembic Enrollmentlist when app is opened and enable JavaScript
         */
        WebView webView = findViewById(R.id.webView1);
        webView.setWebViewClient(new MyWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);

        /*
         * Intent for when user presses a link that redirects to the inschrijflijst when the app is
         * not running in the background, and create a new instance of the application with the
         * link that is referred to
         */
        String appLinkAction = getIntent().getAction();
        if (Intent.ACTION_VIEW.equals(appLinkAction)) {
            onNewIntent(getIntent());
        }

        /*
         * Get Url if user pressed on the notification or used a deep link that redirects to the
         * inschrijflijst
         */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey("URL")) {
                String NotificationUrl = extras.getString("URL");

                /*
                 * If not redirect url is open, overwrite with notification url (Latest call)
                 */
                if (!Url.equals(NotificationUrl)) {
                    Url = NotificationUrl;
                }
            }
        }

        /*
         * Load Url if present
         */
        if (!Url.equals("")) {
            webView.loadUrl(Url);
            UrlLoaded = true;
        }
        /*
         * If user starts the app normally, go to base url
         */
        else {
            webView.loadUrl(DefaultUrl);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BackPressed) {
            /*
             * Get webView instance
             */
            WebView webView = findViewById(R.id.webView1);

            /*
             * Save the current Url
             */
            SharedPreferences prefs = getApplicationContext().
                    getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("lastUrl", webView.getUrl());
            edit.apply();
        }
    }

    /*
     * When the intent is a deep link intent, create a new activity containing the redirect url in
     * the intent extra so the onCreate call knows what to open
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        /*
         * First check if it is actually a deep link intent
         */
        String intentAction = intent.getAction();
        if (Intent.ACTION_VIEW.equals(intentAction)) {
            /*
             * Get redirect url
             */
            Uri intentData = intent.getData();

            /*
             * If redirect url is available, create a new intent and start this intent containing
             * the redirect url as extra
             */
            if (intentData != null) {
                String RedirectUrl = "https://alembic.utwente.nl" + intentData.getPath();

                Intent intentNew = new Intent(this,MainActivity.class);
                intentNew.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentNew.putExtra("URL", RedirectUrl);
                intentNew.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intentNew);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BackPressed) {
            /*
             * Get webView instance
             */
            WebView webView = findViewById(R.id.webView1);

            /*
             * Load the saved Url from OnPause()
             */
            if (webView != null) {
                SharedPreferences prefs = getApplicationContext().
                        getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
                String s = prefs.getString("lastUrl", "");
                if (!s.equals("")) {
                    webView.loadUrl(s);
                }
            }
        }
        BackPressed = false;
    }

    /*
     * Add back button functionality to go back to previous link
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        /*
         * In case user uses back button
         */
        if (keyCode == KeyEvent.KEYCODE_BACK) {

                /*
                 * Get webView instance
                 */
                WebView webView = findViewById(R.id.webView1);

                /*
                 * In case user is in login screen close app
                 */
                if (webView.getUrl().contains("https://alembic.utwente.nl/inschrijflijst/login/")) {
                    return super.onKeyDown(keyCode, event);
                }

                /*
                 * While user in the notification Url go back to DefaultUrl
                 */
                else if (UrlLoaded && ((webView.getUrl().equals(Url)) || ((!Url.equals("")) && !(webView.getUrl().equals(DefaultUrl))))) {
                    UrlLoaded = false;
                    Url = "";
                    restartActivity();
                    return true;
                }

                /*
                 * In any other case when user not on the event screen take him back to event screen
                 */
                else if (webView.canGoBack() && !(webView.getUrl().equals(DefaultUrl))) {
                    webView.loadUrl(DefaultUrl);
                    return true;
                }

                /*
                 * In case user is on defaultUrl screen close the app
                 */
                else {
                    BackPressed = true;
                    return super.onKeyDown(keyCode, event);
                }

        }

        /*
         * In case of any other button, do default action
         */
        else {
            return super.onKeyDown(keyCode, event);
        }
    }

    /*
     * Restart the MainActivity completely to remove the notification activity
     * TODO: Fix the notification activity and via dropdown menu (JavaScript is ignored?)
     */
    private void restartActivity() {
        /*
         * End current activity
         */
        finish();

        /*
         * Start main activity
         */
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /*
     * Allow user to only use app for the enrollmentlist by checking with the BaseUrl
     * If user presses an link outside the enrollmentlist, open default browser
     * Also added notification activity removal support when user goes to BaseUrl after having
     * received a notification
     */
    class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String URL) {

            /*
             * If user goes to BaseUrl via clicking on the BaseUrl, clear notification activity and
             * start main activity
             */
            String BaseUrl = "https://alembic.utwente.nl/inschrijflijst/";
            if (URL.equals(BaseUrl) && !Url.equals("")) {
                restartActivity();
            }

            /*
             * Allow loading of Url if it is in the app
             */
            else if (URL.contains(BaseUrl)) {
                webView.loadUrl(URL);
            }

            /*
             * Make user use different app for urls not in correspondence with BaseUrl
             */
            else {
                webView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
            }
            return true;
        }
    }


}
