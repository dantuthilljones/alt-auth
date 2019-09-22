package me.detj.alt;

import com.lambdaworks.crypto.SCryptUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.*;
import java.util.*;

public class AltAuth extends AbstractHandler {

    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static final Base64.Decoder decoder = Base64.getDecoder();

    private final Config config;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public AltAuth(Config config) throws NoSuchAlgorithmException {
        this.config = config;

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        privateKey = kp.getPrivate();
        publicKey = kp.getPublic();
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        baseRequest.setHandled(true);
        response.setContentType("text/plain");
        if (Objects.equals(request.getHeader("X-IS-LOGIN"), "true")
                && Objects.equals(request.getMethod(), "POST")) {
            handleLogin(request, response);
        } else {
            handleAuth(request, response);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!config.checkPassword(request.getParameter("password"))) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("Password incorrect.");
                return;
            }

            Date expiration = new Date(System.currentTimeMillis() + config.getDurationMillis());
            byte[] expirationBytes = SerializationUtils.serialize(expiration);
            String expirationEncoded = encoder.encodeToString(expirationBytes);
            Cookie authCookie = new Cookie("auth", expirationEncoded);

            //note setMaxAge expects the age to be in seconds
            authCookie.setMaxAge((int) config.getDurationSeconds());

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(expirationBytes);
            String signatureEncoded = encoder.encodeToString(signature.sign());
            Cookie signatureCookie = new Cookie("signature", signatureEncoded);
            signatureCookie.setMaxAge((int) config.getDurationSeconds());

            response.setStatus(HttpServletResponse.SC_OK);
            response.addCookie(authCookie);
            response.addCookie(signatureCookie);
            response.getWriter().print("Auth cookie set.");
        } catch (Exception e) {
            try {
                e.printStackTrace(response.getWriter());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleAuth(HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("No auth or signature cookie.");
                return;
            }
            Optional<Cookie> authCookie = Arrays.stream(cookies).filter(cookie -> cookie.getName().equals("auth")).findAny();
            Optional<Cookie> signatureCookie = Arrays.stream(cookies).filter(cookie -> cookie.getName().equals("signature")).findAny();
            if (!authCookie.isPresent() || !signatureCookie.isPresent()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().print("No auth or signature cookie.");
            } else {
                String encodedDate = authCookie.get().getValue();
                byte[] dateBytes = decoder.decode(encodedDate);
                Date date = SerializationUtils.deserialize(dateBytes);
                Date expiration = new Date(System.currentTimeMillis() + config.getDurationMillis());

                Signature signature = Signature.getInstance("SHA256withRSA");
                signature.initVerify(publicKey);
                signature.update(dateBytes);
                String encodedSignature = signatureCookie.get().getValue();
                byte[] signatureBytes = decoder.decode(encodedSignature);
                if (!signature.verify(signatureBytes)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().print("Signature Invalid.");
                } else if (date.after(expiration)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().print("Auth cookie has expired.");
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().print("Auth cookie valid.");
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            try {
                e.printStackTrace(response.getWriter());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("hash")) {
            System.out.println(SCryptUtil.scrypt(args[1], 16384, 16, 1));
        } else if (args.length == 2 && args[0].equals("server")) {
            Config config = Config.readFromFile(new File(args[1]));
            startServer(config);
        } else {
            System.out.println("Incorrect arguments. Expecting `hash PASSWORD` or `server CONFIG_FILE`");
            System.exit(1);
        }
    }

    private static void startServer(Config config) throws Exception {
        Server server = new Server(config.getPort());
        server.setHandler(new AltAuth(config));

        server.start();
        server.join();
    }
}