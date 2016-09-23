
package jline.internal;

import java.util.ArrayList;
import java.util.List;
import jline.internal.Configuration;
import jline.internal.Log;
import jline.internal.Preconditions;


public class ShutdownHooks {
    public static final String JLINE_SHUTDOWNHOOK = "jline.shutdownhook";
    private static final boolean enabled = Configuration.getBoolean("jline.shutdownhook", true);
    private static final List<Task> tasks = new ArrayList<Task>();
    private static Thread hook;

    public static synchronized <T extends Task> T add(T task) {
        Preconditions.checkNotNull(task);
        if (!enabled) {
            Log.debug("Shutdown-hook is disabled; not installing: ", task);
            return task;
        }
        if (hook == null) {
            hook = ShutdownHooks.addHook(new Thread("JLine Shutdown Hook"){

                public void run() {
                    ShutdownHooks.runTasks();
                }
            });
        }
        Log.debug("Adding shutdown-hook task: ", task);
        tasks.add((Task)task);
        return task;
    }

    private static synchronized void runTasks() {
        Log.debug("Running all shutdown-hook tasks");
        for (Task task : tasks.toArray(new Task[tasks.size()])) {
            Log.debug("Running task: ", task);
            try {
                task.run();
                continue;
            }
            catch (Throwable e) {
                Log.warn("Task failed", e);
            }
        }
        tasks.clear();
    }

    private static Thread addHook(Thread thread) {
        Log.debug("Registering shutdown-hook: ", thread);
        try {
            Runtime.getRuntime().addShutdownHook(thread);
        }
        catch (AbstractMethodError e) {
            Log.debug("Failed to register shutdown-hook", e);
        }
        return thread;
    }

    public static synchronized void remove(Task task) {
        Preconditions.checkNotNull(task);
        if (!enabled || hook == null) {
            return;
        }
        tasks.remove(task);
        if (tasks.isEmpty()) {
            ShutdownHooks.removeHook(hook);
            hook = null;
        }
    }

    private static void removeHook(Thread thread) {
        Log.debug("Removing shutdown-hook: ", thread);
        try {
            Runtime.getRuntime().removeShutdownHook(thread);
        }
        catch (AbstractMethodError e) {
            Log.debug("Failed to remove shutdown-hook", e);
        }
        catch (IllegalStateException e) {
            // empty catch block
        }
    }

    public static interface Task {
        public void run() throws Exception;
    }

}

