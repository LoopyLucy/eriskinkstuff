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

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
