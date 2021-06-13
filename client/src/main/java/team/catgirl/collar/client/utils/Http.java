package team.catgirl.collar.client.utils;

import io.netty.handler.ssl.SslContextBuilder;
import team.catgirl.collar.http.HttpClient;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public final class Http {

    private static final HttpClient client;

    static {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream is = Http.class.getResourceAsStream("/cacerts")) {
                keyStore.load(is, "changeit".toCharArray());
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            client = new HttpClient(SslContextBuilder.forClient().trustManager(trustManagerFactory).build());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpClient client() {
        return client;
    }

    private Http() {}
}
