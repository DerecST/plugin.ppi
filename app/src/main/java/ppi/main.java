package ppi;

import org.bukkit.plugin.java.JavaPlugin;
import ppi.pickplayer.PacketHook;
import ppi.pickplayer.VanillaPickInvoker;
import ppi.pickplayer._Listener;

public final class main extends JavaPlugin {
    @Override
    public void onEnable() {
        VanillaPickInvoker.init();
        getServer().getPluginManager().registerEvents(new _Listener(), this);
        PacketHook packetHook = new PacketHook(this);
        getServer().getPluginManager().registerEvents(packetHook, this);
        packetHook.register();
    }
}
