///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.jasypt:jasypt:1.9.3

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;

@Command(name = "encryptor", mixinStandardHelpOptions = true)
class encryptor implements Callable<Integer> {
    @Option(names = {"-s", "--secret" }, description = "Secret", required = true)
    String secret;
    @Option(names = {"-k", "--key" }, description = "Encryption Key")
    String encryptionKey;
    @Option(names = { "-f", "--format" }, description = "Encryption Key Format (base64 / plain)", defaultValue = "base64")
    KeyFormat encryptionKeyFormat;
    @Option(names = {"-a", "--algorithm" }, description = "Algorithm", defaultValue = "AES", hidden = true)
    String algorithm;
    @Option(names = {"-m", "--mode" }, description = "Mode", defaultValue = "GCM", hidden = true)
    String mode;
    @Option(names = {"-p", "--padding" }, description = "Padding", defaultValue = "NoPadding", hidden = true)
    String padding;

    public static void main(String... args) {
        int exitCode = new CommandLine(new encryptor()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (encryptionKey == null) {
            encryptionKey = encodeToString(generateEncryptionKey().getEncoded());
        } else {
            if (encryptionKeyFormat.equals(KeyFormat.base64)) {
                encryptionKey = encodeToString(encryptionKey.getBytes());
            }
        }

        Cipher cipher = Cipher.getInstance(algorithm + "/" + mode + "/" + padding);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(encryptionKey.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha256.digest(), "AES"), new GCMParameterSpec(128, iv));

        byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));

        ByteBuffer message = ByteBuffer.allocate(1 + iv.length + encrypted.length);
        message.put((byte) iv.length);
        message.put(iv);
        message.put(encrypted);

        String encrypt = Base64.getUrlEncoder().withoutPadding().encodeToString((message.array()));
        System.out.println("${aes-gcm-nopadding::" + encrypt + "}");
        System.out.println("smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key=" + encryptionKey);

        return 0;
    }

    private SecretKey generateEncryptionKey() {
        try {
            return KeyGenerator.getInstance(algorithm).generateKey();
        } catch (Exception e) {
            System.err.println("Error while generating the encryption key: " + e);
            System.exit(-1);
        }
        return null;
    }

    private static String encodeToString(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public enum KeyFormat {
        base64,
        plain
    }
}
