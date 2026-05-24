package co.uk.loopylucy.tameableplayers.common.block.entity;

import co.uk.loopylucy.tameableplayers.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * The block entity for the Pet Bed.
 * Handles storing, synchronizing, and applying custom dye colours to the bed block.
 */
public class PetBedBlockEntity extends BlockEntity {
    /** The custom colour of the bed, stored as a packed RGB integer. Default is white. */
    private int customColour = 0xFFFFFFFF;

    public PetBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PET_BED_BE.get(), pos, state);
    }

    /**
     * Gets the current custom colour of the bed.
     * 
     * @return The packed RGB integer colour.
     */
    public int getCustomColour() {
        return this.customColour;
    }

    /**
     * Sets the custom colour and marks the block entity for synchronization.
     * 
     * @param newColour The new packed RGB colour.
     */
    public void setCustomColour(int newColour) {
        this.customColour = newColour;
        this.setChanged();
        if (this.level != null) {
            // Update the block state and notify clients
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /**
     * Applies implicit components (like DyedItemColor) to the block entity's state.
     * This allows the block to inherit colours from its item form.
     */
    @Override
    protected void applyImplicitComponents(BlockEntity.@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);
        DyedItemColor componentColor = input.get(DataComponents.DYED_COLOR);
        if (componentColor != null) {
            this.customColour = componentColor.rgb();
        }
    }

    /**
     * Collects implicit components from the block entity's state.
     * This allows the block to preserve its colour when broken and turned back into an item.
     */
    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.DYED_COLOR, new DyedItemColor(this.customColour, true));
    }

    /**
     * Loads the block entity's data from NBT.
     */
    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BedColour")) {
            this.customColour = tag.getInt("BedColour");
        }
    }

    /**
     * Saves the block entity's data to NBT.
     */
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BedColour", this.customColour);
    }

    /**
     * Provides the NBT tag used for initial chunk synchronization.
     */
    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("BedColour", this.customColour);
        return tag;
    }

    /**
     * Provides the packet used for synchronizing block entity data to clients.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Handles data packets received from the server on the client side.
     */
    @Override
    public void onDataPacket(@NotNull Connection net, @NotNull ClientboundBlockEntityDataPacket pkt, HolderLookup.@NotNull Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag.contains("BedColor")) {
            this.customColour = tag.getInt("BedColor");
            if (this.level != null && this.level.isClientSide) {
                // Force a block update to trigger re-rendering with the new colour
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }
}