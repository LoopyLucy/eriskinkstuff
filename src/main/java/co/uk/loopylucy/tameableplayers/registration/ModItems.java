package co.uk.loopylucy.tameableplayers.registration;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.common.item.*;
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

@SuppressWarnings("unused")
public class ModItems {
    /** The registry for items, keyed by the mod ID. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TameablePlayers.MODID);


    /** The Collar curio item. Used to enable leashing on players. */
    public static final DeferredItem<CollarItem> COLLAR = ITEMS.register("collar", CollarItem::new);

    public static final DeferredItem<Item> SILVER_RING = ITEMS.register("silver_ring",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> GOLD_RING = ITEMS.register("gold_ring",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SILVER_PENDANT = ITEMS.register("silver_pendant",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> GOLD_PENDANT = ITEMS.register("gold_pendant",
            () -> new Item(new Item.Properties().stacksTo(1)));

    
    /** The Mittens curio item. Prevents players from holding items. */
    public static final DeferredItem<MittensItem> MITTENS = ITEMS.register("mittens", MittensItem::new);
    
    /** The Clicker item. Used to command collared players to look at the user. */
    public static final DeferredItem<ClickerItem> CLICKER = ITEMS.register("clicker", ClickerItem::new);

    /** The Catsuit item. A cosmetic skin replacement style item. */
    public static final DeferredItem<LatexBodysuitItem> LATEX_BODYSUIT = ITEMS.register("latex_bodysuit", LatexBodysuitItem::new);

    /** The Blindfold. Blinds the player like a pumpkin. */
    public static final DeferredItem<BlindfoldItem>  BLINDFOLD = ITEMS.register("blindfold", BlindfoldItem::new);

    /** The Whip weapon. */
    public static final DeferredItem<WhipItem> WHIP = ITEMS.register("whip", WhipItem::new);

    /** Cat ears item. A cosmetic item based on the player's hair colour */
    public static  final DeferredItem<CatEarsItem> CAT_EARS = ITEMS.register("cat_ears", CatEarsItem::new);

    /** 
     * The Pet Bed block item. 
     * Includes a default white dyed colour component for colour consistency.
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