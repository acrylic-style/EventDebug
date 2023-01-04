package xyz.acrylicstyle.eventdebug;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class EventDebugHandler {
    private static final List<Pattern> PATTERNS = new CopyOnWriteArrayList<>();
    private static final List<Pattern> EXCEPTION = new CopyOnWriteArrayList<>();
    private static final List<Pattern> CANCEL = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unused")
    public static void handle(Event event) {
        if (PATTERNS.isEmpty()) return;
        String name = event.getEventName();
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(name).matches()) {
                for (Pattern p : EXCEPTION) {
                    if (p.matcher(name).matches()) return;
                }
                boolean cancelled = false;
                for (Pattern p : CANCEL) {
                    if (p.matcher(name).matches() && event instanceof Cancellable) {
                        ((Cancellable) event).setCancelled(cancelled = true);
                        break;
                    }
                }
                String cancel = cancelled ? "Cancelled " : "";
                System.out.println(cancel + "Event " + event.getEventName() + " : " + getEventDetails(event));
                return;
            }
        }
    }

    private static String getEventDetails(Event event) {
        try {
            return ToStringBuilder.reflectionToString(event);
        } catch (Exception e) {
            return event.toString();
        }
    }

    public static void addPattern(Pattern pattern) {
        PATTERNS.add(pattern);
    }

    public static void addException(Pattern pattern) {
        EXCEPTION.add(pattern);
    }

    public static void addCancel(Pattern pattern) {
        CANCEL.add(pattern);
    }

    public static void clearPatterns() {
        PATTERNS.clear();
        EXCEPTION.clear();
    }
}
