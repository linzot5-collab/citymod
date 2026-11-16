package com.citymod;

import com.citymod.block.ModBlocks;
import com.citymod.city.CityEvents;
import com.citymod.client.HudOverlay;
import com.citymod.client.NpcRenderer;
import com.citymod.casino.CasinoCommands;
import com.citymod.command.ModCommands;
import com.citymod.command.NpcCommand;
import com.citymod.entity.CityNpc;
import com.citymod.crypto.CryptoCommands;
import com.citymod.item.BankCardItem;
import com.citymod.item.PhoneItem;
import com.citymod.network.Net;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(CityMod.MODID)
public class CityMod {
    public static final String MODID = "citymod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> BANK_CARD = ITEMS.register("bank_card",
            () -> new BankCardItem(new Item.Properties()));

    public static final RegistryObject<Item> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties()));

    public static final RegistryObject<EntityType<CityNpc>> CITY_NPC = ENTITIES.register("city_npc",
            () -> EntityType.Builder.of(CityNpc::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("city_npc"));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.literal("City Mod"))
                    .icon(() -> new ItemStack(BANK_CARD.get()))
                    .displayItems((params, out) -> {
                        out.accept(BANK_CARD.get());
                        out.accept(PHONE.get());
                        ModBlocks.ALL.values().forEach(b -> out.accept(b.get()));
                    })
                    .build());

    public CityMod(FMLJavaModLoadingContext context) {
        var modBus = context.getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.BLOCK_ITEMS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        TABS.register(modBus);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Net::init));
        modBus.addListener(CityMod::onAttributes);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CityEvents());
    }

    private static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(CITY_NPC.get(), CityNpc.createAttributes().build());
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        ModCommands.register(e.getDispatcher());
        NpcCommand.register(e.getDispatcher());
        CasinoCommands.register(e.getDispatcher());
        CryptoCommands.register(e.getDispatcher());
    }

    /** Клиентская часть: рендер NPC и панель справа. */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(CITY_NPC.get(), NpcRenderer::new);
        }

      
    }
}
