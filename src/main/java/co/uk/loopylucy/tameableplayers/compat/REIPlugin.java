package co.uk.loopylucy.tameableplayers.compat;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.registration.ModItems;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.forge.REIPluginClient;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapedDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@REIPluginClient
public class REIPlugin implements REIClientPlugin {

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registerCollarVariants(registry);
        registerMittens(registry);
        registerBlindfold(registry);
        registerPetBed(registry);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        for (int i = 1; i <= 6; i++) {
            registry.addEntry(EntryStack.of(VanillaEntryTypes.ITEM, collarWithCmd(i)));
        }
    }

    // =========================================================================
    // COLLAR VARIANTS
    // CollarVariantRecipe has 6 valid combos — each needs its own display.
    // The output is always a Collar, but with a different CustomModelData value
    // so REI can show the correct model for each variant.
    // =========================================================================
    private void registerCollarVariants(DisplayRegistry registry) {

        // Collar + Gold Pendant → CMD 1
        registry.add(shapeless(
                inputs(collar(), entry(ModItems.GOLD_PENDANT.get())),
                output(collarWithCmd(1))
        ));

        // Collar + Silver Pendant → CMD 2
        registry.add(shapeless(
                inputs(collar(), entry(ModItems.SILVER_PENDANT.get())),
                output(collarWithCmd(2))
        ));

        // Collar + Gold Ring → CMD 3
        registry.add(shapeless(
                inputs(collar(), entry(ModItems.GOLD_RING.get())),
                output(collarWithCmd(3))
        ));

        // Collar + Silver Ring → CMD 4
        registry.add(shapeless(
                inputs(collar(), entry(ModItems.SILVER_RING.get())),
                output(collarWithCmd(4))
        ));

        // Collar + 4x Gold Nugget → CMD 5 (Gold Spikes)
        registry.add(shapeless(
                inputs(collar(),
                        entry(Items.GOLD_NUGGET), entry(Items.GOLD_NUGGET),
                        entry(Items.GOLD_NUGGET), entry(Items.GOLD_NUGGET)),
                output(collarWithCmd(5))
        ));

        // Collar + 4x Iron Nugget → CMD 6 (Silver Spikes)
        registry.add(shapeless(
                inputs(collar(),
                        entry(Items.IRON_NUGGET), entry(Items.IRON_NUGGET),
                        entry(Items.IRON_NUGGET), entry(Items.IRON_NUGGET)),
                output(collarWithCmd(6))
        ));
    }

    // =========================================================================
    // MITTENS
    // DyeableWoolRecipe checks for exactly 4 wool blocks anywhere in the grid.
    // We display it as a 2x2 in the top-left of the 3x3 — the natural placement.
    // Uses the #wool tag so REI shows all valid wool variants in the slot.
    // =========================================================================
    private void registerMittens(DisplayRegistry registry) {
        EntryIngredient wool  = EntryIngredients.ofIngredient(Ingredient.of(ItemTags.WOOL));
        EntryIngredient empty = EntryIngredient.empty();

        // Generate a list of 16 coloured blindfolds to match the 16 wool variants
        List<ItemStack> colouredMittens = new java.util.ArrayList<>();
        for (DyeColor colour : DyeColor.values()) {
            ItemStack dyedMittens = new ItemStack(ModItems.MITTENS.get());
            dyedMittens.set(DataComponents.DYED_COLOR, new DyedItemColor(colour.getTextureDiffuseColor(), true));
            colouredMittens.add(dyedMittens);
        }

        // [W][W][ ]
        // [W][W][ ]
        // [ ][ ][ ]
        registry.add(DefaultCustomShapedDisplay.simple(
                Arrays.asList(
                        wool, wool, empty,
                        wool, wool, empty,
                        empty, empty, empty
                ),
                List.of(EntryIngredients.ofItemStacks(colouredMittens)),
                3, 3,
                Optional.empty()
        ));
    }

    // =========================================================================
    // BLINDFOLD
    // DyeableWoolRecipe checks for 3 string + 3 wool anywhere in the grid.
    // We display it as two clean rows: strings on top, wool on bottom.
    // =========================================================================
    private void registerBlindfold(DisplayRegistry registry) {
        EntryIngredient wool   = EntryIngredients.ofIngredient(Ingredient.of(ItemTags.WOOL));
        EntryIngredient string = EntryIngredients.of(Items.STRING);
        EntryIngredient empty  = EntryIngredient.empty();

        // Generate a list of 16 coloured blindfolds to match the 16 wool variants
        List<ItemStack> colouredBlindfolds = new java.util.ArrayList<>();
        for (DyeColor colour : DyeColor.values()) {
            ItemStack dyedBlindfold = new ItemStack(ModItems.BLINDFOLD.get());
            dyedBlindfold.set(DataComponents.DYED_COLOR, new DyedItemColor(colour.getTextureDiffuseColor(), true));
            colouredBlindfolds.add(dyedBlindfold);
        }

        // [S][S][S]
        // [W][W][W]
        // [ ][ ][ ]
        registry.add(DefaultCustomShapedDisplay.simple(
                Arrays.asList(
                        string, string, string,
                        wool,   wool,   wool,
                        empty,  empty,  empty
                ),
                List.of(EntryIngredients.ofItemStacks(colouredBlindfolds)),
                3, 3,
                Optional.empty()
        ));
    }

    // =========================================================================
    // PET BED
    // PetBedRecipe checks for [Wool][Carpet][Wool] in any horizontal row.
    // We use tags for the inputs so REI cycles through all wool/carpet types.
    // For the output, we provide a list of all 16 coloured beds so REI cycles
    // them in sync with the inputs, showing the correct result for each color.
    // =========================================================================
    private void registerPetBed(DisplayRegistry registry) {
        EntryIngredient wool   = EntryIngredients.ofIngredient(Ingredient.of(ItemTags.WOOL));
        EntryIngredient carpet = EntryIngredients.ofIngredient(Ingredient.of(ItemTags.WOOL_CARPETS));
        EntryIngredient empty  = EntryIngredient.empty();

        // Generate a list of all 16 coloured beds to match the 16 wool variants
        List<ItemStack> colouredBeds = new java.util.ArrayList<>();
        for (DyeColor colour : DyeColor.values()) {
            ItemStack dyedBed = new ItemStack(ModItems.PET_BED.get());
            dyedBed.set(DataComponents.DYED_COLOR, new DyedItemColor(colour.getTextureDiffuseColor(), true));
            colouredBeds.add(dyedBed);
        }

        registry.add(DefaultCustomShapedDisplay.simple(
                Arrays.asList(
                        wool,  carpet, wool,
                        empty, empty,  empty,
                        empty, empty,  empty
                ),
                List.of(EntryIngredients.ofItemStacks(colouredBeds)),
                3, 3,
                Optional.empty()
        ));
    }

    // =========================================================================
    // HELPERS — keeps the registration methods above readable
    // =========================================================================

    /** Creates a collar ItemStack with a specific CustomModelData value. */
    private static ItemStack collarWithCmd(int cmd) {
        ItemStack stack = new ItemStack(ModItems.COLLAR.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(cmd));
        return stack;
    }

    /** Wraps a shaped-or-shapeless display (no position = shapeless in REI). */
    private static DefaultCustomShapelessDisplay shapeless(
            List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        TameablePlayers.LOGGER.info("REI Registered inputs: {}, outputs: {}", inputs, outputs);
        return DefaultCustomShapelessDisplay.simple(inputs, outputs, Optional.empty());
    }

    private static EntryIngredient collar() {
        return EntryIngredients.of(ModItems.COLLAR.get());
    }

    private static EntryIngredient entry(net.minecraft.world.item.Item item) {
        return EntryIngredients.of(item);
    }

    private static List<EntryIngredient> inputs(EntryIngredient... entries) {
        return Arrays.asList(entries);
    }

    private static List<EntryIngredient> output(ItemStack stack) {
        return List.of(EntryIngredients.of(stack));
    }
}
