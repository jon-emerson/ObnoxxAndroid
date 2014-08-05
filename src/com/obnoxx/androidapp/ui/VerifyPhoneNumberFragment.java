package com.obnoxx.androidapp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.obnoxx.androidapp.CurrentUser;
import com.obnoxx.androidapp.R;
import com.obnoxx.androidapp.requests.VerifyPhoneNumberRequest;
import com.obnoxx.androidapp.requests.VerifyPhoneNumberResponse;

public class VerifyPhoneNumberFragment extends Fragment {
    private static final String TAG = "VerifyPhoneNumberFragment";
    private static final int MODE_PHONE_NUMBER = 1;
    private static final int MODE_VERIFICATION_CODE = 2;
    private static final int MODE_PROGRESS_BAR = 3;

    private String mTemporaryUserCode = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.verify_phone_number_fragment, parent, false);
        setButtonHandlers(v);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPhoneNumberText().getText().clear();
        setMode(MODE_PHONE_NUMBER);
    }

    private void setButtonHandlers(View v) {
        final Context appContext = this.getActivity().getApplicationContext();

        ((Button) v.findViewById(R.id.phone_number_button)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setMode(MODE_PROGRESS_BAR);
                        String phoneNumber = VerifyPhoneNumberFragment.this.getPhoneNumberText()
                                .getText().toString();
                        VerifyPhoneNumberRequest t =
                                new VerifyPhoneNumberRequest(appContext, phoneNumber) {
                                    @Override
                                    public void onPostExecute(VerifyPhoneNumberResponse result) {
                                        if (result.getStatusCode() == 200) {
                                            mTemporaryUserCode = result.getTemporaryUserCode();
                                            setMode(MODE_VERIFICATION_CODE);
                                        } else {
                                            Toast.makeText(appContext, "Error, try again",
                                                    Toast.LENGTH_SHORT).show();
                                            setMode(MODE_PHONE_NUMBER);
                                        }
                                    }
                                };
                        t.execute();
                    }
                });

        ((Button) v.findViewById(R.id.verification_code_button)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setMode(MODE_PROGRESS_BAR);
                        String verificationCodeText =
                                VerifyPhoneNumberFragment.this.getVerificationCodeText()
                                        .getText().toString();
                        VerifyPhoneNumberRequest t =
                                new VerifyPhoneNumberRequest(appContext, verificationCodeText,
                                        mTemporaryUserCode) {
                                    @Override
                                    public void onPostExecute(VerifyPhoneNumberResponse response) {
                                        if (response.getStatusCode() == 200) {
                                            login(response);
                                        } else {
                                            Toast.makeText(appContext, "Error, try again",
                                                    Toast.LENGTH_SHORT).show();
                                            setMode(MODE_VERIFICATION_CODE);
                                        }
                                    }
                                };
                        t.execute();
                    }
                }
        );

        ((Button) v.findViewById(R.id.verification_code_back_button)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setMode(MODE_PHONE_NUMBER);
                    }
                });
    }

    private EditText getPhoneNumberText() {
        return ((EditText) getActivity().findViewById(R.id.phone_number));
    }

    private EditText getVerificationCodeText() {
        return ((EditText) getActivity().findViewById(R.id.verification_code));
    }

    private void setMode(int mode) {
        ((LinearLayout) getActivity().findViewById(R.id.phone_number_layout))
                .setVisibility(mode == MODE_PHONE_NUMBER ? View.VISIBLE : View.GONE);
        ((LinearLayout) getActivity().findViewById(R.id.verification_code_layout))
                .setVisibility(mode == MODE_VERIFICATION_CODE ? View.VISIBLE : View.GONE);
        ((ProgressBar) getActivity().findViewById(R.id.progress_bar))
                .setVisibility(mode == MODE_PROGRESS_BAR ? View.VISIBLE : View.GONE);

        if (mode == MODE_PHONE_NUMBER) {
            getPhoneNumberText().requestFocus();
        } else if (mode == MODE_VERIFICATION_CODE) {
            getVerificationCodeText().requestFocus();
        }
    }

    private void login(VerifyPhoneNumberResponse response) {
        CurrentUser.setUser(this.getActivity(), response.getUser());

        // Set the session ID last, since its presence indicates that the user has
        // successfully logged in and other fields (e.g. user) have been populated.
        CurrentUser.setSessionId(this.getActivity(), response.getSessionId());

        startActivity(new Intent(this.getActivity(), RecordSoundActivity.class));
    }
}