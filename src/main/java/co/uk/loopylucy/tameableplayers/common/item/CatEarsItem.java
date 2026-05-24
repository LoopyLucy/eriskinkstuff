package co.uk.loopylucy.tameableplayers.common.item;

import co.uk.loopylucy.tameableplayers.common.component.ColourData;
import co.uk.loopylucy.tameableplayers.registration.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Cat Ears curio item.
 * A purely aesthetic item.
 */
public class CatEarsItem extends Item implements ICurioItem {
    public CatEarsItem() {
        super(new Properties()
                .stacksTo(1)
                .component(ModDataComponents.ITEM_COLOURS, new ColourData(0xFFFFFF, 0xFFFFFF))
        );
    }

    /**
     * Gets the first dye colour of the cat ears.
     *
     * @param stack The cat ears ItemStack.
     * @return The packed RGB integer colour.
     */
    public int getColour0(ItemStack stack) {
        ColourData colourData = stack.get(ModDataComponents.ITEM_COLOURS.get());
        return (colourData != null) ? colourData.colour0() : 0xFFFFFF;
    }

    /**
     * Gets the second dye colour of the cat ears.
     *
     * @param stack The cat ears ItemStack.
     * @return The packed RGB integer colour.
     */
    public int getColour1(ItemStack stack) {
        ColourData colourData = stack.get(ModDataComponents.ITEM_COLOURS.get());
        return (colourData != null) ? colourData.colour1() : 0xFFFFFF;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {}
}
