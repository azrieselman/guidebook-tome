package dev.tyty.guidebooktome.tome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.tyty.guidebooktome.index.GuidebookIndex;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import vazkii.akashictome.MorphingHandler;
import vazkii.akashictome.Registries;
import vazkii.akashictome.data_components.ToolContentComponent;

/**
 * Puts guidebook stacks into an Akashic Tome using that mod's public item and data components.
 */
public final class TomeFiller {
    private TomeFiller() {
    }

    public static ItemStack newTome(List<ItemStack> books) {
        ItemStack tome = new ItemStack(Registries.TOME.get());
        fill(tome, books);
        return tome;
    }

    public static void fill(ItemStack tome, List<ItemStack> books) {
        if (tome.isEmpty() || !MorphingHandler.isAkashicTome(tome)) {
            return;
        }
        ToolContentComponent contents = tome.getOrDefault(Registries.TOOL_CONTENT.get(), ToolContentComponent.EMPTY);
        ToolContentComponent.Mutable mutable = new ToolContentComponent.Mutable(contents);
        Set<String> usedKeys = new HashSet<>();
        List<ItemStack> stored = new ArrayList<>(contents.getItems());
        if (!tome.is(Registries.TOME.get())) {
            stored.add(tome);
        }
        reserveExistingKeys(tome, contents, usedKeys);

        for (ItemStack book : books) {
            if (book.isEmpty() || MorphingHandler.isAkashicTome(book)) {
                continue;
            }
            if (alreadyStored(stored, book)) {
                continue;
            }
            ItemStack copy = book.copyWithCount(1);
            String key = uniqueKey(modKey(copy), usedKeys);
            if (MorphingHandler.MINECRAFT.equals(key)) {
                continue;
            }
            copy.set(Registries.DEFINED_MOD.get(), key);
            usedKeys.add(key);
            mutable.tryInsert(copy);
            stored.add(copy);
        }
        tome.set(Registries.TOOL_CONTENT.get(), mutable.toImmutable());
    }

    private static void reserveExistingKeys(ItemStack tome, ToolContentComponent contents, Set<String> usedKeys) {
        for (ItemStack stored : contents.getItems()) {
            reserve(stored, usedKeys);
        }
        if (!tome.is(Registries.TOME.get())) {
            reserve(tome, usedKeys);
        }
    }

    private static void reserve(ItemStack stack, Set<String> usedKeys) {
        if (stack.has(Registries.DEFINED_MOD.get())) {
            String defined = stack.get(Registries.DEFINED_MOD.get());
            if (defined != null && !defined.isEmpty()) {
                usedKeys.add(defined);
                return;
            }
        }
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        if (!namespace.isEmpty()) {
            usedKeys.add(namespace);
        }
    }

    private static boolean alreadyStored(List<ItemStack> stored, ItemStack book) {
        ItemStack probe = withoutTomeMarkers(book);
        for (ItemStack have : stored) {
            if (ItemStack.isSameItemSameComponents(withoutTomeMarkers(have), probe)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack withoutTomeMarkers(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(Registries.DEFINED_MOD.get());
        copy.remove(Registries.IS_MORPHED.get());
        copy.remove(Registries.OG_DISPLAY_NAME.get());
        copy.remove(Registries.TOOL_CONTENT.get());
        copy.remove(DataComponents.CUSTOM_NAME);
        return copy;
    }

    private static String modKey(ItemStack stack) {
        ResourceLocation bookId = GuidebookIndex.bookOwner(stack);
        if (bookId != null) {
            return MorphingHandler.getModOrAlias(bookId.getNamespace());
        }
        return MorphingHandler.getModFromStack(stack);
    }

    private static String uniqueKey(String base, Set<String> usedKeys) {
        if (base == null || base.isEmpty()) {
            base = "unknown";
        }
        if (!usedKeys.contains(base)) {
            return base;
        }
        for (int attempt = 0; attempt < 99; attempt++) {
            String candidate = base + "_" + attempt;
            if (!usedKeys.contains(candidate)) {
                return candidate;
            }
        }
        return base + "_" + usedKeys.size();
    }
}
