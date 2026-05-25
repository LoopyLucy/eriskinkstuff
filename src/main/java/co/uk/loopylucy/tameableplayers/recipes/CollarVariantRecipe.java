package co.uk.loopylucy.tameableplayers.recipes;

import co.uk.loopylucy.tameableplayers.registration.ModItems;
import co.uk.loopylucy.tameableplayers.registration.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * A custom crafting recipe for adding accessories (rings, pendants, spikes) to a collar.
 * It preserves the dye color of the collar while updating its CustomModelData to 
 * change its appearance.
 */
public class CollarVariantRecipe extends CustomRecipe {
    public CollarVariantRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        int collarCount = 0;
        int goldPendantCount = 0;
        int silverPendantCount = 0;
        int goldRingCount = 0;
        int silverRingCount = 0;
        int goldNuggetCount = 0;
        int ironNuggetCount = 0;
        int otherCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.COLLAR.get())) {
                collarCount++;
            } else if (stack.is(ModItems.GOLD_PENDANT.get())) {
                goldPendantCount++;
            } else if (stack.is(ModItems.SILVER_PENDANT.get())) {
                silverPendantCount++;
            } else if (stack.is(ModItems.GOLD_RING.get())) {
                goldRingCount++;
            } else if (stack.is(ModItems.SILVER_RING.get())) {
                silverRingCount++;
            } else if (stack.is(Items.GOLD_NUGGET)) {
                goldNuggetCount++;
            } else if (stack.is(Items.IRON_NUGGET)) {
                ironNuggetCount++;
            } else {
                otherCount++;
            }
        }

        if (collarCount != 1 || otherCount > 0) return false;

        // Valid combinations:
        // 1. Collar + 1 Ring/Pendant (item-based upgrade)
        // 2. Collar + 4 Gold Nuggets -> Gold Spikes (CMD 5)
        // 3. Collar + 4 Iron Nuggets -> Silver Spikes (CMD 6)

        int accessoryCount = goldPendantCount + silverPendantCount + goldRingCount + silverRingCount;
        
        if (accessoryCount == 1 && goldNuggetCount == 0 && ironNuggetCount == 0) return true;
        
        if (accessoryCount == 0) {
            if (goldNuggetCount == 4 && ironNuggetCount == 0) return true;
            if (ironNuggetCount == 4 && goldNuggetCount == 0) return true;
        }

        return false;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        ItemStack collar = findCollar(input).copy();
        int goldPendantCount = 0;
        int silverPendantCount = 0;
        int goldRingCount = 0;
        int silverRingCount = 0;
        int goldNuggetCount = 0;
        int ironNuggetCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.GOLD_PENDANT.get())) goldPendantCount++;
            else if (stack.is(ModItems.SILVER_PENDANT.get())) silverPendantCount++;
            else if (stack.is(ModItems.GOLD_RING.get())) goldRingCount++;
            else if (stack.is(ModItems.SILVER_RING.get())) silverRingCount++;
            else if (stack.is(Items.GOLD_NUGGET)) goldNuggetCount++;
            else if (stack.is(Items.IRON_NUGGET)) ironNuggetCount++;
        }

        int newCMD = 0;

        if (goldRingCount == 1) newCMD = 3;
        else if (silverRingCount == 1) newCMD = 4;
        else if (goldPendantCount == 1) newCMD = 1;
        else if (silverPendantCount == 1) newCMD = 2;
        else if (goldNuggetCount == 4) newCMD = 5;
        else if (ironNuggetCount == 4) newCMD = 6;

        if (newCMD > 0) {
            collar.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(newCMD));
        }

        return collar;
    }

    private ItemStack findCollar(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(ModItems.COLLAR.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    private int getCustomModelData(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data != null ? data.value() : 0;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5; // Minimum for 1 collar + 4 nuggets
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.COLLAR_VARIANT_CRAFTING.get();
    }
}