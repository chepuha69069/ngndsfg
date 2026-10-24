package macrokeybinds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_4587;
import net.minecraft.class_5250;

/**
 * Минималистичное стеклянное меню.
 * Фигуры рисуются в физических пикселях со сглаживанием краёв (никаких "ступенек"),
 * текст - шрифтом Inter из ресурсов мода (assets/macrokeybinds/font).
 */
public class MacroKeybindsScreen extends class_437 {

    // ---------- шрифты ----------
    private static final class_2583 FONT_REGULAR =
        class_2583.field_24360.method_27704(class_2960.method_60655("macrokeybinds", "ui"));
    private static final class_2583 FONT_BOLD =
        class_2583.field_24360.method_27704(class_2960.method_60655("macrokeybinds", "ui_bold"));

    /** Подстройка вертикали текста (в единицах интерфейса), если шрифт сидит выше/ниже. */
    private static final float TEXT_DY = 0.0f;
    /** Расстояние от верха строки до центра заглавных букв при масштабе 1. */
    private static final float CAP_CENTER = 3.7f;

    private static final float K_TITLE = 1.2f;
    private static final float K_BODY = 0.95f;
    private static final float K_CTRL = 0.88f;
    private static final float K_SMALL = 0.80f;

    // ---------- размеры (единицы интерфейса) ----------
    private static final float PANEL_W = 264;
    private static final float PANEL_MAX_H = 340;
    private static final float PANEL_R = 26;
    private static final float PAD = 20;
    private static final float HEADER_H = 92;
    private static final float FOOTER_H = 28;

    private enum Kind { FLAG, SLIDER, MODE }

    private static final class Row {
        final Setting setting;
        final Kind kind;
        final String name;
        final String description;
        float y;
        float h;

        Row(Setting setting, Kind kind, String name, String description) {
            this.setting = setting;
            this.kind = kind;
            this.name = name;
            this.description = description;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final List<Row> visible = new ArrayList<>();

    private float left, top, panelW, panelH;
    private float listTop, listBottom, rowX, rowW;
    private float contentHeight;
    private float scroll, scrollTarget;
    private float openProgress;
    private long lastFrame = System.nanoTime();
    private Row dragging;
    private Row hovered;
    private boolean hoverPower;

    // состояние кадра
    private class_332 ctx;
    private float g = 1.0f;

    public MacroKeybindsScreen() {
        super(class_2561.method_43470("MacroKeybindsV1.3.1"));
    }

    // ================= жизненный цикл =================

    @Override
    protected void method_25426() {
        rows.clear();
        for (Setting s : MacroKeybindsMenu.settings()) {
            if (s instanceof Setting.Flag) {
                rows.add(new Row(s, Kind.FLAG, s.name, s.description));
            } else if (s instanceof Setting.Num) {
                rows.add(new Row(s, Kind.SLIDER, s.name, s.description));
            } else if (s instanceof Setting.Mode) {
                rows.add(new Row(s, Kind.MODE, s.name, s.description));
            }
        }
    }

    @Override
    public void method_25393() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1.0e9f);
        lastFrame = now;
        openProgress = Math.min(1.0f, openProgress + dt * 6.0f);
        scroll += (scrollTarget - scroll) * Math.min(1.0f, dt * 16.0f);
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    @Override
    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
        // без затемнения и размытия фона
    }

    // ================= раскладка =================

    private void layout() {
        panelW = Math.min(PANEL_W, field_22789 - 16);
        panelH = Math.min(PANEL_MAX_H, field_22790 - 24);
        left = (field_22789 - panelW) / 2.0f;
        top = (field_22790 - panelH) / 2.0f;
        rowX = left + PAD;
        rowW = panelW - PAD * 2;
        listTop = top + HEADER_H;
        listBottom = top + panelH - FOOTER_H;

        visible.clear();
        float y = 0;
        for (Row r : rows) {
            if (!MacroKeybindsMenu.isVisible(r.setting)) {
                continue;
            }
            r.h = switch (r.kind) {
                case FLAG -> 32;
                case SLIDER -> 42;
                case MODE -> 50;
            };
            r.y = y;
            visible.add(r);
            y += r.h;
        }
        contentHeight = y;
        float max = Math.max(0, contentHeight - (listBottom - listTop));
        scrollTarget = clamp(scrollTarget, 0, max);
        scroll = clamp(scroll, 0, max);
        for (Row r : visible) {
            r.y = listTop + r.y - scroll;
        }
    }

    private float btnX() { return left + PAD; }
    private float btnY() { return top + 52; }
    private float btnW() { return panelW - PAD * 2; }
    private float btnH() { return 30; }

    // ================= отрисовка =================

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        layout();
        this.ctx = context;
        this.g = (float) class_310.method_1551().method_22683().method_4495();

        float a = easeOut(openProgress);
        float oy = (1.0f - a) * 10.0f;

        hoverPower = inside(mouseX, mouseY, btnX(), btnY(), btnW(), btnH());
        hovered = rowAt(mouseX, mouseY);
        boolean on = MacroKeybindsMenu.isEnabled();

        int sx1 = (int) Math.floor(left), sx2 = (int) Math.ceil(left + panelW);
        int sy1 = (int) Math.floor(listTop), sy2 = (int) Math.ceil(listBottom);

        // ---- проход 1: фигуры ----
        beginPx();
        drawPanel(left, top + oy, panelW, panelH, a);
        drawButton(oy, on, a);
        endPx();

        ctx.method_44379(sx1, sy1, sx2, sy2);
        beginPx();
        for (Row r : visible) {
            if (r.y + r.h < listTop || r.y > listBottom) {
                continue;
            }
            drawRowShapes(r, oy, a);
        }
        endPx();
        ctx.method_44380();

        beginPx();
        drawScrollbar(oy, a);
        endPx();

        ctx.method_51452(); // фигуры уходят в буфер раньше текста

        // ---- проход 2: текст ----
        drawHeaderText(oy, on, a);

        ctx.method_44379(sx1, sy1, sx2, sy2);
        for (Row r : visible) {
            if (r.y + r.h < listTop || r.y > listBottom) {
                continue;
            }
            drawRowText(r, oy, a);
        }
        ctx.method_44380();

        String hint = hovered != null && !hovered.description.isEmpty() ? hovered.description : "ESC - закрыть";
        text(fit(hint, panelW - PAD * 2, K_SMALL), left + PAD, listBottom + FOOTER_H / 2.0f + oy,
            K_SMALL, rgba(255, 255, 255, 0.45f * a), false, true);
    }

    // ---------- панель ----------

    private void drawPanel(float x, float y, float w, float h, float a) {
        float r = PANEL_R;
        // мягкая тень
        for (int i = 1; i <= 6; i++) {
            rr(x - i * 1.6f, y - i * 1.6f + 5, w + i * 3.2f, h + i * 3.2f, r + i * 1.6f,
                rgba(0, 0, 0, 0.032f * a), rgba(0, 0, 0, 0.032f * a));
        }
        // тёмная прозрачная основа
        rr(x, y, w, h, r, rgba(12, 16, 26, 0.40f * a), rgba(12, 16, 26, 0.50f * a));
        // стеклянный блик сверху
        rr(x, y, w, h * 0.55f, r, rgba(255, 255, 255, 0.17f * a), rgba(255, 255, 255, 0.0f));
        // световая кромка: ярче сверху, мягче снизу
        ring(x, y, w, h, r, rgba(255, 255, 255, 0.55f * a), rgba(255, 255, 255, 0.10f * a));
    }

    private void drawButton(float oy, boolean on, float a) {
        float x = btnX(), y = btnY() + oy, w = btnW(), h = btnH();
        if (on) {
            int c1 = rgba(52, 211, 102, (hoverPower ? 1.0f : 0.92f) * a);
            int c2 = rgba(34, 168, 82, (hoverPower ? 1.0f : 0.92f) * a);
            rr(x, y, w, h, h / 2, c1, c2);
            rr(x + 1, y + 1, w - 2, h / 2, h / 2, rgba(255, 255, 255, 0.22f * a), rgba(255, 255, 255, 0.0f));
            ring(x, y, w, h, h / 2, rgba(255, 255, 255, 0.40f * a), rgba(255, 255, 255, 0.10f * a));
        } else {
            float k = hoverPower ? 1.35f : 1.0f;
            rr(x, y, w, h, h / 2, rgba(255, 255, 255, 0.14f * k * a), rgba(255, 255, 255, 0.07f * k * a));
            ring(x, y, w, h, h / 2, rgba(255, 255, 255, 0.34f * a), rgba(255, 255, 255, 0.10f * a));
        }
    }

    private void drawScrollbar(float oy, float a) {
        float viewport = listBottom - listTop;
        if (contentHeight <= viewport) {
            return;
        }
        float barH = Math.max(24, viewport * viewport / contentHeight);
        float max = contentHeight - viewport;
        float barY = listTop + (viewport - barH) * (scroll / Math.max(1.0f, max));
        rr(left + panelW - 9, barY + oy, 3, barH, 1.5f, rgba(255, 255, 255, 0.30f * a), rgba(255, 255, 255, 0.30f * a));
    }

    // ---------- строки: фигуры ----------

    private void drawRowShapes(Row r, float oy, float a) {
        float y = r.y + oy;
        float fade = rowFade(r) * a;

        // тонкий разделитель
        int sepY = X(y + r.h - 0.5f);
        fillPx(X(rowX), sepY, X(rowX + rowW), sepY + 1, rgba(255, 255, 255, 0.08f * fade));

        switch (r.kind) {
            case FLAG -> {
                boolean on = MacroKeybindsMenu.flag(r.setting);
                float sw = 38, sh = 22;
                float sx = rowX + rowW - sw, sy = y + (r.h - sh) / 2;
                if (on) {
                    rr(sx, sy, sw, sh, sh / 2, rgba(52, 211, 102, 0.95f * fade), rgba(34, 168, 82, 0.95f * fade));
                } else {
                    rr(sx, sy, sw, sh, sh / 2, rgba(255, 255, 255, 0.20f * fade), rgba(255, 255, 255, 0.14f * fade));
                }
                ring(sx, sy, sw, sh, sh / 2, rgba(255, 255, 255, 0.30f * fade), rgba(255, 255, 255, 0.08f * fade));
                float kd = 18;
                float kx = on ? sx + sw - kd - 2 : sx + 2;
                knob(kx + kd / 2, sy + sh / 2, kd / 2, fade);
            }
            case SLIDER -> {
                float value = MacroKeybindsMenu.number(r.setting);
                float min = MacroKeybindsMenu.minimum(r.setting);
                float max = MacroKeybindsMenu.maximum(r.setting);
                float t = max - min <= 0 ? 0 : clamp((value - min) / (max - min), 0, 1);
                float ty = y + 30;
                rr(rowX, ty - 1.5f, rowW, 3, 1.5f, rgba(255, 255, 255, 0.20f * fade), rgba(255, 255, 255, 0.20f * fade));
                float fw = Math.max(3, rowW * t);
                rr(rowX, ty - 1.5f, fw, 3, 1.5f, rgba(255, 255, 255, 0.88f * fade), rgba(255, 255, 255, 0.88f * fade));
                knob(rowX + rowW * t, ty, 7, fade);
            }
            case MODE -> {
                List<String> modes = MacroKeybindsMenu.modes(r.setting);
                String current = MacroKeybindsMenu.mode(r.setting);
                float total = 0;
                for (String m : modes) {
                    total += segWidth(m);
                }
                float cy = y + 24;
                rr(rowX, cy, total, 20, 10, rgba(255, 255, 255, 0.10f * fade), rgba(255, 255, 255, 0.10f * fade));
                float px = rowX;
                for (String m : modes) {
                    float w = segWidth(m);
                    if (m.equals(current)) {
                        rr(px + 2, cy + 2, w - 4, 16, 8, rgba(255, 255, 255, 0.30f * fade), rgba(255, 255, 255, 0.22f * fade));
                    }
                    px += w;
                }
            }
        }
    }

    private void knob(float cx, float cy, float rad, float a) {
        disc(cx, cy + 1.2f, rad + 0.6f, rgba(0, 0, 0, 0.22f * a));
        disc(cx, cy, rad, rgba(255, 255, 255, 1.0f * a));
    }

    private float segWidth(String m) {
        return textWidth(m, K_CTRL, false) + 20;
    }

    // ---------- текст ----------

    private void drawHeaderText(float oy, boolean on, float a) {
        text("MacroKeybindsV1.3.1", left + PAD, top + 22 + oy, K_TITLE, rgba(255, 255, 255, 0.96f * a), true, true);
        String sub = on ? "работает" : "выключено";
        text(sub, left + PAD, top + 38 + oy, K_SMALL,
            on ? rgba(52, 211, 102, a) : rgba(255, 255, 255, 0.50f * a), false, true);

        String label = on ? "Выключить" : "Включить";
        float w = textWidth(label, K_BODY, true);
        text(label, btnX() + (btnW() - w) / 2.0f, btnY() + btnH() / 2.0f + oy, K_BODY,
            rgba(255, 255, 255, 0.98f * a), true, true);
    }

    private void drawRowText(Row r, float oy, float a) {
        float y = r.y + oy;
        float fade = rowFade(r) * a;
        int white = rgba(255, 255, 255, 0.94f * fade);
        int dim = rgba(255, 255, 255, 0.55f * fade);

        switch (r.kind) {
            case FLAG -> text(fit(r.name, rowW - 52, K_BODY), rowX, y + r.h / 2, K_BODY, white, false, true);
            case SLIDER -> {
                float value = MacroKeybindsMenu.number(r.setting);
                String shown = MacroKeybindsMenu.integral(r.setting)
                    ? String.valueOf(Math.round(value))
                    : String.format(Locale.ROOT, "%.1f", value);
                float vw = textWidth(shown, K_BODY, false);
                text(fit(r.name, rowW - vw - 14, K_BODY), rowX, y + 12, K_BODY, white, false, true);
                text(shown, rowX + rowW - vw, y + 12, K_BODY, dim, false, true);
            }
            case MODE -> {
                text(fit(r.name, rowW, K_BODY), rowX, y + 12, K_BODY, white, false, true);
                String current = MacroKeybindsMenu.mode(r.setting);
                float px = rowX;
                for (String m : MacroKeybindsMenu.modes(r.setting)) {
                    float w = segWidth(m);
                    float tw = textWidth(m, K_CTRL, false);
                    text(m, px + (w - tw) / 2.0f, y + 34, K_CTRL, m.equals(current) ? white : dim, false, true);
                    px += w;
                }
            }
        }
    }

    private float rowFade(Row r) {
        float edge = Math.min(r.y + r.h - listTop, listBottom - r.y);
        return clamp(0.35f + edge / 40.0f, 0.0f, 1.0f);
    }

    // ================= ввод =================

    @Override
    public boolean method_25402(double mx, double my, int button) {
        if (button == 0) {
            if (inside(mx, my, btnX(), btnY(), btnW(), btnH())) {
                MacroKeybindsMenu.toggle();
                return true;
            }
            Row r = rowAt(mx, my);
            if (r != null) {
                switch (r.kind) {
                    case FLAG -> MacroKeybindsMenu.flag(r.setting, !MacroKeybindsMenu.flag(r.setting));
                    case SLIDER -> {
                        dragging = r;
                        applyDrag(mx);
                    }
                    case MODE -> clickMode(r, mx, my);
                }
                return true;
            }
        }
        return super.method_25402(mx, my, button);
    }

    @Override
    public boolean method_25406(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.method_25406(mx, my, button);
    }

    @Override
    public boolean method_25403(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            applyDrag(mx);
            return true;
        }
        return super.method_25403(mx, my, button, dx, dy);
    }

    @Override
    public boolean method_25401(double mx, double my, double horizontal, double vertical) {
        if (contentHeight > listBottom - listTop) {
            scrollTarget -= (float) vertical * 24.0f;
            return true;
        }
        return super.method_25401(mx, my, horizontal, vertical);
    }

    @Override
    public boolean method_25404(int key, int scancode, int modifiers) {
        if (key == 256) {
            method_25419();
            return true;
        }
        return super.method_25404(key, scancode, modifiers);
    }

    private Row rowAt(double mx, double my) {
        if (mx < rowX - 6 || mx > rowX + rowW + 6 || my < listTop || my > listBottom) {
            return null;
        }
        for (Row r : visible) {
            if (my >= r.y && my < r.y + r.h) {
                return r;
            }
        }
        return null;
    }

    private void clickMode(Row r, double mx, double my) {
        float px = rowX;
        for (String m : MacroKeybindsMenu.modes(r.setting)) {
            float w = segWidth(m);
            if (mx >= px && mx <= px + w && my >= r.y + 22 && my <= r.y + 46) {
                if (!m.equals(MacroKeybindsMenu.mode(r.setting))) {
                    MacroKeybindsMenu.mode(r.setting, m);
                }
                return;
            }
            px += w;
        }
    }

    private void applyDrag(double mx) {
        if (dragging == null) {
            return;
        }
        float t = clamp((float) ((mx - rowX) / Math.max(1.0f, rowW)), 0, 1);
        float min = MacroKeybindsMenu.minimum(dragging.setting);
        float max = MacroKeybindsMenu.maximum(dragging.setting);
        float v = min + (max - min) * t;
        if (MacroKeybindsMenu.integral(dragging.setting)) {
            v = Math.round(v);
        }
        MacroKeybindsMenu.number(dragging.setting, v);
    }

    // ================= текст: примитивы =================

    private class_5250 make(String s, boolean bold) {
        class_5250 t = class_2561.method_43470(s);
        t.method_10862(bold ? FONT_BOLD : FONT_REGULAR);
        return t;
    }

    private float textWidth(String s, float k, boolean bold) {
        return field_22793.method_27525(make(s, bold)) * k;
    }

    /** Рисует текст: (x, y) - левый край и вертикальный центр строки (если center), иначе верх. */
    private void text(String s, float x, float y, float k, int color, boolean bold, boolean center) {
        if ((color >>> 24) < 8 || s.isEmpty()) {
            return;
        }
        float yTop = center ? y - CAP_CENTER * k : y;
        class_4587 ms = ctx.method_51448();
        ms.method_22903();
        ms.method_46416(x, yTop + TEXT_DY * k, 0.0f);
        ms.method_22905(k, k, 1.0f);
        ctx.method_51439(field_22793, make(s, bold), 0, 0, color, false);
        ms.method_22909();
    }

    private String fit(String s, float maxWidth, float k) {
        if (textWidth(s, k, false) <= maxWidth) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && textWidth(cut + "…", k, false) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    // ================= фигуры: физические пиксели + сглаживание =================

    private void beginPx() {
        class_4587 ms = ctx.method_51448();
        ms.method_22903();
        ms.method_22905(1.0f / g, 1.0f / g, 1.0f);
    }

    private void endPx() {
        ctx.method_51448().method_22909();
    }

    private int X(float v) {
        return Math.round(v * g);
    }

    private int ringThickness() {
        return Math.max(1, Math.round(g * 0.7f));
    }

    private void rr(float x, float y, float w, float h, float r, int c1, int c2) {
        int x0 = X(x), y0 = X(y), x1 = X(x + w), y1 = X(y + h);
        roundRectPx(x0, y0, x1 - x0, y1 - y0, r * g, c1, c2);
    }

    private void ring(float x, float y, float w, float h, float r, int c1, int c2) {
        int x0 = X(x), y0 = X(y), x1 = X(x + w), y1 = X(y + h);
        ringPx(x0, y0, x1 - x0, y1 - y0, r * g, ringThickness(), c1, c2);
    }

    private void disc(float cx, float cy, float rad, int color) {
        int r = Math.max(1, X(rad));
        roundRectPx(X(cx) - r, X(cy) - r, r * 2, r * 2, r, color, color);
    }

    private void fillPx(int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || (color >>> 24) == 0) {
            return;
        }
        ctx.method_25294(x1, y1, x2, y2, color);
    }

    private void gradPx(int x, int y, int w, int h, int c1, int c2) {
        if (w <= 0 || h <= 0) {
            return;
        }
        if (c1 == c2) {
            fillPx(x, y, x + w, y + h, c1);
        } else {
            ctx.method_25296(x, y, x + w, y + h, c1, c2);
        }
    }

    /** Горизонтальный отрезок высотой 1 px с дробными краями (сглаживание). */
    private void hspan(int y, float a, float b, int color) {
        if (b <= a || (color >>> 24) == 0) {
            return;
        }
        int ia = (int) Math.ceil(a);
        int ib = (int) Math.floor(b);
        if (ia > ib) {
            pixel((int) Math.floor(a), y, color, b - a);
            return;
        }
        if (ia > a) {
            pixel(ia - 1, y, color, ia - a);
        }
        if (ib > ia) {
            ctx.method_25294(ia, y, ib, y + 1, color);
        }
        if (b > ib) {
            pixel(ib, y, color, b - ib);
        }
    }

    private void pixel(int x, int y, int color, float coverage) {
        int alpha = (int) (((color >>> 24) & 0xFF) * coverage);
        if (alpha <= 0) {
            return;
        }
        ctx.method_25294(x, y, x + 1, y + 1, (alpha << 24) | (color & 0xFFFFFF));
    }

    /** Насколько строка j отступает от левого края из-за скругления (среднее по 3 подвыборкам). */
    private static float inset(float r, int j, int h) {
        if (r <= 0) {
            return 0;
        }
        float sum = 0;
        for (int k = 0; k < 3; k++) {
            float yy = j + (k + 0.5f) / 3.0f;
            float d = Math.min(yy, h - yy);
            if (d < r) {
                float dy = r - d;
                sum += r - (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            }
        }
        return sum / 3.0f;
    }

    private void roundRectPx(int x, int y, int w, int h, float r, int c1, int c2) {
        if (w <= 0 || h <= 0 || ((c1 >>> 24) == 0 && (c2 >>> 24) == 0)) {
            return;
        }
        r = Math.min(r, Math.min(w, h) / 2.0f);
        int rc = (int) Math.ceil(r);
        for (int j = 0; j < h; j++) {
            if (j >= rc && j < h - rc) {
                j = h - rc - 1;
                continue;
            }
            float ins = inset(r, j, h);
            hspan(y + j, x + ins, x + w - ins, mixArgb(c1, c2, (j + 0.5f) / h));
        }
        int mh = h - rc * 2;
        if (mh > 0) {
            gradPx(x, y + rc, w, mh, mixArgb(c1, c2, rc / (float) h), mixArgb(c1, c2, (h - rc) / (float) h));
        }
    }

    private void ringPx(int x, int y, int w, int h, float r, int t, int c1, int c2) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.min(r, Math.min(w, h) / 2.0f);
        float ri = Math.max(0, r - t);
        int rc = (int) Math.ceil(r);
        for (int j = 0; j < h; j++) {
            if (j >= rc && j < h - rc) {
                j = h - rc - 1;
                continue;
            }
            int color = mixArgb(c1, c2, (j + 0.5f) / h);
            float io = inset(r, j, h);
            if (j < t || j >= h - t) {
                hspan(y + j, x + io, x + w - io, color);
            } else {
                float ii = t + inset(ri, j - t, h - 2 * t);
                hspan(y + j, x + io, x + ii, color);
                hspan(y + j, x + w - ii, x + w - io, color);
            }
        }
        int mh = h - rc * 2;
        if (mh > 0) {
            int ca = mixArgb(c1, c2, rc / (float) h);
            int cb = mixArgb(c1, c2, (h - rc) / (float) h);
            gradPx(x, y + rc, t, mh, ca, cb);
            gradPx(x + w - t, y + rc, t, mh, ca, cb);
        }
    }

    // ================= утилиты =================

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static float easeOut(float t) {
        float k = 1.0f - t;
        return 1.0f - k * k * k;
    }

    private static int rgba(int r, int g, int b, float a) {
        int alpha = (int) (clamp(a, 0, 1) * 255.0f + 0.5f);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    /** Меняет только прозрачность готового цвета. */
    private static int rgba(int r, int g, int b) {
        return rgba(r, g, b, 1.0f);
    }

    private static int mixArgb(int c1, int c2, float t) {
        if (c1 == c2) {
            return c1;
        }
        t = clamp(t, 0, 1);
        int a = (int) (((c1 >>> 24) & 0xFF) + (((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int gg = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }
}
