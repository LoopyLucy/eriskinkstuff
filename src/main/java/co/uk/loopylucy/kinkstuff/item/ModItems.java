package co.uk.loopylucy.kinkstuff.item;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.item.items.CollarItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.theillusivec4.curios.api.CuriosApi;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ErisKinkStuff.MODID);

    public static final DeferredItem<CollarItem> COLLAR = ITEMS.register("collar", CollarItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
