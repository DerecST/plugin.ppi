package ppi.pickplayer;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public final class VanillaPickInvoker {

    private static Method tryPickItem;
    private static Method asNmsCopy;
    private static Method getHandlePlayer;
    private static Method getHandleEntity;
    private static boolean available = true;

    private VanillaPickInvoker() {
    }

    public static void init() {
        try {
            Class<?> connectionClass = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
            Class<?> nmsItemStack = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> blockPos = Class.forName("net.minecraft.core.BlockPos");
            Class<?> nmsEntity = Class.forName("net.minecraft.world.entity.Entity");

            tryPickItem = connectionClass.getDeclaredMethod(
                    "tryPickItem", nmsItemStack, blockPos, nmsEntity, boolean.class);
            tryPickItem.setAccessible(true);

            Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
            asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);

            getHandlePlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer")
                    .getMethod("getHandle");
            getHandleEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity")
                    .getMethod("getHandle");
        } catch (ReflectiveOperationException e) {
            available = false;
        }
    }

    static boolean isAvailable() {
        return available;
    }

    static void pickFromEntity(Player picker, ItemStack reference, Entity target, boolean includeData) {
        if (!available || reference == null || reference.getType() == Material.AIR) {
            return;
        }

        try {
            Object connection = getConnection(picker);
            if (connection == null) {
                return;
            }

            Object nmsStack = asNmsCopy.invoke(null, reference);
            Object nmsEntity = getHandleEntity.invoke(target);

            tryPickItem.invoke(connection, nmsStack, null, nmsEntity, includeData);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Object getConnection(Player player) throws ReflectiveOperationException {
        Object nmsPlayer = getHandlePlayer.invoke(player);
        return nmsPlayer.getClass().getField("connection").get(nmsPlayer);
    }
}
