package free.rm.skytube.gui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.db.MySQLiteHelper;
import free.rm.skytube.iap.DonationItems;

public class AppLaunchActivity extends AppCompatActivity {

    public static boolean isPurchased = false;
    public static Context staticContext;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        staticContext = this;
        if (!isInternetIsConnected(this)) {
            new MaterialDialog.Builder(this)
                    .content(R.string.no_internet)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            finish();
                        }
                    })
                    .show();
            return;
        }

        db = MySQLiteHelper.getInstance(this);
        //isPurchased = inAppPurchaseVerification();
        if (isPurchased) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            //finish();
        } else {
            loadInterstialAd();
        }
        //setContentView(R.layout.app_launch_ad_activity);
    }

    public static boolean isInternetIsConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return true;

            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                return true;
            }
        } else {
            // not connected to the internet
            return false;
        }
        return false;
    }
    public static boolean inAppPurchaseVerification() {
        boolean isPurchased = false;

        if (db.getAllIAP() > 0) {
            return true;
        }

        Intent serviceIntent = new Intent(
                "com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        staticContext.startService(serviceIntent);
        staticContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        if (mService == null || staticContext == null) {
            return false;
        }

        Bundle ownedItems;
        try {
            ownedItems = mService.getPurchases(3, staticContext.getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            e.printStackTrace();
            return isPurchased;
        }

        int response = ownedItems.getInt("RESPONSE_CODE");

        if (response != 0) return isPurchased;

        ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
		/*ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
		ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
		String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");*/

        if (ownedSkus != null && ownedSkus.size() > 0) {
            for (int i = 0; i < ownedSkus.size(); ++i) {
                String sku = ownedSkus.get(i);
                if (DonationItems.items.contains(sku)) {
                    isPurchased = true;
                    db.addIAP(sku);
                    //break;
                }

            }
        }

        return isPurchased;
    }

    @Override
    public void onResume() {
        super.onResume();
        db = MySQLiteHelper.getInstance(this);
        isPurchased = inAppPurchaseVerification();
    }

    public static MySQLiteHelper db;
    private static IInAppBillingService mService;
    private static ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onDestroy() {
        if (mServiceConn!= null && mService != null) {
            unbindService(mServiceConn);
        }

        super.onDestroy();
    }

    private void loadInterstialAd() {
        final Intent intent = new Intent(this, MainActivity.class);
        if (!isPurchased) {
            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id2));
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
            mInterstitialAd.setAdListener(new AdListener() {
                public void onAdLoaded() {
                    mInterstitialAd.show();
                }

                public void onAdClosed() {
                    // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
                    startActivity(intent);
                    //finish();

                }

                public void onAdClicked() {
                }

                public void onAdFailedToLoad(int var1) {
                    //AppUtil.startApp(service, appInfo);
                }

            });
        }
    }
}
