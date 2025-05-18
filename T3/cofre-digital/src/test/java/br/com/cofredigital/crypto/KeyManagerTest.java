package br.com.cofredigital.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyManagerTest {
    private KeyManager keyManager = new KeyManager();
    private static final String KEYS_DIR = "Keys/";

    @BeforeEach
    public void setup() {
        keyManager = new KeyManager();
    }

    @Test
    public void testLoadAdminPrivateKey() throws Exception {
        File adminKey = new File(KEYS_DIR + "admin-pkcs8-aes.key");
        PrivateKey privateKey = keyManager.loadPrivateKey(adminKey, "admin");
        Assertions.assertNotNull(privateKey, "Admin private key should be loaded");
    }

    @Test
    public void testLoadAdminCertificate() throws Exception {
        File adminCert = new File(KEYS_DIR + "admin-x509.crt");
        PublicKey publicKey = keyManager.loadPublicKey(adminCert);
        Assertions.assertNotNull(publicKey, "Admin public key (certificate) should be loaded");
    }

    @Test
    public void testLoadUser01PrivateKey() throws Exception {
        File user1Key = new File(KEYS_DIR + "user01-pkcs8-aes.key");
        PrivateKey privateKey = keyManager.loadPrivateKey(user1Key, "user01");
        Assertions.assertNotNull(privateKey, "User01 private key should be loaded");
    }

    @Test
    public void testLoadUser01Certificate() throws Exception {
        File user1Cert = new File(KEYS_DIR + "user01-x509.crt");
        PublicKey publicKey = keyManager.loadPublicKey(user1Cert);
        Assertions.assertNotNull(publicKey, "User01 public key (certificate) should be loaded");
    }

    @Test
    public void testLoadUser02PrivateKey() throws Exception {
        File user2Key = new File(KEYS_DIR + "user02-pkcs8-aes.key");
        PrivateKey privateKey = keyManager.loadPrivateKey(user2Key, "user02");
        Assertions.assertNotNull(privateKey, "User02 private key should be loaded");
    }

    @Test
    public void testLoadUser02Certificate() throws Exception {
        File user2Cert = new File(KEYS_DIR + "user02-x509.crt");
        PublicKey publicKey = keyManager.loadPublicKey(user2Cert);
        Assertions.assertNotNull(publicKey, "User02 public key (certificate) should be loaded");
    }

    @Test
    public void testGenerateAESKeyFromPassphrase() throws Exception {
        SecretKey key = keyManager.generateAESKeyFromPassphrase("somepassphrase", 256);
        Assertions.assertNotNull(key, "AES key should be generated");
        Assertions.assertEquals(32, key.getEncoded().length, "AES-256 key should be 32 bytes");
    }

    @Test
    public void testLoadPrivateKeyWithWrongPassphrase() {
        File adminKey = new File(KEYS_DIR + "admin-pkcs8-aes.key");
        Assertions.assertThrows(Exception.class, () -> {
            keyManager.loadPrivateKey(adminKey, "wrongpass");
        }, "Should throw when passphrase is incorrect");
    }
} 