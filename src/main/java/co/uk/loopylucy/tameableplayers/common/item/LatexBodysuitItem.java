package co.uk.loopylucy.tameableplayers.common.item;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.registration.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The Latex Bodysuit curio item.
 * A cosmetic dyeable item that "replaces" the player skin with a simple "latex" texture.
 * Currently labelled as leather due to vanilla resources.
 */
public class LatexBodysuitItem extends Item implements ICurioItem {
    public LatexBodysuitItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFFFF, false))
                .component(ModDataComponents.OVERLAY_INFO.get(), new ModDataComponents.OverlayInfo(TameablePlayers.MODID + ":textures/entity/latex_bodysuit.png", 0xFFbdbdbd, true))
        );
    }

    /**
     * Gets the dye colour of the catsuit.
     *
     * @param stack The catsuit ItemStack.
     * @return The packed RGB integer colour.
     */
    public int getColour(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFFFFFFFF;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // No periodic logic currently required for the catsuit
    }
}
