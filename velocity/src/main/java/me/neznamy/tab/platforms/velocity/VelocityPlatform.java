package me.neznamy.tab.platforms.velocity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile.Property;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.chat.EnumChatFormat;
import me.neznamy.tab.api.protocol.PacketBuilder;
import me.neznamy.tab.platforms.velocity.event.TabPlayerLoadEvent;
import me.neznamy.tab.platforms.velocity.event.TabLoadEvent;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.features.PluginMessageHandler;
import me.neznamy.tab.shared.features.bossbar.BossBarManagerImpl;
import me.neznamy.tab.shared.features.globalplayerlist.GlobalPlayerlist;
import me.neznamy.tab.shared.permission.LuckPerms;
import me.neznamy.tab.shared.permission.PermissionPlugin;
import me.neznamy.tab.shared.permission.VaultBridge;
import me.neznamy.tab.shared.placeholders.UniversalPlaceholderRegistry;
import me.neznamy.tab.shared.proxy.ProxyPlatform;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;

/**
 * Velocity implementation of Platform
 */
public class VelocityPlatform extends ProxyPlatform {

	//instance of proxyserver
	private ProxyServer server;
	
	private VelocityPacketBuilder packetBuilder = new VelocityPacketBuilder();
	
	/**
	 * Constructs new instance with given parameter
	 * @param server - instance of proxyserver
	 */
	public VelocityPlatform(ProxyServer server, PluginMessageHandler plm) {
		super(plm);
		this.server = server;
	}
	
	@Override
	public PermissionPlugin detectPermissionPlugin() {
		Optional<PluginContainer> luckperms = server.getPluginManager().getPlugin("luckperms");
		if (TAB.getInstance().getConfiguration().isBukkitPermissions()) {
			return new VaultBridge(plm);
		} else if (luckperms.isPresent()) {
			Optional<String> version = luckperms.get().getDescription().getVersion();
			return new LuckPerms(version.isPresent() ? version.get() : "null");
		} else {
			return new VaultBridge(plm);
		}
	}
	
	@Override
	public void loadFeatures() {
		TAB tab = TAB.getInstance();
		new VelocityPlaceholderRegistry(server).registerPlaceholders(tab.getPlaceholderManager());
		new UniversalPlaceholderRegistry().registerPlaceholders(tab.getPlaceholderManager());
		tab.loadUniversalFeatures();
		if (tab.getConfiguration().getConfig().getBoolean("bossbar.enabled", false)) tab.getFeatureManager().registerFeature("bossbar", new BossBarManagerImpl());
		if (tab.getConfiguration().getConfig().getBoolean("global-playerlist.enabled", false)) 	tab.getFeatureManager().registerFeature("globalplayerlist", new GlobalPlayerlist());
		for (Player p : server.getAllPlayers()) {
			tab.addPlayer(new VelocityTabPlayer(p, plm));
		}
	}
	
	@Override
	public void sendConsoleMessage(String message, boolean translateColors) {
		server.getConsoleCommandSource().sendMessage(Identity.nil(), Component.text(translateColors ? EnumChatFormat.color(message) : message));
	}
	
	@Override
	public String getServerVersion() {
		return server.getVersion().getName() + " v" + server.getVersion().getVersion();
	}

	@Override
	public String getSeparatorType() {
		return "server";
	}

	@Override
	public File getDataFolder() {
		return new File("plugins" + File.separatorChar + "TAB");
	}

	@Override
	public void callLoadEvent() {
		server.getEventManager().fireAndForget(new TabLoadEvent());
	}
	
	@Override
	public void callLoadEvent(TabPlayer player) {
		server.getEventManager().fireAndForget(new TabPlayerLoadEvent(player));
	}

	@Override
	public int getMaxPlayers() {
		return server.getConfiguration().getShowMaxPlayers();
	}
	
	@Override
	public String getConfigName() {
		return "velocityconfig.yml";
	}

	@Override
	public PacketBuilder getPacketBuilder() {
		return packetBuilder;
	}

	@Override
	public Object getSkin(List<String> properties) {
		List<Property> list = new ArrayList<>();
		list.add(new Property("textures", properties.get(0), properties.get(1)));
		return list;
	}
}