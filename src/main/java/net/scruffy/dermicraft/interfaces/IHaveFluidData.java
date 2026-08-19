package net.scruffy.dermicraft.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.scruffy.dermicraft.component.FluidData;
import net.scruffy.dermicraft.component.ModDataComponentTypes;
import net.scruffy.dermicraft.hazard.HazardProfile;

public interface IHaveFluidData {

    default DataComponentType<FluidData> getDataType() {
        return ModDataComponentTypes.FLUID_DATA.get();
    }

    default boolean isValidFluidHandler(IFluidHandler handler) {
        return handler != null;
    }

    default IFluidHandler getTargetFluidHandler(Level level, BlockPos pos, Direction face) {
        BlockEntity be = level.getBlockEntity(pos);
        IFluidHandler handler;
        if (be != null) {
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face);
            return handler;
        }
        return null;
    }

    default boolean targetHasEnough(int amount, IFluidHandler handler) {
        FluidStack fluidStack = handler.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        return fluidStack.getAmount() == amount;
    }

    default void emptyFluidData(ItemStack stack) {
        stack.set(getDataType(), FluidData.EMPTY);
    }

    /**
     * A data component belongs to the WHOLE {@link ItemStack}, so writing fluid into a stack of
     * more than one container writes it into every one of them -- a single source block's worth
     * turning into five filled Flasks. Every fill path must therefore refuse stacked containers.
     *
     * <p>Callers that want stacked containers to work anyway are responsible for splitting a single
     * item off first and finding it a home; see {@code DrinkerItem#fillContainers} for the item-side
     * pattern, or {@code EffluentcerBlockEntity#transferToHandlerWithEject} for the machine-side one.
     *
     * <p>Deliberately guards only the FILL direction. Refusing to <em>remove</em> fluid from a stack
     * would be worse than allowing it: filled containers with identical components do stack, and a
     * refused removal after the player has already had the effect (drinking, say) turns a
     * fluid-destruction bug into an unlimited-use one. Removal has to split, not refuse.
     */
    static boolean isSingleContainer(ItemStack stack) {
        return stack.getCount() <= 1;
    }

    /**
     * Guarded stand-in for a raw {@code stack.set(FLUID_DATA, ...)}, for code that writes the
     * component directly instead of going through a handler.
     *
     * @return whether the write happened; false means the stack was stacked and was left untouched.
     */
    static boolean writeFluidData(ItemStack stack, FluidData data) {
        if (!isSingleContainer(stack)) return false;
        stack.set(ModDataComponentTypes.FLUID_DATA.get(), data);
        return true;
    }

    /**
     * Like {@link #writeFluidData}, but never simply fails: a stacked container has one item split
     * off, written to, and handed back to the player, leaving the rest of the stack untouched.
     *
     * <p>Use this wherever the fill is the <em>consequence</em> of something that already happened
     * -- the player has had the effect and the container filling is the price. Refusing the write
     * there would hand out the effect for free, which is a worse bug than the duplication the guard
     * exists to stop (a stack of empty Flasks would grant unlimited underwater breathing).
     */
    static void writeFluidDataSplitting(ItemStack stack, FluidData data, Player player) {
        if (writeFluidData(stack, data)) return;

        ItemStack single = stack.copyWithCount(1);
        single.set(ModDataComponentTypes.FLUID_DATA.get(), data);
        stack.shrink(1);
        giveOrDrop(player, single);
    }

    /**
     * Hands a container back to the player, dropping it at their feet if there's no room.
     *
     * <p>Deliberately not {@code player.drop(stack, false)}: that overload's boolean is
     * {@code includeThrowerName}, not {@code dropAround}, so it lands in the forward-throw branch
     * and hurls the item away exactly like a Q-toss. The three-argument overload takes a real
     * {@code dropAround} flag but only <em>constructs</em> the entity -- {@code addFreshEntity} lives
     * in {@code CommonHooks.onPlayerTossEvent}, which only the two-argument form routes through, so
     * calling it directly would make the item silently vanish.
     */
    static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty() || player.getInventory().add(stack)) return;
        Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), stack);
    }

    default boolean isServerSide(Level level) {
        return !level.isClientSide;
    }

    private boolean hasFluid(FluidData data) {
        return data != null && !data.isFluidEmpty();
    }

    /**
     * General use Fluid Handler for use with FluidData data component.
     * Only fills/drains all-or-nothing, full capacity at a time.
     */
    class RigidFluidDataFluidHandler implements IFluidHandlerItem {

        protected ItemStack container;
        protected final int CAPACITY;

        public RigidFluidDataFluidHandler(ItemStack stack, int capacity) {
            container = stack;
            CAPACITY = capacity;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidData data = container.getOrDefault(getDataType(), FluidData.EMPTY);
            return data.getFluidStack();
        }

        @Override
        public int getTankCapacity(int i) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // See IHaveFluidData#isSingleContainer -- filling a stack would fill every item in it.
            if (!isSingleContainer(container)) return 0;
            if (!getFluidInTank(0).isEmpty() || resource.isEmpty() || resource.getAmount() < getTankCapacity(0)) {
                return 0;
            }

            if (action.execute()) {
                container.set(getDataType(), FluidData.createData(resource.copy()));
            }
            return CAPACITY;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.is(getFluidInTank(0).getFluid())) {
                return drain(resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getFluidInTank(0);
            if (contained.isEmpty() || maxDrain < CAPACITY) return FluidStack.EMPTY;

            if (action.execute()) {
                container.remove(getDataType());
            }
            return contained;
        }

        private DataComponentType<FluidData> getDataType() {
            return ModDataComponentTypes.FLUID_DATA.get();
        }
    }

    /**
     * General use Fluid Handler for use with FluidData data component.
     * Allows partial fills/drains, up to the remaining room/amount available.
     */
    class FlexibleFluidDataFluidHandler implements IFluidHandlerItem {

        protected ItemStack container;
        protected final int CAPACITY;

        public FlexibleFluidDataFluidHandler(ItemStack stack, int capacity) {
            container = stack;
            CAPACITY = capacity;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidData data = container.getOrDefault(getDataType(), FluidData.EMPTY);
            return data.getFluidStack();
        }

        @Override
        public int getTankCapacity(int i) {
            return CAPACITY;
        }

        @Override
        public boolean isFluidValid(int i, FluidStack fluidStack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // See IHaveFluidData#isSingleContainer -- filling a stack would fill every item in it.
            if (!isSingleContainer(container)) return 0;
            if (resource.isEmpty()) return 0;

            FluidStack contained = getFluidInTank(0);
            if (!contained.isEmpty() && !contained.is(resource.getFluid())) return 0;

            int space = CAPACITY - contained.getAmount();
            int amountToFill = Math.min(space, resource.getAmount());
            if (amountToFill <= 0) return 0;

            if (action.execute()) {
                FluidStack newStack = contained.isEmpty() ? resource.copy() : contained.copy();
                newStack.setAmount(contained.getAmount() + amountToFill);
                container.set(getDataType(), FluidData.createData(newStack));
            }
            return amountToFill;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.is(getFluidInTank(0).getFluid())) {
                return drain(resource.getAmount(), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack contained = getFluidInTank(0);
            if (contained.isEmpty()) return FluidStack.EMPTY;

            int amountToDrain = Math.min(maxDrain, contained.getAmount());
            FluidStack drained = contained.copy();
            drained.setAmount(amountToDrain);

            if (action.execute()) {
                int remaining = contained.getAmount() - amountToDrain;
                if (remaining <= 0) {
                    container.remove(getDataType());
                } else {
                    FluidStack remainingStack = contained.copy();
                    remainingStack.setAmount(remaining);
                    container.set(getDataType(), FluidData.createData(remainingStack));
                }
            }
            return drained;
        }

        private DataComponentType<FluidData> getDataType() {
            return ModDataComponentTypes.FLUID_DATA.get();
        }
    }

    /**
     * A {@link FlexibleFluidDataFluidHandler} that refuses every fill arriving through the
     * capability, while leaving reads and drains fully open.
     *
     * <p>For a tool whose intake is meant to be its OWN deliberate interaction rather than something
     * any passing fluid mover can push into -- I.D.E.P. is the case this exists for: it fills itself
     * by clearing a jam it was pointed at, and nothing else should be able to load it. Sealing the
     * capability is what makes that true generically, instead of each would-be filler (D.R.I.N.K.E.R.
     * Transfer, a machine's auto-eject, a future gadget) having to know to skip it.
     *
     * <p>Drain deliberately stays open: the tool's whole purpose is depositing what it holds into a
     * machine, and other tools reading its contents is harmless.
     *
     * <p><b>The owning item must not fill itself through the capability</b> -- it would get refused
     * like anyone else. It should construct a plain {@link FlexibleFluidDataFluidHandler} over its
     * own stack for internal intake; see {@code IdepItem#internalTank}.
     */
    class SealedFillFluidDataFluidHandler extends FlexibleFluidDataFluidHandler {

        public SealedFillFluidDataFluidHandler(ItemStack stack, int capacity) {
            super(stack, capacity);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }
    }

    /**
     * Bottomless sink: accepts anything its {@link HazardProfile} tolerates and destroys it on the
     * spot. Backs the Disposal mode that gadgets share (S.I.P.P.I.N.G., and any later one that
     * sits in a machine's fill slot to be drained into).
     *
     * <p>The odd one out among these handlers: it never touches {@link FluidData} at all, so it has
     * no stored contents, always reports empty, and refuses every drain. That also means it needs
     * no stacked-container guard -- see {@link #isSingleContainer} -- because voiding creates
     * nothing to duplicate.
     *
     * <p>The hazard profile is the whole gate. Without it a Disposal gadget would be a universal
     * fluid destructor, ignoring the tier ladder that decides what a gadget is allowed to handle at
     * all -- so a tier that can't safely CARRY a fluid must not be able to destroy it either.
     *
     * <p>Not used by D.R.I.N.K.E.R., deliberately: it doesn't sit in slots to receive fluid, it
     * reaches out and pulls, so its Disposal is a routing decision made while siphoning rather than
     * an exposed capability. Its registered capability stays the real buffer in every mode, so other
     * tools can still see what it's carrying.
     */
    class DisposalFluidHandler implements IFluidHandlerItem {

        private final ItemStack container;
        private final HazardProfile profile;

        public DisposalFluidHandler(ItemStack stack, HazardProfile profile) {
            this.container = stack;
            this.profile = profile;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return profile.accepts(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !profile.accepts(resource)) return 0;
            return resource.getAmount();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    /**
     * Same partial-fill behavior as {@link FlexibleFluidDataFluidHandler}, but refuses any fluid
     * the given {@link HazardProfile} doesn't tolerate -- matches Drinker's "won't even attempt it"
     * rule for hazardous fluids beyond a container's tier. Bladder-family equipment uses this.
     *
     * <p>An optional extra filter narrows acceptance further (e.g. Fuel Bladder's biofuel-only
     * restriction) -- defaults to accepting anything the hazard profile already allows.
     */
    class HazardGatedFluidDataFluidHandler extends FlexibleFluidDataFluidHandler {

        private final HazardProfile profile;
        private final java.util.function.Predicate<FluidStack> extraFilter;

        public HazardGatedFluidDataFluidHandler(ItemStack stack, int capacity, HazardProfile profile) {
            this(stack, capacity, profile, fluidStack -> true);
        }

        public HazardGatedFluidDataFluidHandler(ItemStack stack, int capacity, HazardProfile profile,
                                                  java.util.function.Predicate<FluidStack> extraFilter) {
            super(stack, capacity);
            this.profile = profile;
            this.extraFilter = extraFilter;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return profile.accepts(stack) && extraFilter.test(stack) && super.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!profile.accepts(resource) || !extraFilter.test(resource)) return 0;
            return super.fill(resource, action);
        }
    }
}
