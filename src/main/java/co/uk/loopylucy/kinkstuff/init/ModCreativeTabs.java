package co.uk.loopylucy.kinkstuff.init;

import co.uk.loopylucy.kinkstuff.item.ModItems;
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
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "eriskinkstuff");

    /** 
     * The main creative tab for Eri's Kink Stuff.
     * Uses the Collar as an icon and includes all modded items plus the Lead.
     */
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KINKSTUFF_TAB =
            CREATIVE_TABS.register("kinkstuff_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COLLAR.get()))
                    .title(Component.translatable("creativetab.eriskinkstuff.tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COLLAR);
                        output.accept(Items.LEAD);
                        output.accept(ModItems.MITTENS);
                        output.accept(ModItems.CLICKER);
                        output.accept(ModItems.PET_BED);
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
