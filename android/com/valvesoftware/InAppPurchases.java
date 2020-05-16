package com.valvesoftware;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class InAppPurchases implements PurchasesUpdatedListener {
    private final int IAP_CONSUMED_STATUS_ERROR = 1;
    private final int IAP_CONSUMED_STATUS_OK = 0;
    private final int IAP_PURCHASE_STATUS_CANCELLED = 3;
    private final int IAP_PURCHASE_STATUS_PENDING = 2;
    private final int IAP_PURCHASE_STATUS_PURCHASED = 1;
    private final int IAP_PURCHASE_STATUS_UNKNOWN = 0;
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
                    if (billingResult.getResponseCode() == 0) {
                        Log.d("com.valvesoftware.InAppPurchases", "Billing Setup Connected");
                        boolean unused = InAppPurchases.this.m_bBillingClientConnected = true;
                        Runnable runnable = runnable;
                        if (runnable != null) {
                            runnable.run();
                            return;
                        }
                        return;
                    }
                    Log.d("com.valvesoftware.InAppPurchases", "Billing Setup Failed with error: " + billingResult.getDebugMessage());
                }

                public void onBillingServiceDisconnected() {
                    boolean unused = InAppPurchases.this.m_bBillingClientConnected = false;
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
        if (billingResult.getResponseCode() == 0) {
            if (list != null) {
                for (Purchase next : list) {
                    String sku = next.getSku();
                    String purchaseToken = next.getPurchaseToken();
                    int i = 0;
                    if (next.getPurchaseState() == 1) {
                        Log.d("com.valvesoftware.InAppPurchases", "Purchased: " + sku);
                        i = 1;
                    } else if (next.getPurchaseState() == 2) {
                        Log.d("com.valvesoftware.InAppPurchases", "Purchased pending for: " + sku);
                        i = 2;
                    }
                    UpdateIAPPurchaseStatus(sku, purchaseToken, i, next.isAcknowledged());
                }
            }
        } else if (billingResult.getResponseCode() != 1) {
            Log.d("com.valvesoftware.InAppPurchases", "onPurchasesUpdated error: " + billingResult.getDebugMessage());
        } else if (list != null) {
            for (Purchase next2 : list) {
                String sku2 = next2.getSku();
                String purchaseToken2 = next2.getPurchaseToken();
                Log.d("com.valvesoftware.InAppPurchases", "user cancelled purchase for: " + sku2);
                UpdateIAPPurchaseStatus(sku2, purchaseToken2, 3, next2.isAcknowledged());
            }
        }
    }

    public void querySkuDetails(final List<String> list, final Runnable runnable) {
        runBillingClientJob(new Runnable() {
            public void run() {
                SkuDetailsParams.Builder newBuilder = SkuDetailsParams.newBuilder();
                newBuilder.setSkusList(list).setType(BillingClient.SkuType.INAPP);
                InAppPurchases.this.m_billingClient.querySkuDetailsAsync(newBuilder.build(), new SkuDetailsResponseListener() {
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                        if (billingResult.getResponseCode() != 0 || list == null) {
                            Log.d("com.valvesoftware.InAppPurchases", "onPurchasesUpdated error: " + billingResult.getDebugMessage());
                            return;
                        }
                        for (SkuDetails next : list) {
                            String sku = next.getSku();
                            if (InAppPurchases.this.m_skuDetailsTable.containsKey(sku)) {
                                InAppPurchases.this.m_skuDetailsTable.replace(sku, next);
                            } else {
                                InAppPurchases.this.m_skuDetailsTable.put(sku, next);
                            }
                            InAppPurchases.UpdateIAPPricing(sku, next.getOriginalPrice(), next.getPrice());
                        }
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                });
            }
        });
    }

    public void queryExistingPurchases() {
        runBillingClientJob(new Runnable() {
            public void run() {
                Purchase.PurchasesResult queryPurchases = InAppPurchases.this.m_billingClient.queryPurchases(BillingClient.SkuType.INAPP);
                if (queryPurchases.getResponseCode() == 0) {
                    InAppPurchases.this.onPurchasesUpdated(queryPurchases.getBillingResult(), queryPurchases.getPurchasesList());
                    return;
                }
                Log.d("com.valvesoftware.InAppPurchases", "queryPurchases error: " + queryPurchases.getResponseCode());
            }
        });
    }

    public void purchaseSku(final Activity activity, final String str) {
        runBillingClientJob(new Runnable() {
            public void run() {
                Log.d("com.valvesoftware.InAppPurchases", "attempting to purchase sku: " + str);
                SkuDetails skuDetails = (SkuDetails) InAppPurchases.this.m_skuDetailsTable.get(str);
                if (skuDetails == null) {
                    Log.d("com.valvesoftware.InAppPurchases", "querying sku details before purchasing: " + str);
                    ArrayList arrayList = new ArrayList();
                    arrayList.add(str);
                    InAppPurchases.this.querySkuDetails(arrayList, new Runnable() {
                        public void run() {
                            SkuDetails skuDetails = (SkuDetails) InAppPurchases.this.m_skuDetailsTable.get(str);
                            if (skuDetails == null) {
                                Log.d("com.valvesoftware.InAppPurchases", "purchaseSku error. sku not found: " + str);
                                return;
                            }
                            InAppPurchases.this.m_billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build());
                        }
                    });
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
            Log.d("com.valvesoftware.InAppPurchases", "Already consumed purchase token: " + str);
            return;
        }
        this.m_consumedTokens.add(str);
        final AnonymousClass5 r0 = new ConsumeResponseListener() {
            public void onConsumeResponse(BillingResult billingResult, String str) {
                int i;
                if (billingResult.getResponseCode() != 0) {
                    Log.d("com.valvesoftware.InAppPurchases", "onConsumeResponse error: " + billingResult.getDebugMessage());
                    i = 0;
                } else {
                    i = 1;
                    Log.d("com.valvesoftware.InAppPurchases", "Consumed token: " + str);
                }
                InAppPurchases.UpdateIAPConsumedStatus(str, i);
            }
        };
        runBillingClientJob(new Runnable() {
            public void run() {
                InAppPurchases.this.m_billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(str).build(), r0);
            }
        });
    }
}
