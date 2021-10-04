package cc.magickiat.crypto.botnaja.service;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ApiKeySecretInterceptor implements Interceptor {
    private static final String BITKUB_API_KEY = System.getenv("BITKUB_API_KEY");

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        request = request.newBuilder()
                .removeHeader("x-btk-apikey")
                .addHeader("x-btk-apikey", BITKUB_API_KEY)
                .build();

        return chain.proceed(request);
    }
}