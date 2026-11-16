package com.citymod.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Запоминает, что город уже построен и где его центр. */
public class CityData extends SavedData {
    public static final String NAME = "citymod_city";

    public boolean generated = false;
    public int cx, cz, by;

    public static CityData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CityData::new, CityData::load, null), NAME);
    }

    public static CityData load(CompoundTag tag, HolderLookup.Provider registries) {
        CityData d = new CityData();
        d.generated = tag.getBoolean("generated");
        d.cx = tag.getInt("cx");
        d.cz = tag.getInt("cz");
        d.by = tag.getInt("by");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("generated", generated);
        tag.putInt("cx", cx);
        tag.putInt("cz", cz);
        tag.putInt("by", by);
        return tag;
    }
}
