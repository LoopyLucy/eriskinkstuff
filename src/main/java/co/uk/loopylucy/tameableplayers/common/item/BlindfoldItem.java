package co.uk.loopylucy.tameableplayers.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Blindfold curio item.
 * When equipped in the 'Head' Curios slot, it blinds the player.
 */
public class BlindfoldItem extends Item implements ICurioItem {

    public BlindfoldItem() {
        super(new Properties()
                .stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFFFF, false))
        );
    }

    /**
     * Gets the dye colour of the blindfold.
     * 
     * @param stack The blindfold ItemStack.
     * @return The packed RGB integer colour.
     */
    public int getColour(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFFFFFFFF;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // No periodic logic currently required for the blindfold
    }
}