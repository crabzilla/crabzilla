package io.github.crabzilla.example1;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.net.ssl.*;
import java.security.cert.CertificateException;

public class OkHttpClientFactory {

  /**
   * @return
   * @throws Exception
   */
  public static OkHttpClient getUnsafeOkHttpClient() throws Exception {

    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
              }
              @Override
              public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
              }
              @Override
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
              }
            }
    };

    // Install the all-trusting trust manager
    final SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

    // Create an ssl socket factory with our all-trusting manager
    final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

    // logger
    final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory)
            .hostnameVerifier(new HostnameVerifier() {
              @Override
              public boolean verify(String s, SSLSession sslSession) {
                return true;
              }
            })
            .addInterceptor(loggingInterceptor)
            .build();

    return okHttpClient;

  }

}
