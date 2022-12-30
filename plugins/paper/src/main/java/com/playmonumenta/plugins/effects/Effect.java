package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
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
import org.jetbrains.annotations.Nullable;

/* NOTE:
 *
 * Effects should not themselves modify other effects when any of the below methods are called
 * If you need to do this, you should use Bukkit.getScheduler().runTask(...) to update the effects after the current operation finishes processing
 */
public abstract class Effect implements Comparable<Effect>, DisplayableEffect {

	protected int mDuration;
	public String mEffectID;
	private boolean mDisplay = true;
	private boolean mDisplayTime = true;

	public Effect(int duration, String effectID) {
		mDuration = duration;
		mEffectID = effectID;
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

	public String getEffectID() {
		return mEffectID;
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

	// Serialize effects into JSON for debug and saving purposes.
	// Override this if we need to save the effect, for effects that we can care less about
	// though, just leave this be for String output purposes.
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("output", toString());

		return object;
	}

	// Deserialize effects from JSON to create a new Effect Object, to be added on login.
	// If we don't want the effect to be loaded on login, leave this be.
	public static @Nullable Effect deserialize(JsonObject object, Plugin plugin) {
		return null;
	}

	// This is used by the Cursed Wound enhancement to determine if the effect should be stored and transferred
	// Default to false, only make true for relatively simple effects
	public boolean isDebuff() {
		return false;
	}

	// This is used by the Heavenly Boon enhancement to determine if the effect should be stored and transferred
	// Default to false, only make true for relatively simple effects
	public boolean isBuff() {
		return false;
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

	@Override
	public @Nullable String getDisplay() {
		if (mDisplay) {
			String specificDisplay = getSpecificDisplay();
			if (specificDisplay != null) {
				return ChatColor.GREEN + specificDisplay + (mDisplayTime ? " " + ChatColor.GRAY + StringUtils.intToMinuteAndSeconds(mDuration / 20) : "");
			}
		}
		return null;
	}

	@Override
	public int getDisplayPriority() {
		if (!mDisplayTime) {
			return -1;
		}
		return mDuration;
	}

	public Effect displays(boolean display) {
		mDisplay = display;
		return this;
	}

	public Effect displaysTime(boolean displayTime) {
		mDisplayTime = displayTime;
		return this;
	}

	/* Must implement this method to print info about what the effect does for debug */
	@Override
	public abstract String toString();

}
