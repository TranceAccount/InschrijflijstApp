package com.enrollmentlist.alembic;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.KeyEvent;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {
    // Set variables
    public static final String CHANNEL_ID = "Alembic";
    public static String BaseUrl = "https://alembic.utwente.nl/inschrijflijst/";
    private final String DefaultUrl = "https://alembic.utwente.nl/inschrijflijst/events/";
    private String Url = DefaultUrl;
    private boolean UrlLoaded = false;

    /*
     * Create the app when the app is open by a notification or by pressing on the app icon
     * In case it is opened by a notification the link of the notification is loaded
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Open activity_main.xml layout
        setContentView(R.layout.activity_main);
        // Subscribe users to topic all for notifications
        FirebaseMessaging.getInstance().subscribeToTopic("all");
        // Create notification channel for higher version of Android
        CreateNotificationChannel createChannel = new CreateNotificationChannel(this);
        createChannel.createNotificationChannel();
        // Open Alembic Enrollmentlist when app is opened and open main page
        WebView webView = findViewById(R.id.webView1);
        webView.setWebViewClient(new MyWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        // Check if intent is received for deep link or notification
        String appLinkAction = getIntent().getAction();
        Bundle extras = getIntent().getExtras();
        String currentUrl = webView.getUrl();
        if (Intent.ACTION_VIEW.equals(appLinkAction)) {
            // Get redirect url
            Uri intentData = getIntent().getData();
            /*
             * If redirect url is available, create a new intent and start this intent containing
             * the redirect url as extra
             */
            if (intentData != null) {
                String redirectUrl = "https://alembic.utwente.nl" + intentData.getPath();
                if (!redirectUrl.equals(BaseUrl) && !redirectUrl.equals(DefaultUrl) && !redirectUrl.equals(currentUrl)) {
                    Url = redirectUrl;
                    UrlLoaded = true;
                }
            }
        }
        else if (extras != null) {
            if (extras.containsKey("URL")) {
                String NotificationUrl = extras.getString("URL");

                // If no redirect url is open, overwrite with notification url (Latest call)
                if (!NotificationUrl.equals(BaseUrl) && !NotificationUrl.equals(DefaultUrl) && !NotificationUrl.equals(currentUrl)) {
                    Url = NotificationUrl;
                    UrlLoaded = true;
                }
            }
        }
        // Always load Url on creation
        webView.loadUrl(Url);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        WebView webView = findViewById(R.id.webView1);
        // Check if intent is received for deep link or notification
        String appLinkAction = intent.getAction();
        String currentUrl = webView.getUrl();
        if (Intent.ACTION_VIEW.equals(appLinkAction)) {
            // Get redirect url
            Uri intentData = intent.getData();
            /*
             * If redirect url is available, create a new intent and start this intent containing
             * the redirect url as extra
             */
            if (intentData != null) {
                String redirectUrl = "https://alembic.utwente.nl" + intentData.getPath();
                if (!redirectUrl.equals(BaseUrl) && !redirectUrl.equals(DefaultUrl) && !redirectUrl.equals(currentUrl)) {
                    Url = redirectUrl;
                    UrlLoaded = true;
                }
            }
        }
        webView.loadUrl(Url);
    }

    // Add back button functionality to go back to previous link
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // In case user uses back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Get webView instance
            WebView webView = findViewById(R.id.webView1);
            String currentUrl = webView.getUrl();
            // In case user is in login screen close app
            String loginUrl = "https://alembic.utwente.nl/inschrijflijst/login/";
            if (currentUrl.contains(loginUrl)) {
                onBackPressed();
                return true;
            }

            // While user is in the notification Url go back to DefaultUrl
            else if (!(currentUrl.equals(DefaultUrl)) && !webView.canGoBack()) {
                if (!UrlLoaded)
                    webView.loadUrl(DefaultUrl);
                else
                    restartActivity();
                return true;
            }

            // When user is not on the defaultUrl and can go back, allow going back to previous Url
            else if (webView.canGoBack() && !(currentUrl.equals(DefaultUrl))) {
                WebBackForwardList webBackForwardList = webView.copyBackForwardList();
                String previousUrl = "";
                if (webBackForwardList.getCurrentIndex() > 0)
                    previousUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex()-1).getUrl();
                    if (previousUrl.contains(loginUrl) || (currentUrl.equals(Url) && UrlLoaded))
                        restartActivity();
                    else
                        webView.goBack();
                return true;
            }

            // In case user is on defaultUrl screen close the app
            else
                onBackPressed();
            return true;
        }

        // In case of any other button, do default action
        return super.onKeyDown(keyCode,event);
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
            if (URL.equals(BaseUrl) && UrlLoaded)
                restartActivity();

            // Allow loading of Url if it is in the app
            else if (URL.contains(BaseUrl)) {
                webView.loadUrl(URL);
            }

            // Make user use different app for urls not in correspondence with BaseUrl
            else
                webView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));

            return true;
        }
    }

    /*
     * Restart the MainActivity completely to remove the notification activity
     */
    private void restartActivity() {
        // End current activity
        finish();

        // Start main activity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
