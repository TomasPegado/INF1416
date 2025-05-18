package br.com.cofredigital.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

public class PrivateKeyUtilTest {
    private static final String KEYS_DIR = "Keys/";

    @Test
    public void testLoadAdminPrivateKey() {
        File adminKey = new File(KEYS_DIR + "admin-pkcs8-aes.key");
        PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(adminKey.getPath(), "admin");
        Assertions.assertNotNull(privateKey, "Admin private key should be loaded");
    }

    @Test
    public void testLoadAdminCertificate() {
        File adminCert = new File(KEYS_DIR + "admin-x509.crt");
        PublicKey publicKey = PrivateKeyUtil.loadPublicKeyFromCertificateFile(adminCert.getPath());
        Assertions.assertNotNull(publicKey, "Admin public key (certificate) should be loaded");
    }

    @Test
    public void testLoadUser01PrivateKey() {
        File user1Key = new File(KEYS_DIR + "user01-pkcs8-aes.key");
        PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(user1Key.getPath(), "user01");
        Assertions.assertNotNull(privateKey, "User01 private key should be loaded");
    }

    @Test
    public void testLoadUser01Certificate() {
        File user1Cert = new File(KEYS_DIR + "user01-x509.crt");
        PublicKey publicKey = PrivateKeyUtil.loadPublicKeyFromCertificateFile(user1Cert.getPath());
        Assertions.assertNotNull(publicKey, "User01 public key (certificate) should be loaded");
    }

    @Test
    public void testLoadUser02PrivateKey() {
        File user2Key = new File(KEYS_DIR + "user02-pkcs8-aes.key");
        PrivateKey privateKey = PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(user2Key.getPath(), "user02");
        Assertions.assertNotNull(privateKey, "User02 private key should be loaded");
    }

    @Test
    public void testLoadUser02Certificate() {
        File user2Cert = new File(KEYS_DIR + "user02-x509.crt");
        PublicKey publicKey = PrivateKeyUtil.loadPublicKeyFromCertificateFile(user2Cert.getPath());
        Assertions.assertNotNull(publicKey, "User02 public key (certificate) should be loaded");
    }

    @Test
    public void testGenerateAESKeyFromPassphrase() {
        SecretKey key = PrivateKeyUtil.generateAESKeyFromPassphrase("somepassphrase", 256);
        Assertions.assertNotNull(key, "AES key should be generated");
        Assertions.assertEquals(32, key.getEncoded().length, "AES-256 key should be 32 bytes");
    }

    @Test
    public void testLoadPrivateKeyWithWrongPassphrase() {
        File adminKey = new File(KEYS_DIR + "admin-pkcs8-aes.key");
        Assertions.assertNull(
            PrivateKeyUtil.loadEncryptedPKCS8PrivateKey(adminKey.getPath(), "wrongpass"),
            "Should return null when passphrase is incorrect"
        );
    }
} 