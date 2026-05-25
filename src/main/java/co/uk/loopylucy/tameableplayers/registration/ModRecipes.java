package co.uk.loopylucy.tameableplayers.registration;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import co.uk.loopylucy.tameableplayers.recipes.CollarVariantRecipe;
import co.uk.loopylucy.tameableplayers.recipes.DyeableWoolRecipe;
import co.uk.loopylucy.tameableplayers.recipes.PetBedRecipe;
import co.uk.loopylucy.tameableplayers.recipes.TwinToneDyeRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for custom recipe serializers.
 * This is used to register our custom PetBedRecipe so the game knows how to 
 * decode and handle it.
 */
public class ModRecipes {
    /** The registry for recipe serializers. */
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TameablePlayers.MODID);

    /** Serializer for the custom Pet Bed crafting recipe. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PetBedRecipe>> PET_BED_CRAFTING =
            RECIPE_SERIALIZERS.register("pet_bed_crafting", () -> new SimpleCraftingRecipeSerializer<>(PetBedRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<TwinToneDyeRecipe>> TWIN_TONE_DYE =
            RECIPE_SERIALIZERS.register("twin_tone_dye", () -> new SimpleCraftingRecipeSerializer<>(TwinToneDyeRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CollarVariantRecipe>> COLLAR_VARIANT_CRAFTING =
            RECIPE_SERIALIZERS.register("collar_variant_crafting", () -> new SimpleCraftingRecipeSerializer<>(CollarVariantRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<DyeableWoolRecipe>> DYEABLE_WOOL_CRAFTING =
            RECIPE_SERIALIZERS.register("dyeable_wool_crafting", () -> new SimpleCraftingRecipeSerializer<>(DyeableWoolRecipe::new));
}