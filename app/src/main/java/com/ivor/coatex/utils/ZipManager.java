package com.ivor.coatex.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipManager {

    static final int BUFFER = 2048;
    private static final String TAG = "ZipManager";

    private Context mContext;

    ZipOutputStream out;
    ZipInputStream in;
    byte[] data;

    public ZipManager(Context context) {
        mContext = context;
    }

    public void makeZip(String name) {
        FileOutputStream dest = null;
        try {
            dest = new FileOutputStream(name);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        out = new ZipOutputStream(new BufferedOutputStream(dest));
        data = new byte[BUFFER];
    }

    public void openZip(String name) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(name);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        in = new ZipInputStream(new BufferedInputStream(fis));
        data = new byte[BUFFER];
    }

    public boolean unZip(String pathToExtract, String zipFile) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();
                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(pathToExtract, filename);
                    Log.d(TAG, "unZip: Creating Directory: " + fmd.getAbsolutePath());
                    fmd.mkdirs();
                    continue;
                }

                File extractFile = new File(pathToExtract, filename);
                Log.d(TAG, "unZip: " + extractFile.getAbsolutePath());
                if (!extractFile.getParentFile().exists()) {
                    Log.d(TAG, "unZip: Creating Directory: " + extractFile.getParentFile().getAbsolutePath());
                    extractFile.getParentFile().mkdirs();
                }

                FileOutputStream fout = new FileOutputStream(extractFile);

                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void addZipFile(String filename, String zipFilename) {
        Log.v("addFile", "Adding: ");
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(filename);
            Log.v("addFile", "Adding: ");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v("atch", "Adding: ");
        }
        BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
        ZipEntry entry = new ZipEntry(zipFilename);
        try {
            out.putNextEntry(entry);
            Log.v("put", "Adding: ");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int count;
        try {
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
                //Log.v("Write", "Adding: "+origin.read(data, 0, BUFFER));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v("catch", "Adding: ");
        }
        try {
            origin.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void closeZip() {
        try {
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
