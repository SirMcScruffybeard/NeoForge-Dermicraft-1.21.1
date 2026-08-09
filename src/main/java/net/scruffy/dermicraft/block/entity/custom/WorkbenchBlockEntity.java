package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.custom.WorkbenchBlock;
import net.scruffy.dermicraft.interfaces.IGadget;
import net.scruffy.dermicraft.interfaces.IPreserveContentsOnPickup;
import net.scruffy.dermicraft.interfaces.IWorkbenchSwappable;
import net.scruffy.dermicraft.item.ModItems;
import net.scruffy.dermicraft.screen.custom.workbench.StorageStripSlot;
import net.scruffy.dermicraft.screen.custom.workbench.WorkbenchMenu;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Workbench (see dermicraft-gear-stations-notes.md -> Workbench). This slice builds the Storage
 * strip (Duty 1, restricted to gadgets/Frames/exchangeable parts/Damageable items, scrollable one
 * row of 9 at a time), the Mod/Swap page, and the Fabrication display. Implements
 * IPreserveContentsOnPickup so Forceps recovery (COLLECTIBLE tag, bottom only) carries Storage/
 * Work-item contents onto the recovered item instead of spilling them -- same as the Craw.
 */
public class WorkbenchBlockEntity extends MachineBaseBlockEntity implements MenuProvider, GeoBlockEntity, IPreserveContentsOnPickup {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Tier 1 fixed for now -- doubles per station tier (9 -> 18 -> 36 -> 72) once station tiering
    // exists; see the Storage strip's own design notes for the growth curve.
    public static final int STORAGE_CAPACITY = 9;

    public final ItemStackHandler STORAGE = new ItemStackHandler(STORAGE_CAPACITY) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isStorageEligible(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }

        // ItemStackHandler#deserializeNBT resizes itself to whatever "Size" was saved in NBT
        // *before* loading items, overriding whatever size the constructor requested. A save made
        // at a smaller capacity (an earlier tier, or this session's pre-bump test data) would
        // otherwise silently shrink STORAGE back down on load, desyncing it from
        // StorageStripSlot's index math (which assumes STORAGE_CAPACITY slots always exist) --
        // this restores the real capacity afterward, preserving whatever loaded. Matters for real
        // tier-ups too, not just this test value.
        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            super.deserializeNBT(provider, nbt);
            if (getSlots() != STORAGE_CAPACITY) {
                NonNullList<ItemStack> loaded = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
                for (int i = 0; i < getSlots(); i++) {
                    loaded.set(i, getStackInSlot(i));
                }
                setSize(STORAGE_CAPACITY);
                for (int i = 0; i < Math.min(loaded.size(), STORAGE_CAPACITY); i++) {
                    setStackInSlot(i, loaded.get(i));
                }
            }
        }
    };

    // Saved/loaded purely to ride the existing BE-sync pathway (see saveAdditional), same "no
    // DataSlot" convention MrShepardBlockEntity's population cap already uses in this codebase --
    // not because a UI scroll position is meaningful long-term world state.
    private int scrollRow = 0;

    // Mod page's working-item slot (Duties 4+5, see the Swap page design notes) -- separate from
    // STORAGE, persists across menu close so a gadget left mid-service stays put.
    public final ItemStackHandler WORK_ITEM = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.isEmpty() || stack.getItem() instanceof IWorkbenchSwappable;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }
    };

    public WorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(net.scruffy.dermicraft.block.entity.ModBlockEntities.WORKBENCH_BE.get(), pos, state);
    }

    // "active" is the persistent, independently-toggled power state -- right-clicking the bottom
    // (see WorkbenchBlock#useWithoutItem) flips it directly, with no GUI involved at all. "open" is
    // the actual visual/animation-driving flag (synced/persisted below, same pathway as scrollRow):
    // it's forced true for as long as the GUI (opened only via the top, see WorkbenchTopBlock) is
    // being viewed -- regardless of what active is -- and snaps back to whatever active is the
    // moment the last viewer closes it. Since active can only change via a bottom right-click, and
    // the GUI captures input while open, active can never actually change mid-view -- so this
    // reduces to "the models return to the state they were in when the GUI was opened."
    private int openCount = 0;
    private boolean active = false;
    private boolean open = false;

    public void toggleActive() {
        active = !active;
        if (openCount == 0) {
            setOpen(active);
        }
    }

    public void onMenuOpened() {
        openCount++;
        if (openCount == 1) {
            setOpen(true);
        }
    }

    public void onMenuClosed() {
        openCount = Math.max(0, openCount - 1);
        if (openCount == 0) {
            setOpen(active);
        }
    }

    // Client-side read by WorkbenchScreenGlowLayer to gate the screen-on glow overlay to exactly
    // the activate/active window (not deactivate) -- see that class.
    public boolean isOpen() {
        return open;
    }

    private void setOpen(boolean open) {
        this.open = open;
        setChanged();
        updateBlock();
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(WorkbenchBlock.LIT) && state.getValue(WorkbenchBlock.LIT) != open) {
                level.setBlock(worldPosition, state.setValue(WorkbenchBlock.LIT, open), 3);
            }
            if (level.getBlockEntity(worldPosition.above()) instanceof WorkbenchTopBlockEntity top) {
                top.setOpen(open);
            }
        }
    }

    public static boolean isStorageEligible(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getItem() instanceof IGadget) return true;
        if (stack.isDamageableItem()) return true;
        return stack.is(ModItems.CHASSIS.get()) || stack.is(ModItems.ADDON_FRAME.get());
    }

    public int getMaxScrollRow() {
        return Math.max(0, (STORAGE_CAPACITY - 1) / StorageStripSlot.COLUMNS);
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public void changeScrollRow(int delta) {
        setScrollRow(scrollRow + delta);
    }

    public void setScrollRow(int row) {
        int clamped = Math.max(0, Math.min(getMaxScrollRow(), row));
        if (clamped != scrollRow) {
            scrollRow = clamped;
            setChanged();
            updateBlock();
        }
    }

    @Override
    public Component getDisplayName() {
        return getDisplayName(ModBlocks.WORKBENCH);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WorkbenchMenu(containerId, inventory, this);
    }

    public void drops() {
        drops(STORAGE);
        drops(WORK_ITEM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "screen", 0, state -> open
                ? state.setAndContinue(RawAnimation.begin().thenPlay("activate").thenLoop("active"))
                : state.setAndContinue(RawAnimation.begin().thenPlay("deactivate"))));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("storage", STORAGE.serializeNBT(registries));
        tag.put("work_item", WORK_ITEM.serializeNBT(registries));
        // scrollRow isn't meaningfully "world state" (it's a UI scroll position), but this
        // save/load pathway IS how the client-side BE mirror gets kept in sync in this codebase
        // (see MrShepardBlockEntity's populationCap) -- skipping it here means the server value
        // changes but the client never finds out, so the thumb/visible slots never update.
        tag.putInt("scrollRow", scrollRow);
        // Same rationale as scrollRow above -- keeps a late-joining client's "screen" bone in the
        // right pose instead of always starting closed regardless of current viewer count.
        tag.putBoolean("open", open);
        // The actual power-toggle state -- needs its own persistence separate from "open" so a
        // reload doesn't lose what the bottom's right-click was last set to.
        tag.putBoolean("active", active);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        STORAGE.deserializeNBT(registries, tag.getCompound("storage"));
        WORK_ITEM.deserializeNBT(registries, tag.getCompound("work_item"));
        // Re-clamped against the CURRENT capacity, not just loaded verbatim -- a row saved while
        // capacity was higher (e.g. this session's now-reverted test bump) would otherwise still
        // compute an out-of-range slot index once capacity shrinks back down. Same category of bug
        // as STORAGE's own deserializeNBT-resize fix above, just on the derived row instead of the
        // handler's size.
        if (tag.contains("scrollRow")) {
            scrollRow = Math.max(0, Math.min(getMaxScrollRow(), tag.getInt("scrollRow")));
        }
        open = tag.getBoolean("open");
        active = tag.getBoolean("active");
    }
}
