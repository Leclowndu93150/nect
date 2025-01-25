package com.leclowndu93150.notenoughchesttextures.resources;

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
        ResourceLocation plankLoc = new ResourceLocation(woodType.getNamespace(),
                "block/" + woodType.getPath() + "_planks");

        try (TextureImage plankTexture = TextureImage.open(manager, plankLoc)) {
            Palette plankPalette = extractPlankPalette(plankTexture);

            for (String variant : VARIANTS) {
                ResourceLocation baseLoc = new ResourceLocation("notenoughchesttextures",
                        "textures/entity/chest/base/chest_" + variant + ".png");

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