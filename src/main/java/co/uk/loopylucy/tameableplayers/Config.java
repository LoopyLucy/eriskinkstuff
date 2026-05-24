package co.uk.loopylucy.tameableplayers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
/**
 * Handles mod configuration using NeoForge's configuration system.
 * This class allows for defining and validating mod settings that can be 
 * adjusted by users via config files.
 */
@SuppressWarnings("unused")
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** The compiled configuration specification. */
    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * Helper method to validate if a string represents a valid item name.
     * 
     * @param obj The object to validate.
     * @return true if valid, false otherwise.
     */
    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}