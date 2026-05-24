package net.scruffy.dermicraft.block.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scruffy.dermicraft.block.ModBlocks;
import net.scruffy.dermicraft.block.entity.MachineBaseBlockEntity;
import net.scruffy.dermicraft.block.entity.ModBlockEntities;
import net.scruffy.dermicraft.interfaces.IFleshBlockEntity;
import net.scruffy.dermicraft.interfaces.IHaveInventory;
import net.scruffy.dermicraft.interfaces.IHaveTank;
import net.scruffy.dermicraft.interfaces.IProcessFood;
import net.scruffy.dermicraft.main.Dermicraft;
import net.scruffy.dermicraft.screen.custom.drooling_cauldron.DroolingCauldronMenu;
import net.scruffy.dermicraft.tank.WaterTank;
import net.scruffy.dermicraft.util.ModMath;
import net.scruffy.dermicraft.util.ModPlayerUtil;
import org.jetbrains.annotations.Nullable;

public class DroolingCauldronBlockEntity extends MachineBaseBlockEntity implements MenuProvider, IFleshBlockEntity, IHaveTank, IHaveInventory, IProcessFood {

    private final int PROCESSES_TICKS = 10;
    private final int AUTO_TICKS = 20;
    private final int AUTO_RATE = 1;
    private final int FOOD_MODIFIER = 10;
    private final int FLESH_MODIFIER = 15; //Use instead of FOOD_MODIFIER when processing PART_ITEMS
    public final static int CAPACITY = BUCKET_VOLUME * 5;

    public final static int INPUT = 0;
    public final static int OUTPUT = 1;

    private int progress = 0;
    private int maxProgress = 0;

    private final ContainerData data;

    public final ItemStackHandler INVENTORY = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlock();
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 1 ? 1 : super.getSlotLimit(slot);
        }
    };

    private final WaterTank TANK = new WaterTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            updateBlock();
        }
    };

    public DroolingCauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DROOLING_CAULDRON_BE.get(), pos, blockState);
        this.data = createContainerData();
    }

    @Override
    public FluidStack getFluid() {
        return TANK.getFluid();
    }

    @Override
    public IFluidHandler getTank(@Nullable Direction face) {
        return TANK;
    }

    public IItemHandler getItemHandler(@Nullable Direction face) {
        if (face == null) return INVENTORY;

        if (face == Direction.UP) {
            return new IItemHandlerModifiable() {
                @Override
                public void setStackInSlot(int slot, ItemStack itemStack) {
                    INVENTORY.setStackInSlot(INPUT, itemStack);
                }

                @Override
                public int getSlots() {
                    return 1;
                }

                @Override
                public ItemStack getStackInSlot(int slot) {
                    return INVENTORY.getStackInSlot(INPUT);
                }

                @Override
                public ItemStack insertItem(int slot, ItemStack itemStack, boolean simulate) {
                    return INVENTORY.insertItem(INPUT, itemStack, simulate);
                }

                @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return INVENTORY.extractItem(INPUT, amount, simulate);
                }

                @Override
                public int getSlotLimit(int i) {
                    return INVENTORY.getSlotLimit(INPUT);
                }

                @Override
                public boolean isItemValid(int i, ItemStack itemStack) {
                    return INVENTORY.isItemValid(INPUT, itemStack);
                }
            };
        }
        return null;
    }

    public IItemHandler getItemHandler() {
        return getItemHandler(null);
    }

    @Override
    public void drops() {
        dropItems(level, INVENTORY, worldPosition);
    }

    public void insertInput(ItemStack stack, Player player, InteractionHand hand) {
        ItemStack remainder = insertItem(INVENTORY, INPUT, stack);
        player.setItemInHand(hand, remainder);
    }

    public void extractInput(Player player) {
        ItemStack stack = extractItem(INVENTORY, INPUT, Integer.MAX_VALUE);
        ModPlayerUtil.giveItem(player, stack);
    }


    public void tick(Level level) {

        if (level.isClientSide) return;

        //////////Transfer\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, 5)) {
            if (hasFluidHandlerInSlot(INVENTORY, INPUT)) {
                transferFluidToTank(INVENTORY, INPUT, TANK);
            }

            if (hasEmptyFluidHandlerInSlot(INVENTORY, OUTPUT, TANK)) {
                transferFluidFromTankToHandler(INVENTORY, OUTPUT, TANK);
            }

            pushFluidToBelowNeighbour(level, worldPosition, TANK);
            setChanged();
            updateBlock();
        }

        //////////Auto-fill\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, AUTO_TICKS)) {
            transferResultFluidToTank(TANK, createWater(AUTO_RATE));
        }

        //////////Craft\\\\\\\\\\
        if (ModMath.Time.hasTicksPassed(level, PROCESSES_TICKS)) {

            ItemStack stack = INVENTORY.getStackInSlot(INPUT);
            int amount = getCraftedAmount(stack);

            if (isFood(stack) && hasRoom(TANK, amount, 0)) {
                maxProgress = getProcessTime(stack, PROCESSES_TICKS);

                if (ModMath.Time.isTaskFinished(progress, maxProgress) ) {
                    craftWater(amount);
                    resetProgress();
                } else {
                    incrementProgress();
                }
            } else {
                resetProgress();
            }
        }
    }

    private int getCraftedAmount(ItemStack stack) {
        if (hasNutrition(stack)) {
            int mod = isFood(stack) ? FOOD_MODIFIER : 1;
            mod = isPartItem(stack) ? FLESH_MODIFIER : mod;
            return getProcessAmount(stack, mod, 1);
        }
        return 0;
    }

    private void craftWater(int amount) {
        FluidStack resource = createWater(amount);
        if (hasRoom(TANK, resource)) {
            transferResultFluidToTank(TANK, resource);
            consumeSingleItem(INVENTORY, INPUT);
        }
    }

    private void resetProgress() {
        progress = 0;
    }

    private void incrementProgress() {
        progress += PROCESSES_TICKS;
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("blockentity." + Dermicraft.MOD_ID + "." + ModBlocks.DROOLING_CAULDRON.getId());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DroolingCauldronMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("dc_items", INVENTORY.serializeNBT(registries));
        tag.put("dc_tank", TANK.writeToNBT(registries, new CompoundTag()));
        tag.putInt("dc_progress", progress);
        tag.putInt("dc_max", maxProgress);

        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("dc_items")) INVENTORY.deserializeNBT(registries, tag.getCompound("dc_items"));
        if (tag.contains("dc_tank")) TANK.readFromNBT(registries, tag.getCompound("dc_tank"));
        this.progress = tag.getInt("dc_progress");
        this.maxProgress = tag.getInt("dc_max");
    }

    private void updateBlock() {
        super.updateBlock(level);
    }

    private ContainerData createContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> DroolingCauldronBlockEntity.this.progress;
                    case 1 -> DroolingCauldronBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> DroolingCauldronBlockEntity.this.progress = value;
                    case 1 -> DroolingCauldronBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public int getScaledProgress(int scale) {
        return getScaledProgress(progress, maxProgress, scale);
    }

    public boolean isStillCrafting() {
        return isStillCrafting(progress);
    }
}
