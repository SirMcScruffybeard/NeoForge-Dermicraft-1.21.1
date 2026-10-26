package net.scruffy.dermicraft.item.custom.module;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * One loaded module item and its variants: the item, the component that tells the variants apart, and the
 * list of variants. The client item property, the generated models and the creative tab all read this one
 * definition. A stack without the component is the bare module and keeps the plain frame model.
 *
 * <p>Variants are listed in ascending order: item overrides pick the last entry whose value is at or below
 * the property, and the property is a variant's position in the list plus one (0 is the bare module).
 *
 * @param <D> the component's data type
 */
public final class ModuleFamily<D> {

    /** The client item property a family's models are selected with. */
    public static final String VARIANT_PROPERTY = "variant";

    private final String MODEL_PREFIX;
    private final Supplier<? extends Item> ITEM;
    private final Supplier<DataComponentType<D>> COMPONENT;
    private final List<ModuleVariant<D>> VARIANTS;
    private final BiPredicate<D, D> MATCHER;

    /**
     * @param modelPrefix the generated models are named {@code <modelPrefix>_<variant name>}
     * @param matcher     whether a variant's sample value stands for an actual component value; usually plain
     *                    equality, but a variant such as Overgrowth can match every level
     */
    public ModuleFamily(String modelPrefix, Supplier<? extends Item> item, Supplier<DataComponentType<D>> component,
                        List<ModuleVariant<D>> variants, BiPredicate<D, D> matcher) {
        MODEL_PREFIX = modelPrefix;
        ITEM = item;
        COMPONENT = component;
        VARIANTS = variants;
        MATCHER = matcher;
    }

    public Item getItem() {
        return ITEM.get();
    }

    public DataComponentType<D> getComponent() {
        return COMPONENT.get();
    }

    public List<ModuleVariant<D>> getVariants() {
        return VARIANTS;
    }

    public String getModelName(ModuleVariant<D> variant) {
        return MODEL_PREFIX + "_" + variant.name();
    }

    /** The item property value that selects {@code variant}'s model. */
    public int getPredicateValue(ModuleVariant<D> variant) {
        return VARIANTS.indexOf(variant) + 1;
    }

    /** The item property value for a stack's component: 0 (the bare module) for none or no match. */
    public int getPredicateValue(@Nullable D data) {
        if (data == null) return 0;
        for (ModuleVariant<D> variant : VARIANTS) {
            if (MATCHER.test(variant.data(), data)) return getPredicateValue(variant);
        }
        return 0;
    }

    /** A stack of {@code variant}, for the creative tab and JEI. */
    public ItemStack createStack(ModuleVariant<D> variant) {
        ItemStack stack = new ItemStack(getItem());
        stack.set(getComponent(), variant.data());
        return stack;
    }

    public List<ItemStack> createStacks() {
        return VARIANTS.stream().map(this::createStack).toList();
    }
}
