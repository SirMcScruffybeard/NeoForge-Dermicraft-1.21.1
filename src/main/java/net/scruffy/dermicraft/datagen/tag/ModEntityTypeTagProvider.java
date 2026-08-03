package net.scruffy.dermicraft.datagen.tag;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.main.Dermicraft;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModEntityTypeTagProvider extends EntityTypeTagsProvider {
    public ModEntityTypeTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Pulls in the whole vanilla SKELETONS tag (Skeleton/Stray/WitherSkeleton/Bogged) rather
        // than listing variants by hand, plus golems, which have no equivalent vanilla tag.
        tag(ModTags.EntityTypes.NOT_BLEEDABLE)
                .addTag(EntityTypeTags.SKELETONS)
                .add(EntityType.IRON_GOLEM)
                .add(EntityType.SNOW_GOLEM)

        ;
    }
}
