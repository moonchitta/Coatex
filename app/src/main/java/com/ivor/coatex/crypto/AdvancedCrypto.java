package com.ivor.coatex.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AdvancedCrypto {

    private static String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static int IV_LENGTH = 16;
    private static String SECRET_KEY_ALGORITHM = "AES";
    private static String HASH_ALGORITHM = "SHA-512";
    private static int SALT_LENGTH = 20;
    private static String RANDOM_ALGORITHM = "SHA1PRNG";
    private static int KEY_SIZE = 256;


    private static String SALT = "E79A7E78A03C";


    private static int PBE_ITERATION_COUNT = 100;

    private static String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";

    private String key;

    public String getKey() {
        return key;
    }

    /**
     * set key for the cipher to use
     *
     * @param key
     * @throws Exception
     */
    public void setKey(String key) throws Exception {
        this.key = key;
        this.secret = getSecretKey(key);
    }

    public SecretKey getSecret() {
        return secret;
    }

    public void setSecret(SecretKey secret) {
        this.secret = secret;
    }

    private SecretKey secret;

    /**
     * the string text of the key which will be used for encryption and decryption
     *
     * @param key
     * @throws Exception
     */
    public AdvancedCrypto(String key) throws Exception {
        this.key = key;
        this.secret = getSecretKey(key);
    }

    /**
     * @param key SecretKey to be used
     */
    public AdvancedCrypto(SecretKey key) {
        this.secret = key;
    }

    /**
     * {@link java.security.spec.X509EncodedKeySpec} encoded key bytes
     *
     * @param key
     */
    public AdvancedCrypto(byte[] key) {
        this.secret = convertKey(key);
    }

    /**
     * Encrypt data
     * the returned data will be encoded into HEX string
     *
     * @param cleartext
     * @return
     * @throws Exception
     */
    public String encrypt(String cleartext)
            throws Exception {
        byte[] iv = generateIv();
        String ivHex = toHex(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
        encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
        byte[] encryptedText = encryptionCipher.doFinal(cleartext
                .getBytes(StandardCharsets.UTF_8));
        String encryptedHex = toHex(encryptedText);

        return ivHex + encryptedHex;
    }

    /**
     * convert provided keyByte into {@link SecretKey}
     *
     * @param keyBytes
     * @return
     */
    public SecretKey convertKey(byte[] keyBytes) {
        SecretKey key = new SecretKeySpec(keyBytes, SECRET_KEY_ALGORITHM);
        return key;
    }

    /**
     * decrypt data
     * the provided string should be encoded into HEX string
     *
     * @param encrypted
     * @return
     * @throws Exception
     */
    public String decrypt(String encrypted)
            throws Exception {

        Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM);
        String ivHex = encrypted.substring(0, IV_LENGTH * 2);
        String encryptedHex = encrypted.substring(IV_LENGTH * 2);
        IvParameterSpec ivspec = new IvParameterSpec(toByte(ivHex));
        decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
        byte[] decryptedText = decryptionCipher
                .doFinal(toByte(encryptedHex));
        return new String(decryptedText, StandardCharsets.UTF_8);
    }

    /**
     * Encrypts the inputFile and saves to ouputFile location
     *
     * @param key        SecretKey to use
     * @param inputFile  Path of the file to be encrypted
     * @param outputFile Path of the file where encrypted file will be write to
     * @throws Exception
     */
    public void encryptFile(SecretKey key, String inputFile, String outputFile)
            throws Exception {

        byte[] iv = generateIv();
        String ivHex = toHex(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
        File file = new File(outputFile);

        FileInputStream fis = new FileInputStream(inputFile);

        FileOutputStream fos = new FileOutputStream(file);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);

        // write ivhex
        fos.write(ivHex.getBytes());
        fos.flush();

        // write encrypted file
        int read = -1;
        byte[] buffer = new byte[2048];
        while ((read = fis.read(buffer)) > 0) {
            cos.write(buffer, 0, read);
            cos.flush();
        }

        cos.close();
        fos.close();
        fis.close();
    }

    /**
     * encrypts file, with specified headers
     *
     * @param key        SecretKey to be used
     * @param headers    bytes which are being written before actual file
     * @param inputFile  the path of input file
     * @param outputFile the path of file where to save
     * @throws Exception
     */
    public void encryptFile(SecretKey key, byte[] headers, String inputFile, String outputFile)
            throws Exception {

        byte[] iv = generateIv();
        String ivHex = toHex(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
        File file = new File(outputFile);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileInputStream fis = new FileInputStream(inputFile);

        FileOutputStream fos = new FileOutputStream(file);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);

        if (headers != null && headers.length > 0) {
            fos.write(headers);
            fos.flush();
        }

        // write ivhex
        fos.write(ivHex.getBytes());
        fos.flush();

        // write encrypted file
        int read = -1;
        byte[] buffer = new byte[2048];
        while ((read = fis.read(buffer)) > 0) {
            cos.write(buffer, 0, read);
            cos.flush();
        }

        cos.close();
        fos.close();
        fis.close();
    }

    /**
     * Decrypts the inputFile to outputFile location
     *
     * @param key        SecretKey to be used
     * @param inputFile  encrypted file path
     * @param outputFile file that will be written to
     * @throws Exception
     */
    public void decryptFile(SecretKey key, String inputFile, String outputFile)
            throws Exception {

        FileInputStream fis = new FileInputStream(inputFile);
        // read ivHex first

        byte[] ivHexBytes = new byte[IV_LENGTH * 2];
        fis.read(ivHexBytes, 0, ivHexBytes.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        String ivHex = new String(ivHexBytes);
        IvParameterSpec ivspec = new IvParameterSpec(toByte(ivHex));
        cipher.init(Cipher.DECRYPT_MODE, secret, ivspec);

        File file = new File(outputFile);

        FileOutputStream fos = new FileOutputStream(file);

        CipherInputStream cis = new CipherInputStream(fis, cipher);

        int read = -1;
        byte[] buffer = new byte[2048];
        while ((read = cis.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
            fos.flush();
        }

        cis.close();
        fis.close();
        fos.close();
    }

    /**
     * Decrypts the file, skipping initial skipBytes.
     *
     * @param key        SecretKey to be used
     * @param skipBytes  number of bytes to skip
     * @param inputFile  encrypted file path
     * @param outputFile file that will be written to
     * @throws Exception
     */
    public void decryptFile(SecretKey key, int skipBytes, String inputFile, String outputFile)
            throws Exception {

        FileInputStream fis = new FileInputStream(inputFile);
        // read ivHex first
        fis.skip(skipBytes);

        byte[] ivHexBytes = new byte[IV_LENGTH * 2];
        fis.read(ivHexBytes, 0, ivHexBytes.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        String ivHex = new String(ivHexBytes);
        IvParameterSpec ivspec = new IvParameterSpec(toByte(ivHex));
        cipher.init(Cipher.DECRYPT_MODE, secret, ivspec);

        File file = new File(outputFile);

        FileOutputStream fos = new FileOutputStream(file);

        CipherInputStream cis = new CipherInputStream(fis, cipher);

        int read = -1;
        byte[] buffer = new byte[2048];
        while ((read = cis.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
            fos.flush();
        }

        cis.close();
        fis.close();
        fos.close();
    }

    /**
     * get {@link SecretKey} from provided password
     *
     * @param password
     * @return
     * @throws Exception
     */
    public SecretKey getSecretKey(String password) throws Exception {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(),
                toByte(SALT), PBE_ITERATION_COUNT, KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(
                PBE_ALGORITHM);
        SecretKey tmp = factory.generateSecret(pbeKeySpec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(),
                SECRET_KEY_ALGORITHM);
        return secret;
    }


    /**
     * get hash of the provided string
     * the hash used is SHA512
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static String getHash(String data) throws Exception {
        String input = data;
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return toHex(out);

    }

    /**
     * generate salt for encryption
     *
     * @return
     * @throws Exception
     */
    public String generateSalt() throws Exception {
        final int SALT_LENGTH = 20;
        final String RANDOM_ALGORITHM = "SHA1PRNG";
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return toHex(salt);
    }

    /**
     * generate random IV for encryption
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    public byte[] generateIv() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * convert String into HEX string
     *
     * @param txt
     * @return
     */
    public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }

    /**
     * convert HEX string into string
     *
     * @param hex
     * @return
     */
    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    /**
     * convert HEX string into bytes
     *
     * @param hexString
     * @return
     */
    public static byte[] toByte(String hexString) {
        int len = hexString.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2),
                    16).byteValue();
        return result;
    }

    /**
     * convert provided bytes into HEX string
     *
     * @param buf
     * @return
     */
    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2 * buf.length);
        for (byte b : buf) {
            appendHex(result, b);
        }
        return result.toString();
    }

    private final static String HEX = "0123456789ABCDEF";

    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
    }
}