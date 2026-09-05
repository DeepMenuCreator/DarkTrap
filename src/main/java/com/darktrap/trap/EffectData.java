package com.darktrap.trap;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * A single configured potion effect (type + amplifier + duration in ticks)
 * applied to entities caught inside a trap.
 */
public final class EffectData {

    private final PotionEffectType type;
    private final int amplifier;
    private final int durationTicks;

    private EffectData(PotionEffectType type, int amplifier, int durationTicks) {
        this.type = type;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
    }

    public static EffectData of(String typeName, int amplifier, int durationTicks) {
        PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(typeName.trim().toLowerCase()));
        if (type == null) {
            return null;
        }
        return new EffectData(type, Math.max(0, amplifier), Math.max(1, durationTicks));
    }

    public PotionEffect toPotionEffect() {
        return new PotionEffect(type, durationTicks, amplifier, true, true, true);
    }
}
