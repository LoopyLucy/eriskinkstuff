package co.uk.loopylucy.kinkstuff.init;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import co.uk.loopylucy.kinkstuff.recipes.PetBedRecipe;
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
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ErisKinkStuff.MODID);

    /** Serializer for the custom Pet Bed crafting recipe. */
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<PetBedRecipe>> PET_BED_CRAFTING =
            RECIPE_SERIALIZERS.register("pet_bed_crafting", () -> new SimpleCraftingRecipeSerializer<>(PetBedRecipe::new));
}