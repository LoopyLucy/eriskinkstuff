package co.uk.loopylucy.kinkstuff.item.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class MittensItem extends Item implements ICurioItem {

    public MittensItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(0xFFFFFFFF, false))
        );
    }

    public int getColor(ItemStack stack) {
        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        return dyedItemColor != null ? dyedItemColor.rgb() : 0xFFFFFFFF;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext == null || stack.isEmpty() || slotContext.entity() == null) {
            return;
        }

        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            int selectedSlot = player.getInventory().selected;

            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                ItemStack itemToMove = mainHand.copy();

                player.getInventory().setItem(selectedSlot, ItemStack.EMPTY);

                moveItemSafely(player, itemToMove, selectedSlot);
                player.inventoryMenu.broadcastChanges();
            }

            ItemStack offHand = player.getOffhandItem();
            if (!offHand.isEmpty()) {
                ItemStack itemToMove = offHand.copy();

                player.getInventory().setItem(40, ItemStack.EMPTY);

                moveItemSafely(player, itemToMove, selectedSlot);
                player.inventoryMenu.broadcastChanges();
            }
        }
    }

    private static void moveItemSafely(Player player, ItemStack stack, int activeHotbarSlot) {
        for (int i = 0; i < 36; i++) {
            if (i == activeHotbarSlot) continue;

            ItemStack target = player.getInventory().getItem(i);
            if (!target.isEmpty() && ItemStack.isSameItemSameComponents(target, stack)) {
                int maxStackSize = Math.min(target.getMaxStackSize(), player.getInventory().getMaxStackSize());
                int remainingSpace = maxStackSize - target.getCount();

                if (remainingSpace > 0) {
                    int toAdd = Math.min(remainingSpace, stack.getCount());
                    target.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
            if (stack.isEmpty()) return;
        }

        for (int i = 0; i < 9; i++) {
            if (i == activeHotbarSlot) continue;
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }
}