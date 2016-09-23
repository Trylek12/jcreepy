/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.EventManager
 *  jcreepy.event.HandlerList
 *  jcreepy.event.Listener
 *  jcreepy.event.Order
 *  jcreepy.event.events.ChatMessageEvent
 *  jcreepy.event.events.DisconnectEvent
 */
package foxcraft;

import foxcraft.command.ConnectCommand;
import foxcraft.command.DisconnectCommand;
import foxcraft.command.ExitCommand;
import foxcraft.command.HelloCommand;
import foxcraft.command.SendChatCommand;
import foxcraft.listener.ChatMessageListener;
import foxcraft.listener.DisconnectListener;
import foxcraft.network.ConnectionThread;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jcreepy.command.Command;
import jcreepy.command.CommandManager;
import jcreepy.command.CommandSender;
import jcreepy.console.CommandReaderThread;
import jcreepy.console.ConsoleCommandSender;
import jcreepy.console.TerminalHandler;
import jcreepy.event.EventManager;
import jcreepy.event.HandlerList;
import jcreepy.event.Listener;
import jcreepy.event.Order;
import jcreepy.event.events.ChatMessageEvent;
import jcreepy.event.events.DisconnectEvent;
import jcreepy.logging.CreepyLogger;
import jcreepy.network.Protocol;
import jcreepy.network.Session;
import jcreepy.protocol.MinecraftProtocol;
import jline.console.ConsoleReader;
import org.fusesource.jansi.AnsiConsole;

public class CreepyClient
implements TerminalHandler {
    private ConsoleReader cr;
    private CreepyLogger logger;
    private boolean isConnected;
    private Session session;
    private final EventManager ev = new EventManager();
    private final ConsoleCommandSender commandSender = new ConsoleCommandSender();
    private final CommandManager commandManager = new CommandManager();
    public ConnectionThread connectionThread;
    private ExecutorService ex = Executors.newSingleThreadExecutor();
    public boolean isRunning = true;
    public CommandReaderThread reader;
    private static CreepyClient instance;

    public CreepyClient() {
        try {
            this.setup();
            ChatMessageEvent.handlers.register((Listener)new ChatMessageListener(), Order.Internal);
            DisconnectEvent.handlers.register((Listener)new DisconnectListener(), Order.Internal);
            this.commandManager.registerCommand("hello", new HelloCommand());
            this.commandManager.registerCommand("connect", new ConnectCommand(this));
            this.commandManager.registerCommand("exit", new ExitCommand());
            this.commandManager.registerCommand("dis", new DisconnectCommand());
            this.commandManager.registerCommand("c", new SendChatCommand());
            instance = this;
            this.spawnCommandReader();
            Thread.sleep(10000000000L);
        }
        catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ConsoleReader getConsoleReader() {
        return this.cr;
    }

    @Override
    public void handleConsoleCommand(final String cmd) {
        this.ex.submit(new Runnable(){

            @Override
            public void run() {
                try {
                    String[] a = cmd.split(" ");
                    Command c = CreepyClient.this.commandManager.getCommand(a[0]);
                    if (c != null) {
                        c.execute(CreepyClient.this.commandSender, Arrays.copyOfRange(a, 1, a.length));
                    } else {
                        CreepyClient.this.commandSender.sendMessage("Unknown command.");
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean isUsingJLine() {
        return true;
    }

    private void spawnCommandReader() {
        this.reader = new CommandReaderThread(this);
        this.reader.start();
    }

    private void spawnConnThread(String host, int port, String username) {
        this.session.setPlayerName(username);
        this.connectionThread = new ConnectionThread(this.session);
        this.connectionThread.start();
    }

    public void closeConnection() {
        if (this.connectionThread != null) {
            this.connectionThread.shutdown();
        }
    }

    private void setup() throws IOException {
        AnsiConsole.systemInstall();
        this.cr = new ConsoleReader(System.in, System.out);
        this.cr.setExpandEvents(false);
        this.logger = new CreepyLogger(this);
    }

    public EventManager getEventManager() {
        return this.ev;
    }

    public CommandSender getConsoleCommandSender() {
        return this.commandSender;
    }

    public void connect(String username, String host, int port) {
        if (this.session == null || !this.session.isActive()) {
            this.session = new Session(MinecraftProtocol.INSTANCE, username);
            this.session.setHost(host);
            this.session.setPort(port);
            this.spawnConnThread(host, port, username);
        }
    }

    public static CreepyClient getInstance() {
        return instance;
    }

    public Session getSession() {
        return this.session;
    }

    public static void main(String[] args) {
        new CreepyClient();
    }

    public void exit() {
        this.closeConnection();
        Thread.currentThread().interrupt();
        this.isRunning = false;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

}

