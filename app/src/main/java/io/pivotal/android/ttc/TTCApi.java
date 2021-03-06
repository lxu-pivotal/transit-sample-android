package io.pivotal.android.ttc;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Set;

import io.pivotal.android.auth.Auth;
import io.pivotal.android.push.Push;
import io.pivotal.android.push.registration.RegistrationListener;
import io.pivotal.android.push.registration.UnregistrationListener;
import io.pivotal.android.push.util.Logger;
import io.pivotal.android.ttc.activities.NotificationsActivity;
import io.pivotal.android.ttc.models.Route;
import io.pivotal.android.ttc.models.Stop;

public class TTCApi {

    public static final String DEVICE_ALIAS = "TransitAndroid";
    public static final String API_GATEWAY_BASE_URL = "http://transit-gateway-app.cfapps.io/ttc/routes";

    private static int TEN_SECONDS = 10000;

    public static void updatePushTags(final Context context, Set<String> tags) {
        Push.getInstance(context).startRegistration(DEVICE_ALIAS, tags, new RegistrationListener() {
            @Override
            public void onRegistrationComplete() {
                Logger.i("Push registration complete");
            }

            @Override
            public void onRegistrationFailed(String reason) {
                Logger.e("Push registration failed: " + reason);
            }
        });
    }

    public static void pushUnregister(final Context context, UnregistrationListener listener) {
        Push.getInstance(context).startUnregistration(listener);
    }

    public static void dataLogout(final Context context) {
        Auth.logout(context);
    }

    public static void fullLogoutFromActivity(final Activity activity) {
        TTCApi.pushUnregister(activity, new UnregistrationListener() {

            @Override
            public void onUnregistrationComplete() {
                // Does nothing
            }

            @Override
            public void onUnregistrationFailed(String s) {
                final String message = activity.getString(R.string.unable_to_unregister_push) + ": " + s;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        Toast.makeText(activity, activity.getString(R.string.user_logged_out), Toast.LENGTH_SHORT).show();
        dataLogoutOnlyFromActivity(activity);
    }

    public static void dataLogoutOnlyFromActivity(final Activity activity) {
        TTCPreferences.setIsAuthenticated(activity, false);
        TTCApi.dataLogout(activity);
        NotificationsActivity.newInstance(activity);
        activity.finish();
    }

    public static Route.List getRoutes() throws Exception {
        final HttpGet request = new HttpGet(API_GATEWAY_BASE_URL);
        return execute(request, new Gson(), Route.List.class);
    }

    public static Stop.List getStops(final String tag) throws Exception {
        final HttpGet request = new HttpGet(API_GATEWAY_BASE_URL + "/" + tag);
        final Stop.Response response = execute(request, new Gson(), Stop.Response.class);
        return response != null ? response.stops : null;
    }

    private static <T> T execute(final HttpUriRequest request, final Gson gson, final Type type) throws Exception {
        final DefaultHttpClient client = getClient();
        final HttpResponse response = client.execute(request);
        final InputStream inputStream = response.getEntity().getContent();
        final InputStreamReader inputReader = new InputStreamReader(inputStream);
        final JsonReader jsonReader = new JsonReader(inputReader);
        final T list = gson.fromJson(jsonReader, type);
        jsonReader.close();
        return list;
    }

    private static DefaultHttpClient getClient() {
        final DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TEN_SECONDS);
        HttpConnectionParams.setSoTimeout(params, TEN_SECONDS);
        return client;
    }
}
