package com.citymod.city;

import com.citymod.CityMod;
import com.citymod.block.ModBlocks;
import com.citymod.entity.CityNpc;
import com.citymod.entity.NpcRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Город 128x128 вокруг центра: дороги, здания (банк, казино, ночной клуб, магазины, склад, дома),
 * NPC и лесопосадка. Строится кодом через очередь задач, поэтому не требует датапаков.
 */
public final class City {
    public static City current;
    private static int cracks = 0;

    public record Rect(int x0, int z0, int w, int d, int h) {
        public int x1() { return x0 + w - 1; }
        public int z1() { return z0 + d - 1; }
        public int mx() { return x0 + w / 2; }
        public int mz() { return z0 + d / 2; }
        public boolean contains(double x, double z) { return x >= x0 && x <= x0 + w && z >= z0 && z <= z0 + d; }
    }

    private static final int F = 2 | 16; // отправить клиентам, без обновления соседей
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public final ServerLevel level;
    public final int cx, cz, by;
    public final Rect casino, club, bank, jobs, house, market, clothier, cafe, warehouse, garage;
    public final List<Rect> homes = new ArrayList<>();
    public final List<BlockPos> trees = new ArrayList<>();
    public final BlockPos spawn;

    // танцпол клуба
    private final int danceX, danceZ;
    private static final Block[] CLUB_LAMPS = {
            Blocks.SEA_LANTERN, Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT,
            Blocks.VERDANT_FROGLIGHT, Blocks.SHROOMLIGHT, Blocks.GLOWSTONE
    };

    public City(ServerLevel level, int cx, int cz, int by) {
        this.level = level;
        this.cx = cx;
        this.cz = cz;
        this.by = by;
        casino = new Rect(cx + 6, cz - 46, 44, 39, 14);
        club = new Rect(cx + 6, cz + 6, 40, 30, 10);
        bank = new Rect(cx - 34, cz - 32, 26, 25, 8);
        jobs = new Rect(cx - 58, cz - 24, 18, 17, 6);
        house = new Rect(cx - 30, cz + 6, 14, 12, 5);
        market = new Rect(cx - 14, cz + 6, 10, 10, 5);
        clothier = new Rect(cx - 44, cz + 6, 10, 10, 5);
        cafe = new Rect(cx - 58, cz + 6, 14, 12, 5);
        warehouse = new Rect(cx - 58, cz + 24, 26, 12, 7);
        garage = new Rect(cx - 30, cz + 24, 14, 12, 5);
        for (int i = 0; i < 4; i++) homes.add(new Rect(cx - 58 + 12 * i, cz + 41, 9, 9, 4));
        for (int tx = 10; tx <= 45; tx += 7) {
            for (int tz = 46; tz <= 60; tz += 7) trees.add(new BlockPos(cx + tx, by + 1, cz + tz));
        }
        spawn = new BlockPos(house.x0() + 3, by + 1, house.z0() + 3);
        danceX = club.x0() + 10;
        danceZ = club.z0() + 8;
    }

    // ------------------------------------------------------------------ утилиты

    private void set(int x, int y, int z, BlockState s) { level.setBlock(new BlockPos(x, y, z), s, F); }
    private void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, s);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
        fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState());
    }

    private void sign(int x, int y, int z, Direction facing, String... lines) {
        BlockPos p = new BlockPos(x, y, z);
        level.setBlock(p, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), 3);
        if (level.getBlockEntity(p) instanceof SignBlockEntity be) {
            SignText t = be.getFrontText();
            for (int i = 0; i < lines.length && i < 4; i++) t = t.setMessage(i, Component.literal(lines[i]));
            be.setText(t, true);
            be.setChanged();
        }
    }

    private CityNpc npc(double x, double z, NpcRole role, int skin, boolean fixed, float yaw) {
        CityNpc n = CityMod.CITY_NPC.get().create(level);
        if (n == null) return null;
        n.moveTo(x, by + 1, z, yaw, 0);
        n.setYBodyRot(yaw);
        n.setYHeadRot(yaw);
        n.setRole(role);
        n.setSkin(skin);
        n.setHome(new BlockPos((int) x, by + 1, (int) z), fixed ? 0 : 10);
        if (fixed) n.setNoAi(true);
        level.addFreshEntity(n);
        return n;
    }

    /** Стены, пол, потолок. Интерьер: y от by+1 до by+h, потолок на by+h+1. */
    private void shell(Rect r, Block wall, Block floor, Block ceil) {
        fill(r.x0(), by, r.z0(), r.x1(), by, r.z1(), floor);
        fill(r.x0(), by + 1, r.z0(), r.x1(), by + r.h(), r.z1(), wall);
        fill(r.x0() + 1, by + 1, r.z0() + 1, r.x1() - 1, by + r.h(), r.z1() - 1, AIR);
        fill(r.x0(), by + r.h() + 1, r.z0(), r.x1(), by + r.h() + 1, r.z1(), ceil);
    }

    private void windows(Rect r, int y1, int y2, Block glass) {
        for (int x = r.x0() + 2; x <= r.x1() - 2; x++) {
            if ((x - r.x0()) % 4 == 0) continue;
            for (int y = y1; y <= y2; y++) { set(x, y, r.z0(), glass); set(x, y, r.z1(), glass); }
        }
        for (int z = r.z0() + 2; z <= r.z1() - 2; z++) {
            if ((z - r.z0()) % 4 == 0) continue;
            for (int y = y1; y <= y2; y++) { set(r.x0(), y, z, glass); set(r.x1(), y, z, glass); }
        }
    }

    /** Проём двери по центру стены. side: 'N' (z0), 'S' (z1), 'W' (x0), 'E' (x1). */
    private void door(Rect r, char side, int width, int height) {
        int half = width / 2;
        switch (side) {
            case 'N' -> fill(r.mx() - half, by + 1, r.z0(), r.mx() - half + width - 1, by + height, r.z0(), AIR);
            case 'S' -> fill(r.mx() - half, by + 1, r.z1(), r.mx() - half + width - 1, by + height, r.z1(), AIR);
            case 'W' -> fill(r.x0(), by + 1, r.mz() - half, r.x0(), by + height, r.mz() - half + width - 1, AIR);
            default -> fill(r.x1(), by + 1, r.mz() - half, r.x1(), by + height, r.mz() - half + width - 1, AIR);
        }
    }

    private void doorSign(Rect r, char side, int height, String... lines) {
        switch (side) {
            case 'N' -> sign(r.mx(), by + height + 1, r.z0() - 1, Direction.NORTH, lines);
            case 'S' -> sign(r.mx(), by + height + 1, r.z1() + 1, Direction.SOUTH, lines);
            case 'W' -> sign(r.x0() - 1, by + height + 1, r.mz(), Direction.WEST, lines);
            default -> sign(r.x1() + 1, by + height + 1, r.mz(), Direction.EAST, lines);
        }
    }

    private void ceilingLights(Rect r, int step, Block... lamps) {
        int i = 0;
        for (int x = r.x0() + 2; x <= r.x1() - 2; x += step)
            for (int z = r.z0() + 2; z <= r.z1() - 2; z += step)
                set(x, by + r.h() + 1, z, lamps[(i++) % lamps.length]);
    }

    // ------------------------------------------------------------------ сборка города

    public void build(Runnable onDone) {
        for (int x = -64; x < 64; x += 8) {
            final int xa = cx + x;
            BuildQueue.add(() -> ground(xa, xa + 7));
        }
        BuildQueue.add(this::roads);
        BuildQueue.add(this::lamps);
        BuildQueue.add(this::buildHouse);
        BuildQueue.add(this::buildBank);
        BuildQueue.add(this::buildJobs);
        BuildQueue.add(this::buildMarket);
        BuildQueue.add(this::buildClothier);
        BuildQueue.add(this::buildCafe);
        BuildQueue.add(this::buildWarehouse);
        BuildQueue.add(this::buildGarage);
        BuildQueue.add(this::buildHomes);
        BuildQueue.add(this::casinoShell);
        BuildQueue.add(this::casinoTables);
        BuildQueue.add(this::casinoDecor);
        BuildQueue.add(this::buildClub);
        BuildQueue.add(this::clubDecor);
        BuildQueue.add(this::lumberCamp);
        BuildQueue.add(this::atmsAndCracks);
        BuildQueue.add(this::citizens);
        BuildQueue.add(onDone);
    }

    private void ground(int xa, int xb) {
        for (int x = xa; x <= xb; x++) {
            for (int z = cz - 64; z < cz + 64; z++) {
                for (int y = by - 1; y > by - 40; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    if (!st.isAir() && st.getFluidState().isEmpty()) break;
                    level.setBlock(p, Blocks.DIRT.defaultBlockState(), F);
                }
                level.setBlock(new BlockPos(x, by, z), Blocks.GRASS_BLOCK.defaultBlockState(), F);
                for (int y = by + 1; y <= by + 26; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!level.getBlockState(p).isAir()) level.setBlock(p, AIR, F);
                }
            }
        }
    }

    private void roads() {
        BlockState road = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState walk = Blocks.STONE_BRICKS.defaultBlockState();
        // тротуары
        for (int x = -64; x < 64; x++) {
            for (int z : new int[]{-7, -6, -5, -4, 3, 4, 5}) set(cx + x, by, cz + z, walk);
        }
        for (int z = -64; z < 64; z++) {
            for (int x : new int[]{-7, -6, -5, -4, 3, 4, 5}) set(cx + x, by, cz + z, walk);
        }
        for (int x = -64; x <= -4; x++) {
            for (int z : new int[]{17, 18, 22, 23, 35, 36, 40}) set(cx + x, by, cz + z, walk);
        }
        // проезжая часть
        for (int x = -64; x < 64; x++) for (int z = -3; z <= 2; z++) set(cx + x, by, cz + z, road);
        for (int z = -64; z < 64; z++) for (int x = -3; x <= 2; x++) set(cx + x, by, cz + z, road);
        for (int x = -64; x <= -4; x++) for (int z = 19; z <= 21; z++) set(cx + x, by, cz + z, road);
        for (int x = -64; x <= -4; x++) for (int z = 37; z <= 39; z++) set(cx + x, by, cz + z, road);
        // разметка
        for (int x = -64; x < 64; x++) if (x % 4 == 0 && (x < -4 || x > 3)) set(cx + x, by, cz, Blocks.WHITE_CONCRETE);
        for (int z = -64; z < 64; z++) if (z % 4 == 0 && (z < -4 || z > 3)) set(cx, by, cz + z, Blocks.WHITE_CONCRETE);
    }

    private void lamps() {
        for (int x = -60; x <= 60; x += 12) {
            if (x > -8 && x < 8) continue;
            lampPost(cx + x, cz - 5);
            lampPost(cx + x, cz + 4);
        }
        for (int z = -60; z <= 60; z += 12) {
            if (z > -8 && z < 8) continue;
            lampPost(cx - 5, cz + z);
            lampPost(cx + 4, cz + z);
        }
    }

    private void lampPost(int x, int z) {
        fill(x, by + 1, z, x, by + 4, z, Blocks.OAK_FENCE);
        set(x, by + 5, z, Blocks.SEA_LANTERN);
    }

    // ------------------------------------------------------------------ дом при спавне

    private void buildHouse() {
        Rect r = house;
        shell(r, Blocks.SPRUCE_PLANKS, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
        windows(r, by + 2, by + 3, Blocks.GLASS);
        door(r, 'N', 2, 3);
        doorSign(r, 'N', 3, "§lВАШ ДОМ", "Здесь начинается", "ваша история");
        ceilingLights(r, 4, Blocks.GLOWSTONE);
        fill(r.x0() + 1, by + 1, r.z0() + 1, r.x1() - 1, by + 1, r.z1() - 1, Blocks.RED_CARPET);

        int x = r.x0(), z = r.z0();
        // кровать у восточной стены
        set(x + 11, by + 1, z + 3, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
        set(x + 11, by + 1, z + 4, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
        // кухня и мастерская
        set(x + 2, by + 1, z + 9, Blocks.CRAFTING_TABLE);
        set(x + 3, by + 1, z + 9, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        set(x + 4, by + 1, z + 9, Blocks.SMOKER.defaultBlockState());
        set(x + 5, by + 1, z + 9, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        set(x + 6, by + 1, z + 9, Blocks.BARREL);
        set(x + 7, by + 1, z + 9, Blocks.ANVIL);
        set(x + 8, by + 1, z + 9, Blocks.ENCHANTING_TABLE);
        fill(x + 1, by + 1, z + 3, x + 1, by + 3, z + 6, Blocks.BOOKSHELF);
        // сундук со стартовыми вещами
        BlockPos chest = new BlockPos(x + 10, by + 1, z + 9);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 3);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity c) {
            c.setItem(0, new ItemStack(Items.BREAD, 12));
            c.setItem(1, new ItemStack(Items.TORCH, 16));
            c.setItem(2, new ItemStack(Items.STONE_AXE));
            c.setItem(3, new ItemStack(Items.WOODEN_PICKAXE));
            c.setItem(4, new ItemStack(Items.OAK_PLANKS, 16));
            c.setChanged();
        }
        set(x + 6, by + 1, z + 5, Blocks.OAK_PRESSURE_PLATE);
        set(x + 6, by + 3, z + 5, Blocks.LANTERN);
    }

    // ------------------------------------------------------------------ банк

    private void buildBank() {
        Rect r = bank;
        shell(r, Blocks.QUARTZ_BLOCK, Blocks.POLISHED_ANDESITE, Blocks.SMOOTH_QUARTZ);
        windows(r, by + 3, by + 5, Blocks.LIGHT_BLUE_STAINED_GLASS);
        door(r, 'S', 4, 4);
        doorSign(r, 'S', 4, "§1§lБАНК", "Карты и кредиты");
        ceilingLights(r, 4, Blocks.SEA_LANTERN, Blocks.GLOWSTONE);
        fill(r.x0() + 1, by + 1, r.z0() + 1, r.x1() - 1, by + 1, r.z1() - 1, Blocks.BLUE_CARPET);

        int x = r.x0(), z = r.z0();
        // стойка с двумя окнами
        fill(x + 3, by + 1, z + 8, x + 22, by + 1, z + 8, Blocks.POLISHED_DIORITE);
        fill(x + 3, by + 2, z + 8, x + 22, by + 2, z + 8, Blocks.GLASS_PANE);
        for (int wx : new int[]{7, 17}) {
            set(x + wx, by + 1, z + 8, ModBlocks.BANK_DESK.get());
            set(x + wx + 1, by + 1, z + 8, ModBlocks.BANK_DESK.get());
            set(x + wx, by + 2, z + 8, AIR);
            set(x + wx + 1, by + 2, z + 8, AIR);
            npc(x + wx + 1.0, z + 6.5, NpcRole.BANKER, wx == 7 ? 2 : 5, true, 0f);
        }
        sign(x + 12, by + 3, z + 9, Direction.SOUTH, "§lОЧЕРЕДЬ", "ПКМ по окну", "§7или /bank queue");
        // скамейки для ожидания
        for (int bx = 4; bx <= 20; bx += 4) {
            set(x + bx, by + 1, z + 14, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            set(x + bx + 1, by + 1, z + 14, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        }
        // банкоматы внутри
        set(x + 2, by + 1, z + 20, ModBlocks.ATM.get());
        set(x + 3, by + 1, z + 20, ModBlocks.ATM.get());
        // хранилище для инкассаторов
        fill(x + 21, by, z + 2, x + 24, by, z + 5, Blocks.GOLD_BLOCK);
        set(x + 22, by + 1, z + 3, ModBlocks.BANK_VAULT.get());
        set(x + 23, by + 1, z + 3, ModBlocks.BANK_VAULT.get());
        fill(x + 21, by + 1, z + 5, x + 24, by + 3, z + 5, Blocks.IRON_BARS);
        sign(x + 22, by + 3, z + 2, Direction.SOUTH, "§lХРАНИЛИЩЕ", "Сдача инкассации");
    }

    // ------------------------------------------------------------------ биржа труда

    private void buildJobs() {
        Rect r = jobs;
        shell(r, Blocks.BRICKS, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS);
        windows(r, by + 2, by + 3, Blocks.GLASS);
        door(r, 'S', 3, 3);
        doorSign(r, 'S', 3, "§2§lБИРЖА ТРУДА", "Работа для всех");
        ceilingLights(r, 4, Blocks.GLOWSTONE);
        int x = r.x0(), z = r.z0();
        set(x + 5, by + 1, z + 4, ModBlocks.JOB_BOARD.get());
        set(x + 6, by + 1, z + 4, ModBlocks.JOB_BOARD.get());
        set(x + 12, by + 1, z + 4, ModBlocks.JOB_BOARD.get());
        npc(x + 9.5, z + 4.5, NpcRole.FOREMAN, 1, true, 180f);
        fill(x + 3, by + 1, z + 9, x + 14, by + 1, z + 9, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        sign(x + 9, by + 3, z + 1, Direction.SOUTH, "§lВАКАНСИИ", "Дровосек  4$", "Грузчик  12$", "Дорожник 10$");
    }

    // ------------------------------------------------------------------ магазины

    private void buildMarket() {
        Rect r = market;
        shell(r, Blocks.WHITE_CONCRETE, Blocks.SMOOTH_QUARTZ, Blocks.LIGHT_GRAY_CONCRETE);
        windows(r, by + 2, by + 3, Blocks.GLASS);
        door(r, 'N', 3, 3);
        doorSign(r, 'N', 3, "§c§lСУПЕРМАРКЕТ", "Еда и инструменты");
        ceilingLights(r, 3, Blocks.SEA_LANTERN);
        int x = r.x0(), z = r.z0();
        for (int sz = 3; sz <= 7; sz += 2) {
            fill(x + 2, by + 1, z + sz, x + 7, by + 1, z + sz, Blocks.BARREL);
            fill(x + 2, by + 2, z + sz, x + 7, by + 2, z + sz, Blocks.BOOKSHELF);
        }
        npc(x + 8.5, z + 2.5, NpcRole.SHOPKEEPER, 3, true, 180f);
    }

    private void buildClothier() {
        Rect r = clothier;
        shell(r, Blocks.PINK_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.WHITE_CONCRETE);
        windows(r, by + 2, by + 3, Blocks.GLASS);
        door(r, 'N', 3, 3);
        doorSign(r, 'N', 3, "§d§lОДЕЖДА", "Кожаная мода");
        ceilingLights(r, 3, Blocks.GLOWSTONE);
        int x = r.x0(), z = r.z0();
        for (int sx = 2; sx <= 7; sx += 2) {
            set(x + sx, by + 1, z + 7, Blocks.OAK_FENCE);
            set(x + sx, by + 2, z + 7, Blocks.RED_WOOL);
        }
        npc(x + 5.5, z + 3.5, NpcRole.CLOTHIER, 7, true, 180f);
    }

    private void buildCafe() {
        Rect r = cafe;
        shell(r, Blocks.ORANGE_TERRACOTTA, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS);
        windows(r, by + 2, by + 3, Blocks.GLASS);
        door(r, 'N', 3, 3);
        doorSign(r, 'N', 3, "§6§lКАФЕ-БАР", "Напитки и закуски");
        ceilingLights(r, 4, Blocks.SHROOMLIGHT);
        int x = r.x0(), z = r.z0();
        fill(x + 2, by + 1, z + 8, x + 11, by + 1, z + 8, Blocks.DARK_OAK_PLANKS);
        npc(x + 6.5, z + 9.5, NpcRole.BARTENDER, 4, true, 0f);
        for (int tx = 3; tx <= 10; tx += 4) {
            set(x + tx, by + 1, z + 3, Blocks.OAK_FENCE);
            set(x + tx, by + 2, z + 3, Blocks.OAK_PRESSURE_PLATE);
            set(x + tx - 1, by + 1, z + 3, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            set(x + tx + 1, by + 1, z + 3, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        }
    }

    // ------------------------------------------------------------------ склад и гараж

    private void buildWarehouse() {
        Rect r = warehouse;
        shell(r, Blocks.IRON_BLOCK, Blocks.STONE, Blocks.SMOOTH_STONE);
        windows(r, by + 4, by + 5, Blocks.GLASS);
        door(r, 'N', 6, 4);
        doorSign(r, 'N', 4, "§e§lСКЛАД", "Грузчики нужны!");
        ceilingLights(r, 5, Blocks.SEA_LANTERN);
        int x = r.x0(), z = r.z0();
        // стеллажи с ящиками у дальней стены
        for (int cxo = 3; cxo <= 22; cxo += 3) {
            set(x + cxo, by + 1, z + 9, ModBlocks.CRATE_PILE.get());
            set(x + cxo, by + 2, z + 9, Blocks.BARREL);
            set(x + cxo, by + 1, z + 10, Blocks.OAK_PLANKS);
        }
        // зона разгрузки у входа
        fill(x + 10, by, z + 2, x + 15, by, z + 3, Blocks.YELLOW_CONCRETE);
        set(x + 11, by + 1, z + 2, ModBlocks.DELIVERY_POINT.get());
        set(x + 12, by + 1, z + 2, ModBlocks.DELIVERY_POINT.get());
        set(x + 13, by + 1, z + 2, ModBlocks.DELIVERY_POINT.get());
        sign(x + 12, by + 3, z + 1, Direction.SOUTH, "§lРАЗГРУЗКА", "ПКМ с ящиком");
        npc(x + 6.5, z + 4.5, NpcRole.FOREMAN, 6, true, 180f);
    }

    private void buildGarage() {
        Rect r = garage;
        shell(r, Blocks.LIGHT_GRAY_CONCRETE, Blocks.STONE, Blocks.SMOOTH_STONE);
        windows(r, by + 2, by + 3, Blocks.GLASS);
        door(r, 'N', 5, 4);
        doorSign(r, 'N', 4, "§7§lДОРОЖНАЯ СЛУЖБА", "Ремонт дорог");
        ceilingLights(r, 4, Blocks.GLOWSTONE);
        int x = r.x0(), z = r.z0();
        set(x + 3, by + 1, z + 6, Blocks.ANVIL);
        set(x + 4, by + 1, z + 6, Blocks.GRINDSTONE.defaultBlockState());
        set(x + 5, by + 1, z + 6, Blocks.SMITHING_TABLE);
        set(x + 10, by + 1, z + 6, ModBlocks.JOB_BOARD.get());
        npc(x + 8.5, z + 7.5, NpcRole.FOREMAN, 8, true, 0f);
    }

    private void buildHomes() {
        int idx = 0;
        for (Rect r : homes) {
            shell(r, idx % 2 == 0 ? Blocks.OAK_PLANKS : Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS, Blocks.COBBLESTONE);
            windows(r, by + 2, by + 3, Blocks.GLASS_PANE);
            door(r, 'N', 2, 3);
            doorSign(r, 'N', 3, "§lЖилой дом", "№ " + (idx + 1));
            set(r.x0() + 6, by + 1, r.z0() + 5, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT));
            set(r.x0() + 6, by + 1, r.z0() + 6, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.HEAD));
            set(r.x0() + 2, by + 1, r.z0() + 6, Blocks.CRAFTING_TABLE);
            set(r.x0() + 3, by + 1, r.z0() + 6, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
            set(r.x0() + 4, by + 4, r.z0() + 4, Blocks.LANTERN);
            npc(r.x0() + 4.5, r.z0() + 3.5, NpcRole.CITIZEN, idx * 2 + 1, false, 0f);
            idx++;
        }
    }

    // ------------------------------------------------------------------ казино

    private void casinoShell() {
        Rect r = casino;
        shell(r, Blocks.BLACK_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.BLACK_CONCRETE);
        // золотые колонны по углам и золотой пояс
        for (int[] c : new int[][]{{r.x0(), r.z0()}, {r.x1(), r.z0()}, {r.x0(), r.z1()}, {r.x1(), r.z1()}}) {
            fill(c[0], by + 1, c[1], c[0], by + r.h() + 1, c[1], Blocks.GOLD_BLOCK);
        }
        fill(r.x0(), by + r.h(), r.z0(), r.x1(), by + r.h(), r.z0(), Blocks.GOLD_BLOCK);
        fill(r.x0(), by + r.h(), r.z1(), r.x1(), by + r.h(), r.z1(), Blocks.GOLD_BLOCK);
        fill(r.x0(), by + r.h(), r.z0(), r.x0(), by + r.h(), r.z1(), Blocks.GOLD_BLOCK);
        fill(r.x1(), by + r.h(), r.z0(), r.x1(), by + r.h(), r.z1(), Blocks.GOLD_BLOCK);
        windows(r, by + 3, by + 7, Blocks.RED_STAINED_GLASS);
        door(r, 'S', 4, 5);
        doorSign(r, 'S', 5, "§6§l★ CASINO ★", "§eROYAL", "Фишки · Игры · Удача");
        // красный ковёр
        fill(r.x0() + 1, by + 1, r.z0() + 1, r.x1() - 1, by + 1, r.z1() - 1, Blocks.RED_CARPET);
        // разноцветное освещение потолка
        ceilingLights(r, 4, Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.SEA_LANTERN, Blocks.SHROOMLIGHT);
        // золотой пол-дорожка от входа
        fill(r.mx() - 1, by, r.z1() - 1, r.mx() + 1, by, r.z0() + 12, Blocks.GOLD_BLOCK);
        // колонны
        for (int px : new int[]{14, 30}) {
            for (int pz : new int[]{10, 20, 30}) {
                fill(r.x0() + px, by + 1, r.z0() + pz, r.x0() + px, by + r.h(), r.z0() + pz, Blocks.QUARTZ_PILLAR);
                set(r.x0() + px, by + 4, r.z0() + pz + 1, Blocks.SEA_LANTERN);
            }
        }
    }

    /** Стол 3x2 с сукном, крупье стоит за столом. */
    private void table(Rect r, int rx, int rz, Block game, int skin) {
        int x = r.x0() + rx, z = r.z0() + rz;
        fill(x - 1, by, z - 1, x + 3, by, z + 2, Blocks.GREEN_CONCRETE);
        fill(x, by + 1, z, x + 2, by + 1, z + 1, game);
        npc(x + 1.5, z - 0.5, NpcRole.CROUPIER, skin, true, 0f);
    }

    private void casinoTables() {
        Rect r = casino;
        // кости (2 стола)
        table(r, 8, 10, ModBlocks.DICE_TABLE.get(), 0);
        table(r, 8, 17, ModBlocks.DICE_TABLE.get(), 4);
        // американская рулетка
        table(r, 20, 10, ModBlocks.ROULETTE_TABLE.get(), 5);
        table(r, 20, 17, ModBlocks.ROULETTE_TABLE.get(), 6);
        // блэкджек
        table(r, 32, 10, ModBlocks.BLACKJACK_TABLE.get(), 2);
        table(r, 32, 16, ModBlocks.BLACKJACK_TABLE.get(), 7);
        table(r, 32, 22, ModBlocks.BLACKJACK_TABLE.get(), 8);
        // касса с фишками у входа
        int x = r.x0(), z = r.z0();
        fill(x + 10, by + 1, z + 33, x + 14, by + 1, z + 33, ModBlocks.CHIP_BOOTH.get());
        fill(x + 10, by + 2, z + 33, x + 14, by + 2, z + 33, Blocks.GLASS_PANE);
        npc(x + 12.5, z + 34.5, NpcRole.CROUPIER, 1, true, 180f);
        sign(x + 12, by + 4, z + 32, Direction.NORTH, "§lКАССА", "Фишки 1:1", "§7/chips buy <n>");
        // игровые автоматы вдоль западной стены
        for (int sz = 20; sz <= 34; sz += 2) set(x + 2, by + 1, z + sz, ModBlocks.SLOT_MACHINE.get());
        for (int sz = 20; sz <= 34; sz += 2) set(x + 2, by + 2, z + sz, Blocks.SEA_LANTERN);
        // банкомат в холле
        set(x + 3, by + 1, z + 36, ModBlocks.ATM.get());
    }

    private void casinoDecor() {
        Rect r = casino;
        int x = r.x0(), z = r.z0();
        // колесо фортуны на северной стене
        Block[] colors = {Blocks.RED_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.BLUE_CONCRETE,
                Blocks.ORANGE_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.CYAN_CONCRETE,
                Blocks.PINK_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE};
        int wx = x + 22, wy = by + 7, wz = z + 1;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 5.2) continue;
                if (dist < 1.2) { set(wx + dx, wy + dy, wz, Blocks.GOLD_BLOCK); continue; }
                double ang = Math.atan2(dy, dx) + Math.PI;
                int seg = (int) (ang / (2 * Math.PI) * 12) % 12;
                set(wx + dx, wy + dy, wz, dist > 4.4 ? Blocks.SEA_LANTERN.defaultBlockState() : colors[seg].defaultBlockState());
            }
        }
        set(wx, wy + 6, wz, Blocks.REDSTONE_BLOCK);
        fill(x + 21, by + 1, z + 5, x + 23, by + 1, z + 5, ModBlocks.WHEEL_OF_FORTUNE.get());
        fill(x + 20, by, z + 4, x + 24, by, z + 6, Blocks.GOLD_BLOCK);
        sign(x + 22, by + 3, z + 6, Direction.SOUTH, "§6§lКОЛЕСО", "§6§lФОРТУНЫ");
        npc(x + 22.5, z + 3.5, NpcRole.CROUPIER, 3, true, 0f);

        // ипподром: 6 дорожек
        Block[] horse = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.YELLOW_CONCRETE,
                Blocks.LIME_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.PURPLE_CONCRETE};
        for (int i = 0; i < 6; i++) {
            int lz = z + 25 + i * 2;
            fill(x + 26, by, lz, x + 41, by, lz, Blocks.LIGHT_GRAY_CONCRETE);
            set(x + 26, by, lz, horse[i]);
            set(x + 41, by, lz, i % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE);
            set(x + 40, by, lz, i % 2 == 0 ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE);
        }
        fill(x + 25, by + 1, z + 25, x + 25, by + 1, z + 36, Blocks.OAK_FENCE);
        set(x + 24, by + 1, z + 29, ModBlocks.HORSE_BOARD.get());
        set(x + 24, by + 1, z + 30, ModBlocks.HORSE_BOARD.get());
        set(x + 24, by + 2, z + 29, Blocks.SEA_LANTERN);
        set(x + 24, by + 2, z + 30, Blocks.SEA_LANTERN);
        sign(x + 24, by + 3, z + 29, Direction.WEST, "§lСКАЧКИ", "6 лошадей", "x2.6 ... x20");
        npc(x + 23.5, z + 31.5, NpcRole.CROUPIER, 6, true, 270f);

        // бар в северо-восточном углу
        fill(x + 36, by + 1, z + 4, x + 42, by + 1, z + 4, Blocks.QUARTZ_BLOCK);
        fill(x + 36, by + 2, z + 4, x + 42, by + 2, z + 4, Blocks.GOLD_BLOCK);
        npc(x + 39.5, z + 2.5, NpcRole.BARTENDER, 5, true, 0f);
    }

    // ------------------------------------------------------------------ ночной клуб

    private void buildClub() {
        Rect r = club;
        shell(r, Blocks.BLACK_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.BLACK_CONCRETE);
        windows(r, by + 3, by + 6, Blocks.PURPLE_STAINED_GLASS);
        // неоновая полоса по фасаду
        fill(r.x0(), by + r.h(), r.z0(), r.x1(), by + r.h(), r.z0(), Blocks.SEA_LANTERN);
        fill(r.x0(), by + r.h(), r.z1(), r.x1(), by + r.h(), r.z1(), Blocks.SEA_LANTERN);
        door(r, 'N', 4, 4);
        doorSign(r, 'N', 4, "§5§lНОЧНОЙ КЛУБ", "§dNEON", "Музыка до утра");
        ceilingLights(r, 5, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT, Blocks.OCHRE_FROGLIGHT);
    }

    private void clubDecor() {
        Rect r = club;
        int x = r.x0(), z = r.z0();
        paintDanceFloor(0);
        // диджейский пульт у южной стены
        fill(x + 14, by + 1, z + 26, x + 25, by + 1, z + 26, Blocks.JUKEBOX);
        fill(x + 14, by + 2, z + 26, x + 25, by + 2, z + 26, Blocks.NOTE_BLOCK);
        fill(x + 13, by + 1, z + 27, x + 26, by + 4, z + 27, Blocks.PURPLE_CONCRETE);
        npc(x + 19.5, z + 28.0, NpcRole.DJ, 3, true, 0f);
        // колонки
        for (int sx : new int[]{6, 33}) {
            fill(x + sx, by + 1, z + 24, x + sx + 1, by + 3, z + 25, Blocks.NOTE_BLOCK);
            fill(x + sx, by + 4, z + 24, x + sx + 1, by + 4, z + 25, Blocks.SEA_LANTERN);
        }
        // барная стойка
        fill(x + 34, by + 1, z + 4, x + 34, by + 1, z + 14, Blocks.QUARTZ_BLOCK);
        fill(x + 34, by + 2, z + 4, x + 34, by + 2, z + 14, Blocks.SEA_LANTERN);
        for (int bz = 5; bz <= 13; bz += 2) set(x + 33, by + 1, z + bz, Blocks.QUARTZ_SLAB);
        npc(x + 36.5, z + 9.5, NpcRole.BARTENDER, 4, true, 90f);
        // лаундж-зона: диваны
        for (int sz = 5; sz <= 17; sz += 4) {
            set(x + 3, by + 1, z + sz, Blocks.CRIMSON_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            set(x + 3, by + 1, z + sz + 1, Blocks.CRIMSON_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            set(x + 5, by + 1, z + sz, Blocks.DARK_OAK_PRESSURE_PLATE);
        }
        // шары над танцполом
        set(x + 20, by + r.h(), z + 15, Blocks.SEA_LANTERN);
        set(x + 20, by + r.h() - 1, z + 15, Blocks.GLASS);
        sign(x + 19, by + 3, z + 1, Direction.SOUTH, "§lДОБРО", "ПОЖАЛОВАТЬ");
        // посетители клуба
        for (int i = 0; i < 6; i++) {
            npc(x + 12 + i * 3, z + 12 + (i % 2) * 4, NpcRole.CITIZEN, i + 3, false, 0f);
        }
    }

    /** Танцпол: клетки 2x2 по кругу меняют цвет. Вызывается из тика, пока рядом есть игроки. */
    public void paintDanceFloor(int phase) {
        for (int cxl = 0; cxl < 10; cxl++) {
            for (int czl = 0; czl < 7; czl++) {
                Block b = CLUB_LAMPS[Math.floorMod(cxl + czl + phase, CLUB_LAMPS.length)];
                int x = danceX + cxl * 2, z = danceZ + czl * 2;
                BlockState s = b.defaultBlockState();
                set(x, by, z, s);
                set(x + 1, by, z, s);
                set(x, by, z + 1, s);
                set(x + 1, by, z + 1, s);
            }
        }
    }

    // ------------------------------------------------------------------ лесопосадка

    private void lumberCamp() {
        for (BlockPos base : trees) growTree(base);
        sign(cx + 8, by + 3, cz + 44, Direction.NORTH, "§2§lЛЕСОПОСАДКА", "Дровосекам: 4$/бревно");
        fill(cx + 8, by + 1, cz + 44, cx + 8, by + 2, cz + 44, Blocks.OAK_FENCE);
    }

    public void growTree(BlockPos base) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int top = base.getY() + 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 2; dy <= 5; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + (dy - 4) * (dy - 4) * 0.8);
                    if (d > 2.6) continue;
                    BlockPos p = new BlockPos(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    if (level.getBlockState(p).isAir()) level.setBlock(p, leaves, F);
                }
            }
        }
        for (int y = base.getY(); y <= top; y++) {
            level.setBlock(new BlockPos(base.getX(), y, base.getZ()), log, F);
        }
        level.setBlock(base.below(), Blocks.GRASS_BLOCK.defaultBlockState(), F);
    }

    public boolean inLumberZone(BlockPos p) {
        return p.getX() >= cx + 5 && p.getX() <= cx + 50 && p.getZ() >= cz + 42 && p.getZ() <= cz + 64;
    }

    /** После вырубки через 1 минуту отращиваем ближайшую пострадавшую пальму заново. */
    public void scheduleRegrow(BlockPos broken) {
        BlockPos best = null;
        double bd = 9999;
        for (BlockPos t : trees) {
            double d = Math.abs(t.getX() - broken.getX()) + Math.abs(t.getZ() - broken.getZ());
            if (d < bd) { bd = d; best = t; }
        }
        if (best == null || bd > 4) return;
        final BlockPos base = best;
        com.citymod.util.Sched.later(20 * 60, () -> growTree(base));
    }

    // ------------------------------------------------------------------ банкоматы, трещины, горожане

    private void atmsAndCracks() {
        int[][] spots = {{-6, -6}, {4, -6}, {-6, 4}, {4, 4}, {-40, 18}, {10, 4}};
        for (int[] s : spots) set(cx + s[0], by + 1, cz + s[1], ModBlocks.ATM.get());
        Random rnd = new Random(42);
        for (int i = 0; i < 6; i++) spawnCrack(rnd);
    }

    public void spawnCrack(Random rnd) {
        if (cracks >= 10) return;
        for (int attempt = 0; attempt < 20; attempt++) {
            int x, z;
            if (rnd.nextBoolean()) { x = cx - 60 + rnd.nextInt(121); z = cz - 3 + rnd.nextInt(6); }
            else { x = cx - 3 + rnd.nextInt(6); z = cz - 60 + rnd.nextInt(121); }
            BlockPos p = new BlockPos(x, by, z);
            if (level.getBlockState(p).is(Blocks.GRAY_CONCRETE)) {
                level.setBlock(p, ModBlocks.CRACKED_ROAD.get().defaultBlockState(), 3);
                cracks++;
                return;
            }
        }
    }

    public static void crackRepaired() { if (cracks > 0) cracks--; }

    private void citizens() {
        int[][] spots = {{-10, 0}, {8, 0}, {0, -12}, {0, 12}, {-25, 0}, {25, 0}, {-40, 20}, {-15, 20}};
        int skin = 0;
        for (int[] s : spots) {
            npc(cx + s[0] + 0.5, cz + s[1] + 0.5, NpcRole.CITIZEN, skin++, false, 0f);
        }
    }

    // ------------------------------------------------------------------ прочее

    public void teleportToSpawn(ServerPlayer p) {
        p.teleportTo(level, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f);
    }

    /** "casino", "club" или "" в зависимости от того, где стоит игрок. */
    public String zoneAt(double x, double y, double z) {
        if (y < by - 2 || y > by + 25) return "";
        if (casino.contains(x, z)) return "casino";
        if (club.contains(x, z)) return "club";
        return "";
    }

    public Rect rectOf(String zone) {
        return zone.equals("casino") ? casino : club;
    }
}
