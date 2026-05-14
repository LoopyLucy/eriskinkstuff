package co.uk.loopylucy.kinkstuff.sound;

import co.uk.loopylucy.kinkstuff.ErisKinkStuff;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENT =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, ErisKinkStuff.MODID);

    public static final Supplier<SoundEvent> CLICKER = registerSoundEvent("clicker");

    public static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ErisKinkStuff.MODID, name);
        return SOUND_EVENT.register(name, () -> SoundEvent.createFixedRangeEvent(id, 32.0F));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENT.register(eventBus);
    }
}