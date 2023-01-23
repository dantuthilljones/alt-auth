package me.detj.alt.handler;

import com.lambdaworks.crypto.SCryptUtil;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import me.detj.alt.Util;
import me.detj.alt.config.Config;
import org.apache.commons.lang3.SerializationUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Date;

@AllArgsConstructor
public class LoginHandler {

    private static final Base64.Encoder encoder = Base64.getEncoder();

    private final Config config;
    private final PrivateKey privateKey;

    public void handleLogin(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!checkPassword(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("Password incorrect.");
                return;
            }

            Date expiration = new Date(System.currentTimeMillis() + config.getDurationMillis());
            byte[] expirationBytes = SerializationUtils.serialize(expiration);

            response.setStatus(HttpServletResponse.SC_OK);
            response.addCookie(buildAuthCookie(expirationBytes));
            response.addCookie(buildSignatureCookie(expirationBytes));
            response.getWriter().print("Password OK.");
        } catch (Exception e) {
            Util.handleException(response, e);
        }
    }

    private boolean checkPassword(HttpServletRequest request) {
        String password = request.getParameter("password");
        return password != null && SCryptUtil.check(password, config.getPasswordHash());
    }

    private Cookie buildAuthCookie(byte[] expirationBytes) {
        String expirationEncoded = encoder.encodeToString(expirationBytes);
        Cookie cookie = new Cookie(config.getCookieNameAuth(), expirationEncoded);
        cookie.setMaxAge((int) config.getDurationMillis() / 1000);//setMaxAge expects seconds
        return cookie;
    }

    @SneakyThrows
    private Cookie buildSignatureCookie(byte[] expirationBytes) {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(expirationBytes);
        String signatureEncoded = encoder.encodeToString(signature.sign());
        Cookie cookie = new Cookie(config.getCookieNameSignature(), signatureEncoded);
        cookie.setMaxAge((int) config.getDurationMillis() / 1000);//setMaxAge expects seconds
        return cookie;
    }
}
