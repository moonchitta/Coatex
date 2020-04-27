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

import android.util.Log;

import com.ivor.coatex.BuildConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Sock {

    static final int TIMEOUT = 60000;
    Socket mSock;
    BufferedReader mReader;
    BufferedWriter mWriter;

    public Sock(String host, int port) {
        InetSocketAddress proxyAddr = new InetSocketAddress("127.0.0.1", Tor.getSocksPort());
        Proxy proxyTor = new Proxy(Proxy.Type.SOCKS, proxyAddr);
        mSock = new Socket(proxyTor);
        try {
            mSock.connect(new InetSocketAddress(host, port), TIMEOUT);
            mReader = new BufferedReader(new InputStreamReader(mSock.getInputStream()));
            mWriter = new BufferedWriter(new OutputStreamWriter(mSock.getOutputStream()));
        } catch (IOException e) {
            log("IO Exception");
            e.printStackTrace();
            try {
                mSock.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void log(String s) {
        if (!BuildConfig.DEBUG) return;
        Log.i("Sock", s);
    }

    public void writeLine(String... ss) {
        String s = "";

        if (ss.length > 0) s = ss[0];
        for (int i = 1; i < ss.length; i++)
            s += " " + ss[i];

        log(" >> " + s);
        if (mWriter != null) {
            try {
                mWriter.write(s + "\r\n");
            } catch (SocketTimeoutException ex) {
                log("timeout");
                try {
                    mSock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
            }
        }
    }

    public String readLine() {
        String s = null;
        if (mReader != null) {
            try {
                s = mReader.readLine();
            } catch (SocketTimeoutException ex) {
                log("timeout");
                try {
                    mSock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
            }
        }
        if (s == null)
            s = "";
        else
            s = s.trim();
        log(" << " + s);
        return s;
    }

    public boolean readBool() {
        return readLine().equals("1");
    }

    public String readString() {
        return readLine();
    }

    public boolean queryBool(String... request) {
        writeLine(request);
        flush();
        return readBool();
    }

    public String queryString(String... request) {
        writeLine(request);
        flush();
        return readString();
    }

    public void queryOrClose(String... request) {
        if (!queryBool(request)) close();
    }

    public boolean queryAndClose(String... request) {
        boolean x = queryBool(request);
        close();
        return x;
    }

    public String queryAndCloseString(String... request) {
        String x = queryString(request);
        close();
        return x;
    }

    public void flush() {
        if (mWriter != null) {
            try {
                mWriter.flush();
            } catch (SocketTimeoutException ex) {
                log("timeout");
                try {
                    mSock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
            }
        }
    }

    public void close() {

        flush();

        if (mSock != null) {
            try {
                mSock.close();
            } catch (Exception ex) {
            }
        }

        mReader = null;
        mWriter = null;
        mSock = null;

    }

    public boolean isClosed() {
        //try {
        return mSock.isClosed();
        /*} catch(IOException ex) {
            return true;
        }*/
    }


}
