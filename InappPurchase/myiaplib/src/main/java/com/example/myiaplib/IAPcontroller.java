package com.example.myiaplib;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;
public  class IAPcontroller implements PurchasesUpdatedListener {
    public static BillingClient billingClient;

    private String product = "noads";
    Context mcontext;
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;
    public static IAPcontroller paymentControl;

    public IAPcontroller(Context mcontext) {
        this.mcontext = mcontext;
        sharedPreferences = mcontext.getSharedPreferences("ONPURCHASEDISABLEADS", mcontext.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static IAPcontroller getInstance() {
        return paymentControl;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
            for (Purchase purchase : list) {
                verifyPayment(purchase);
            }
        } else if (BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED == BillingClient.BillingResponseCode.OK) {
            for (Purchase purchase : list) {
                verifyPayment(purchase);// to know if user alerdy own it
            }
        }
    }

    public  void establishConnection(Context context,String purhasekey) {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                establishConnection(context,purhasekey);
                Toast.makeText(context, "connection Failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    getProducts(purhasekey);
                }

            }
        });
    }

    public void getProducts(String key) {
        List<String> skuList = new ArrayList<>();
        skuList.add(key);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, @Nullable List<SkuDetails> skuDetailsList) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    for (SkuDetails skuDetails : skuDetailsList) {
                        if (skuDetails.getSku().equals(key)) {
                            launchPurchaseFlow(skuDetails);
//                            removeads.setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View view) {
//                                    launchPurchaseFlow(skuDetails);
//                                }
//                            });
                        }
                    }
                }
            }
        });
    }

    public void launchPurchaseFlow(SkuDetails skuDetails) {
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
        billingClient.launchBillingFlow((Activity) mcontext, billingFlowParams);
    }

    public void verifyPayment(Purchase purchase) {
//    if (PRODUCT_ID.equals(purchase.getSku()) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            // add a shared prefernce value to disable ads
                            editor.putBoolean("ITEMPURCHASED", true);
                            editor.apply();
//                            commonMethods.disableAds();

                        }
                    }
                });
            }
        } else {
            editor.putBoolean("ITEMPURCHASED", false);
            editor.apply();
        }
    }

    public void OnactivityResume() {
        PurchasesResponseListener purchasesResponseListener = new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : list) {
                        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                            verifyPayment(purchase);
                        }
                    }
                }
            }
        };
    }

//    public void Restorethepurchase() {
//        if (billingClient != null) {
//            billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, new PurchasesResponseListener() {
//                @Override
//                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
//                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                        for (Purchase purchase : list) {
//                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
//                                verifyPayment(purchase);
//                            } else {
//                                editor.putBoolean("ITEMPURCHASED", false);
//                                editor.apply();
//                            }
//                        }
//                    }
//                }
//            });
//        }
//
//    }

    public void Restorethepurchaserecods(Context context) {
        billingClient = BillingClient.newBuilder(mcontext).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalbillingclient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {

            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalbillingclient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), ((billingResult1, list) -> {
                        if (list.size() > 0) {
                            editor.putBoolean("ITEMPURCHASED", true);
                            editor.apply();
                        } else {
                            editor.putBoolean("ITEMPURCHASED", false);
                            editor.apply();
                        }
                    }));
                }
            }
        });
    }




}
