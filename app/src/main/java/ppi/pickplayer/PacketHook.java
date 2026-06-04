package ppi.pickplayer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PacketHook implements Listener {

    private static final String HANDLER_NAME = "pickplayeritem_handler";
    private static final double PICK_RANGE = 3.0;

    private final JavaPlugin plugin;

    public PacketHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!VanillaPickInvoker.isAvailable()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        uninject(event.getPlayer());
    }

    private void inject(Player player) {
        try {
            Channel channel = getChannel(player);
            if (channel == null) {
                return;
            }
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                return;
            }
            pipeline.addBefore("packet_handler", HANDLER_NAME, new PickEntityHandler(player));
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private void uninject(Player player) {
        try {
            Channel channel = getChannel(player);
            if (channel == null) {
                return;
            }
            ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Channel getChannel(Player player) throws ReflectiveOperationException {
        Method getHandle = player.getClass().getMethod("getHandle");
        Object nmsPlayer = getHandle.invoke(player);
        Object connection = nmsPlayer.getClass().getField("connection").get(nmsPlayer);
        Field networkField = connection.getClass().getField("connection");
        Object networkManager = networkField.get(connection);
        return (Channel) networkManager.getClass().getField("channel").get(networkManager);
    }

    private void processPickFromEntity(Player player, int entityId, boolean includeData) {
        if (!player.isOnline()) {
            return;
        }

        Entity target = findEntityById(player, entityId);
        if (target == null || !isWithinPickRange(player, target)) {
            return;
        }

        if (target instanceof Player targetPlayer) {
            ItemStack reference = targetPlayer.getInventory().getItemInMainHand();
            if (reference == null || reference.getType() == Material.AIR || reference.getAmount() < 1) {
                return;
            }
            ItemStack pickStack = reference.clone();
            pickStack.setAmount(1);
            VanillaPickInvoker.pickFromEntity(player, pickStack, target, includeData);
            return;
        }

        ItemStack pick = target.getPickItemStack();
        if (pick != null && !pick.getType().isAir() && pick.getAmount() >= 1) {
            VanillaPickInvoker.pickFromEntity(player, pick, target, includeData);
        }
    }

    private static Entity findEntityById(Player player, int entityId) {
        try {
            Object craftWorld = player.getWorld();
            Object level = craftWorld.getClass().getMethod("getHandle").invoke(craftWorld);
            Object nmsEntity = level.getClass().getMethod("getEntity", int.class).invoke(level, entityId);
            if (nmsEntity == null) {
                return null;
            }
            return (Entity) nmsEntity.getClass().getMethod("getBukkitEntity").invoke(nmsEntity);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean isWithinPickRange(Player picker, Entity target) {
        if (!picker.getWorld().equals(target.getWorld())) {
            return false;
        }
        return picker.getLocation().distanceSquared(target.getLocation()) <= PICK_RANGE * PICK_RANGE;
    }

    private final class PickEntityHandler extends ChannelDuplexHandler {

        private final Player player;

        private PickEntityHandler(Player player) {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!isPickItemFromEntityPacket(msg)) {
                super.channelRead(ctx, msg);
                return;
            }

            try {
                int entityId = (int) msg.getClass().getMethod("id").invoke(msg);
                boolean includeData = (boolean) msg.getClass().getMethod("includeData").invoke(msg);
                Bukkit.getScheduler().runTask(plugin, () -> processPickFromEntity(player, entityId, includeData));
            } catch (ReflectiveOperationException e) {
                super.channelRead(ctx, msg);
            }
        }

        private static boolean isPickItemFromEntityPacket(Object msg) {
            return msg != null && msg.getClass().getSimpleName().contains("PickItemFromEntity");
        }
    }
}
