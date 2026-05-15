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

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "eriskinkstuff");

    // 2. Build and configure your custom menu tab layout
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KINKSTUFF_TAB =
            CREATIVE_TABS.register("kinkstuff_tab", () -> CreativeModeTab.builder()
                    // Set your pet bed item as the primary display icon for the tab header grid
                    .icon(() -> new ItemStack(ModItems.COLLAR.get()))
                    // Sets the language translation reference string key
                    .title(Component.translatable("creativetab.eriskinkstuff.tab"))
                    // Inject your custom blocks and item entries cleanly into the tab list
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COLLAR);
                        output.accept(Items.LEAD);
                        output.accept(ModItems.MITTENS);
                        output.accept(ModItems.CLICKER);
                        output.accept(ModItems.PET_BED);
                    })
                    .build());

    // 3. Initialize registration handler method
    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
