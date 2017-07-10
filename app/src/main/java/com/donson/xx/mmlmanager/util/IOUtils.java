package com.donson.xx.mmlmanager.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by Administrator on 2017/5/11.
 */
public class IOUtils {
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public static void closeAllQuietly(Closeable... closeables) {
        try {
            if (closeables != null) {
                for (Closeable closeable:closeables){
                    if (closeable != null) {
                        closeable.close();
                    }
                }
            }
        } catch (IOException ioe) {
            // ignore
        }
    }
}
