package com.valvesoftware;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsParams.Builder;
import com.android.billingclient.api.SkuDetailsResponseListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class InAppPurchases implements PurchasesUpdatedListener {
    private final String k_sSpewPackageName = "com.valvesoftware.InAppPurchases";
    /* access modifiers changed from: private */
    public boolean m_bBillingClientConnected = false;
    /* access modifiers changed from: private */
    public final BillingClient m_billingClient;
    private HashSet<String> m_consumedTokens;
    /* access modifiers changed from: private */
    public final Hashtable<String, SkuDetails> m_skuDetailsTable;

    /* access modifiers changed from: private */
    public static native void UpdateIAPConsumedStatus(String str, int i);

    /* access modifiers changed from: private */
    public static native void UpdateIAPPricing(String str, String str2, String str3);

    private static native void UpdateIAPPurchaseStatus(String str, String str2, int i, boolean z);

    public InAppPurchases(Context context) {
        this.m_billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build();
        this.m_skuDetailsTable = new Hashtable<>();
    }

    public void connectToBillingClient(final Runnable runnable) {
        if (!this.m_bBillingClientConnected) {
            this.m_billingClient.startConnection(new BillingClientStateListener() {
                public void onBillingSetupFinished(BillingResult billingResult) {
                    String str = "com.valvesoftware.InAppPurchases";
                    if (billingResult.getResponseCode() == 0) {
                        Log.d(str, "Billing Setup Connected");
                        InAppPurchases.this.m_bBillingClientConnected = true;
                        Runnable runnable = runnable;
                        if (runnable != null) {
                            runnable.run();
                            return;
                        }
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Billing Setup Failed with error: ");
                    sb.append(billingResult.getDebugMessage());
                    Log.d(str, sb.toString());
                }

                public void onBillingServiceDisconnected() {
                    InAppPurchases.this.m_bBillingClientConnected = false;
                }
            });
        }
    }

    public void disconnectFromBillingClient() {
        Log.d("com.valvesoftware.InAppPurchases", "Billing Setup Disconnected");
        BillingClient billingClient = this.m_billingClient;
        if (billingClient != null && billingClient.isReady()) {
            this.m_billingClient.endConnection();
            this.m_bBillingClientConnected = false;
        }
    }

    private void runBillingClientJob(Runnable runnable) {
        if (this.m_bBillingClientConnected) {
            runnable.run();
        } else {
            connectToBillingClient(runnable);
        }
    }

    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> list) {
        String str = "com.valvesoftware.InAppPurchases";
        if (billingResult.getResponseCode() == 0) {
            for (Purchase purchase : list) {
                String sku = purchase.getSku();
                String purchaseToken = purchase.getPurchaseToken();
                if (purchase.getPurchaseState() == 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Purchased: ");
                    sb.append(sku);
                    Log.d(str, sb.toString());
                } else if (purchase.getPurchaseState() == 2) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Purchased pending for: ");
                    sb2.append(sku);
                    Log.d(str, sb2.toString());
                }
                UpdateIAPPurchaseStatus(sku, purchaseToken, purchase.getPurchaseState(), purchase.isAcknowledged());
            }
        } else if (billingResult.getResponseCode() == 1) {
            Log.d(str, "user cancelled purchase");
        } else {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("onPurchasesUpdated error: ");
            sb3.append(billingResult.getDebugMessage());
            Log.d(str, sb3.toString());
        }
    }

    public void querySkuDetails(final List<String> list) {
        runBillingClientJob(new Runnable() {
            public void run() {
                Builder newBuilder = SkuDetailsParams.newBuilder();
                newBuilder.setSkusList(list).setType(SkuType.INAPP);
                InAppPurchases.this.m_billingClient.querySkuDetailsAsync(newBuilder.build(), new SkuDetailsResponseListener() {
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                        if (billingResult.getResponseCode() != 0 || list == null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("onPurchasesUpdated error: ");
                            sb.append(billingResult.getDebugMessage());
                            Log.d("com.valvesoftware.InAppPurchases", sb.toString());
                            return;
                        }
                        for (SkuDetails skuDetails : list) {
                            String sku = skuDetails.getSku();
                            if (InAppPurchases.this.m_skuDetailsTable.containsKey(sku)) {
                                InAppPurchases.this.m_skuDetailsTable.replace(sku, skuDetails);
                            } else {
                                InAppPurchases.this.m_skuDetailsTable.put(sku, skuDetails);
                            }
                            InAppPurchases.UpdateIAPPricing(sku, skuDetails.getOriginalPrice(), skuDetails.getPrice());
                        }
                    }
                });
            }
        });
    }

    public void queryExistingPurchases() {
        runBillingClientJob(new Runnable() {
            public void run() {
                PurchasesResult queryPurchases = InAppPurchases.this.m_billingClient.queryPurchases(SkuType.INAPP);
                if (queryPurchases.getResponseCode() == 0) {
                    InAppPurchases.this.onPurchasesUpdated(queryPurchases.getBillingResult(), queryPurchases.getPurchasesList());
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("queryPurchases error: ");
                sb.append(queryPurchases.getResponseCode());
                Log.d("com.valvesoftware.InAppPurchases", sb.toString());
            }
        });
    }

    public void purchaseSku(final Activity activity, final String str) {
        runBillingClientJob(new Runnable() {
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("attempting to purchase sku: ");
                sb.append(str);
                String str = "com.valvesoftware.InAppPurchases";
                Log.d(str, sb.toString());
                SkuDetails skuDetails = (SkuDetails) InAppPurchases.this.m_skuDetailsTable.get(str);
                if (skuDetails == null) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("purchaseSku error. sku not found: ");
                    sb2.append(str);
                    Log.d(str, sb2.toString());
                    return;
                }
                InAppPurchases.this.m_billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build());
            }
        });
    }

    public void consumePurchase(final String str) {
        HashSet<String> hashSet = this.m_consumedTokens;
        if (hashSet == null) {
            this.m_consumedTokens = new HashSet<>();
        } else if (hashSet.contains(str)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Already consumed purchase token: ");
            sb.append(str);
            Log.d("com.valvesoftware.InAppPurchases", sb.toString());
            return;
        }
        this.m_consumedTokens.add(str);
        final AnonymousClass5 r0 = new ConsumeResponseListener() {
            public void onConsumeResponse(BillingResult billingResult, String str) {
                String str2 = "com.valvesoftware.InAppPurchases";
                if (billingResult.getResponseCode() != 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("onConsumeResponse error: ");
                    sb.append(billingResult.getDebugMessage());
                    Log.d(str2, sb.toString());
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Consumed token: ");
                    sb2.append(str);
                    Log.d(str2, sb2.toString());
                }
                InAppPurchases.UpdateIAPConsumedStatus(str, billingResult.getResponseCode());
            }
        };
        runBillingClientJob(new Runnable() {
            public void run() {
                InAppPurchases.this.m_billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(str).build(), r0);
            }
        });
    }
}
