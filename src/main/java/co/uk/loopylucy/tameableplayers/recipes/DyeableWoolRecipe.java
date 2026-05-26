package co.uk.loopylucy.tameableplayers.recipes;

import co.uk.loopylucy.tameableplayers.registration.ModItems;
import co.uk.loopylucy.tameableplayers.registration.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DyeableWoolRecipe extends CustomRecipe {
    public DyeableWoolRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
        if (isMittensPattern(input)) return true;
        return isBlindfoldPattern(input);
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        ItemStack result = ItemStack.EMPTY;
        if (isMittensPattern(input)) {
            result = new ItemStack(ModItems.MITTENS.get());
        } else if (isBlindfoldPattern(input)) {
            result = new ItemStack(ModItems.BLINDFOLD.get());
        }

        if (result.isEmpty()) return ItemStack.EMPTY;

        List<DyeColor> coloursFound = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                DyeColor colour = getBlockDyeColour(stack);
                if (colour != null) {
                    coloursFound.add(colour);
                }
            }
        }

        if (!coloursFound.isEmpty()) {
            int[] rbgComponents = new int[3];
            int maxColourValue = 0;
            int totalColours = 0;

            for (DyeColor colour : coloursFound) {
                int rgbHex = colour.getTextureDiffuseColor();
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
            result.set(DataComponents.DYED_COLOR, new DyedItemColor(finalHexColour, true));
        }

        return result;
    }

    private boolean isMittensPattern(CraftingInput input) {
        // Pattern: W W, W W (2x2 or similar in 3x3)
        // We'll just check if it has exactly 4 wool and nothing else
        int woolCount = 0;
        int otherCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (isWoolBlock(stack)) woolCount++;
            else otherCount++;
        }
        return woolCount == 4 && otherCount == 0;
    }

    private boolean isBlindfoldPattern(CraftingInput input) {
        // Pattern: SSS, WWW
        int woolCount = 0;
        int stringCount = 0;
        int otherCount = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (isWoolBlock(stack)) woolCount++;
            else if (stack.is(Items.STRING)) stringCount++;
            else otherCount++;
        }
        return woolCount == 3 && stringCount == 3 && otherCount == 0;
    }

    private boolean isWoolBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).getPath().endsWith("_wool");
    }

    private DyeColor getBlockDyeColour(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        Block block = blockItem.getBlock();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        for (DyeColor colour : DyeColor.values()) {
            if (path.startsWith(colour.getName())) {
                return colour;
            }
        }
        return null;
    }

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
        return width >= 2 && height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYEABLE_WOOL_CRAFTING.get();
    }
}