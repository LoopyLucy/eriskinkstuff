package co.uk.loopylucy.tameableplayers.recipes;

import co.uk.loopylucy.tameableplayers.registration.ModRecipes;
import co.uk.loopylucy.tameableplayers.registration.ModItems;
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
 * The resulting Pet Bed will have a colour blended from the input wool and carpet colours.
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

        if (input.width() < 3) return false;

        for (int y = 0; y < input.height(); y++) {
            ItemStack left = input.getItem(y * input.width());
            ItemStack middle = input.getItem(y * input.width() + 1);
            ItemStack right = input.getItem(y * input.width() + 2);
            if (isWoolBlock(left) && isCarpetBlock(middle) && isWoolBlock(right)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Assembles the resulting Pet Bed item with a blended colour.
     * 
     * @param input The crafting grid input.
     * @param registries Registry access for handling components.
     * @return A new Pet Bed ItemStack with the blended colour applied.
     */
    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        List<DyeColor> coloursFound = new ArrayList<>();

        // Gather all dye colour profiles from the wool ingredients inside the active grid
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                DyeColor colour = getBlockDyeColour(stack);
                if (colour != null) {
                    coloursFound.add(colour);
                }
            }
        }

        // Generate the output pet bed item base
        ItemStack result = new ItemStack(ModItems.PET_BED.get());

        if (!coloursFound.isEmpty()) {
            // Blending logic: averages the RGB components and scales based on brightness
            int[] rbgComponents = new int[3];
            int maxColourValue = 0;
            int totalColours = 0;

            for (DyeColor colour : coloursFound) {
                // COMPILER FIX: In 1.21.1, getTextureDiffuseColour() is a direct packed integer value
                int rgbHex = colour.getTextureDiffuseColor();

                // Extract individual 0-255 RGB channels using standard bitwise shifting
                int r = (rgbHex >> 16) & 0xFF;
                int g = (rgbHex >> 8) & 0xFF;
                int b = rgbHex & 0xFF;

                maxColourValue += Math.max(r, Math.max(g, b));
                rbgComponents[0] += r; 
                rbgComponents[1] += g;
                rbgComponents[2] += b;
                totalColours++;
            }

            int finalHexColour = getFinalHexColour(rbgComponents, totalColours, (float) maxColourValue);

            // Stamp the blended colour directly onto the crafted item output component map
            result.set(DataComponents.DYED_COLOR, new DyedItemColor(finalHexColour, true));
        }


        return result;
    }

    /**
     * Internal helper to calculate the final blended hex colour.
     * 
     * @param rbgComponents Summed RGB values.
     * @param totalColours Number of colours being blended.
     * @param maxColourValue Summed maximum channel values for brightness scaling.
     * @return The resulting packed RGB integer.
     */
    private static int getFinalHexColour(int[] rbgComponents, int totalColours, float maxColourValue) {
        int blendedR = rbgComponents[0] / totalColours;
        int blendedG = rbgComponents[1] / totalColours;
        int blendedB = rbgComponents[2] / totalColours;
        float averageColourValue = maxColourValue / (float) totalColours;
        float maxBlendedChannel = (float)Math.max(blendedR, Math.max(blendedG, blendedB));

        if (maxBlendedChannel > 0) {
            float scalingFactor = averageColourValue / maxBlendedChannel;
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

    /** Extracts the DyeColour associated with a wool or carpet block. */
    private @org.jetbrains.annotations.Nullable DyeColor getBlockDyeColour(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        Block block = blockItem.getBlock();

        // Extract the Colour enum identifier out of the vanilla block name strings directly
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        for (DyeColor colour : DyeColor.values()) {
            if (path.startsWith(colour.getName())) {
                return colour;
            }
        }
        return DyeColor.WHITE; // Default fallback fallback
    }
}