package com.leclowndu93150.notenoughchesttextures;

import com.leclowndu93150.notenoughchesttextures.resources.ChestTextureGenerator;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(NotEnoughMain.MODID)
public class NotEnoughMain {

    public static final String MODID = "notenoughchesttextures";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NotEnoughMain() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ChestTextureGenerator::init);
    }
}
