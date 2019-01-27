package com.appcoins.sdk.android_appcoins_billing;

import android.app.Activity;
import android.util.Log;

import com.appcoins.sdk.android_appcoins_billing.exception.IabAsyncInProgressException;
import com.appcoins.sdk.android_appcoins_billing.helpers.IabHelper;
import com.appcoins.sdk.android_appcoins_billing.listeners.OnIabPurchaseFinishedListener;
import com.appcoins.sdk.android_appcoins_billing.listeners.OnIabSetupFinishedListener;
import com.appcoins.sdk.android_appcoins_billing.listeners.OnSkuDetailsResponseListener;
import com.appcoins.sdk.billing.Billing;

import com.appcoins.sdk.billing.PurchasesResult;
import com.appcoins.sdk.billing.ResponseListener;
import com.appcoins.sdk.billing.SkuDetailsParam;

import java.util.List;

public class CatapultAppcoinsBilling {

  private IabHelper iabHelper;
  private final Billing billing;
  private final RepositoryConnection connection;

  public CatapultAppcoinsBilling(IabHelper iabHelper, Billing billing, RepositoryConnection connection) {
    this.iabHelper = iabHelper;
    this.billing = billing;
    this.connection = connection;
  }

  public PurchasesResult queryPurchases(String skuType) {
    return billing.queryPurchases(skuType);
  }

  public void querySkuDetailsAsync(SkuDetailsParam skuDetailsParam,
      ResponseListener onSkuDetailsResponseListener) {
    try {
      iabHelper.querySkuDetailsAsync(skuDetailsParam,
          (OnSkuDetailsResponseListener) onSkuDetailsResponseListener);
    } catch (IabAsyncInProgressException e) {
      Log.e("Message: ", "Error querying inventory. Another async operation in progress.");
    }
  }

  public void launchPurchaseFlow(Object act, String sku, String itemType, List<String> oldSkus,
      int requestCode, ResponseListener listener, String extraData) {
    try {
      iabHelper.launchPurchaseFlow((Activity) act, sku, itemType, oldSkus, requestCode,
          (OnIabPurchaseFinishedListener) listener, extraData);
    } catch (IabAsyncInProgressException e) {

    }
  }

  public void startService(final OnIabSetupFinishedListener listener) {
    iabHelper.startService(listener);
    connection.startService();
  }
}



