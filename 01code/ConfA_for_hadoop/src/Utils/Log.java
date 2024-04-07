package Utils;

import java.io.FileWriter;
import java.io.IOException;

public class Log {
    public static FileWriter log = null;
    public static final String lineSep = System.getProperty("line.separator");

    public static void logln(String s, boolean flag) {
        if(flag) {
            logln(s);
        }
    }
    public static void logln(String s) {
        if (! isLoggingOn()) return;

        try {
            log.write(s);
            log.write(Log.lineSep);
            log.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public static boolean isLoggingOn() {
        return log != null;
    }
}
