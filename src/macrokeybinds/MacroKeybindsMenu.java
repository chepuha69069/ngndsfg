package macrokeybinds;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_310;

/** Связка GUI с фермой: открытие экрана, чтение и запись настроек. */
public final class MacroKeybindsMenu {

    private MacroKeybindsMenu() {
    }

    private static boolean pendingOpen;

    /**
     * Меню нельзя открывать сразу: чат после отправки сообщения сам вызывает setScreen(null)
     * и закрыл бы наше окно. Поэтому только ставим флаг, а открываем на следующем тике.
     */
    public static void open() {
        pendingOpen = true;
    }

    /** Вызывается каждый клиентский тик. */
    public static void tick() {
        if (!pendingOpen) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null) {
            pendingOpen = false;
            return;
        }
        if (client.field_1755 != null && !(client.field_1755 instanceof MacroKeybindsScreen)
            && client.field_1755.getClass().getName().contains("class_408")) {
            return; // чат ещё не закрылся - ждём следующий тик
        }
        pendingOpen = false;
        if (!(client.field_1755 instanceof MacroKeybindsScreen)) {
            client.method_1507(new MacroKeybindsScreen());
        }
    }

    public static MacroKeybinds module() { return MacroKeybinds.INSTANCE; }
    public static boolean isEnabled() { return MacroKeybinds.INSTANCE.isEnabled(); }
    public static void toggle() { MacroKeybinds.INSTANCE.toggle(); }
    public static List<Setting> settings() { return new ArrayList<>(MacroKeybinds.INSTANCE.settings()); }
    public static boolean isVisible(Setting s) { return s.isVisible(); }

    public static boolean flag(Setting s) { return s instanceof Setting.Flag f && f.get(); }
    public static void flag(Setting s, boolean v) { if (s instanceof Setting.Flag f) f.set(v); }

    public static float number(Setting s) { return s instanceof Setting.Num n ? n.getFloat() : 0.0f; }
    public static void number(Setting s, float v) { if (s instanceof Setting.Num n) n.set(v); }
    public static boolean integral(Setting s) { return s instanceof Setting.Num n && n.integral; }
    public static float minimum(Setting s) { return s instanceof Setting.Num n ? n.min : 0.0f; }
    public static float maximum(Setting s) { return s instanceof Setting.Num n ? n.max : 0.0f; }

    public static String mode(Setting s) { return s instanceof Setting.Mode m ? m.get() : ""; }
    public static List<String> modes(Setting s) { return s instanceof Setting.Mode m ? m.modes() : new ArrayList<>(); }
    public static void mode(Setting s, String v) { if (s instanceof Setting.Mode m) m.set(v); }
}
