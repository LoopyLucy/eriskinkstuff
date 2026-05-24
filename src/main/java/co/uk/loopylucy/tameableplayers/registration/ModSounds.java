package co.uk.loopylucy.tameableplayers.registration;

import co.uk.loopylucy.tameableplayers.TameablePlayers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Central registry for all custom sound events.
 * Uses DeferredRegister to ensure sounds are registered correctly.
 */
public class ModSounds {
    /** The registry for sound events. */
    public static final DeferredRegister<SoundEvent> SOUND_EVENT =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TameablePlayers.MODID);

    /** The Clicker sound event. Plays with a fixed range of 32 blocks. */
    public static final Supplier<SoundEvent> CLICKER = registerSoundEvent("clicker");

    /**
     * Helper method to register a sound event with a fixed range.
     * 
     * @param name The registry name of the sound.
     * @return A supplier for the sound event.
     */
    public static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TameablePlayers.MODID, name);
        return SOUND_EVENT.register(name, () -> SoundEvent.createFixedRangeEvent(id, 32.0F));
    }

    /**
     * Entry point for registering sound events to the mod event bus.
     * 
     * @param eventBus The mod event bus.
     */
    public static void register(IEventBus eventBus) {
        SOUND_EVENT.register(eventBus);
    }
}