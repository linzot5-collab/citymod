package com.citymod.block;

import com.citymod.CityMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Функциональные блоки города. Неразрушимы в выживании (как бедрок), ломаются в творческом режиме. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CityMod.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CityMod.MODID);

    public static final Map<String, RegistryObject<Block>> ALL = new LinkedHashMap<>();

    public static final RegistryObject<Block> ATM = reg("atm", Actions::atm);
    public static final RegistryObject<Block> BANK_DESK = reg("bank_desk", Actions::bankDesk);
    public static final RegistryObject<Block> BANK_VAULT = reg("bank_vault", Actions::bankVault);
    public static final RegistryObject<Block> JOB_BOARD = reg("job_board", Actions::jobBoard);
    public static final RegistryObject<Block> CRATE_PILE = reg("crate_pile", Actions::cratePile);
    public static final RegistryObject<Block> DELIVERY_POINT = reg("delivery_point", Actions::deliveryPoint);
    public static final RegistryObject<Block> CRACKED_ROAD = reg("cracked_road", Actions::crackedRoad);
    public static final RegistryObject<Block> CHIP_BOOTH = reg("chip_booth", Actions::chipBooth);
    public static final RegistryObject<Block> ROULETTE_TABLE = reg("roulette_table", Actions::rouletteTable);
    public static final RegistryObject<Block> BLACKJACK_TABLE = reg("blackjack_table", Actions::blackjackTable);
    public static final RegistryObject<Block> DICE_TABLE = reg("dice_table", Actions::diceTable);
    public static final RegistryObject<Block> WHEEL_OF_FORTUNE = reg("wheel_of_fortune", Actions::wheel);
    public static final RegistryObject<Block> HORSE_BOARD = reg("horse_board", Actions::horseBoard);
    public static final RegistryObject<Block> SLOT_MACHINE = reg("slot_machine", Actions::slotMachine);

    private ModBlocks() {}

    private static RegistryObject<Block> reg(String name, BlockAction action) {
        RegistryObject<Block> o = BLOCKS.register(name, () -> new ActionBlock(
                BlockBehaviour.Properties.of()
                        .strength(-1.0f, 3600000.0f)
                        .sound(SoundType.METAL)
                        .lightLevel(s -> name.equals("cracked_road") ? 0 : 7),
                action));
        BLOCK_ITEMS.register(name, () -> new BlockItem(o.get(), new Item.Properties()));
        ALL.put(name, o);
        return o;
    }
}
