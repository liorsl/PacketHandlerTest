package net.voigon.packethandlertest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class PacketHandlerTest extends JavaPlugin implements Listener {

    static final Map<UUID, PacketHandler> packetHandlers = new HashMap<>();

    @Override
    public void onEnable() {
	Bukkit.getPluginManager().registerEvents(this, this);
	
	for (Player player : Bukkit.getOnlinePlayers())
	    try {
		register(player);
	    } catch (IllegalAccessException e) {
		e.printStackTrace();
	    }
	
    }
    
    @Override
    public void onDisable() {
	try {
	    unregisterAll();
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
	try {
	    register(event.getPlayer());
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	}
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
	try {
	    unregister(event.getPlayer());
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	}
    }
    
    void unregisterAll() throws IllegalAccessException {
	for (UUID uuid : packetHandlers.keySet())
	    unregister(Bukkit.getPlayer(uuid));
	
    }
    
    void register(Player player) throws IllegalAccessException {
	PacketHandler handler = new PacketHandler();
	Channel channel = (Channel) FieldUtils.readDeclaredField(getNetworkManager(player),
		"channel");
	channel.pipeline().addBefore("packet_handler", "WHATEVER_YOU_WANT", handler);
	packetHandlers.put(player.getUniqueId(), handler);

    }

    void unregister(Player player) throws IllegalAccessException {
	Channel channel = (Channel) FieldUtils.readDeclaredField(getNetworkManager(player), "channel");	
	channel.pipeline().remove("WHATEVER_YOU_WANT");
	
	packetHandlers.remove(player.getUniqueId());
    }
    
    public static Object getNetworkManager(Player player) {
	Object con = getConnection(player);
	try {
	    Field field = con.getClass().getDeclaredField("networkManager");
	    return field.get(con);
	} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public static Object getConnection(Player player) {
	try {
	    return getFieldValue(player, true, "playerConnection");
	} catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException
		| IllegalAccessException e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public static Object getFieldValue(Player player, boolean nms, String fieldName) throws ClassNotFoundException,
	    NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

	Class<?> clazz;
	Object instance;
	if (nms) {
	    clazz = NMSUtils.getClass(true, "EntityPlayer");
	    instance = NMSUtils.getHandle(player);
	} else {
	    clazz = NMSUtils.getClass(false, "entity.CraftPlayer");
	    instance = player;
	}

	Field field = clazz.getField(fieldName);
	if (!field.isAccessible())
	    field.setAccessible(true);
	Object value = field.get(instance);

	field.setAccessible(false);

	return value;

    }

    public static class PacketHandler extends ChannelDuplexHandler {

	@Override
	public void write(ChannelHandlerContext arg0, Object arg1, ChannelPromise arg2) throws Exception {
	    System.out.println("packet out " + arg1);
	    super.write(arg0, arg1, arg2);
	}

	@Override
	public void read(ChannelHandlerContext arg0) throws Exception {
	    System.out.println("packet in " + arg0);
	    super.read(arg0);
	}
    }
}
