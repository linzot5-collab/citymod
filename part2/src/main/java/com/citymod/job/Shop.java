package com.citymod.job;

import com.citymod.economy.Economy;
import com.citymod.util.Msg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Каталог магазинов: /shop buy <id>. Цены задаёт сервер. */
public final class Shop {
    private record Entry(long price, String name, Supplier<ItemStack> stack, Consumer<ServerPlayer> effect) {}

    private static final Map<String, Entry> CATALOG = new LinkedHashMap<>();

    static {
        item("bread", 5, "хлеб x3", () -> new ItemStack(Items.BREAD, 3));
        item("steak", 12, "стейк x4", () -> new ItemStack(Items.COOKED_BEEF, 4));
        item("torch", 8, "факелы x16", () -> new ItemStack(Items.TORCH, 16));
        item("iron_axe", 70, "железный топор", () -> new ItemStack(Items.IRON_AXE));
        item("iron_pickaxe", 80, "железная кирка", () -> new ItemStack(Items.IRON_PICKAXE));
        item("shears", 40, "ножницы", () -> new ItemStack(Items.SHEARS));
        item("helmet", 15, "кожаная шапка", () -> new ItemStack(Items.LEATHER_HELMET));
        item("chestplate", 30, "кожаная куртка", () -> new ItemStack(Items.LEATHER_CHESTPLATE));
        item("leggings", 25, "кожаные штаны", () -> new ItemStack(Items.LEATHER_LEGGINGS));
        item("boots", 15, "кожаные ботинки", () -> new ItemStack(Items.LEATHER_BOOTS));
        CATALOG.put("beer", new Entry(8, "пиво", null,
                p -> p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0))));
        CATALOG.put("cocktail", new Entry(15, "коктейль", null,
                p -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1))));
    }

    private Shop() {}

    private static void item(String id, long price, String name, Supplier<ItemStack> s) {
        CATALOG.put(id, new Entry(price, name, s, null));
    }

    public static void buy(ServerPlayer p, String id) {
        Entry e = CATALOG.get(id);
        if (e == null) { Msg.shop(p, "Такого товара нет."); return; }
        if (!Economy.spendCash(p, e.price)) { Msg.shop(p, "Не хватает наличных: нужно " + e.price + " $."); return; }
        if (e.stack != null) {
            ItemStack st = e.stack.get();
            if (!p.getInventory().add(st)) p.drop(st, false);
        }
        if (e.effect != null) e.effect.accept(p);
        Msg.shop(p, "Куплено: " + e.name + " за " + e.price + " $.");
    }
}
