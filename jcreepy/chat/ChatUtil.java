/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.chat.ChatColor
 */
package jcreepy.chat;

import java.util.EnumMap;
import java.util.Map;
import jcreepy.chat.ChatColor;
import org.fusesource.jansi.Ansi;

public final class ChatUtil {
    private static final Map<ChatColor, String> replacements = new EnumMap<ChatColor, String>(ChatColor.class);

    private ChatUtil() {
    }

    public static String messageToAnsi(String message) {
        String result = message;
        for (ChatColor color : ChatColor.values()) {
            result = replacements.containsKey((Object)color) ? result.replaceAll("(?i)" + color.toString(), replacements.get((Object)color)) : result.replaceAll("(?i)" + color.toString(), "");
        }
        return result;
    }

    static {
        replacements.put(ChatColor.BLACK, Ansi.ansi().fg(Ansi.Color.BLACK).boldOff().toString());
        replacements.put(ChatColor.DARK_BLUE, Ansi.ansi().fg(Ansi.Color.BLUE).boldOff().toString());
        replacements.put(ChatColor.DARK_GREEN, Ansi.ansi().fg(Ansi.Color.GREEN).boldOff().toString());
        replacements.put(ChatColor.DARK_AQUA, Ansi.ansi().fg(Ansi.Color.CYAN).boldOff().toString());
        replacements.put(ChatColor.DARK_RED, Ansi.ansi().fg(Ansi.Color.RED).boldOff().toString());
        replacements.put(ChatColor.DARK_PURPLE, Ansi.ansi().fg(Ansi.Color.MAGENTA).boldOff().toString());
        replacements.put(ChatColor.GOLD, Ansi.ansi().fg(Ansi.Color.YELLOW).boldOff().toString());
        replacements.put(ChatColor.GRAY, Ansi.ansi().fg(Ansi.Color.WHITE).boldOff().toString());
        replacements.put(ChatColor.DARK_GRAY, Ansi.ansi().fg(Ansi.Color.BLACK).bold().toString());
        replacements.put(ChatColor.BLUE, Ansi.ansi().fg(Ansi.Color.BLUE).bold().toString());
        replacements.put(ChatColor.GREEN, Ansi.ansi().fg(Ansi.Color.GREEN).bold().toString());
        replacements.put(ChatColor.AQUA, Ansi.ansi().fg(Ansi.Color.CYAN).bold().toString());
        replacements.put(ChatColor.RED, Ansi.ansi().fg(Ansi.Color.RED).bold().toString());
        replacements.put(ChatColor.LIGHT_PURPLE, Ansi.ansi().fg(Ansi.Color.MAGENTA).bold().toString());
        replacements.put(ChatColor.YELLOW, Ansi.ansi().fg(Ansi.Color.YELLOW).bold().toString());
        replacements.put(ChatColor.WHITE, Ansi.ansi().fg(Ansi.Color.WHITE).bold().toString());
        replacements.put(ChatColor.MAGIC, Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString());
        replacements.put(ChatColor.BOLD, Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString());
        replacements.put(ChatColor.STRIKETHROUGH, Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString());
        replacements.put(ChatColor.UNDERLINE, Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString());
        replacements.put(ChatColor.ITALIC, Ansi.ansi().a(Ansi.Attribute.ITALIC).toString());
        replacements.put(ChatColor.RESET, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.DEFAULT).toString());
    }
}

