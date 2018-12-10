package org.ritocoin.aw.presenter.activities.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.ritocoin.aw.R;
import org.ritocoin.aw.presenter.activities.util.BRActivity;
import org.ritocoin.aw.presenter.customviews.BRDialogView;
import org.ritocoin.aw.presenter.interfaces.BRAuthCompletion;
import org.ritocoin.aw.tools.animation.BRAnimator;
import org.ritocoin.aw.tools.animation.BRDialog;
import org.ritocoin.aw.tools.manager.BRSharedPrefs;
import org.ritocoin.aw.tools.security.AuthManager;
import org.ritocoin.aw.tools.security.BRKeyStore;
import org.ritocoin.aw.tools.util.BRConstants;
import org.ritocoin.aw.tools.util.CurrencyUtils;
import org.ritocoin.aw.tools.util.Utils;
import org.ritocoin.aw.wallet.WalletsMaster;

import java.math.BigDecimal;


public class FingerprintActivity extends BRActivity {
    private static final String TAG = FingerprintActivity.class.getName();

    public RelativeLayout layout;
    public static boolean appVisible = false;
    private static FingerprintActivity app;
    private TextView limitExchange;
    private TextView limitInfo;

    private ToggleButton toggleButton;

    public static FingerprintActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        limitExchange = (TextView) findViewById(R.id.limit_exchange);
        limitInfo = (TextView) findViewById(R.id.limit_info);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.enableFingerprint);
            }
        });

        toggleButton.setChecked(BRSharedPrefs.getUseFingerprint(this));

        limitExchange.setText(getLimitText());

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Activity app = FingerprintActivity.this;
                if (isChecked && !Utils.isFingerprintEnrolled(app)) {
                    Log.e(TAG, "onCheckedChanged: fingerprint not setup");
                    BRDialog.showCustomDialog(app, getString(R.string.TouchIdSettings_disabledWarning_title_android), getString(R.string.TouchIdSettings_disabledWarning_body_android), getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                    buttonView.setChecked(false);
                } else {
                    BRSharedPrefs.putUseFingerprint(app, isChecked);
                }

            }
        });
        SpannableString ss = new SpannableString(getString(R.string.TouchIdSettings_customizeText_android));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                AuthManager.getInstance().authPrompt(FingerprintActivity.this, null, getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        Intent intent = new Intent(FingerprintActivity.this, SpendLimitActivity.class);
                        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancel() {

                    }
                });
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        //start index of the last space (beginning of the last word)
        int indexOfSpace = limitInfo.getText().toString().lastIndexOf(" ");
        // make the whole text clickable if failed to select the last word
        ss.setSpan(clickableSpan, indexOfSpace == -1 ? 0 : indexOfSpace, limitInfo.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        limitInfo.setText(ss);
        limitInfo.setMovementMethod(LinkMovementMethod.getInstance());
        limitInfo.setHighlightColor(Color.TRANSPARENT);

    }

    private String getLimitText() {
        String iso = BRSharedPrefs.getPreferredFiatIso(this);
        //amount in satoshis
        BigDecimal satoshis = new BigDecimal(BRKeyStore.getSpendLimit(this));
        WalletsMaster master = WalletsMaster.getInstance(this);
        //amount in RITO, mRITO or uRITO
        BigDecimal amount = master.getCurrentWallet(this).getFiatForSmallestCrypto(this, satoshis, null);
        //amount in user preferred ISO (e.g. USD)
        BigDecimal curAmount = master.getCurrentWallet(this).getFiatForSmallestCrypto(this, satoshis, null);
        //formatted string for the label
        return String.format(getString(R.string.TouchIdSettings_spendingLimit), CurrencyUtils.getFormattedAmount(this, "RITO", amount), CurrencyUtils.getFormattedAmount(this, iso, curAmount));
    }

    @Deprecated
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void changeStatusBarColor() {
        Window window = app.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(app, R.color.primaryColor));

        final int lFlags = window.getDecorView().getSystemUiVisibility();
        // update the SystemUiVisibility depending on whether we want a Light or Dark theme.
        window.getDecorView().setSystemUiVisibility((lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setStatusBarGradiant(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            Drawable background = activity.getResources().getDrawable(R.drawable.gradient_blue);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(activity.getResources().getColor(android.R.color.transparent));
            window.setNavigationBarColor(activity.getResources().getColor(android.R.color.transparent));

            final int lFlags = window.getDecorView().getSystemUiVisibility();
            // update the SystemUiVisibility depending on whether we want a Light or Dark theme.
            window.getDecorView().setSystemUiVisibility((lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));

            window.setBackgroundDrawable(background);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;

//        changeStatusBarColor();
        setStatusBarGradiant(app);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        BRAnimator.startBreadActivity(this, false);
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    public void onPause() {
        super.onPause();
        appVisible = false;
    }

}