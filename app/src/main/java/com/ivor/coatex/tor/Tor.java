/*
 * Chat.onion - P2P Instant Messenger
 *
 * http://play.google.com/store/apps/details?id=onion.chat
 * http://onionapps.github.io/Chat.onion/
 * http://github.com/onionApps/Chat.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package com.ivor.coatex.tor;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.ivor.coatex.R;
import com.ivor.coatex.crypto.AdvancedCrypto;
import com.ivor.coatex.utils.Util;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.spongycastle.asn1.ASN1OutputStream;
import org.spongycastle.asn1.x509.RSAPublicKeyStructure;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Tor {

    private static String torname = "ctor";
    private static String tordirname = "tordata";
    private static String torservdir = "torserv";
    private static String torCfg = "torcfg";
    public static int HIDDEN_SERVICE_VERSION = 2;
    private static Tor instance = null;
    private Context context;
    private static int mSocksPort = 9151;
    private static int mHttpPort = 8191;
    private String domain = "";
    private ArrayList<Listener> mListeners;
    private ArrayList<LogListener> mLogListeners;
    private String status = "";
    private boolean ready = false;

    private File mTorDir;

    private Process mProcessTor;

    public Tor(Context c) {

        this.context = c;

        mListeners = new ArrayList<>();
        mLogListeners = new ArrayList<>();

        mTorDir = new File(c.getFilesDir(), "tor");
        if (!mTorDir.exists()) {
            mTorDir.mkdir();
        }

        final Server server = Server.getInstance(context);

        domain = Util.filestr(new File(getServiceDir(), "hostname")).trim();
        log(domain);

        new Thread() {
            @Override
            public void run() {
                try {
                    //test();

                    log("kill");
                    Native.killTor();

                    log("install");
                    extractFile(context, R.raw.tor, torname);

                    //log("delete on exit");
                    //context.getFileStreamPath(torname).deleteOnExit();

                    log("set executable");
                    context.getFileStreamPath(torname).setExecutable(true);

                    log("make dir");
                    File tordir = new File(mTorDir, tordirname);
                    tordir.mkdirs();

                    log("make service");
                    File torsrv = new File(mTorDir, torservdir);
                    torsrv.mkdirs();

                    log("configure");
                    PrintWriter torcfg = new PrintWriter(context.openFileOutput(torCfg, Context.MODE_PRIVATE));
                    //torcfg.println("Log debug stdout");
//                    torcfg.println("Log notice stdout");
                    torcfg.println("DataDirectory " + tordir.getAbsolutePath());
                    torcfg.println("SOCKSPort " + mSocksPort);
                    torcfg.println("HTTPTunnelPort " + mHttpPort);
                    torcfg.println("HiddenServiceDir " + torsrv.getAbsolutePath());
                    torcfg.println("HiddenServiceVersion " + HIDDEN_SERVICE_VERSION);
                    torcfg.println("HiddenServicePort " + getHiddenServicePort() + " " + server.getSocketName());
                    torcfg.println("HiddenServicePort " + getFileServerPort() + " 127.0.0.1:" + getFileServerPort());
                    torcfg.println();
                    torcfg.close();
                    log(Util.filestr(new File(context.getFilesDir(), torCfg)));

                    log("start: " + new File(torname).getAbsolutePath());

                    String[] command = new String[]{
                            context.getFileStreamPath(torname).getAbsolutePath(),
                            "-f", context.getFileStreamPath(torCfg).getAbsolutePath()
                    };

                    StringBuilder sb = new StringBuilder();
                    for (String s : command) {
                        sb.append(s);
                        sb.append(" ");
                    }

                    log("Command: " + sb.toString());

                    mProcessTor = Runtime.getRuntime().exec(command);
                    BufferedReader torReader = new BufferedReader(new InputStreamReader(mProcessTor.getInputStream()));
                    while (true) {
                        final String line = torReader.readLine();
                        if (line == null) break;
                        log(line);
                        status = line;

                        boolean ready2 = ready;

                        if (domain == null || domain.length() == 0) {
                            domain = Util.filestr(new File(torsrv, "hostname")).trim();
                        }

                        if (line.contains("100%")) {
                            ls(mTorDir);
                            domain = Util.filestr(new File(torsrv, "hostname")).trim();
                            log(domain);
                            try {
                                for (Listener l : mListeners) {
                                    if (l != null) l.onChange();
                                }
                            } catch (Exception e) {
                            }
                            //ready = true;
                            //test();
                            ready2 = true;
                        }

                        if (!ready) {
                            ready = ready2;
                        }
                        ready = ready2;

                        try {
                            for (LogListener ll : mLogListeners) {
                                if (ll != null) {
                                    ll.onLog();
                                }
                            }
                        } catch (Exception e) {

                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //throw new Error(ex);
                }
            }
        }.start();
    }

    public static Tor getInstance(Context context) {
        if (instance == null) {
            instance = new Tor(context.getApplicationContext());
        }
        return instance;
    }

    static String computeID(RSAPublicKeySpec pubkey) {
        RSAPublicKeyStructure myKey = new RSAPublicKeyStructure(pubkey.getModulus(), pubkey.getPublicExponent());
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ASN1OutputStream as = new ASN1OutputStream(bs);
        try {
            as.writeObject(myKey.toASN1Object());
        } catch (IOException ex) {
            // TODO: error handling? ignore error?
            throw new Error(ex);
        }
        byte[] b = bs.toByteArray();
        b = DigestUtils.getSha1Digest().digest(b);
        return new Base32().encodeAsString(b).toLowerCase().substring(0, 16);
    }

    public static int getHiddenServicePort() {
        return 31512;
    }

    public static int getFileServerPort() {
        return 8088;
    }

    private void log(String s) {
        Log.d("Tor", "Data: " + s);
    }

    void ls(File f) {
        log(f.toString());
        if (f.isDirectory()) {
            for (File s : f.listFiles()) {
                ls(s);
            }
        }
    }

    public static int getSocksPort() {
        return mSocksPort;
    }

    public static int getHttpPort() {
        return mHttpPort;
    }

    public String getOnion() {
        return domain.trim();
    }

    public String getID() {
        return domain.replace(".onion", "").trim();
    }

    public void addListener(Listener l) {
        if (l != null && !mListeners.contains(l)) {
            mListeners.add(l);
            l.onChange();
        }
    }

    public void removeListener(Listener l) {
        mListeners.remove(l);
    }

    private void extractFile(Context context, int id, String name) {
        try {
            InputStream i = context.getResources().openRawResource(id);
            OutputStream o = context.openFileOutput(name, Context.MODE_PRIVATE);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = i.read(buffer)) > 0) {
                o.write(buffer, 0, read);
            }
            i.close();
            o.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new Error(ex);
        }
    }

    public File getServiceDir() {
        return new File(mTorDir, torservdir);
    }

    private KeyFactory getKeyFactory() {
//        if (Security.getProvider("BC") == null) {
        Security.addProvider(new BouncyCastleProvider());
//        }
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                return KeyFactory.getInstance("RSA", "BC");
            } else {
                return KeyFactory.getInstance("RSA");
            }
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public String readPrivateKeyFile() {
//        return Utils.filestr(new File(getServiceDir(), "private_key"));
        return Util.filestr(new File(getServiceDir(), HIDDEN_SERVICE_VERSION == 3 ? "hs_ed25519_secret_key" : "private_key"));
    }

    public RSAPrivateKey getPrivateKey() {
        String priv = readPrivateKeyFile();
//        log(priv);
        priv = priv.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
        priv = priv.replace("-----END RSA PRIVATE KEY-----", "");
        priv = priv.replaceAll("\\s", "");
//        log(priv);
        byte[] data = Base64.decode(priv, Base64.DEFAULT);
//        log("" + data.length);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
//        log(keySpec.toString());
        try {
            return (RSAPrivateKey) getKeyFactory().generatePrivate(keySpec);
        } catch (InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    private RSAPrivateKeySpec getPrivateKeySpec() {
        try {
            return getKeyFactory().getKeySpec(getPrivateKey(), RSAPrivateKeySpec.class);
        } catch (InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    private RSAPublicKeySpec getPublicKeySpec() {
        return new RSAPublicKeySpec(getPrivateKeySpec().getModulus(), BigInteger.valueOf(65537));
    }

    public RSAPublicKey getPublicKey() {
        try {
            return (RSAPublicKey) getKeyFactory().generatePublic(getPublicKeySpec());
        } catch (InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    private String computeOnion() {
        return computeID(getPublicKeySpec()) + ".onion";
    }

    public byte[] getPubKeySpec() {
        return getPrivateKeySpec().getModulus().toByteArray();
    }

    public byte[] sign(byte[] msg) {
        try {
            Signature signature;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                signature = Signature.getInstance("SHA1withRSA", "BC");
            } else {
                signature = Signature.getInstance("SHA1withRSA");
            }
            signature.initSign(getPrivateKey());
            signature.update(msg);
            return signature.sign();
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public String encryptByPublicKey(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchProviderException {
        Cipher encrypt;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        } else {
            encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
        encrypt.init(Cipher.ENCRYPT_MODE, getPublicKey());
        return AdvancedCrypto.toHex(encrypt.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public String encryptByPublicKey(String data, byte[] pubKeySpecBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException, NoSuchProviderException {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(pubKeySpecBytes), BigInteger.valueOf(65537));
        PublicKey publicKey = getKeyFactory().generatePublic(publicKeySpec);

        Cipher encrypt;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        } else {
            encrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
        encrypt.init(Cipher.ENCRYPT_MODE, publicKey);
        return AdvancedCrypto.toHex(encrypt.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    public String decryptByPrivateKey(String data) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchProviderException {
        Cipher decrypt;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        } else {
            decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
        decrypt.init(Cipher.DECRYPT_MODE, getPrivateKey());
        return new String(decrypt.doFinal(AdvancedCrypto.toByte(data)), StandardCharsets.UTF_8);
    }

    public PublicKey convertKeySpec(byte[] pubkey) {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(pubkey), BigInteger.valueOf(65537));
        PublicKey publicKey;
        try {
            publicKey = getKeyFactory().generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace();
            return null;
        }
        return publicKey;
    }

    boolean checkSig(String id, byte[] pubkey, byte[] sig, byte[] msg) {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(pubkey), BigInteger.valueOf(65537));

        if (!id.equals(computeID(publicKeySpec))) {
            log("invalid id");
            return false;
        }

        PublicKey publicKey;
        try {
            publicKey = getKeyFactory().generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            Signature signature;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                signature = Signature.getInstance("SHA1withRSA", "BC");
            } else {
                signature = Signature.getInstance("SHA1withRSA");
            }
            signature.initVerify(publicKey);
            signature.update(msg);
            return signature.verify(sig);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    void test() {
        try {
            String domain = Util.filestr(new File(getServiceDir(), "hostname")).trim();

            log(Util.filestr(new File(getServiceDir(), "hostname")).trim());
            log(computeID(getPublicKeySpec()));
            log(computeOnion());
            log(Util.filestr(new File(getServiceDir(), "hostname")).trim());

            log(Base64.encodeToString(getPubKeySpec(), Base64.DEFAULT));
            log("pub " + Base64.encodeToString(getPubKeySpec(), Base64.DEFAULT));

            byte[] msg = "alkjdalwkdjaw".getBytes();
            log("msg " + Base64.encodeToString(msg, Base64.DEFAULT));

            byte[] sig = sign(msg);
            log("sig " + Base64.encodeToString(sig, Base64.DEFAULT));

            log("chk " + checkSig(getID(), getPubKeySpec(), sig, msg));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addLogListener(LogListener l) {
        if (!mLogListeners.contains(l)) {
            mLogListeners.add(l);
        }
    }

    public String getStatus() {
        return status;
    }

    public boolean isReady() {
        return ready;
    }

    public void removeLogListener(LogListener ll) {
        mLogListeners.remove(ll);
    }

    public interface Listener {
        void onChange();
    }

    public interface LogListener {
        void onLog();
    }
}
