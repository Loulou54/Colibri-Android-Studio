package com.network.colibri;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.game.colibri.R;
import com.game.colibri.Toast;
import com.loopj.android.http.AsyncHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;

public final class CommonUtilities {

	// previous address: https://ssl17.ovh.net/~louisworix/
    // then: https://cluster017.hosting.ovh.net/~louisworix/
    public static final String SERVER_URL = "https://cluster017.hosting.ovh.net/~louisworix/colibri";

    // Communication token
    public static final String APP_TOKEN = "!,éà_fzsàç12e*rge<€4>";

    /**
     * Tag used on log messages.
     */
    public static final String BROADCAST_MESSAGE_ACTION =
            "com.game.colibri.BROADCAST_MESSAGE";
 
    public static final String EXTRA_MESSAGE = "message";
 
    /**
     * Envoie un message au BroadcastReceiver de Multijoueur
     *
     * @param context application's context.
     * @param message message to be broadcasted.
     */
    public static void broadcastMessage(Context context, String message) {
        Intent intent = new Intent(BROADCAST_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
    }

    public static void upgradeMessage(Context context) {
        Toast.makeText(context, R.string.maj_req, Toast.LENGTH_LONG).show();
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public static void addServerCACertToClient(Context context, AsyncHttpClient client) {
        try {
            client.setSSLSocketFactory(new SSLSocketFactory(getKeystoreWithCA(context.getResources().openRawResource(R.raw.cacert))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static KeyStore getKeystoreWithCA(InputStream customCert) {
        // Load CAs from an InputStream
        InputStream caInput = null;
        Certificate ca = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            caInput = new BufferedInputStream(customCert);
            ca = cf.generateCertificate(caInput);
        } catch (CertificateException e1) {
            e1.printStackTrace();
        } finally {
            try {
                if (caInput != null) {
                    caInput.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Create a KeyStore containing our trusted CAs
        KeyStore keyStore = null;
        KeyStore defaultCAs = null;
        try {
            keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            defaultCAs = KeyStore.getInstance("AndroidCAStore");
            defaultCAs.load(null,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(keyStore == null) {
            return defaultCAs;
        } else if(defaultCAs != null) {
            try {
                Enumeration<String> keyAliases=defaultCAs.aliases();
                while(keyAliases.hasMoreElements()) {
                    String alias = keyAliases.nextElement();
                    try {
                        Certificate cert = defaultCAs.getCertificate(alias);
                        if(!keyStore.containsAlias(alias))
                            keyStore.setCertificateEntry(alias,cert);
                    } catch(KeyStoreException e) {
                        e.printStackTrace();
                    }
                }
            } catch(KeyStoreException e) {
                e.printStackTrace();
            }
        }
        return keyStore;
    }
}
