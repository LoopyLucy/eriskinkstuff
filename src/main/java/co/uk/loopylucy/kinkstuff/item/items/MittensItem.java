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

        // 1. Get the entity and verify it is a player on the logical Server
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide()) {
            int selectedSlot = player.getInventory().selected;

            // 2. Process and clear Main Hand
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                ItemStack itemToMove = mainHand.copy();

                // Clear the active hand slot immediately to avoid race conditions
                player.getInventory().setItem(selectedSlot, ItemStack.EMPTY);

                // Move item safely while avoiding the active hand slot
                moveItemSafely(player, itemToMove, selectedSlot);
                player.inventoryMenu.broadcastChanges();
            }

            // 3. Process and clear Off Hand
            ItemStack offHand = player.getOffhandItem();
            if (!offHand.isEmpty()) {
                ItemStack itemToMove = offHand.copy();

                // Slot 40 is the hardcoded off-hand inventory index
                player.getInventory().setItem(40, ItemStack.EMPTY);

                moveItemSafely(player, itemToMove, selectedSlot);
                player.inventoryMenu.broadcastChanges();
            }
        }
    }

    // THE FIXED MOVEMENT LOGIC: Stops item destruction and hand-flashing
    // UPDATED MOVEMENT LOGIC: Prioritizes the hotbar line before using the backpack bag
    private static void moveItemSafely(Player player, ItemStack stack, int activeHotbarSlot) {
        // Step A: Merge into existing matching stacks first (skipping the active hand slot)
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

        // Step B: NOW RUNS FIRST - Put items into empty INACTIVE hotbar slots (0-8)
        for (int i = 0; i < 9; i++) {
            if (i == activeHotbarSlot) continue; // Skip the active selection dead-zone
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

        // Step C: NOW RUNS SECOND - Use empty main inventory backpack slots (9-35) if hotbar is full
        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
                stack.setCount(0);
                return;
            }
        }

        // Step D: Ground drop safety backup if inventory is entirely packed solid
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }
}