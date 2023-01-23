package me.detj.alt;

import com.lambdaworks.crypto.SCryptUtil;
import lombok.SneakyThrows;
import me.detj.alt.config.Config;
import me.detj.alt.handler.AuthHandler;
import me.detj.alt.handler.LoginHandler;
import me.detj.alt.handler.RequestHandler;
import org.eclipse.jetty.server.Server;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class AltAuth {

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equalsIgnoreCase("hash")) {
            System.out.println(SCryptUtil.scrypt(args[1], 16384, 16, 1));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("server")) {
            Config config = Config.readFromFile(new File(args[1]));
            startServer(config);
        } else {
            System.out.println("Incorrect arguments. Expecting `hash PASSWORD` or `server CONFIG_FILE`");
            System.exit(1);
        }
    }

    private static void startServer(Config config) throws Exception {
        Server server = new Server(config.getPort());

        KeyPair keyPair = generateKeyPair();

        server.setHandler(new RequestHandler(
                new LoginHandler(config, keyPair.getPrivate()),
                new AuthHandler(config, keyPair.getPublic())
        ));

        server.start();
        server.join();
    }

    @SneakyThrows
    private static KeyPair generateKeyPair() {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }
}