package dev.tyty.guidebooktome.index;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.tyty.guidebooktome.GuidebookTome;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import vazkii.akashictome.IModdedBook;
import vazkii.akashictome.MorphingHandler;

/**
 * Rebuilds the guidebook list from mods and tags that are actually loaded.
 * Book ids are never hardcoded.
 */
public final class GuidebookIndex {
    private static final List<TagKey<Item>> GUIDE_TAGS = List.of(
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(GuidebookTome.MOD_ID, "guidebooks")),
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "guidebooks")),
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "books/guidebooks")));

    private static volatile List<ItemStack> books = List.of();

    private GuidebookIndex() {
    }

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EventPriority.LOW, GuidebookIndex::onTagsUpdated);
    }

    private static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }
        rebuild();
    }

    public static List<ItemStack> books() {
        return books;
    }

    public static void rebuild() {
        Map<String, ItemStack> found = new LinkedHashMap<>();
        collectPatchouli(found);
        collectModonomicon(found);
        collectTaggedAndMarkedItems(found);
        books = List.copyOf(found.values());
        GuidebookTome.LOGGER.info("Indexed {} guidebook item(s) from the mods that are loaded", books.size());
    }

    private static void add(Map<String, ItemStack> found, ItemStack stack) {
        if (stack == null || stack.isEmpty() || MorphingHandler.isAkashicTome(stack)) {
            return;
        }
        if (isBlankPatchouliBook(stack)) {
            return;
        }
        ItemStack copy = stack.copyWithCount(1);
        String key = identityKey(copy);
        found.putIfAbsent(key, copy);
    }

    private static String identityKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + stack.getComponents();
    }

    private static void collectPatchouli(Map<String, ItemStack> found) {
        if (!ModList.get().isLoaded("patchouli")) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Method getBookStack = api.getClass().getMethod("getBookStack", ResourceLocation.class);

            Class<?> registryClass = Class.forName("vazkii.patchouli.common.book.BookRegistry");
            Object registry = registryClass.getField("INSTANCE").get(null);
            Object booksField = registryClass.getField("books").get(registry);
            if (!(booksField instanceof Map<?, ?> loaded)) {
                return;
            }
            List<ResourceLocation> ids = new ArrayList<>();
            for (Object key : loaded.keySet()) {
                if (key instanceof ResourceLocation id) {
                    ids.add(id);
                }
            }
            ids.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation id : ids) {
                try {
                    Object stack = getBookStack.invoke(api, id);
                    if (stack instanceof ItemStack itemStack) {
                        add(found, itemStack);
                    }
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    GuidebookTome.LOGGER.warn("Skipping Patchouli book {}", id, ex);
                }
            }
        } catch (ReflectiveOperationException ex) {
            GuidebookTome.LOGGER.warn("Patchouli is loaded, but its book API could not be read", ex);
        }
    }

    private static void collectModonomicon(Map<String, ItemStack> found) {
        if (!ModList.get().isLoaded("modonomicon")) {
            return;
        }
        try {
            Class<?> managerClass = Class.forName("com.klikli_dev.modonomicon.data.BookDataManager");
            Object manager = managerClass.getMethod("get").invoke(null);
            Object booksObject = managerClass.getMethod("getBooks").invoke(manager);
            if (!(booksObject instanceof Map<?, ?> loaded)) {
                return;
            }

            Class<?> items = Class.forName("com.klikli_dev.modonomicon.registry.ItemRegistry");
            Object itemHolder = items.getField("MODONOMICON").get(null);
            Object itemObject = itemHolder.getClass().getMethod("get").invoke(itemHolder);
            if (!(itemObject instanceof Item modonomiconItem)) {
                return;
            }

            Class<?> components = Class.forName("com.klikli_dev.modonomicon.registry.DataComponentRegistry");
            Object componentHolder = components.getField("BOOK_ID").get(null);
            Object componentObject = componentHolder.getClass().getMethod("get").invoke(componentHolder);
            if (!(componentObject instanceof DataComponentType<?> bookIdType)) {
                return;
            }

            List<Object> bookObjects = new ArrayList<>(loaded.values());
            bookObjects.sort(Comparator.comparing(book -> {
                try {
                    Object id = book.getClass().getMethod("getId").invoke(book);
                    return String.valueOf(id);
                } catch (ReflectiveOperationException ex) {
                    return "";
                }
            }));

            for (Object book : bookObjects) {
                try {
                    Object idObject = book.getClass().getMethod("getId").invoke(book);
                    if (!(idObject instanceof ResourceLocation bookId)) {
                        continue;
                    }
                    boolean generate = Boolean.TRUE.equals(book.getClass().getMethod("generateBookItem").invoke(book));
                    if (generate) {
                        ItemStack stack = new ItemStack(modonomiconItem);
                        setComponent(stack, bookIdType, bookId);
                        add(found, stack);
                        continue;
                    }
                    Object customObject = book.getClass().getMethod("getCustomBookItem").invoke(book);
                    if (customObject instanceof ResourceLocation customId && BuiltInRegistries.ITEM.containsKey(customId)) {
                        Item custom = BuiltInRegistries.ITEM.get(customId);
                        if (custom != null && custom != Items.AIR) {
                            add(found, new ItemStack(custom));
                        }
                    }
                } catch (ReflectiveOperationException ex) {
                    GuidebookTome.LOGGER.warn("Skipping a Modonomicon book", ex);
                }
            }
        } catch (ReflectiveOperationException ex) {
            GuidebookTome.LOGGER.warn("Modonomicon is loaded, but its book API could not be read", ex);
        }
    }

    private static void collectTaggedAndMarkedItems(Map<String, ItemStack> found) {
        List<Item> items = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(items::add);
        items.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        for (Item item : items) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || "minecraft".equals(id.getNamespace())) {
                continue;
            }
            ItemStack stack = item.getDefaultInstance();
            if (stack.isEmpty()) {
                continue;
            }
            if (item instanceof IModdedBook || hasGuideTag(stack)) {
                add(found, stack);
            }
        }
    }

    private static boolean hasGuideTag(ItemStack stack) {
        for (TagKey<Item> tag : GUIDE_TAGS) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlankPatchouliBook(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !"patchouli".equals(id.getNamespace()) || !"guide_book".equals(id.getPath())) {
            return false;
        }
        return readLocation(stack, "patchouli", "book") == null;
    }

    public static ResourceLocation bookOwner(ItemStack stack) {
        ResourceLocation patchouli = readLocation(stack, "patchouli", "book");
        if (patchouli != null) {
            return patchouli;
        }
        return readLocation(stack, "modonomicon", "book_id");
    }

    private static ResourceLocation readLocation(ItemStack stack, String namespace, String path) {
        ResourceLocation componentId = ResourceLocation.fromNamespaceAndPath(namespace, path);
        if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(componentId)) {
            return null;
        }
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(componentId);
        if (type == null) {
            return null;
        }
        Object value = readComponent(stack, type);
        return value instanceof ResourceLocation location ? location : null;
    }

    @SuppressWarnings("unchecked")
    private static Object readComponent(ItemStack stack, DataComponentType<?> type) {
        return stack.get((DataComponentType<Object>) type);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setComponent(ItemStack stack, DataComponentType<?> type, T value) {
        stack.set((DataComponentType<T>) type, value);
    }
}
