package com.yungnickyoung.minecraft.bettermineshafts.init;

import com.yungnickyoung.minecraft.bettermineshafts.BetterMineshafts;
import com.yungnickyoung.minecraft.bettermineshafts.config.Configuration;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

public class BMConfig {
    public static void init() {
        AutoConfig.register(Configuration.class, Toml4jConfigSerializer::new);
        BetterMineshafts.CONFIG = AutoConfig.getConfigHolder(Configuration.class).getConfig();
    }
}
