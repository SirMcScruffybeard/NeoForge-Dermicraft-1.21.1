package net.scruffy.dermicraft.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.main.Dermicraft;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

    }
}
