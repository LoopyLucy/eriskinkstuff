package co.uk.loopylucy.kinkstuff.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Collar curio item.
 * When equipped in the 'Necklace' Curios slot, it allows other players to 
 * leash the wearer using a Lead.
 */
public class CollarItem extends Item implements ICurioItem {

    public CollarItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(0xFF86644C, false))
        );
    }

    /**
     * Gets the dye colour of the collar.
     * 
     * @param stack The collar ItemStack.
     * @return The packed RGB integer colour.
     */
    public int getColour(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFF86644C;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // No periodic logic currently required for the collar
    }
}