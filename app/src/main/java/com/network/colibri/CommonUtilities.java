package com.network.colibri;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.game.colibri.MyApp;
import com.game.colibri.R;
import com.game.colibri.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import cz.msebera.android.httpclient.conn.ssl.SSLContexts;
import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;
import cz.msebera.android.httpclient.conn.ssl.X509HostnameVerifier;

public final class CommonUtilities {

	// previous cluster address: https://ssl17.ovh.net/~louisworix/
    // then: https://cluster017.hosting.ovh.net/~louisworix/
    private static final String CLUSTER_HOSTNAME = "cluster017.hosting.ovh.net";

    public static final String SERVER_URL = "https://louisworkplace.net/colibri";

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

    public static SSLSocketFactory SSL_SOCKET_FACTORY = getCustomSSLSocketFactory();

    /**
     * Création d'une SSL Socket Factory spécifique pour supporter les anciennes versions d'Android qui:
     * 1) Ne supportent pas SNI (Server Name Indication). Dans ce cas, l'appareil reçoit le certificat
     *    de cluster017.hosting.ovh.net au lieu de louisworkplace.net.
     * 2) N'ont pas le certificat de l'autorité de certification "USERTrust RSA Certification Authority"
     *    qui est à la racine des certificats d'OVH pour le domaine cluster017.hosting.ovh.net.
     * @return custom SSL Socket Factory or default
     */
    private static SSLSocketFactory getCustomSSLSocketFactory() {
        KeyStore keystore = getKeystoreWithCA(MyApp.getApp().getResources().openRawResource(R.raw.cacert));
        X509HostnameVerifier hostnameVerifier = new X509HostnameVerifier() {
            @Override
            public void verify(String host, SSLSocket ssl) throws IOException {
                try {
                    SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(host, ssl);
                } catch (IOException e) {
                    try {
                        SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(CLUSTER_HOSTNAME, ssl);
                    } catch (IOException e2) {
                        e.printStackTrace();
                        e2.printStackTrace();
                        throw e; // On throw l'exception d'origine
                    }
                }
            }
            @Override
            public void verify(String host, X509Certificate cert) throws SSLException {
                try {
                    SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(host, cert);
                } catch (SSLException e) {
                    try {
                        SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(CLUSTER_HOSTNAME, cert);
                    } catch (SSLException e2) {
                        e.printStackTrace();
                        e2.printStackTrace();
                        throw e; // On throw l'exception d'origine
                    }
                }
            }
            @Override
            public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
                try {
                    SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(host, cns, subjectAlts);
                } catch (SSLException e) {
                    try {
                        SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(CLUSTER_HOSTNAME, cns, subjectAlts);
                    } catch (SSLException e2) {
                        e.printStackTrace();
                        e2.printStackTrace();
                        throw e; // On throw l'exception d'origine
                    }
                }
            }
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(s, sslSession);
            }
        };
        try {
            return new SSLSocketFactory(SSLContexts.custom().loadTrustMaterial(keystore).build(), hostnameVerifier);
        } catch (Exception e) {
            e.printStackTrace();
            return SSLSocketFactory.getSocketFactory();
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
