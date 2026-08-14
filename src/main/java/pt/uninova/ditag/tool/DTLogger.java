package pt.uninova.ditag.tool;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DTLogger {

    public static final Logger logger = Logger.getLogger("DITAG");
    private static final ConsoleHandler handler = new ConsoleHandler() {
        {
            setOutputStream(System.out);
        }
    };

    static {
        Logger rootLogger = Logger.getLogger("");
        for (Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }

        handler.setLevel(Level.ALL);
        handler.setFormatter(new Formatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                String time = sdf.format(new Date(record.getMillis()));
                String level = record.getLevel().getName();
                String clazz = record.getSourceClassName();
                String method = record.getSourceMethodName();
                String message = formatMessage(record);
                return String.format("[%s][%s][%s.%s]\n%s%n",
                        time, level, clazz, method, message);
            }
        });

        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    private DTLogger() {}

    public static void setLevel(Level level) {
        logger.setLevel(level);
        handler.setLevel(level);
    }
}