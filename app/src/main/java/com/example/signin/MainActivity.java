package com.example.signin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends Activity {

    Prefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    private void reportError(Throwable err) {
        Log.e("XXX", err.toString());

        Toast.makeText(this, err.toString(), Toast.LENGTH_LONG).show();
    }


    private Task<String> getTwitterToken(Account account) {
        final Task<String>.TaskCompletionSource task = Task.<String>create();

        AccountManager am = AccountManager.get(this);
        assert am != null;

        am.getAuthToken(account, "com.twitter.android.oauth.token", new Bundle(), this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    task.setResult(future.getResult().getString(AccountManager.KEY_AUTHTOKEN));
                } catch (Exception e) {
                    task.setError(e);
                }
            }
        }, null);


        return task.getTask();
    }

    private Task<String> getTwitterSecret(Account account) {
        final Task<String>.TaskCompletionSource task = Task.<String>create();

        AccountManager am = AccountManager.get(this);
        assert am != null;

        am.getAuthToken(account, "com.twitter.android.oauth.token.secret", new Bundle(), this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    task.setResult(future.getResult().getString(AccountManager.KEY_AUTHTOKEN));
                } catch (Exception e) {
                    task.setError(e);
                }
            }
        }, null);


        return task.getTask();
    }


    public void authWithTwitter(View view) {
        AccountManager am = AccountManager.get(this);
        assert am != null;

        Account[] accounts = am.getAccountsByType("com.twitter.android.auth.login");
        if (accounts.length > 0) {
            final Account account = accounts[0];

            getTwitterToken(account).onSuccessTask(new Continuation<String, Task<String>>() {
                @Override
                public Task<String> then(Task<String> stringTask) throws Exception {
                    setToken("com.twitter.android.oauth.token", stringTask.getResult());
                    return getTwitterSecret(account);
                }
            }).onSuccess(new Continuation<String, Void>() {
                @Override
                public Void then(Task<String> stringTask) throws Exception {
                    setToken("com.twitter.android.oauth.token.secret", stringTask.getResult());
                    return null;
                }
            });
        }
    }


    private final static String kGPlusScope = "oauth2:https://www.googleapis.com/auth/plus.me";
    private final static int kRequestCodeRecoverFromAuthError = 1001;

    public void authWithGoogle(View button) throws GoogleAuthException, IOException {
        final long t0 = System.currentTimeMillis();

        final AccountManager am = AccountManager.get(this);
        assert am != null;

        for (final Account account : am.getAccounts()) {
            Log.d("XXX", String.format("%s %s", account.name, account.type));
        }

        Log.d("XXX", String.format("authWithGoogle: elapsed %dms", System.currentTimeMillis() - t0));

        for (final Account account : am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE)) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    try {
                        return GoogleAuthUtil.getToken(MainActivity.this, account.name, kGPlusScope, new Bundle());
                    } catch (UserRecoverableAuthException e) {
                        startActivityForResult(e.getIntent(), kRequestCodeRecoverFromAuthError);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (GoogleAuthException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String token) {
                    setToken("google.token", token);
                }
            }.execute();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == kRequestCodeRecoverFromAuthError) {

        } else {
            Log.e("XXX", "Unknown request code: " + requestCode);
        }
    }

    private void setToken(String name, String token) {
        Log.d("XXX", "setToken " + name + "=" + token);

        prefs.put(name, token);

        Toast.makeText(this, String.format("Authenticated!\n%s=%s)", name, token), Toast.LENGTH_SHORT).show();
    }

    public void invalidateTokens(View view) {
        AccountManager am = AccountManager.get(this);
        assert am != null;

        String nullStr = null;
        String twitterToken = prefs.get("com.twitter.android.oauth.token", nullStr);
        String twitterSecret = prefs.get("com.twitter.android.oauth.token.secret", nullStr);
        String googleToken = prefs.get("google.token", "");

        am.invalidateAuthToken("com.twitter.android.oauth.token", twitterToken);
        am.invalidateAuthToken("com.twitter.android.oauth.token.secret", twitterSecret);
        GoogleAuthUtil.invalidateToken(this, googleToken);

        Toast.makeText(this, "Cleared token caches!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.k
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

}
