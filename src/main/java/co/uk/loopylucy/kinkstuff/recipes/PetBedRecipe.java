package co.uk.loopylucy.kinkstuff.recipes;

import co.uk.loopylucy.kinkstuff.registration.ModRecipes;
import co.uk.loopylucy.kinkstuff.registration.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WoolCarpetBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom crafting recipe for the Pet Bed.
 * It requires three items in a horizontal row: [Wool] [Carpet] [Wool].
 * The resulting Pet Bed will have a color blended from the input wool and carpet colors.
 */
public class PetBedRecipe extends CustomRecipe {
    public PetBedRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * Checks if the crafting grid contains exactly three items in a row: 
     * two wool blocks flanking a carpet block.
     * 
     * @param input The crafting grid input.
     * @param level The current world level.
     * @return true if the pattern matches, false otherwise.
     */
    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        // Enforce the layout pattern: exactly 3 items in a horizontal row (Wool, Carpet, Wool)
        if (input.ingredientCount() != 3) return false;

        ItemStack slot0 = ItemStack.EMPTY;
        ItemStack slot1 = ItemStack.EMPTY;
        ItemStack slot2 = ItemStack.EMPTY;

        // Extract the row ingredients from the grid
        int found = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (found == 0) slot0 = stack;
                else if (found == 1) slot1 = stack;
                else if (found == 2) slot2 = stack;
                found++;
            }
        }

        if (found != 3) return false;

        // Verify slot types: Slots 0 and 2 must be Wool Blocks, Slot 1 must be a Wool Carpet Block
        return isWoolBlock(slot0) && isCarpetBlock(slot1) && isWoolBlock(slot2);
    }

    /**
     * Assembles the resulting Pet Bed item with a blended color.
     * 
     * @param input The crafting grid input.
     * @param registries Registry access for handling components.
     * @return A new Pet Bed ItemStack with the blended color applied.
     */
    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        List<DyeColor> colorsFound = new ArrayList<>();

        // Gather all dye color profiles from the wool ingredients inside the active grid
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                DyeColor color = getBlockDyeColor(stack);
                if (color != null) {
                    colorsFound.add(color);
                }
            }
        }

        // Generate the output pet bed item base
        ItemStack result = new ItemStack(ModItems.PET_BED.get());

        if (!colorsFound.isEmpty()) {
            // Blending logic: averages the RGB components and scales based on brightness
            int[] rbgComponents = new int[3];
            int maxColorValue = 0;
            int totalColors = 0;

            for (DyeColor color : colorsFound) {
                // COMPILER FIX: In 1.21.1, getTextureDiffuseColor() is a direct packed integer value
                int rgbHex = color.getTextureDiffuseColor();

                // Extract individual 0-255 RGB channels using standard bitwise shifting
                int r = (rgbHex >> 16) & 0xFF;
                int g = (rgbHex >> 8) & 0xFF;
                int b = rgbHex & 0xFF;

                maxColorValue += Math.max(r, Math.max(g, b));
                rbgComponents[0] += r; 
                rbgComponents[1] += g;
                rbgComponents[2] += b;
                totalColors++;
            }

            int finalHexColor = getFinalHexColor(rbgComponents, totalColors, (float) maxColorValue);

            // Stamp the blended color directly onto the crafted item output component map
            result.set(DataComponents.DYED_COLOR, new DyedItemColor(finalHexColor, true));
        }


        return result;
    }

    /**
     * Internal helper to calculate the final blended hex color.
     * 
     * @param rbgComponents Summed RGB values.
     * @param totalColors Number of colors being blended.
     * @param maxColorValue Summed maximum channel values for brightness scaling.
     * @return The resulting packed RGB integer.
     */
    private static int getFinalHexColor(int[] rbgComponents, int totalColors, float maxColorValue) {
        int blendedR = rbgComponents[0] / totalColors;
        int blendedG = rbgComponents[1] / totalColors;
        int blendedB = rbgComponents[2] / totalColors;
        float averageColorValue = maxColorValue / (float) totalColors;
        float maxBlendedChannel = (float)Math.max(blendedR, Math.max(blendedG, blendedB));

        if (maxBlendedChannel > 0) {
            float scalingFactor = averageColorValue / maxBlendedChannel;
            blendedR = (int)((float)blendedR * scalingFactor);
            blendedG = (int)((float)blendedG * scalingFactor);
            blendedB = (int)((float)blendedB * scalingFactor);
        }

        return (blendedR << 16) + (blendedG << 8) + blendedB;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 1; // Fits inside a 3x3 crafting grid easily
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.PET_BED_CRAFTING.get();
    }

    // --- Internal Helpers ---
    
    /** Checks if an item represents a vanilla Wool block. */
    private boolean isWoolBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        // Identifies vanilla wool blocks cleanly by checking their registry identifier pathway strings
        return BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath().endsWith("_wool");
    }

    /** Checks if an item represents a vanilla Carpet block. */
    private boolean isCarpetBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock() instanceof WoolCarpetBlock;
    }

    /** Extracts the DyeColor associated with a wool or carpet block. */
    private @org.jetbrains.annotations.Nullable DyeColor getBlockDyeColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        Block block = blockItem.getBlock();

        // Extract the Color enum identifier out of the vanilla block name strings directly
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        for (DyeColor color : DyeColor.values()) {
            if (path.startsWith(color.getName())) {
                return color;
            }
        }
        return DyeColor.WHITE; // Default fallback fallback
    }
}