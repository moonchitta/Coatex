package com.ivor.coatex.crypto;

import com.ivor.coatex.utils.Util;

import org.spongycastle.x509.X509V1CertificateGenerator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

public class CryptoUtils {

    public static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator gen = KeyGenerator.getInstance("AES");
        gen.init(256); /* 256-bit AES */
        SecretKey secret = gen.generateKey();
        return secret;
    }

    public static String getBase64Encoded(SecretKey secretKey) {
        return Util.base64encode(secretKey.getEncoded());
    }

    public static SecretKey getBase64Decoded(String base64) {
        byte[] encoded = Util.base64decode(base64);
        return new SecretKeySpec(encoded, 0, encoded.length, "AES");
    }

    public static void createKeyStore(String certPath, String cn, KeyPair keyPair) {
        try {
            FileOutputStream fos = new FileOutputStream(certPath);
            X509Certificate certificate = generateCertificate("cn=" + cn, keyPair);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("selfsigned", certificate);
            keyStore.store(fos, cn.toCharArray());
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public static X509Certificate generateCertificate(String dn, KeyPair keyPair) throws GeneralSecurityException, IOException {
        // yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
// in 2 years
        Date validityEndDate = new Date(System.currentTimeMillis() + (2 * 365 * 24 * 60 * 60 * 1000));

        X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
        X500Principal dnName = new X500Principal(dn);
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName); // use the same
        certGen.setNotBefore(validityBeginDate);
        certGen.setNotAfter(validityEndDate);
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        return certGen.generate(keyPair.getPrivate());
    }
}
