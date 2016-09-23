
package jcreepy.logging;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fusesource.jansi.Ansi;

public class ConsoleLogFormatter
extends Formatter {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder bob = new StringBuilder();
        Level level = record.getLevel();
        bob.append(this.getColor(Ansi.Color.BLUE));
        bob.append(this.dateFormat.format(record.getMillis()));
        bob.append(Ansi.ansi().reset());
        if (level == Level.FINEST || level == Level.FINER || level == Level.FINE || level == Level.INFO) {
            bob.append(this.getColor(Ansi.Color.GREEN));
        } else if (level == Level.WARNING) {
            bob.append(this.getColor(Ansi.Color.YELLOW));
        } else if (level == Level.SEVERE) {
            bob.append(this.getColor(Ansi.Color.RED));
        }
        bob.append(" [" + level.getName().toUpperCase() + "] ");
        bob.append(Ansi.ansi().reset());
        bob.append(this.formatMessage(record));
        bob.append('\n');
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            bob.append(ExceptionUtils.getStackTrace(throwable));
        }
        return bob.toString();
    }

    private String getColor(Ansi.Color c) {
        return Ansi.ansi().fg(c).toString();
    }
}

