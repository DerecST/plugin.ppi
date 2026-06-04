package ppi.pickplayer;

import io.papermc.paper.event.player.PlayerPickEntityEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class _Listener implements Listener {

    @EventHandler
    public void onPlayerPickEntity(PlayerPickEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player target)) {
            return;
        }

        ItemStack reference = target.getInventory().getItemInMainHand();
        if (reference == null || reference.getType() == Material.AIR || reference.getAmount() < 1) {
            event.setCancelled(true);
            return;
        }

        Player picker = event.getPlayer();
        int sourceSlot = findSimilarSlot(picker.getInventory(), reference);
        if (sourceSlot < 0) {
            if (picker.getGameMode() == GameMode.CREATIVE) {
                return;
            }
            event.setCancelled(true);
            return;
        }

        event.setSourceSlot(sourceSlot);
    }

    private static int findSimilarSlot(PlayerInventory inventory, ItemStack reference) {
        for (int slot = 0; slot <= 35; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            if (stack.isSimilar(reference)) {
                return slot;
            }
        }
        return -1;
    }
}
