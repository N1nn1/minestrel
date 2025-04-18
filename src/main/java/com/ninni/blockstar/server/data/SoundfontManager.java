package com.ninni.blockstar.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ninni.blockstar.Blockstar;
import com.ninni.blockstar.registry.BInstrumentTypeRegistry;
import com.ninni.blockstar.server.intstrument.InstrumentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.*;
import java.util.stream.Collectors;

public class SoundfontManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private final Map<ResourceLocation, SoundfontDefinition> instruments = new HashMap<>();

    public SoundfontManager() {
        super(GSON, "soundfonts");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler) {
        instruments.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsons.entrySet()) {
            try {
                SoundfontDefinition def = SoundfontDefinition.CODEC.parse(JsonOps.INSTANCE, entry.getValue()).result().orElseThrow(() -> new JsonParseException("Invalid soundfont definition"));

                List<Integer> outOfRangeNotes = def.noteMap().keySet().stream().filter(note -> !def.instrumentType().isInRange(note)).toList();
                if (!outOfRangeNotes.isEmpty()) {
                    Blockstar.LOGGER.error("Soundfont '{}' has notes out of range for instrument '{}': {}", entry.getKey(), BInstrumentTypeRegistry.get(def.instrumentType()), outOfRangeNotes);
                }

                instruments.put(entry.getKey(), def);
            } catch (Exception e) {
                Blockstar.LOGGER.error("Failed to load soundfont '{}': {}", entry.getKey(), e.getMessage());
            }
        }

        Blockstar.LOGGER.info("Loaded {} soundfonts", instruments.size());
    }

    public SoundfontDefinition get(ResourceLocation id) {
        return instruments.get(id);
    }

    public Collection<ResourceLocation> getAllInstrumentIds() {
        return instruments.keySet();
    }

    public Collection<SoundfontDefinition> getAll() {
        return instruments.values();
    }

    public record SoundfontDefinition(InstrumentType instrumentType, boolean instrumentExclusive, ResourceLocation name, Map<String, String> noteMapRaw, Optional<Integer> velocityLayers, boolean held, boolean creativeTab, Rarity rarity) {
        public static final Codec<Rarity> RARITY_CODEC = Codec.STRING.xmap(
                string -> {
                    try {
                        return Rarity.valueOf(string.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid rarity: " + string);
                    }
                },
                Rarity::name
        );

        public static final Codec<SoundfontDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BInstrumentTypeRegistry.CODEC.fieldOf("instrument_type").forGetter(SoundfontDefinition::instrumentType),
                Codec.BOOL.fieldOf("instrument_exclusive").orElse(false).forGetter(SoundfontDefinition::instrumentExclusive),
                ResourceLocation.CODEC.fieldOf("name").forGetter(SoundfontDefinition::name),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("note_map").forGetter(SoundfontDefinition::noteMapRaw),
                Codec.INT.optionalFieldOf("velocity_layers").forGetter(SoundfontDefinition::velocityLayers),
                Codec.BOOL.fieldOf("held").orElse(false).forGetter(SoundfontDefinition::held),
                Codec.BOOL.fieldOf("creative_tab").orElse(true).forGetter(SoundfontDefinition::creativeTab),
                RARITY_CODEC.fieldOf("rarity").orElse(Rarity.COMMON).forGetter(SoundfontDefinition::rarity)
        ).apply(inst, SoundfontDefinition::new));

        public Map<Integer, String> noteMap() {
            return noteMapRaw.entrySet().stream().collect(Collectors.toMap(e -> Integer.parseInt(e.getKey()), Map.Entry::getValue));
        }

        public int getClosestSampleNote(int targetNote) {
            return noteMap().keySet().stream().min(Comparator.comparingInt(n -> Math.abs(n - targetNote))).orElse(targetNote);
        }
    }

}
