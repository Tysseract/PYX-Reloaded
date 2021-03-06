package com.gianlu.pyxreloaded.paths;

import com.gianlu.pyxreloaded.Consts;
import com.gianlu.pyxreloaded.Utils;
import com.gianlu.pyxreloaded.socials.twitter.TwitterAuthHelper;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TwitterCallbackPath implements HttpHandler {
    private static final Logger logger = Logger.getLogger(TwitterCallbackPath.class);
    private static final int COOKIE_MAX_AGE = (int) TimeUnit.MINUTES.toSeconds(5); // sec
    private static final String REDIRECT_LOCATION = "/?" + Consts.GeneralKeys.AUTH_TYPE + "=" + Consts.AuthType.TWITTER;
    private final TwitterAuthHelper helper;

    public TwitterCallbackPath(@NotNull TwitterAuthHelper helper) {
        this.helper = helper;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        try {
            String token = Utils.extractParam(exchange, "oauth_token");
            String verifier = Utils.extractParam(exchange, "oauth_verifier");

            if (token == null || verifier == null)
                throw new IllegalArgumentException("Missing token or verifier!");

            Cookie tokensCookie = exchange.getRequestCookies().get("PYX-Twitter-Token");
            if (tokensCookie == null)
                throw new IllegalArgumentException("Missing 'PYX-Twitter-Token' cookie!");

            List<NameValuePair> tokens = URLEncodedUtils.parse(tokensCookie.getValue(), Charset.forName("UTF-8"));
            if (!Objects.equals(token, Utils.get(tokens, "oauth_token")))
                throw new IllegalStateException("Missing token in cookie or tokens don't match!");

            String secret = Utils.get(tokens, "oauth_token_secret");
            if (secret == null)
                throw new IllegalArgumentException("Missing token secret in cookie!");

            OAuth1AccessToken accessToken = helper.accessToken(new OAuth1RequestToken(token, secret), verifier);
            CookieImpl cookie = new CookieImpl("PYX-Twitter-Token", accessToken.getRawResponse());
            cookie.setMaxAge(COOKIE_MAX_AGE);
            exchange.setResponseCookie(cookie);
            exchange.getResponseHeaders().add(Headers.LOCATION, REDIRECT_LOCATION);
            exchange.setStatusCode(StatusCodes.TEMPORARY_REDIRECT);
        } catch (Throwable ex) {
            logger.error("Failed processing the request: " + exchange, ex);
            throw ex;
        }
    }
}
