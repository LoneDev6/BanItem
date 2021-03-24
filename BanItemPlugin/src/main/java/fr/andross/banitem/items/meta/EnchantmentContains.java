/*
 * BanItem - Lightweight, powerful & configurable per world ban item plugin
 * Copyright (C) 2021 André Sustac
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your action) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.andross.banitem.items.meta;

import fr.andross.banitem.utils.BanVersion;
import fr.andross.banitem.utils.debug.Debug;
import fr.andross.banitem.utils.enchantments.EnchantmentHelper;
import fr.andross.banitem.utils.enchantments.EnchantmentWrapper;
import fr.andross.banitem.utils.list.Listable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple meta comparator to compare the enchantments
 * @version 3.1
 * @author Andross
 */
public final class EnchantmentContains extends MetaTypeComparator {
    private final Set<Object> enchantsWithoutLevels = new HashSet<>(); // >=1.13: Enchantment, <1.13 String
    private final Set<EnchantmentWrapper> enchants = new HashSet<>(); // Specific enchantments with specific levels
    private final Map<Object, Integer[]> enchantsIntervals = new HashMap<>(); // >=1.13: Enchantment, <1.13 String

    public EnchantmentContains(final Object o, final Debug debug) {
        super(o);

        for (final String string : Listable.getSplittedStringList(o)) {
            final String[] s = string.split(":");

            // 'Enchantment': if the item contains this enchantment, does not consider the level;
            if (s.length == 1) {
                final Enchantment enchantment = EnchantmentHelper.getEnchantment(s[0]);
                if (enchantment == null) {
                    debug.clone().add("&cUnknown enchantment '" + s[0] + "'.").sendDebug();
                    setValid(false);
                    return;
                }

                // Adding
                enchantsWithoutLevels.add(BanVersion.v13OrMore ? enchantment : enchantment.getName());
                continue;
            }

            // 'Enchantment:Level': if the item contains this enchantment with this level;
            if (s.length == 2) {
                final Enchantment enchantment = EnchantmentHelper.getEnchantment(s[0]);
                if (enchantment == null) {
                    debug.clone().add("&cUnknown enchantment '" + s[0] + "' in value '" + string + "'.").sendDebug();
                    setValid(false);
                    return;
                }

                final int level;
                try {
                    level = Integer.parseInt(s[1]);
                } catch (final NumberFormatException e) {
                    debug.clone().add("&cInvalid level '" + s[1] + "' in value '" + string + "'.").sendDebug();
                    setValid(false);
                    return;
                }

                // Adding
                enchants.add(new EnchantmentWrapper(enchantment, level));
                continue;
            }

            // 'Enchantment:MinLevel:MaxLevel': if the item contains this enchantment, within the min & max level interval [inclusive];
            if (s.length >= 3) {
                final Enchantment enchantment = EnchantmentHelper.getEnchantment(s[0]);
                if (enchantment == null) {
                    debug.clone().add("&cUnknown enchantment '" + s[0] + "' in value '" + string + "'.").sendDebug();
                    setValid(false);
                    return;
                }

                final int minLevel;
                try {
                    minLevel = Integer.parseInt(s[1]);
                } catch (final NumberFormatException e) {
                    debug.clone().add("&cInvalid minimum level '" + s[1] + "' in value '" + string + "'.").sendDebug();
                    setValid(false);
                    return;
                }

                final int maxLevel;
                try {
                    maxLevel = Integer.parseInt(s[2]);
                } catch (final NumberFormatException e) {
                    debug.clone().add("&cInvalid maximum level '" + s[2] + "' in value '" + string + "'.").sendDebug();
                    setValid(false);
                    return;
                }

                // Adding
                enchantsIntervals.put(BanVersion.v13OrMore ? enchantment : enchantment.getName(), new Integer[]{ minLevel, maxLevel });
            }
        }
    }

    @Override
    public boolean matches(@NotNull final ItemStack itemStack, @Nullable final ItemMeta itemMeta) {
        if (itemMeta == null || !itemMeta.hasEnchants()) return false;

        for (final Map.Entry<Enchantment, Integer> e : itemMeta.getEnchants().entrySet()) {
            final Enchantment enchantment = e.getKey();
            final int level = e.getValue();
            final Object object = BanVersion.v13OrMore ? enchantment : enchantment.getName();

            // Containing enchantment (not considering level) ?
            if (enchantsWithoutLevels.contains(object)) return true;

            // Containing enchantment (considering level)?
            if (enchants.contains(new EnchantmentWrapper(enchantment, level))) return true;

            // Containing enchantment (in interval)?
            if (enchantsIntervals.containsKey(object)) {
                final Integer[] interval = enchantsIntervals.get(object);
                if (level >= interval[0] && level <= interval[1]) return true;
            }
        }

        return false;
    }
}
