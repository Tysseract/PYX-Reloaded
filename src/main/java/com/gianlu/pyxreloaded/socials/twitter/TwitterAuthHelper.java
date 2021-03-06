package com.gianlu.pyxreloaded.socials.twitter;

import com.gianlu.pyxreloaded.singletons.Preferences;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.undertow.util.Headers;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TwitterAuthHelper {
    private static final String VERIFY_CREDENTIALS_URL = "https://api.twitter.com/1.1/account/verify_credentials.json?include_email=true&skip_status=true&include_entities=false";
    private final OAuth10aService service;
    private final JsonParser parser = new JsonParser();

    private TwitterAuthHelper(@NotNull String appId, @NotNull String appSecret, @NotNull String callback) {
        service = new ServiceBuilder(appId)
                .apiSecret(appSecret)
                .callback(callback)
                .httpClient(new HttpClientWrapper())
                .build(TwitterApi.Authenticate.instance());
    }

    @Nullable
    public static TwitterAuthHelper instantiate(Preferences preferences) {
        String appId = preferences.getString("socials/twitterAppId", null);
        if (appId == null || appId.isEmpty()) return null;

        String appSecret = preferences.getString("socials/twitterAppSecret", null);
        if (appSecret == null || appSecret.isEmpty()) return null;

        String callback = preferences.getString("socials/twitterCallback", null);
        if (callback == null || callback.isEmpty()) return null;

        return new TwitterAuthHelper(appId, appSecret, callback);
    }

    @NotNull
    public OAuth1RequestToken requestToken() throws IOException, ExecutionException, InterruptedException {
        return service.getRequestToken();
    }

    @NotNull
    public String authorizationUrl(OAuth1RequestToken token) {
        return service.getAuthorizationUrl(token);
    }

    @NotNull
    public OAuth1AccessToken accessToken(OAuth1RequestToken token, String verifier) throws InterruptedException, ExecutionException, IOException {
        return service.getAccessToken(token, verifier);
    }

    @NotNull
    public TwitterProfileInfo info(OAuth1AccessToken token) throws InterruptedException, ExecutionException, IOException, TwitterEmailNotVerifiedException {
        OAuthRequest request = new OAuthRequest(Verb.GET, VERIFY_CREDENTIALS_URL);
        service.signRequest(token, request);
        Response response = service.execute(request);

        try {
            return new TwitterProfileInfo(parser.parse(response.getBody()).getAsJsonObject());
        } catch (JsonParseException | NullPointerException ex) {
            throw new IOException(ex);
        }
    }

    @NotNull
    public String appId() {
        return service.getConfig().getApiKey();
    }

    public void close() throws IOException {
        service.getConfig().getHttpClient().close();
    }

    private static class HttpClientWrapper implements HttpClient {
        private final CloseableHttpClient client;

        HttpClientWrapper() {
            client = HttpClients.createDefault();
        }

        private static HttpRequestBase initialSetup(Verb httpVerb, String completeUrl, String userAgent, Map<String, String> headers) {
            HttpRequestBase request;
            switch (httpVerb) {
                case GET:
                    request = new HttpGet(completeUrl);
                    break;
                case POST:
                    request = new HttpPost(completeUrl);
                    break;
                case PUT:
                    request = new HttpPut(completeUrl);
                    break;
                case DELETE:
                    request = new HttpDelete(completeUrl);
                    break;
                case HEAD:
                    request = new HttpHead(completeUrl);
                    break;
                case OPTIONS:
                    request = new HttpOptions(completeUrl);
                    break;
                case TRACE:
                    request = new HttpTrace(completeUrl);
                    break;
                case PATCH:
                    request = new HttpPatch(completeUrl);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Verb.");
            }

            request.addHeader(Headers.USER_AGENT_STRING, userAgent);

            for (Map.Entry<String, String> entry : headers.entrySet())
                request.addHeader(entry.getKey(), entry.getValue());

            return request;
        }

        @Override
        public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, byte[] bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, String bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, File bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequest.ResponseConverter<T> converter) {
            throw new UnsupportedOperationException();
        }

        @NotNull
        private Response execute(HttpRequestBase request) throws IOException {
            HttpResponse resp = client.execute(request);

            Map<String, String> headers = new HashMap<>();
            for (Header header : request.getAllHeaders())
                headers.put(header.getName(), header.getValue());

            StatusLine sl = resp.getStatusLine();
            return new Response(sl.getStatusCode(), sl.getReasonPhrase(), headers, resp.getEntity().getContent());
        }

        @Override
        public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, byte[] bodyContents) throws IOException {
            HttpRequestBase request = initialSetup(httpVerb, completeUrl, userAgent, headers);
            if (request instanceof HttpEntityEnclosingRequestBase)
                ((HttpEntityEnclosingRequestBase) request).setEntity(new ByteArrayEntity(bodyContents));

            return execute(request);
        }

        @Override
        public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, String bodyContents) throws IOException {
            HttpRequestBase request = initialSetup(httpVerb, completeUrl, userAgent, headers);
            if (request instanceof HttpEntityEnclosingRequestBase)
                ((HttpEntityEnclosingRequestBase) request).setEntity(new StringEntity(bodyContents));

            return execute(request);
        }

        @Override
        public Response execute(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, File bodyContents) throws IOException {
            HttpRequestBase request = initialSetup(httpVerb, completeUrl, userAgent, headers);
            if (request instanceof HttpEntityEnclosingRequestBase)
                ((HttpEntityEnclosingRequestBase) request).setEntity(new FileEntity(bodyContents));

            return execute(request);
        }

        @Override
        public void close() throws IOException {
            client.close();
        }
    }
}
