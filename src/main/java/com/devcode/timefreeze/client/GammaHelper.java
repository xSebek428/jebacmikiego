package com.devcode.timefreeze.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Ustawianie jasności POWYŻEJ limitu 1.0.
 * OptionInstance#set() przycina gamma do zakresu 0..1, więc zapisujemy wartość
 * bezpośrednio w polu 'value'. Pole wyszukujemy po typie/modyfikatorach (jedyne
 * niestatyczne, nie-final pole typu Object), dzięki czemu działa i na nazwach
 * mojang, i na SRG (bez Access Transformera).
 */
public final class GammaHelper {
    private static Field valueField;

    public static void set(double gamma) {
        OptionInstance<Double> option = Minecraft.getInstance().options.gamma();
        try {
            if (valueField == null) {
                for (Field f : OptionInstance.class.getDeclaredFields()) {
                    int m = f.getModifiers();
                    if (f.getType() == Object.class && !Modifier.isStatic(m) && !Modifier.isFinal(m)) {
                        f.setAccessible(true);
                        valueField = f;
                        break;
                    }
                }
            }
            if (valueField != null) {
                valueField.set(option, gamma);
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // spadamy do wersji "bezpiecznej" poniżej
        }
        option.set(Math.min(gamma, 1.0D)); // fallback: max. jasność vanilli
    }

    private GammaHelper() {}
}
