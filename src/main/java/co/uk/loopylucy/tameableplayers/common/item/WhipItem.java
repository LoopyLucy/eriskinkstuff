package co.uk.loopylucy.tameableplayers.common.item;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.registration.ModDataComponents;
import co.uk.loopylucy.tameableplayers.registration.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The Whip weapon.
 * Features an extended entity interaction range (reach) and plays a cracking
 * sound when swung, delaying its damage until the audio/animation finishes.
 */
public class WhipItem extends Item {

    public WhipItem() {
        super(new Properties()
                .stacksTo(1)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "whip_damage"),
                                4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "whip_speed"),
                                -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, "whip_reach"),
                                3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build())
                .component(ModDataComponents.WHIP_STATUS.get(), 0.0F)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(0xFF86644C, false))
        );
    }

    public int getColour(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFF86644C;
    }

    @Override
    public boolean onEntitySwing(@NotNull ItemStack stack, @NotNull LivingEntity entity, @NotNull InteractionHand hand) {
        if (entity instanceof Player player) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return true;
            }
            player.getCooldowns().addCooldown(this, 20);
            stack.set(ModDataComponents.WHIP_STATUS.get(), 11.0F);
        }

        if (!entity.level().isClientSide) {
            entity.level().playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.WHIP_CRACK.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
        return false;
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity entity) {
        return true;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            if (isSelected) {
                float currentStatus = stack.getOrDefault(ModDataComponents.WHIP_STATUS.get(), 0.0F);
                if (currentStatus > 0.0F) {
                    float nextStatus = currentStatus - 1.0F;
                    stack.set(ModDataComponents.WHIP_STATUS.get(), nextStatus);

                    if (nextStatus == 0.0F) {
                        performDelayedAttack(level, player);
                    }
                }
            } else {
                if (stack.getOrDefault(ModDataComponents.WHIP_STATUS.get(), 0.0F) > 0.0F) {
                    stack.set(ModDataComponents.WHIP_STATUS.get(), 0.0F);
                }
            }
        }
    }

    private void performDelayedAttack(Level level, Player player) {
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(reach)).inflate(1.0D);

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos, searchBox,
                target -> !target.isSpectator() && target.isPickable()
        );

        if (hitResult != null) {
            Entity target = hitResult.getEntity();
            float damageAmount = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

            target.hurt(level.damageSources().playerAttack(player), damageAmount);

            if (target instanceof LivingEntity livingTarget) {
                livingTarget.knockback(0.4F,
                        Math.sin(player.getYRot() * (Math.PI / 180)),
                        -Math.cos(player.getYRot() * (Math.PI / 180))
                );
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @EventBusSubscriber(modid = TameablePlayers.MODID)
    public static class WhipModEvents {
        @SubscribeEvent
        public static void onLivingTick(EntityTickEvent.Post event) {
            if (event.getEntity() instanceof LivingEntity livingEntity){
                if (livingEntity.swinging && livingEntity.getMainHandItem().getItem() instanceof WhipItem) {
                    if (livingEntity.tickCount % 2 == 0 && livingEntity.swingTime > 0) {
                        livingEntity.swingTime--;
                    }
                }
            }
        }
    }

    @EventBusSubscriber(modid = TameablePlayers.MODID, value = Dist.CLIENT)
    public static class WhipClientEvents {
        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (event.isAttack()) {
                Minecraft mc = Minecraft.getInstance();

                if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof WhipItem whip) {
                    if (mc.player.getCooldowns().isOnCooldown(whip)) {
                        event.setCanceled(true);
                        event.setSwingHand(false);
                    }
                }
            }
        }
    }
}