/*
 * Copyright(C) 2016 大前良介(OHMAE Ryosuke)
 */

package net.mm2d.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介(OHMAE Ryosuke)</a>
 */
public class Log {
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;
    private static int sLogLevel = VERBOSE;
    private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void setLogLevel(int level) {
        sLogLevel = level;
    }

    public static void v(String tag, String message) {
        println(VERBOSE, tag, message);
    }

    public static void v(String tag, String message, Throwable tr) {
        println(VERBOSE, tag, message, tr);
    }

    public static void d(String tag, String message) {
        println(DEBUG, tag, message);
    }

    public static void d(String tag, String message, Throwable tr) {
        println(DEBUG, tag, message, tr);
    }

    public static void i(String tag, String message) {
        println(INFO, tag, message);
    }

    public static void i(String tag, String message, Throwable tr) {
        println(INFO, tag, message, tr);
    }

    public static void w(String tag, String message) {
        println(WARN, tag, message);
    }

    public static void w(String tag, String message, Throwable tr) {
        println(WARN, tag, message, tr);
    }

    public static void w(String tag, Throwable tr) {
        println(WARN, tag, tr);
    }

    public static void e(String tag, String message) {
        println(ERROR, tag, message);
    }

    public static void e(String tag, String message, Throwable tr) {
        println(ERROR, tag, message, tr);
    }

    private static void println(int level, String tag, Throwable tr) {
        if (level < sLogLevel) {
            return;
        }
        println(level, tag, getStackTraceString(tr));
    }

    private static void println(int level, String tag, String message, Throwable tr) {
        if (level < sLogLevel) {
            return;
        }
        println(level, tag, message + "\n" + getStackTraceString(tr));
    }

    private static void println(int level, String tag, String message) {
        if (level < sLogLevel) {
            return;
        }
        synchronized (FORMAT) {
            final StringBuilder sb = new StringBuilder();
            sb.append(FORMAT.format(new Date(System.currentTimeMillis())));
            switch (level) {
                case VERBOSE:
                    sb.append(" V ");
                    break;
                case DEBUG:
                    sb.append(" D ");
                    break;
                case INFO:
                    sb.append(" I ");
                    break;
                case WARN:
                    sb.append(" W ");
                    break;
                case ERROR:
                    sb.append(" E ");
                    break;
            }
            sb.append("[");
            sb.append(tag);
            sb.append("] ");
            sb.append(message);
            System.out.println(sb.toString());
        }
    }

    private static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

}
