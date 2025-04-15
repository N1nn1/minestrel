package com.ninni.minestrel.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.ninni.minestrel.Minestrel;
import com.ninni.minestrel.server.intstrument.InstrumentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class MInstrumentTypeRegistry {
    public static final DeferredRegister<InstrumentType> DEF_REG = DeferredRegister.create(new ResourceLocation(Minestrel.MODID, "instrument_type"), Minestrel.MODID);
    public static final Codec<InstrumentType> CODEC = ResourceLocation.CODEC.flatXmap(
            id -> {
                InstrumentType type = MInstrumentTypeRegistry.getInternalRegistry().getValue(id);
                return type != null ? DataResult.success(type) : DataResult.error(() -> "Unknown instrument type: " + id);
            },
            instrumentType -> {
                Optional<ResourceKey<InstrumentType>> key = MInstrumentTypeRegistry.getInternalRegistry().getResourceKey(instrumentType);
                return key.map(resourceKey -> DataResult.success(resourceKey.location())).orElseGet(() -> DataResult.error(() -> "Unknown resource key for instrument type"));
            }
    );


    public static final RegistryObject<InstrumentType> KEYBOARD = DEF_REG.register("keyboard", () -> new InstrumentType(48, 76));


    private static IForgeRegistry<InstrumentType> internalRegistry;

    public static void setInternalRegistry(IForgeRegistry<InstrumentType> registry) {
        internalRegistry = registry;
    }

    public static IForgeRegistry<InstrumentType> getInternalRegistry() {
        return internalRegistry;
    }

    public static ResourceLocation get(InstrumentType instrumentType) {
        return internalRegistry.getKey(instrumentType);
    }

    public static InstrumentType readInstrument(FriendlyByteBuf buf) {
        return internalRegistry.getValue(buf.readResourceLocation());
    }

    public static FriendlyByteBuf writeInstrument(FriendlyByteBuf buf, InstrumentType instrumentType) {
        return buf.writeResourceLocation(internalRegistry.getResourceKey(instrumentType).orElse(KEYBOARD.getKey()).location());
    }
}
