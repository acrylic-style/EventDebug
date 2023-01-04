package xyz.acrylicstyle.eventdebug;

import com.google.common.io.ByteStreams;
import net.blueberrymc.nativeutil.ClassDefinition;
import net.blueberrymc.nativeutil.NativeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Pattern;

public class EventDebug extends JavaPlugin {
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onLoad() {
        try {
            boolean loaded = false;
            for (Class<?> clazz : NativeUtil.getLoadedClasses()) {
                if (clazz.getTypeName().equals("xyz.acrylicstyle.eventdebug.EventDebugHandler")) {
                    loaded = true;
                    break;
                }
            }
                URL eventDebugHandlerUrl = new URL("jar:" + EventDebug.class.getProtectionDomain().getCodeSource().getLocation().toString() +
                        "!/" + EventDebug.class.getName().replace('.', '/').replace("EventDebug", "EventDebugHandler") + ".class");
                getLogger().info("Attempting to load " + eventDebugHandlerUrl);
                try (InputStream in = eventDebugHandlerUrl.openStream();
                     BufferedInputStream bin = new BufferedInputStream(in)) {
                    byte[] bytes = ByteStreams.toByteArray(bin);
                    if (loaded) {
                        NativeUtil.redefineClasses(new ClassDefinition[]{
                                new ClassDefinition(EventDebugHandler.class, bytes)
                        });
                    } else {
                        NativeUtil.defineClass("xyz/acrylicstyle/eventdebug/EventDebugHandler", Bukkit.class.getClassLoader(), bytes, bytes.length);
                    }
                }
            URL simplePluginLoaderUrl = new URL("jar:" + SimplePluginManager.class.getProtectionDomain().getCodeSource().getLocation().toString() +
                    "!/" + SimplePluginManager.class.getName().replace('.', '/') + ".class");
            getLogger().info("Attempting to load " + simplePluginLoaderUrl);
            try (InputStream in = simplePluginLoaderUrl.openStream();
                 BufferedInputStream bin = new BufferedInputStream(in)) {
                byte[] bytes = rewriteClass(ByteStreams.toByteArray(bin));
                ClassDefinition def = new ClassDefinition(SimplePluginManager.class, bytes);
                NativeUtil.redefineClasses(new ClassDefinition[]{def});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        Objects.requireNonNull(Bukkit.getPluginCommand("eventdebug")).setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                sender.sendMessage("Usage: /eventdebug add <pattern>");
                sender.sendMessage("Usage: /eventdebug addex <pattern>");
                sender.sendMessage("Usage: /eventdebug addcancel <pattern>");
                sender.sendMessage("Usage: /eventdebug clear");
                return true;
            }
            if (args[0].equals("add")) {
                if (args.length == 1) {
                    sender.sendMessage("Usage: /eventdebug add <pattern>");
                    return true;
                }
                Pattern pattern = Pattern.compile(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                EventDebugHandler.addPattern(pattern);
            }
            if (args[0].equals("addex")) {
                if (args.length == 1) {
                    sender.sendMessage("Usage: /eventdebug addex <pattern>");
                    return true;
                }
                Pattern pattern = Pattern.compile(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                EventDebugHandler.addException(pattern);
            }
            if (args[0].equals("addcancel")) {
                if (args.length == 1) {
                    sender.sendMessage("Usage: /eventdebug addcancel <pattern>");
                    return true;
                }
                Pattern pattern = Pattern.compile(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                EventDebugHandler.addCancel(pattern);
            }
            if (args[0].equals("clear")) {
                EventDebugHandler.clearPatterns();
            }
            return true;
        });
        Objects.requireNonNull(Bukkit.getPluginCommand("eventdebug"))
                .setTabCompleter((sender, command, alias, args) -> Collections.emptyList());
    }

    private static byte[] rewriteClass(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(0);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (access == Opcodes.ACC_PUBLIC && "callEvent".equals(name) && "(Lorg/bukkit/event/Event;)V".equals(descriptor)) {
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "xyz/acrylicstyle/eventdebug/EventDebugHandler", "handle", "(Lorg/bukkit/event/Event;)V", false);
                }
                return mv;
            }
        }, 0);
        return cw.toByteArray();
    }
}
