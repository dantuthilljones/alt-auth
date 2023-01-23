package me.detj.alt.handler;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import me.detj.alt.Util;
import me.detj.alt.config.Config;
import org.apache.commons.lang3.SerializationUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@AllArgsConstructor
public class AuthHandler {
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private final Config config;

    private PublicKey publicKey;

    private static Optional<byte[]> decodeCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        Optional<Cookie> foundCookie = Arrays.stream(cookies).filter(cookie -> cookie.getName().equals(cookieName)).findAny();
        if (!foundCookie.isPresent()) {
            return Optional.empty();
        }

        String encodedSignature = foundCookie.get().getValue();
        return Optional.ofNullable(decoder.decode(encodedSignature));
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) {
        try {
            Optional<byte[]> authDateBytesOptional = decodeCookie(request, config.getCookieNameAuth());
            Optional<byte[]> signatureBytesOptional = decodeCookie(request, config.getCookieNameSignature());
            if (authDateBytesOptional.isEmpty() || signatureBytesOptional.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("No auth or signature cookie.");
                return;
            }

            byte[] authDateBytes = authDateBytesOptional.get();
            byte[] signatureBytes = signatureBytesOptional.get();

            Date date = SerializationUtils.deserialize(authDateBytes);

            if (!verifySignature(authDateBytes, signatureBytes)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("Signature Invalid.");
                return;
            } else if (date.before(new Date(System.currentTimeMillis()))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("Auth cookie has expired.");
                return;
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("Auth cookie valid.");

        } catch (Exception e) {
            Util.handleException(response, e);
        }
    }

    @SneakyThrows
    private boolean verifySignature(byte[] authDateBytes, byte[] signatureBytes) {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(authDateBytes);
        return signature.verify(signatureBytes);
    }
}
