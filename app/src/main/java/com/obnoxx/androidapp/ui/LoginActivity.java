package com.obnoxx.androidapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.obnoxx.androidapp.CurrentUser;
import com.obnoxx.androidapp.GetSoundsOperation;
import com.obnoxx.androidapp.R;
import com.obnoxx.androidapp.data.User;
import com.obnoxx.androidapp.requests.CreateUserRequest;
import com.obnoxx.androidapp.requests.CreateUserResponse;
import com.obnoxx.androidapp.requests.VerifyPhoneNumberRequest;
import com.obnoxx.androidapp.requests.VerifyPhoneNumberResponse;

public class LoginActivity extends FragmentActivity
        implements LoginPhoneNumberFragment.OnPhoneNumberSelectedListener,
        LoginVerificationCodeFragment.OnVerificationCodeSelectedListener,
        LoginNewUserFragment.OnUserNameSelectedListener {
    private String mTemporaryUserCode = null;
    private String mVerificationCode = null;
    private LoginPhoneNumberFragment mLoginPhoneNumberFragment = new LoginPhoneNumberFragment();
    private LoginVerificationCodeFragment mLoginVerificationCodeFragment =
            new LoginVerificationCodeFragment();
    private LoginNewUserFragment mLoginNewUserFragment = new LoginNewUserFragment();
    private View mProgressBarView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // If we're being restored from a previous state, then we don't need to
        // do anything and should return or else we could end up with
        // overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Add the fragment to the 'fragment_container' FrameLayout.
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, mLoginPhoneNumberFragment).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideProgressBar();
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this
        // fragment, and add the transaction to the back stack so the user
        // can navigate back.
        transaction.replace(R.id.fragment_container, fragment);

        // Commit the transaction.
        transaction.commit();
    }

    @Override
    public void onPhoneNumberSelected(String phoneNumber) {
        showProgressBar();

        VerifyPhoneNumberRequest t = new VerifyPhoneNumberRequest(this, phoneNumber) {
                    @Override
                    public void onPostExecute(VerifyPhoneNumberResponse response) {
                        hideProgressBar();

                        if (response.getStatusCode() == 200) {
                            mTemporaryUserCode = response.getTemporaryUserCode();
                            showFragment(mLoginVerificationCodeFragment);
                        } else {
                            Toast.makeText(LoginActivity.this, "Error, try again",
                                    Toast.LENGTH_SHORT).show();
                            LoginActivity.this.showFragment(
                                    LoginActivity.this.mLoginPhoneNumberFragment);
                        }
                    }
                };
        t.execute();
    }

    @Override
    public void onVerificationCodeSelected(final String verificationCode) {
        showProgressBar();

        VerifyPhoneNumberRequest t = new VerifyPhoneNumberRequest(
                this, verificationCode, mTemporaryUserCode) {
            @Override
            public void onPostExecute(VerifyPhoneNumberResponse response) {
                hideProgressBar();

                if (response.getStatusCode() == 200) {
                    // If we get a user back, the user's logged in.  If we don't, but the response
                    // was 200 OK, that means we should invite the user to create a new account.
                    User user = response.getUser();
                    if (user != null) {
                        completeLogin(user, response.getSessionId());
                    } else {
                        mVerificationCode = verificationCode;
                        showFragment(mLoginNewUserFragment);
                    }

                } else {
                    Toast.makeText(LoginActivity.this, "Error, try again",
                            Toast.LENGTH_SHORT).show();
                    LoginActivity.this.showFragment(
                            LoginActivity.this.mLoginPhoneNumberFragment);
                }
            }
        };
        t.execute();
    }

    @Override
    public void onUserNameSelected(String userName) {
        showProgressBar();

        CreateUserRequest t = new CreateUserRequest(
                this, userName, mVerificationCode, mTemporaryUserCode) {
            @Override
            public void onPostExecute(CreateUserResponse response) {
                hideProgressBar();

                if (response.getStatusCode() == 200) {
                    completeLogin(response.getUser(), response.getSessionId());
                } else {
                    Toast.makeText(LoginActivity.this, "Error, try again", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };
        t.execute();
    }

    @Override
    public void onBackPressed() {
        // Make sure we close if the user presses back on our initial view.
        if (mLoginPhoneNumberFragment.isVisible()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void showProgressBar() {
        if (mProgressBarView == null) {
            mProgressBarView = getLayoutInflater().inflate(R.layout.progress_bar_view, null);
            getWindow().addContentView(mProgressBarView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            mProgressBarView.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBar() {
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }
    }

    /**
     * Persists the newly logged in user, starts fetching his data, and sends the
     * user to the main application activity.
     * @param user The new user's account.
     * @param sessionId The user's session ID.
     */
    private void completeLogin(User user, String sessionId) {
        // Set the user.
        CurrentUser.setUser(LoginActivity.this, user);

        // Set the session ID last, since its presence indicates that the user has
        // successfully logged in and other fields (e.g. user) have been populated.
        CurrentUser.setSessionId(LoginActivity.this, sessionId);

        // Start fetching the user's previous sounds/deliveries.
        // This will put them into the SoundDeliveryProvider.
        new GetSoundsOperation(getApplicationContext()).execute();

        startActivity(new Intent(LoginActivity.this, RecordSoundActivity.class));
    }
}
