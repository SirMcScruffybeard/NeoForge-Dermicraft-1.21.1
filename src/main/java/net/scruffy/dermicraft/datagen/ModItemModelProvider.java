package net.scruffy.dermicraft.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.scruffy.dermicraft.fluid.ModFluids;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.main.Dermicraft;

import java.util.Objects;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Dermicraft.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
        handheldItem(ModItems.SCALPEL.get());
        handheldItem(ModItems.PRIMITIVE_SCALPEL.get());
        handheldItem(ModItems.SUTURE_KIT.get());
        handheldItem(ModItems.PRIMITIVE_SUTURE_KIT.get());
        handheldItem(ModItems.FORCEPS.get());
        handheldItem(ModItems.PRIMITIVE_FORCEPS.get());


        ////////////////////Basic Tools\\\\\\\\\\\\\\\\\\\\
        handheldItem(ModItems.EYE.get());
        basicItem(ModItems.NERVE_CLUSTER.get());
        basicItem(ModItems.DENSE_MUSCLE.get());
        basicItem(ModItems.BLOOD_NUGGET.get());
        basicItem(ModItems.PROTO_BRAIN.get());
        basicItem(ModItems.CHASSIS.get());
        basicItem(ModItems.MRE.get());
        basicItem(ModItems.MEAT_FLAVORED_MEAT.get());

        // Module Frame lives under item/module/ alongside the rest of the Module family's textures
        // (see Gadget Modules direction, dermicraft-gadget-notes.md), not item/ directly --
        // basicItem()'s default texture-path-matches-item-id assumption doesn't reach it.
        singleTextureItem(ModItems.MODULE_FRAME.get(), "item/module/module_frame");

        // Gadget Modules: hand-painted family background (target/fluid/hazard, per which slot-family
        // the module belongs to) as layer0, a small centered icon as layer1 -- no rendering needed,
        // just a second texture reference, same two-layer idea the biofuel bucket overlays already
        // use, just without runtime tinting. The icon layer is NOT the raw vanilla texture directly
        // (gravel/water bucket/lava bucket) -- item/generated always stretches each layer to the
        // full 16x16 canvas, so a same-size icon would completely cover the background frame. These
        // are our own pre-shrunk, centered, transparent-padded copies (item/module/icons/) instead,
        // so the frame stays visible around a smaller icon.
        // Vanilla's own iron_shovel texture as the icon layer directly -- mirrors the recipe's own
        // Iron Shovel ingredient (see ModRecipeProvider), no custom pre-shrunk icon asset needed,
        // vanilla's item textures already sit comfortably inside the frame at this scale.
        moduleItem(ModItems.AGGREGATE_MODULE.get(), "item/module/target_module_frame",
                ResourceLocation.withDefaultNamespace("item/iron_shovel"));
        // Same "target" family frame as Aggregate -- Beam is a targeting module too, just a
        // different material roster (see ModTags.Blocks.STONE_ORE). Three-layer stack: frame, then
        // Ender Pearl (our own icon asset) with vanilla's Iron Pickaxe texture layered on top --
        // mirrors the recipe's own Ender Pearl + Iron Pickaxe ingredients (see ModRecipeProvider).
        moduleItem(ModItems.BEAM_MODULE.get(), "item/module/target_module_frame",
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "item/module/icons/ender_pearl_icon"),
                ResourceLocation.withDefaultNamespace("item/iron_pickaxe"));
        moduleItem(ModItems.FLUID_BYPASS_MODULE.get(), "item/module/fluid_module_frame",
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "item/module/icons/water_bucket_icon"));
        moduleItem(ModItems.HEAT_SAFETY_MODULE.get(), "item/module/hazard_module_frame",
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "item/module/icons/lava_bucket_icon"));
        moduleItem(ModItems.METAPHYSICAL_SAFETY_MODULE.get(), "item/module/hazard_module_frame",
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "item/module/icons/ender_pearl_icon"));

        // Evolution Module family -- same lava bucket icon Thermal Safety Module uses (this is its own
        // Thermal-flavored variant), on the new Evolution frame background rather than the hazard one,
        // so the two families read as visually distinct at a glance despite sharing an icon.
        moduleItem(ModItems.HEAT_EVOLUTION_MODULE.get(), "item/module/evolution_module_frame",
                ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "item/module/icons/lava_bucket_icon"));

        // Non-standard texture folder (item/sunder_chains/, not item/) -- basicItem()'s default
        // texture-path-matches-item-id assumption doesn't reach it, hence the explicit path.
        singleTextureItem(ModItems.IRON_SUNDER_CHAIN.get(), "item/sunder_chains/iron_sunder_chain");
        singleTextureItem(ModItems.COPPER_SUNDER_CHAIN.get(), "item/sunder_chains/copper_sunder_chain");
        singleTextureItem(ModItems.GOLD_SUNDER_CHAIN.get(), "item/sunder_chains/gold_sunder_chain");
        singleTextureItem(ModItems.DIAMOND_SUNDER_CHAIN.get(), "item/sunder_chains/diamond_sunder_chain");
        singleTextureItem(ModItems.BONE_SUNDER_CHAIN.get(), "item/sunder_chains/bone_sunder_chain");
        singleTextureItem(ModItems.NETHERITE_SUNDER_CHAIN.get(), "item/sunder_chains/netherite_sunder_chain");

        ////////////////////Shatter Heads\\\\\\\\\\\\\\\\\\\\
        shatterHeadItem(ModItems.IRON_SHATTER_HEAD.get());
        paintedShatterHeadItem(ModItems.BONE_SHATTER_HEAD.get(), "item/shatter/bone_face_tail");
        shatterHeadItem(ModItems.GOLD_SHATTER_HEAD.get());
        paintedShatterHeadItem(ModItems.DIAMOND_SHATTER_HEAD.get(), "item/shatter/diamond_face_tail");
        shatterHeadItem(ModItems.COPPER_SHATTER_HEAD.get());
        shatterHeadItem(ModItems.NETHERITE_SHATTER_HEAD.get());


        ////////////////////Buckets\\\\\\\\\\\\\\\\\\\\
        chunkyBucketItem(ModFluids.CALCIUM_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.CARBON_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.PROTEIN_BLEND_BUCKET.get());

        chunkyBucketItem(ModFluids.CRUDE_SLURRY_BUCKET.get());
        chunkyBucketItem(ModFluids.CONCENTRATED_SLURRY_BUCKET.get());

        thinBucketItem(ModFluids.PRIMITIVE_CATALYST_BUCKET.get());
        thinBucketItem(ModFluids.REINFORCING_CATALYST_BUCKET.get());
        thinBucketItem(ModFluids.SYNAPSE_CATALYST_BUCKET.get());
        thinBucketItem(ModFluids.EVOLUTION_CATALYST_BUCKET.get());

        chunkyBucketItem(ModFluids.STONE_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.SILICA_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.CLAY_BLEND_BUCKET.get());

        chunkyBucketItem(ModFluids.FERROUS_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.CUPROUS_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.AUROUS_BLEND_BUCKET.get());
        chunkyBucketItem(ModFluids.PULP_BLEND_BUCKET.get());

        thinBucketItem(ModFluids.F_STUFF_BUCKET.get());
        thinBucketItem(ModFluids.C_STUFF_BUCKET.get());

        chunkyBucketItem(ModFluids.MOLTEN_REDSTONE_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_QUARTZ_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_GLOWSTONE_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_AMETHYST_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_DIAMOND_BUCKET.get());        chunkyBucketItem(ModFluids.MOLTEN_LAPIS_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_EMERALD_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_RAW_NETHERITE_SCRAP_BUCKET.get());
        chunkyBucketItem(ModFluids.MOLTEN_NETHERITE_BUCKET.get());

        thinBucketItem(ModFluids.BLAZE_ESSENCE_BUCKET.get());
        thinBucketItem(ModFluids.GHAST_ESSENCE_BUCKET.get());
        thinBucketItem(ModFluids.WITHER_ESSENCE_BUCKET.get());
        thinBucketItem(ModFluids.ENDER_ESSENCE_BUCKET.get());

        chunkyBucketItem(ModFluids.MOLTEN_SOUL_SILICA_BUCKET.get());
    }

    // Chunky fluids (blends/slurries) use the chunky fill overlay; layer1 gets tinted at runtime per-fluid.
    private ItemModelBuilder chunkyBucketItem(Item item) {
        return bucketItem(item, "item/bucket/bucket_chunky_fluid");
    }

    // Thin fluids (catalysts, F/C-Stuff) use the thin, water-like fill overlay.
    private ItemModelBuilder thinBucketItem(Item item) {
        return bucketItem(item, "item/bucket/bucket_fluid_thin");
    }

    // Generic single-layer item model with an explicit texture path, for anything living under its
    // own subfolder (item/sunder_chains/, item/module/, ...) rather than directly under item/ where
    // basicItem()'s default texture-path-matches-item-id assumption would find it.
    private void singleTextureItem(Item item, String texturePath) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
        getBuilder(id.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), texturePath));
    }

    // layer0 is the shared face/tail texture, tinted per-material via item color handlers (see
    // ModClientEvents#registerShatterHeadTint); layer1 is the piston top, deliberately untinted (no
    // color handler registered for index 1). Shared across every head material -- only the tint
    // varies, no per-material texture files (unlike Sunder's chains).
    private ItemModelBuilder shatterHeadItem(Item item) {
        return paintedShatterHeadItem(item, "item/shatter/face_tail");
    }

    // Same layer1 (piston, untinted) as shatterHeadItem() above, but layer0 is a dedicated,
    // already-colored texture rather than the shared grayscale one -- for hand-painted materials
    // (e.g. Bone) where the material's look comes from the art itself, not a runtime tint. No color
    // handler gets registered for these items' layer0 (see ModClientEvents), so it renders exactly
    // as painted.
    private ItemModelBuilder paintedShatterHeadItem(Item item, String faceTailTexturePath) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
        return getBuilder(id.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, faceTailTexturePath))
                .texture("layer1", ResourceLocation.fromNamespaceAndPath(Dermicraft.MOD_ID, "item/shatter/piston"));
    }

    // layer0 is the untinted vanilla bucket body; layer1 is the fill overlay, tinted per-fluid via item color handlers.
    private ItemModelBuilder bucketItem(Item item, String fillTexturePath) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
        return getBuilder(id.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.withDefaultNamespace("item/bucket"))
                .texture("layer1", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), fillTexturePath));
    }

    // Gadget Module: layer0 is the hand-painted family background (this mod's own texture, hence
    // the explicit namespace via item id), layer1 is an existing item's icon referenced directly --
    // no runtime tint, unlike bucketItem() above, since the icon's color is baked into the source
    // texture already (gravel/water bucket/lava bucket all read correctly as-is).
    private ItemModelBuilder moduleItem(Item item, String backgroundTexturePath, ResourceLocation iconTexture) {
        ResourceLocation id = Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item));
        return getBuilder(id.toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(id.getNamespace(), backgroundTexturePath))
                .texture("layer1", iconTexture);
    }

    /** Three-layer variant -- frame, then two stacked icons, for modules whose crafting identity
     * needs more than one ingredient represented (first use: Beam Module's Ender Pearl + Iron
     * Pickaxe). {@code item/generated} supports arbitrary numbered layers natively, so this is just
     * one more texture reference, no new model shape. */
    private ItemModelBuilder moduleItem(Item item, String backgroundTexturePath, ResourceLocation icon1Texture, ResourceLocation icon2Texture) {
        return moduleItem(item, backgroundTexturePath, icon1Texture)
                .texture("layer2", icon2Texture);
    }
}
