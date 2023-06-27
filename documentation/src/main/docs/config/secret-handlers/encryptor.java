///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.jasypt:jasypt:1.9.3

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.crypto.Cipher;
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
    @Parameters(index = "0")
    private String secret;
    @Parameters(index = "1")
    private String encryptionKey;
    @Parameters(index = "2", hidden = true, defaultValue = "AES/GCM/NoPadding")
    private String algorithm;
    @Parameters(index = "3", hidden = true, defaultValue = "128")
    private Integer length;

    public static void main(String... args) {
        int exitCode = new CommandLine(new encryptor()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        sha256.update(encryptionKey.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha256.digest(), "AES"), new GCMParameterSpec(length, iv));

        byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));

        ByteBuffer message = ByteBuffer.allocate(1 + iv.length + encrypted.length);
        message.put((byte) iv.length);
        message.put(iv);
        message.put(encrypted);

        String encrypt = Base64.getUrlEncoder().withoutPadding().encodeToString((message.array()));
        System.out.println("${aes-gcm-nopadding::" + encrypt + "}");

        return 0;
    }
}
