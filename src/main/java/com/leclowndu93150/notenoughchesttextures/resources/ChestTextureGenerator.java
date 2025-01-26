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
        if (INSTANCE != null) return;
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
        TextureImage plankTexture = null;
        ResourceLocation directPath = new ResourceLocation(woodType.getNamespace(),
                "textures/block/" + woodType.getPath() + "_planks.png");

        try {
            plankTexture = TextureImage.open(manager, directPath);
        } catch (Exception e) {
            // Try JSON parsing for custom paths
            ResourceLocation blockStateLoc = new ResourceLocation(woodType.getNamespace(),
                    "blockstates/" + woodType.getPath() + "_planks.json");

            try (InputStream inputStream = manager.getResource(blockStateLoc).orElseThrow().open()) {
                JsonObject blockState = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
                String modelPath = blockState.getAsJsonObject("variants")
                        .getAsJsonObject("")
                        .get("model")
                        .getAsString();

                ResourceLocation modelLoc = new ResourceLocation(modelPath);
                modelLoc = new ResourceLocation(modelLoc.getNamespace(), "models/" + modelLoc.getPath() + ".json");

                try (InputStream modelStream = manager.getResource(modelLoc).orElseThrow().open()) {
                    JsonObject model = JsonParser.parseReader(new InputStreamReader(modelStream)).getAsJsonObject();
                    String texturePath = model.getAsJsonObject("textures")
                            .get("all")
                            .getAsString();

                    ResourceLocation textureLocation = new ResourceLocation(texturePath);
                    plankTexture = TextureImage.open(manager, textureLocation);
                }
            }
        }

        if (plankTexture == null) {
            LOGGER.error("Could not find plank texture for " + woodType);
            return;
        }

        try {
            Palette plankPalette = extractPlankPalette(plankTexture);

            for (String variant : VARIANTS) {
                ResourceLocation baseLoc = new ResourceLocation("notenoughchesttextures",
                        "entity/chest/base/chest_" + variant.replace("trapped_", ""));

                manager.listResources("notenoughchesttextures/entity/chest/base",
                                resource -> resource.getPath().endsWith(".png"))
                        .forEach((loc, res) -> LOGGER.info("Found texture: {}", loc));

                try (TextureImage baseImage = TextureImage.open(manager, baseLoc)) {
                    Respriter respriter = Respriter.of(baseImage);
                    TextureImage recolored = respriter.recolor(plankPalette);

                    String targetPath = variant.contains("trapped") ?
                            "chest_trapped_" + variant.replace("trapped_", "") :
                            "chest_" + variant;

                    ResourceLocation targetLoc = new ResourceLocation("nec",
                            "entity/chest/" + targetPath + "/" + ModAbbreviation.getChestTexture(woodType));

                    this.dynamicPack.addAndCloseTexture(targetLoc, recolored);
                } catch (Exception e) {
                    LOGGER.error("Failed to process variant " + variant + " for " + woodType, e);
                }
            }
        } finally {
            if (plankTexture != null) {
                plankTexture.close();
            }
        }
    }

    private static Palette extractPlankPalette(TextureImage plankTexture) {
        Palette palette = Palette.fromImage(plankTexture);
        palette.remove(palette.getDarkest());
        if (palette.getLuminanceSpan() < 0.2) {
            palette.increaseUp();
        }
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
        return true;
    }
}