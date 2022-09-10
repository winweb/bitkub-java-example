package cc.magickiat.crypto.botnaja.service;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

public class ErrorInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        try(Response response = chain.proceed(request)) {

            if (response.code() >= 400) {
                throw new HttpException(Objects.requireNonNull(response.body()).toString());
            }

            return response;
        }
    }
}
