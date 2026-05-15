package co.uk.loopylucy.kinkstuff.item;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.block.ModBlocks;
import co.uk.loopylucy.kinkstuff.item.items.ClickerItem;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
import co.uk.loopylucy.kinkstuff.item.items.MittensItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ErisKinkStuff.MODID);

    public static final DeferredItem<CollarItem> COLLAR = ITEMS.register("collar", CollarItem::new);
    public static final DeferredItem<MittensItem> MITTENS = ITEMS.register("mittens", MittensItem::new);
    public static final DeferredItem<ClickerItem> CLICKER = ITEMS.register("clicker", ClickerItem::new);

    //Block Items
    public static final DeferredItem<Item> PET_BED = ITEMS.register("pet_bed",
            () -> new BlockItem(ModBlocks.PET_BED.get(), new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF, true))
            )
    );

    public static void register(IEventBus eventBus) { ITEMS.register(eventBus); }
}