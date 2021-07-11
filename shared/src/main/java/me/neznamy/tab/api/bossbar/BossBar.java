package me.neznamy.tab.api.bossbar;

import java.util.Set;
import java.util.UUID;

import me.neznamy.tab.api.TabPlayer;

public interface BossBar {

	/**
	 * Returns name of this bossbar
	 * @return name of bossbar
	 */
	public String getName();
	
	/**
	 * Returns uuid of this bossbar
	 * @return uuid of this bossbar
	 */
	public UUID getUniqueId();
	
	/**
	 * Sets bossbar title to specified string, supporting placeholders
	 * @param title - new title
	 */
	public void setTitle(String title);
	
	/**
	 * Sets progress to specified string with placeholder support
	 * @param progress - progress placeholder
	 */
	public void setProgress(String progress);
	
	/**
	 * Sets progress to specified value
	 * @param progress - new progress
	 */
	public void setProgress(float progress);
	
	/**
	 * Sets color to specified value supporting placeholders
	 * @param color - color placeholder / value
	 */
	public void setColor(String color);
	
	/**
	 * Sets color to specified value
	 * @param color - new color
	 */
	public void setColor(BarColor color);
	
	/**
	 * Sets style to specified value supporting placeholders
	 * @param style - style placeholder / value
	 */
	public void setStyle(String style);
	
	/**
	 * Sets style to specified value
	 * @param style - new style
	 */
	public void setStyle(BarStyle style);
	
	/**
	 * Returns title of the bossbar
	 * @return title of the bossbar
	 */
	public String getTitle();
	
	/**
	 * Returns progress of the bossbar as string, which is either entered string 
	 * containing placeholders or entered number converted to string
	 * @return entered progress as string
	 */
	public String getProgress();
	
	/**
	 * Returns color of the bossbar as string, which is either entered string 
	 * containing placeholders or entered enum value converted to string
	 * @return entered color as string
	 */
	public String getColor();
	
	/**
	 * Returns style of the bossbar as string, which is either entered string 
	 * containing placeholders or entered enum value converted to string
	 * @return entered style as string
	 */
	public String getStyle();
	
	/**
	 * Sends this bossbar to specified player. If the player already sees this bossbar, nothing happens.
	 * @param player - player to register this bossbar to
	 */
	public void addPlayer(TabPlayer player);
	
	/**
	 * Removes this bossbar from specified player. If the player does not see this bossbat, nothing happens.
	 * @param player - player to remove this bossbar from
	 */
	public void removePlayer(TabPlayer player);
	
	/**
	 * Returns set of players seeing this bossbar
	 * @return set of players seeing this bossbar
	 */
	public Set<TabPlayer> getPlayers();
}