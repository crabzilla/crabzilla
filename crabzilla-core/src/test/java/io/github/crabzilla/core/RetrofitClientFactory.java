package io.github.crabzilla.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClientFactory {

  static private final Logger log = LoggerFactory.getLogger(RetrofitClientFactory.class);

  public static <T> T create(String url, Class<T> retrofitInterface, ObjectMapper mapper) {

    final Boolean isSsl = false;

    log.info("using url: " + url + " ssl: = " + isSsl);

    try {

      final Retrofit.Builder builder = new Retrofit.Builder()
              .baseUrl(url)
              .addConverterFactory(ScalarsConverterFactory.create())
              .addConverterFactory(JacksonConverterFactory.create(mapper));

      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(HttpLoggingInterceptor.Level.BODY);
      OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
      httpClient.addInterceptor(logging);

      if (!isSsl) { // ssl is disabled ?
        builder.client(OkHttpClientFactory.getUnsafeOkHttpClient());
      } else {
        builder.client(httpClient.build());
      }

      final Retrofit retrofit = builder.build();
      final T retrofitClient = retrofit.create(retrofitInterface);
      return retrofitClient;

    } catch (Exception e) {
      log.error("Retrofit client for " + retrofitInterface.getName(), e);
    }

    return null;
  }

}
