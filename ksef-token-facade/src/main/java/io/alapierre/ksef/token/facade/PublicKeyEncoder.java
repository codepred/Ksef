package io.alapierre.ksef.token.facade;

import io.alapierre.io.IOUtils;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * @author Adrian Lapierre {@literal al@alapierre.io}
 * Copyrights by original author 2022.11.06
 */
@Slf4j
public class PublicKeyEncoder {

    private final byte[] publicKey;

    @SneakyThrows
    public static PublicKeyEncoder withBundledKey(@NonNull String env)  {

        String keyName;

        switch (env) {
            case "TEST":
                keyName = "publicKey-test.der";
                break;
            case "DEMO":
                keyName = "publicKey-demo.der";
                break;
            case "PROD":
                keyName = "publicKey-prod.der";
                break;
            default: throw new IllegalStateException("Unknown environment name '" + env + "'");
        }

        System.out.println("klucz publiczny {}" + keyName);
        InputStream pk = PublicKeyEncoder.class.getClassLoader().getResourceAsStream(keyName);
        if (pk == null) {
            throw new IllegalStateException("Can't load bundled key " + keyName + " from classpath");
        } else {
            return new PublicKeyEncoder(pk);
        }
    }

    public PublicKeyEncoder(@NonNull InputStream publicKeyStream) throws IOException {
        this.publicKey = IOUtils.toByteArray(publicKeyStream);
    }

    public static @NotNull Date parseChallengeTimestamp(@NotNull String challengeTimestamp) throws ParseException {
        return (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")).parse(challengeTimestamp);
    }

    @SneakyThrows
    public @NotNull String encodeSessionToken(@NotNull String token, @NotNull Date challengeTimestamp) {

        String encryptedToken;
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(this.publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
        byte[] message = (token + "|" + challengeTimestamp.getTime()).getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(1, publicKey);
        byte[] encrypted = cipher.doFinal(message);
        encryptedToken = Base64.getEncoder().encodeToString(encrypted);
        return encryptedToken;
    }

}
