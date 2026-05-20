package co.uk.loopylucy.kinkstuff.item.items;

import co.uk.loopylucy.kinkstuff.init.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class LatexBodysuit extends Item implements ICurioItem {
    public LatexBodysuit() {
        super(new Item.Properties()
                .stacksTo(1)
                .component(ModComponents.OVERLAY_INFO.get(), new ModComponents.OverlayInfo("eriskinkstuff:textures/entity/latex_bodysuit.png", 0xFFbdbdbd, true))
        );
    }

    /**
     * Gets the dye color of the collar.
     *
     * @param stack The collar ItemStack.
     * @return The packed RGB integer color.
     */
    public int getColor(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFFFFFFFF;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // No periodic logic currently required for the collar
    }
}
