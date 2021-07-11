package me.neznamy.tab.shared.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.shared.ITabPlayer;
import me.neznamy.tab.shared.PacketAPI;
import me.neznamy.tab.shared.Property;
import me.neznamy.tab.shared.PropertyUtils;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.cpu.UsageType;
import me.neznamy.tab.shared.features.sorting.Sorting;
import me.neznamy.tab.shared.packets.PacketPlayOutScoreboardTeam;

public class NameTag extends TabFeature {

	private boolean collisionRule;
	private List<String> revertedCollision;
	private boolean invisibleNametags;
	protected Set<String> invisiblePlayers = new HashSet<>();
	private Sorting sorting;
	protected Map<String, Boolean> collision = new HashMap<>();

	public NameTag() {
		disabledWorlds = TAB.getInstance().getConfiguration().getConfig().getStringList("disable-features-in-"+TAB.getInstance().getPlatform().getSeparatorType()+"s.nametag", Arrays.asList("disabled" + TAB.getInstance().getPlatform().getSeparatorType()));
		collisionRule = TAB.getInstance().getConfiguration().getConfig().getBoolean("enable-collision", true);
		revertedCollision = TAB.getInstance().getConfiguration().getConfig().getStringList("revert-collision-rule-in-" + TAB.getInstance().getPlatform().getSeparatorType()+"s", Arrays.asList("reverted" + TAB.getInstance().getPlatform().getSeparatorType()));
		invisibleNametags = TAB.getInstance().getConfiguration().getConfig().getBoolean("invisible-nametags", false);
		sorting = new Sorting(this);
		usedPlaceholders = new ArrayList<>(TAB.getInstance().getConfiguration().getConfig().getUsedPlaceholderIdentifiersRecursive(PropertyUtils.TAGPREFIX, PropertyUtils.TAGSUFFIX));
		for (TabPlayer p : TAB.getInstance().getPlayers()) {
			usedPlaceholders.addAll(TAB.getInstance().getPlaceholderManager().getUsedPlaceholderIdentifiersRecursive(
					p.getProperty(PropertyUtils.TAGPREFIX).getCurrentRawValue(), p.getProperty(PropertyUtils.TAGSUFFIX).getCurrentRawValue()));
		}
		TAB.getInstance().debug(String.format("Loaded NameTag feature with parameters collisionRule=%s, revertedCollision=%s, disabledWorlds=%s, invisibleNametags=%s",
				collisionRule, revertedCollision, disabledWorlds, invisibleNametags));
	}
	
	@Override
	public void load(){
		for (TabPlayer all : TAB.getInstance().getPlayers()) {
			((ITabPlayer) all).setTeamName(getSorting().getTeamName(all));
			updateProperties(all);
			collision.put(all.getName(), true);
			if (all.hasInvisibilityPotion()) invisiblePlayers.add(all.getName());
			if (isDisabledWorld(disabledWorlds, all.getWorldName())) {
				playersInDisabledWorlds.add(all);
				continue;
			}
			registerTeam(all);
		}
		startRefreshingTasks();
	}
	
	@Override
	public void unload() {
		for (TabPlayer p : TAB.getInstance().getPlayers()) {
			if (!playersInDisabledWorlds.contains(p)) unregisterTeam(p);
		}
	}
	
	public void startRefreshingTasks() {
		//workaround for a 1.8.x client-sided bug
		TAB.getInstance().getCPUManager().startRepeatingMeasuredTask(500, "refreshing nametag visibility", getFeatureType(), UsageType.REFRESHING_NAMETAG_VISIBILITY_AND_COLLISION, () -> {

			for (TabPlayer p : TAB.getInstance().getPlayers()) {
				if (!p.isLoaded() || playersInDisabledWorlds.contains(p)) continue;
				//nametag visibility
				boolean invisible = p.hasInvisibilityPotion();
				if (invisible && !invisiblePlayers.contains(p.getName())) {
					invisiblePlayers.add(p.getName());
					updateTeamData(p);
				}
				if (!invisible && invisiblePlayers.contains(p.getName())) {
					invisiblePlayers.remove(p.getName());
					updateTeamData(p);
				}
				//cannot control collision rule on <1.9 servers in any way
				if (TAB.getInstance().getServerVersion().getMinorVersion() >= 9) updateCollision(p);
			}
		});
	}
	
	public boolean isDisabledWorld(String world) {
		return isDisabledWorld(disabledWorlds, world);
	}

	public Set<String> getInvisiblePlayers(){
		return invisiblePlayers;
	}

	public void unregisterTeam(TabPlayer p) {
		if (p.hasTeamHandlingPaused()) return;
		if (p.getTeamName() == null) return;
		for (TabPlayer viewer : TAB.getInstance().getPlayers()) {
			viewer.sendCustomPacket(new PacketPlayOutScoreboardTeam(p.getTeamName()), getFeatureType());
		}
	}

	public void unregisterTeam(TabPlayer p, TabPlayer viewer) {
		if (p.hasTeamHandlingPaused()) return;
		viewer.sendCustomPacket(new PacketPlayOutScoreboardTeam(p.getTeamName()), getFeatureType());
	}

	public void registerTeam(TabPlayer p) {
		if (p.hasTeamHandlingPaused()) return;
		Property tagprefix = p.getProperty(PropertyUtils.TAGPREFIX);
		Property tagsuffix = p.getProperty(PropertyUtils.TAGSUFFIX);
		for (TabPlayer viewer : TAB.getInstance().getPlayers()) {
			String currentPrefix = tagprefix.getFormat(viewer);
			String currentSuffix = tagsuffix.getFormat(viewer);
			PacketAPI.registerScoreboardTeam(viewer, p.getTeamName(), currentPrefix, currentSuffix, getTeamVisibility(p, viewer), getCollision(p), Arrays.asList(p.getName()), null, getFeatureType());
		}
	}

	public void registerTeam(TabPlayer p, TabPlayer viewer) {
		if (p.hasTeamHandlingPaused()) return;
		Property tagprefix = p.getProperty(PropertyUtils.TAGPREFIX);
		Property tagsuffix = p.getProperty(PropertyUtils.TAGSUFFIX);
		String replacedPrefix = tagprefix.getFormat(viewer);
		String replacedSuffix = tagsuffix.getFormat(viewer);
		PacketAPI.registerScoreboardTeam(viewer, p.getTeamName(), replacedPrefix, replacedSuffix, getTeamVisibility(p, viewer), getCollision(p), Arrays.asList(p.getName()), null, getFeatureType());
	}

	public void updateTeam(TabPlayer p) {
		if (p.getTeamName() == null) return; //player not loaded yet
		String newName = getSorting().getTeamName(p);
		if (p.getTeamName().equals(newName)) {
			updateTeamData(p);
		} else {
			unregisterTeam(p);
			((ITabPlayer) p).setTeamName(newName);
			registerTeam(p);
		}
	}

	public void updateTeamData(TabPlayer p) {
		Property tagprefix = p.getProperty(PropertyUtils.TAGPREFIX);
		Property tagsuffix = p.getProperty(PropertyUtils.TAGSUFFIX);
		for (TabPlayer viewer : TAB.getInstance().getPlayers()) {
			String currentPrefix = tagprefix.getFormat(viewer);
			String currentSuffix = tagsuffix.getFormat(viewer);
			boolean visible = getTeamVisibility(p, viewer);
			viewer.sendCustomPacket(new PacketPlayOutScoreboardTeam(p.getTeamName(), currentPrefix, currentSuffix, translate(visible), translate(getCollision(p)), 0), getFeatureType());
		}
	}

	public void updateTeamData(TabPlayer p, TabPlayer viewer) {
		Property tagprefix = p.getProperty(PropertyUtils.TAGPREFIX);
		Property tagsuffix = p.getProperty(PropertyUtils.TAGSUFFIX);
		boolean visible = getTeamVisibility(p, viewer);
		String currentPrefix = tagprefix.getFormat(viewer);
		String currentSuffix = tagsuffix.getFormat(viewer);
		viewer.sendCustomPacket(new PacketPlayOutScoreboardTeam(p.getTeamName(), currentPrefix, currentSuffix, translate(visible), translate(getCollision(p)), 0), getFeatureType());
	}
	
	private String translate(boolean b) {
		return b ? "always" : "never";
	}
	
	private void updateCollision(TabPlayer p) {
		if (TAB.getInstance().getFeatureManager().getNameTagFeature() == null || !p.isOnline()) return;
		if (p.getCollisionRule() != null) {
			if (getCollision(p) != p.getCollisionRule()) {
				collision.put(p.getName(), p.getCollisionRule());
				updateTeamData(p);
			}
		} else {
			boolean newCollision = !p.isDisguised() && revertedCollision.contains(p.getWorldName()) ? !collisionRule : collisionRule;
			if (collision.get(p.getName()) == null || getCollision(p) != newCollision) {
				collision.put(p.getName(), newCollision);
				updateTeamData(p);
			}
		}
	}
	
	protected Boolean getCollision(TabPlayer p) {
		if (!p.isOnline()) return false;
		if (p.getCollisionRule() != null) return p.getCollisionRule();
		if (!collision.containsKey(p.getName())) {
			collision.put(p.getName(), revertedCollision.contains(p.getWorldName()) ? !collisionRule : collisionRule);
		}
		return collision.get(p.getName());
	}
	

	@Override
	public void onLoginPacket(TabPlayer packetReceiver) {
		for (TabPlayer all : TAB.getInstance().getPlayers()) {
			if (!all.isLoaded()) continue;
			if (!playersInDisabledWorlds.contains(all)) registerTeam(all, packetReceiver);
		}
	}
	
	@Override
	public void refresh(TabPlayer refreshed, boolean force) {
		if (playersInDisabledWorlds.contains(refreshed)) return;
		boolean refresh;
		if (force) {
			updateProperties(refreshed);
			refresh = true;
		} else {
			boolean prefix = refreshed.getProperty(PropertyUtils.TAGPREFIX).update();
			boolean suffix = refreshed.getProperty(PropertyUtils.TAGSUFFIX).update();
			refresh = prefix || suffix;
		}

		if (refresh) updateTeam(refreshed);
	}

	@Override
	public void onJoin(TabPlayer connectedPlayer) {
		((ITabPlayer) connectedPlayer).setTeamName(getSorting().getTeamName(connectedPlayer));
		updateProperties(connectedPlayer);
		collision.put(connectedPlayer.getName(), true);
		for (TabPlayer all : TAB.getInstance().getPlayers()) {
			if (!all.isLoaded() || all == connectedPlayer) continue; //avoiding double registration
			if (!playersInDisabledWorlds.contains(all)) {
				registerTeam(all, connectedPlayer);
			}
		}
		if (isDisabledWorld(disabledWorlds, connectedPlayer.getWorldName())) {
			playersInDisabledWorlds.add(connectedPlayer);
			return;
		}
		registerTeam(connectedPlayer);
	}
	
	@Override
	public void onQuit(TabPlayer disconnectedPlayer) {
		if (!playersInDisabledWorlds.contains(disconnectedPlayer)) unregisterTeam(disconnectedPlayer);
		invisiblePlayers.remove(disconnectedPlayer.getName());
		collision.remove(disconnectedPlayer.getName());
		playersInDisabledWorlds.remove(disconnectedPlayer);
		for (TabPlayer all : TAB.getInstance().getPlayers()) {
			if (all == disconnectedPlayer) continue;
			all.showNametag(disconnectedPlayer.getUniqueId()); //clearing memory from API method
		}
	}

	@Override
	public void onWorldChange(TabPlayer p, String from, String to) {
		boolean disabledBefore = playersInDisabledWorlds.contains(p);
		boolean disabledNow = false;
		if (isDisabledWorld(disabledWorlds, p.getWorldName())) {
			disabledNow = true;
			playersInDisabledWorlds.add(p);
		} else {
			playersInDisabledWorlds.remove(p);
		}
		updateProperties(p);
		if (disabledNow && !disabledBefore) {
			unregisterTeam(p);
		} else if (!disabledNow && disabledBefore) {
			registerTeam(p);
		} else {
			updateTeam(p);
		}
	}

	@Override
	public void refreshUsedPlaceholders() {
		usedPlaceholders = new ArrayList<>(TAB.getInstance().getConfiguration().getConfig().getUsedPlaceholderIdentifiersRecursive(PropertyUtils.TAGPREFIX, PropertyUtils.TAGSUFFIX));
		for (TabPlayer p : TAB.getInstance().getPlayers()) {
			usedPlaceholders.addAll(TAB.getInstance().getPlaceholderManager().getUsedPlaceholderIdentifiersRecursive(
					p.getProperty(PropertyUtils.TAGPREFIX).getCurrentRawValue(), p.getProperty(PropertyUtils.TAGSUFFIX).getCurrentRawValue()));
		}
	}

	public void updateProperties(TabPlayer p) {
		p.loadPropertyFromConfig(PropertyUtils.TAGPREFIX);
		p.loadPropertyFromConfig(PropertyUtils.TAGSUFFIX);
	}
	
	public boolean getTeamVisibility(TabPlayer p, TabPlayer viewer) {
		return !p.hasHiddenNametag() && !p.hasHiddenNametag(viewer.getUniqueId()) && 
			!invisibleNametags && !invisiblePlayers.contains(p.getName());
	}
	
	@Override
	public String getFeatureType() {
		return "Nametags";
	}

	public Sorting getSorting() {
		return sorting;
	}
	
	public Set<TabPlayer> getPlayersInDisabledWorlds(){
		return playersInDisabledWorlds;
	}
}