package com.citymod.crypto;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Цены монет и история для графиков. Хранится в мире, цены общие для всех игроков. */
public class CryptoData extends SavedData {
    public static final String NAME = "citymod_crypto";

    public final double[] prices = Market.BASE.clone();
    public final double[][] hist = new double[4][Market.HIST];

    public CryptoData() {
        for (int c = 0; c < 4; c++) java.util.Arrays.fill(hist[c], prices[c]);
    }

    public static CryptoData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CryptoData::new, CryptoData::load, null), NAME);
    }

    public static CryptoData load(CompoundTag tag, HolderLookup.Provider registries) {
        CryptoData d = new CryptoData();
        for (int c = 0; c < 4; c++) {
            if (tag.contains("p" + c)) d.prices[c] = tag.getDouble("p" + c);
            ListTag l = tag.getList("h" + c, Tag.TAG_DOUBLE);
            if (l.size() == Market.HIST) {
                for (int i = 0; i < Market.HIST; i++) d.hist[c][i] = l.getDouble(i);
            } else {
                java.util.Arrays.fill(d.hist[c], d.prices[c]);
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (int c = 0; c < 4; c++) {
            tag.putDouble("p" + c, prices[c]);
            ListTag l = new ListTag();
            for (double v : hist[c]) l.add(DoubleTag.valueOf(v));
            tag.put("h" + c, l);
        }
        return tag;
    }
}
