package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.recipe.early_implant.EarlyImplantRecipe;
import org.jetbrains.annotations.Nullable;

public abstract class EarlySurgeryTumorBlockEntity extends BlockEntity {

    protected @Nullable ResourceLocation lazyRecipeId = null;
    protected @Nullable RecipeHolder<EarlyImplantRecipe> cachedRecipeHolder = null;

    public EarlySurgeryTumorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public @Nullable EarlyImplantRecipe getCachedRecipe() {
        return this.cachedRecipeHolder != null ? this.cachedRecipeHolder.value() : null;
    }

    public @Nullable RecipeHolder<EarlyImplantRecipe> getCachedRecipeHolder() {
        return this.cachedRecipeHolder;
    }

    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                EarlySurgeryTumorBlockEntity.this.onInventoryChanged(slot);
            }
        };
    }

    protected void onInventoryChanged(int slot) {
        setChanged();
    }

    protected void saveRecipeHolder(CompoundTag tag, String key) {
        if (this.cachedRecipeHolder != null) {
            tag.putString(key, this.cachedRecipeHolder.id().toString());
        }
    }

    protected void loadLazyRecipeId(CompoundTag tag, String key) {
        if (tag.contains(key, CompoundTag.TAG_STRING)) {
            this.lazyRecipeId = ResourceLocation.parse(tag.getString(key));
        }
    }

    @SuppressWarnings("unchecked")
    protected void resolveLazyRecipeHolder() {
        if (this.level == null || this.level.isClientSide || this.lazyRecipeId == null) return;

        this.level.getRecipeManager().byKey(this.lazyRecipeId).ifPresent(holder -> {
            if (holder.value() instanceof EarlyImplantRecipe) {
                this.cachedRecipeHolder = (RecipeHolder<EarlyImplantRecipe>) holder;
            }
        });
        this.lazyRecipeId = null;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
}
