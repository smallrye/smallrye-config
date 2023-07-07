package io.smallrye.config.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.smallrye.config.SecretKeysHandler;

public class AESGCMNoPaddingSecretKeysHandler implements SecretKeysHandler {
    private final SecretKeySpec encryptionKey;

    public AESGCMNoPaddingSecretKeysHandler(final byte[] encryptionKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(encryptionKey);
            this.encryptionKey = new SecretKeySpec(sha256.digest(), "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String decode(final String secret) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            ByteBuffer byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(secret.getBytes(UTF_8)));
            int ivLength = byteBuffer.get();
            byte[] iv = new byte[ivLength];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "aes-gcm-nopadding";
    }
}
