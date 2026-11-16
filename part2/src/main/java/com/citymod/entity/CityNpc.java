package com.citymod.entity;

import com.citymod.bank.BankQueue;
import com.citymod.network.Net;
import com.citymod.network.OpenMenuPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Городской NPC: работает на месте или гуляет около дома, неуязвим, разговаривает по правому клику. */
public class CityNpc extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ROLE =
            SynchedEntityData.defineId(CityNpc.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(CityNpc.class, EntityDataSerializers.INT);

    private BlockPos home = null;
    private int homeRadius = 0;

    public CityNpc(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.6));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(SKIN, 0);
    }

    public NpcRole getRole() { return NpcRole.byId(entityData.get(ROLE)); }
    public int getSkin() { return entityData.get(SKIN); }

    public void setRole(NpcRole role) {
        entityData.set(ROLE, role.ordinal());
        setCustomName(Component.literal(role.title));
        setCustomNameVisible(true);
    }

    public void setSkin(int skin) { entityData.set(SKIN, skin); }

    /** Дом NPC: если радиус > 0, NPC гуляет только рядом с ним. */
    public void setHome(BlockPos pos, int radius) {
        this.home = pos;
        this.homeRadius = radius;
        if (radius > 0) restrictTo(pos, radius);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            NpcRole role = getRole();
            sp.sendSystemMessage(Component.literal("§6[" + role.title + "] §f" + role.randomLine(random)));
            if (role == NpcRole.BANKER) {
                BankQueue.join(sp);
            } else if (role.menu != null) {
                Net.toPlayer(sp, new OpenMenuPacket(role.menu));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** NPC неуязвимы к обычному урону (кроме творческого режима и /kill). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", entityData.get(ROLE));
        tag.putInt("Skin", entityData.get(SKIN));
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
            tag.putInt("HomeR", homeRadius);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ROLE, tag.getInt("Role"));
        entityData.set(SKIN, tag.getInt("Skin"));
        if (tag.contains("HomeX")) {
            setHome(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), tag.getInt("HomeR"));
        }
    }
}
