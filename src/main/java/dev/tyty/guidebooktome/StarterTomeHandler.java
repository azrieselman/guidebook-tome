package dev.tyty.guidebooktome;

import dev.tyty.guidebooktome.index.GuidebookIndex;
import dev.tyty.guidebooktome.tome.TomeFiller;

import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import vazkii.akashictome.MorphingHandler;

public final class StarterTomeHandler {
    private StarterTomeHandler() {
    }

    public static void register() {
        GuidebookIndex.register();
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, StarterTomeHandler::onPlayerLogin);
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        if (!Config.ENABLED.get() || player.getData(ModAttachments.RECEIVED_TOME)) {
            return;
        }
        var server = player.getServer();
        if (server == null) {
            return;
        }
        server.tell(new TickTask(server.getTickCount() + 1, () -> deliver(player)));
    }

    private static void deliver(ServerPlayer player) {
        if (player.hasDisconnected() || !Config.ENABLED.get() || player.getData(ModAttachments.RECEIVED_TOME)) {
            return;
        }
        GuidebookIndex.rebuild();
        var books = GuidebookIndex.books();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!MorphingHandler.isAkashicTome(stack)) {
                continue;
            }
            TomeFiller.fill(stack, books);
            inventory.setItem(slot, stack);
            player.setData(ModAttachments.RECEIVED_TOME, true);
            GuidebookTome.LOGGER.info("Filled {}'s existing Akashic Tome with {} guidebook(s)", player.getGameProfile().getName(), books.size());
            return;
        }

        ItemStack tome = TomeFiller.newTome(books);
        if (!inventory.add(tome)) {
            player.drop(tome, false);
        }
        player.setData(ModAttachments.RECEIVED_TOME, true);
        GuidebookTome.LOGGER.info("Gave {} an Akashic Tome with {} guidebook(s)", player.getGameProfile().getName(), books.size());
    }
}
