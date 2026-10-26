package net.scruffy.dermicraft.item.custom.module;

/**
 * One look of a variant module: its name (part of the model name), the icon texture under
 * {@code item/module/icons/}, and a sample component value that creates a stack of it.
 */
public record ModuleVariant<D>(String name, String icon, D data) {
}
