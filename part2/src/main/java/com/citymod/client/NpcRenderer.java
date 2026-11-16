package com.citymod.client;

import com.citymod.entity.CityNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class NpcRenderer extends HumanoidMobRenderer<CityNpc, HumanoidModel<CityNpc>> {
    private static final String[] SKINS = {
            "steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri"
    };

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(CityNpc npc) {
        String skin = SKINS[Math.floorMod(npc.getSkin(), SKINS.length)];
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/" + skin + ".png");
    }
}
