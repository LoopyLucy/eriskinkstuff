package co.uk.loopylucy.kinkstuff.item;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
import co.uk.loopylucy.kinkstuff.item.items.MittensItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ErisKinkStuff.MODID);

    public static final DeferredItem<CollarItem> COLLAR = ITEMS.register("collar", CollarItem::new);
    public static final DeferredItem<MittensItem> MITTENS = ITEMS.register("mittens", MittensItem::new);

    public static void register(IEventBus eventBus) { ITEMS.register(eventBus); }
}