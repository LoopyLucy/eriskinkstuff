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

/**
 * Central registry for all modded items.
 * This class uses NeoForge's DeferredRegister system to ensure items are registered 
 * in the correct order and at the correct time during mod loading.
 */
public class ModItems {
    /** The registry for items, keyed by the mod ID. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ErisKinkStuff.MODID);

    /** The Collar curio item. Used to enable leashing on players. */
    public static final DeferredItem<CollarItem> COLLAR = ITEMS.register("collar", CollarItem::new);
    public static final DeferredItem<CollarItem> COLLAR_TEST = ITEMS.register("collar_test", CollarItem::new);
    
    /** The Mittens curio item. Prevents players from holding items. */
    public static final DeferredItem<MittensItem> MITTENS = ITEMS.register("mittens", MittensItem::new);
    
    /** The Clicker item. Used to command collared players to look at the user. */
    public static final DeferredItem<ClickerItem> CLICKER = ITEMS.register("clicker", ClickerItem::new);

    /** 
     * The Pet Bed block item. 
     * Includes a default white dyed color component for color consistency.
     */
    public static final DeferredItem<Item> PET_BED = ITEMS.register("pet_bed",
            () -> new BlockItem(ModBlocks.PET_BED.get(), new Item.Properties()
                    .stacksTo(1)
                    .component(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0xFFFFFF, false))
            )
    );

    /**
     * Entry point for registering the items to the mod event bus.
     * 
     * @param eventBus The mod event bus from the main mod class.
     */
    public static void register(IEventBus eventBus) { ITEMS.register(eventBus); }
}