package com.leclowndu93150.notenoughchesttextures.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.iglee42.notenoughchests.NotEnoughChests;
import fr.iglee42.notenoughchests.utils.ModAbbreviation;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynClientResourcesGenerator;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicTexturePack;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.Respriter;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;


public class ChestTextureGenerator extends DynClientResourcesGenerator {
    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("NECTextures");
    private static final String[] VARIANTS = {
            "single", "left", "right",
            "trapped_single", "trapped_left", "trapped_right"
    };

    private static ChestTextureGenerator INSTANCE;

    public static void init() {
        INSTANCE = new ChestTextureGenerator();
        INSTANCE.register();
    }

    protected ChestTextureGenerator() {
        super(new DynamicTexturePack(
                new ResourceLocation("notenoughchesttextures", "generated_pack"),
                Pack.Position.TOP,
                true,
                false
        ));
    }

    @Override
    public void regenerateDynamicAssets(ResourceManager manager) {
        NotEnoughChests.WOOD_TYPES.forEach(woodType -> {
            try {
                generateWoodTypeTextures(manager, woodType);
            } catch (Exception e) {
                LOGGER.error("Failed generating textures for " + woodType, e);
            }
        });
    }

    private void generateWoodTypeTextures(ResourceManager manager, ResourceLocation woodType) throws Exception {
        ResourceLocation blockStateLoc = new ResourceLocation(woodType.getNamespace(),
                "blockstates/" + woodType.getPath() + "_planks.json");

        try (InputStream blockStateStream = manager.getResource(blockStateLoc).orElseThrow().open()) {
            JsonObject blockState = JsonParser.parseReader(new InputStreamReader(blockStateStream)).getAsJsonObject();

            String modelPath = blockState.getAsJsonObject("variants")
                    .getAsJsonObject("")
                    .get("model")
                    .getAsString();

            ResourceLocation modelLoc = new ResourceLocation(modelPath);
            modelLoc = new ResourceLocation(modelLoc.getNamespace(),
                    "models/" + modelLoc.getPath() + ".json");

            try (InputStream modelStream = manager.getResource(modelLoc).orElseThrow().open()) {
                JsonObject model = JsonParser.parseReader(new InputStreamReader(modelStream)).getAsJsonObject();
                String texturePath = model.getAsJsonObject("textures")
                        .get("all")
                        .getAsString();

                ResourceLocation textureLoc = new ResourceLocation(texturePath);
                textureLoc = new ResourceLocation(textureLoc.getNamespace(),
                        "textures/" + textureLoc.getPath() + ".png");

                try (TextureImage plankTexture = TextureImage.open(manager, textureLoc)) {
                    Palette plankPalette = extractPlankPalette(plankTexture);

                    for (String variant : VARIANTS) {
                        ResourceLocation baseLoc = new ResourceLocation("minecraft",
                                "textures/entity/chest/" + variant.replace("trapped_", "") + ".png");

                        try (TextureImage baseImage = TextureImage.open(manager, baseLoc)) {
                            Respriter respriter = Respriter.of(baseImage);
                            TextureImage recolored = respriter.recolor(plankPalette);

                            String targetPath = variant.contains("trapped") ?
                                    "chest_trapped_" + variant.replace("trapped_", "") :
                                    "chest_" + variant;

                            ResourceLocation targetLoc = new ResourceLocation("nec",
                                    "entity/chest/" + targetPath + "/" + ModAbbreviation.getChestTexture(woodType));

                            this.dynamicPack.addAndCloseTexture(targetLoc, recolored);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to process model file for " + woodType, e);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process blockstate file for " + woodType, e);
        }
    }

    private static Palette extractPlankPalette(TextureImage plankTexture) {
        Palette palette = Palette.fromImage(plankTexture);
        palette.remove(palette.getDarkest());
        palette.increaseUp();
        palette.increaseInner();
        palette.reduceAndAverage();
        return palette;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean dependsOnLoadedPacks() {
        return false;
    }
}