package co.uk.loopylucy.kinkstuff.recipes;

import co.uk.loopylucy.kinkstuff.common.component.ColourData;
import co.uk.loopylucy.kinkstuff.registration.ModDataComponents;
import co.uk.loopylucy.kinkstuff.registration.ModItems;
import co.uk.loopylucy.kinkstuff.registration.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TwinToneDyeRecipe extends CustomRecipe {

    public TwinToneDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, @NotNull Level level) {
        int dyeableItems = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isDyeable(stack)) dyeableItems++;
        }
        // Ensure only one base item is present
        return dyeableItems == 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.@NotNull Provider registries) {
        ItemStack baseItem = ItemStack.EMPTY;
        int itemIndex = -1;
        int width = input.width(); // Dynamically gets 2 or 3

        // 1. Find the Item and its Index
        for (int i = 0; i < input.size(); i++) {
            if (isDyeable(input.getItem(i))) {
                baseItem = input.getItem(i).copy();
                itemIndex = i;
                break;
            }
        }

        if (itemIndex == -1) return ItemStack.EMPTY;

        // Calculate Item Coordinates
        int itemX = itemIndex % width;

        List<DyeColor> outerColors = new ArrayList<>();
        List<DyeColor> innerColors = new ArrayList<>();

        // 2. Find Dyes and categorize based on Horizontal Offset
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof DyeItem dye) {
                int dyeX = i % width;

                // If Dye is to the left of Item
                if (dyeX < itemX) {
                    outerColors.add(dye.getDyeColor());
                }
                // If Dye is to the right of Item
                else if (dyeX > itemX) {
                    innerColors.add(dye.getDyeColor());
                }
                // If Dye is in the same column as the item, you can decide
                // to ignore it or add it to both. Here we ignore it.
            }
        }

        // 3. Blend colors
        ColourData current = baseItem.getOrDefault(ModDataComponents.ITEM_COLOURS.get(), new ColourData(0xFFFFFF, 0xFFFFFF));

        int color1 = blendColors(outerColors, current.colour0());
        int color2 = blendColors(innerColors, current.colour1());

        baseItem.set(ModDataComponents.ITEM_COLOURS.get(), new ColourData(color1, color2));
        return baseItem;
    }

    private int blendColors(List<DyeColor> colors, int defaultColor) {
        if (colors.isEmpty()) return defaultColor;

        int[] rgbComponents = new int[3];
        int maxColorValue = 0;


        int r1 = FastColor.ARGB32.red(defaultColor);
        int g1 = FastColor.ARGB32.green(defaultColor);
        int b1 = FastColor.ARGB32.blue(defaultColor);
        maxColorValue += Math.max(r1, Math.max(g1, b1));
        rgbComponents[0] += r1;
        rgbComponents[1] += g1;
        rgbComponents[2] += b1;

        for (DyeColor color : colors) {
            int rgbHex = color.getTextureDiffuseColor();
            int r = FastColor.ARGB32.red(rgbHex);
            int g = FastColor.ARGB32.green(rgbHex);
            int b = FastColor.ARGB32.blue(rgbHex);
            maxColorValue += Math.max(r, Math.max(g, b));
            rgbComponents[0] += r;
            rgbComponents[1] += g;
            rgbComponents[2] += b;
        }

        return getFinalHexColor(rgbComponents, colors.size()+1, (float) maxColorValue);
    }

    private static int getFinalHexColor(int[] rgb, int totalColours, float maxColorValue) {
        int blendedR = rgb[0] / totalColours;
        int blendedG = rgb[1] / totalColours;
        int blendedB = rgb[2] / totalColours;
        float averageColourValue = maxColorValue / (float) totalColours;
        float maxBlendedChannel = (float) Math.max(blendedR, Math.max(blendedG, blendedB));

        if (maxBlendedChannel > 0) {
            float scalingFactor = averageColourValue / maxBlendedChannel;
            blendedR = (int)((float)blendedR * scalingFactor);
            blendedG = (int)((float)blendedG * scalingFactor);
            blendedB = (int)((float)blendedB * scalingFactor);
        }
        return FastColor.ARGB32.color(0, blendedR, blendedG, blendedB);
    }

    private boolean isDyeable(ItemStack stack) {
        // Replace with your specific item checks or an interface check
        return stack.getItem() == ModItems.CAT_EARS.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 1;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.TWIN_TONE_DYE.get();
    }
}