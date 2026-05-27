package co.uk.loopylucy.tameableplayers.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Handles the registration of custom Creative Mode tabs for the mod.
 * This ensures all modded items are easily accessible in a single location 
 * within the creative inventory.
 */
public class ModCreativeTabs {
    /** The registry for creative tabs. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "tameableplayers");

    /** 
     * The main creative tab for Tameable Players.
     * Uses the Collar as an icon and includes all modded items plus the Lead.
     */
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAMEABLEPLAYERS_TAB =
            CREATIVE_TABS.register("tameableplayers_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COLLAR.get()))
                    .title(Component.translatable("creativetab.tameableplayers.tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COLLAR);
                        output.accept(Items.LEAD);
                        output.accept(ModItems.LATEX_BODYSUIT);
                        output.accept(ModItems.BLINDFOLD);
                        output.accept(ModItems.CAT_EARS);
                        output.accept(ModItems.MITTENS);
                        output.accept(ModItems.CLICKER);
                        output.accept(ModItems.PET_BED);
                        output.accept(ModItems.SILVER_RING);
                        output.accept(ModItems.SILVER_PENDANT);
                        output.accept(ModItems.GOLD_RING);
                        output.accept(ModItems.GOLD_PENDANT);
                    })
                    .build());

    /**
     * Entry point for registering creative tabs to the mod event bus.
     * 
     * @param eventBus The mod event bus.
     */
    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
