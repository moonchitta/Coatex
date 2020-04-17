package com.ivor.coatex.tor;


import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.ivor.coatex.crypto.CryptoUtils;
import com.ivor.coatex.db.Database;
import com.ivor.coatex.db.FileShare;
import com.ivor.coatex.db.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import fi.iki.elonen.NanoHTTPD;
import io.realm.Realm;
import io.realm.Sort;

public class FileServer extends NanoHTTPD {

    public static final int FILE_NOT_FOUND = 4;
    public static final int FILE_NOT_SERVABLE = 5;

    private static final String TAG = "FileServer";

    private Context mContext;

    private static FileServer mInstance;

    public static FileServer getInstance(Context context, int port, boolean forceNew) {
        if (mInstance == null) {
            mInstance = new FileServer(context, port);
        }
        if (forceNew) {
            if (mInstance != null) {
                mInstance.stop();
                mInstance = null;
                mInstance = new FileServer(context, port);
            }
        }
        return mInstance;
    }

    private FileServer(Context context, int port) {
        super(port);
        mContext = context;
        try {
            makeServerSecure();
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() throws IOException {
        if (!isAlive()) {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }
    }

    private void makeServerSecure() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        File keyStore = new File(mContext.getFilesDir(), "keystore");
        Tor tor = Tor.getInstance(mContext);
        if (!keyStore.exists()) {
            Log.d(TAG, "testFileServer: Creating keystore");
            CryptoUtils.createKeyStore(keyStore.getAbsolutePath(), tor.getID(), new KeyPair(tor.getPublicKey(), tor.getPrivateKey()));
        }

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream keystoreStream = mContext.getAssets().open("certificates/coatex.bks");

        keystore.load(keystoreStream, "coatex".toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, "coatex".toCharArray());
        makeSecure(NanoHTTPD.makeSSLSocketFactory(keystore, keyManagerFactory), null);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "serve: Got file request: " + session.getUri());

        Response response;

//        if(mActivity.isServeFile()) {

        File file;
        if (session.getUri().equals("/dp")) {
            file = new File(Database.getInstance(mContext).get("dp"));
            if (!file.exists()) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/*", "Forbidden");
            }
        } else {
            Map<String, String> headers = session.getHeaders();
            Realm realm = Realm.getDefaultInstance();
            String fn = session.getUri().substring(1);
            String password = headers.get("password");
            Log.d(TAG, "serve: Trying to find file: " + fn);
            FileShare fileShare = realm.where(FileShare.class)
                    .beginGroup()
                    .equalTo("filename", fn)
                    .and()
                    .equalTo("password", password)
                    .endGroup()
                    .sort("_id", Sort.DESCENDING).findFirst();

            if (fileShare != null && !fileShare.isServed()) {
                file = new File(fileShare.getFilePath());
                if (file.exists()) {
                    Log.d(TAG, "serve: File found serving: " + fileShare.getFilePath());
                } else {
                    Log.d(TAG, "serve: File not found: " + fileShare.getFilePath());
                    return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/*", FILE_NOT_FOUND + "");
                }
            } else {
                Log.d(TAG, "serve: File not found: " + fn);
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/*", "" + FILE_NOT_SERVABLE);
            }
            realm.close();
        }
        String mimeType = getMimeType(file.getAbsolutePath());
        response = serveFile(session.getUri(), session.getHeaders(), file, mimeType);
        return response;
    }

    //Announce that the file server accepts partial content requests
    private Response createResponse(Response.Status status, String mimeType,
                                    InputStream message) {
        Response res = newFixedLengthResponse(status, mimeType, message, 50);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    private Response serveFile(String uri, Map<String, String> header,
                               File file, String mime) {
        Response res;
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath()
                    + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range
                                    .substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                            NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime,
                            fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-"
                            + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Response.Status.OK, mime,
                            new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = newFixedLengthResponse(Response.Status.FORBIDDEN,
                    NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }

    public static String getMimeType(String url) {
        Log.d(TAG, "get mime type of " + url);
        if (url.lastIndexOf(".") < 0) {
            return null;
        }
        String extension = url.substring(url.lastIndexOf("."));
        String mimeTypeMap = MimeTypeMap.getFileExtensionFromUrl(extension);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimeTypeMap);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        Log.d(TAG, "getMimeType: " + url + " type is " + mimeType);
        return mimeType;
    }

    public static int getMessageType(String mime) {
        if (mime == null) return Message.TYPE_FILE;

        if (mime.startsWith("image")) {
            Log.d(TAG, "getMessageType: image");
            return Message.TYPE_IMAGE;
        } else if (mime.startsWith("video")) {
            Log.d(TAG, "getMessageType: video");
            return Message.TYPE_VIDEO;
        } else if (mime.startsWith("audio")) {
            Log.d(TAG, "getMessageType: audio");
            return Message.TYPE_AUDIO;
        }

        Log.d(TAG, "getMessageType: file");
        return Message.TYPE_FILE;
    }
}
