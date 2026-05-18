package co.uk.loopylucy.kinkstuff.client;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry for custom model layer locations.
 * These identifiers are used to link model parts with their baked geometry 
 * during the layer registration process.
 */
public class ModModelLayers {
    /** Layer location for the Mittens model. */
    public static final ModelLayerLocation MITTENS = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, "mittens"), "main");
}