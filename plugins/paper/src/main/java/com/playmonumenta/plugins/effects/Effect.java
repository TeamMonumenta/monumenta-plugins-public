package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

/* NOTE:
 *
 * Effects should not themselves modify other effects when any of the below methods are called
 * If you need to do this, you should use Bukkit.getScheduler().runTask(...) to update the effects after the current operation finishes processing
 */
public abstract class Effect implements Comparable<Effect> {

	private int mDuration;

	public Effect(int duration) {
		mDuration = duration;
	}

	public EffectPriority getPriority() {
		return EffectPriority.NORMAL;
	}

	public int getDuration() {
		return mDuration;
	}

	public void setDuration(int duration) {
		mDuration = duration;
	}

	public double getMagnitude() {
		return 0;
	}

	public void clearEffect() {
		mDuration = 0;
	}

	@Override
	public int compareTo(Effect otherEffect) {
		if (getMagnitude() > otherEffect.getMagnitude()) {
			return 1;
		} else if (getMagnitude() < otherEffect.getMagnitude()) {
			return -1;
		} else {
			return 0;
		}
	}

	public boolean entityRegainHealthEvent(EntityRegainHealthEvent event) {
		return true;
	}

	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {

	}

	public void onHurt(LivingEntity entity, DamageEvent event) {

	}

	public void onHurtByEntity(LivingEntity entity, DamageEvent event, Entity damager) {

	}

	public void onHurtByEntityWithSource(LivingEntity entity, DamageEvent event, Entity damager, LivingEntity source) {

	}

	public void onExpChange(Player player, PlayerExpChangeEvent event) {

	}

	public void onProjectileLaunch(Player player, AbstractArrow arrow) {

	}

	public void onDurabilityDamage(Player player, PlayerItemDamageEvent event) {

	}

	public void onDeath(EntityDeathEvent event) {

	}

	public void onKill(EntityDeathEvent event) {

	}

	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {

	}

	public void entityGainEffect(Entity entity) {

	}

	public void entityLoseEffect(Entity entity) {

	}

	/**
	 * Ticks the effect, called regularly
	 *
	 * @param ticks Ticks passed since the last time this method was called to check duration expiry
	 * @return Returns true if effect has expired and should be removed by the EffectManager
	 */
	public boolean tick(int ticks) {
		mDuration -= ticks;
		return mDuration <= 0;
	}

	//Display used by tab list; return null to not display
	public @Nullable String getSpecificDisplay() {
		return null;
	}

	public final @Nullable String getDisplay() {
		String specificDisplay = getSpecificDisplay();
		if (specificDisplay != null) {
			return ChatColor.GREEN + specificDisplay + " " + ChatColor.GRAY + StringUtils.intToMinuteAndSeconds(mDuration / 20);
		}
		return null;
	}

	/* Must implement this method to print info about what the effect does for debug */
	@Override
	public abstract String toString();
}
