package co.uk.loopylucy.kinkstuff.block.entity;

import co.uk.loopylucy.kinkstuff.init.ModBlockEntities;
import co.uk.loopylucy.kinkstuff.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PetBedBlockEntity extends BlockEntity {
    private int customColour = 0xFFFFFFFF;

    public PetBedBlockEntity(BlockPos pos, BlockState state) {
        // Pointing directly to your custom registry holder block
        super(ModBlockEntities.PET_BED_BE.get(), pos, state);
    }

    public int getCustomColour() {
        return this.customColour;
    }

    public void setCustomColour(int newColour) {
        this.customColour = newColour;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    // 1. DATA COMPONENT HANDLING (Minecraft 1.21.1 Native)
    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        DyedItemColor componentColor = input.get(DataComponents.DYED_COLOR);
        if (componentColor != null) {
            this.customColour = componentColor.rgb();
        }
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.DYED_COLOR, new DyedItemColor(this.customColour, true));
    }

    // 2. NBT STORAGE FALLBACKS (Restored Disk IO Systems)
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("BedColour")) {
            this.customColour = tag.getInt("BedColour");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BedColour", this.customColour);
    }

    // 3. SERVER-TO-CLIENT PACKET SYNCING (Restored Network Systems)
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("BedColour", this.customColour);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("BedColor")) {
            this.customColour = tag.getInt("BedColor");
            if (this.level != null && this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }
}
