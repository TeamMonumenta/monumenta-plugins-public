package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;


public class PotionUtils {

	public static final ImmutableSet<PotionEffectType> POSITIVE_EFFECTS = ImmutableSet.of(
			PotionEffectType.ABSORPTION,
			PotionEffectType.DAMAGE_RESISTANCE,
			PotionEffectType.FAST_DIGGING,
			PotionEffectType.FIRE_RESISTANCE,
			PotionEffectType.HEAL,
			PotionEffectType.HEALTH_BOOST,
			PotionEffectType.INCREASE_DAMAGE,
			PotionEffectType.INVISIBILITY,
			PotionEffectType.JUMP,
			PotionEffectType.NIGHT_VISION,
			PotionEffectType.REGENERATION,
			PotionEffectType.SATURATION,
			PotionEffectType.SPEED,
			PotionEffectType.LUCK,
			PotionEffectType.WATER_BREATHING,
			PotionEffectType.CONDUIT_POWER
	);

	private static final ImmutableSet<PotionEffectType> NEGATIVE_EFFECTS = ImmutableSet.of(
			PotionEffectType.BLINDNESS,
			PotionEffectType.POISON,
			PotionEffectType.CONFUSION,
			PotionEffectType.SLOW,
			PotionEffectType.SLOW_DIGGING,
			PotionEffectType.SLOW_FALLING,
			PotionEffectType.WITHER,
			PotionEffectType.WEAKNESS,
			PotionEffectType.HARM,
			PotionEffectType.HUNGER,
			PotionEffectType.LEVITATION,
			PotionEffectType.UNLUCK,
			PotionEffectType.DARKNESS
	);

	public static final ImmutableSet<PotionType> BASE_POTION_ITEM_TYPES = ImmutableSet.of(PotionType.AWKWARD, PotionType.THICK, PotionType.MUNDANE, PotionType.WATER);

	// This map only notes any "useful" effect pairs, i.e. effects that would be non-annoying and balanced to invert
	private static final Map<PotionEffectType, PotionEffectType> OPPOSITE_EFFECTS = new HashMap<PotionEffectType, PotionEffectType>();

	static {
		OPPOSITE_EFFECTS.put(PotionEffectType.SPEED, PotionEffectType.SLOW);
		OPPOSITE_EFFECTS.put(PotionEffectType.SLOW, PotionEffectType.SPEED);
		OPPOSITE_EFFECTS.put(PotionEffectType.FAST_DIGGING, PotionEffectType.SLOW_DIGGING);
		OPPOSITE_EFFECTS.put(PotionEffectType.SLOW_DIGGING, PotionEffectType.FAST_DIGGING);
		OPPOSITE_EFFECTS.put(PotionEffectType.REGENERATION, PotionEffectType.WITHER);
		OPPOSITE_EFFECTS.put(PotionEffectType.WITHER, PotionEffectType.REGENERATION);
		OPPOSITE_EFFECTS.put(PotionEffectType.POISON, PotionEffectType.REGENERATION);
		OPPOSITE_EFFECTS.put(PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.UNLUCK);
		OPPOSITE_EFFECTS.put(PotionEffectType.UNLUCK, PotionEffectType.DAMAGE_RESISTANCE);
		OPPOSITE_EFFECTS.put(PotionEffectType.INCREASE_DAMAGE, PotionEffectType.WEAKNESS);
		OPPOSITE_EFFECTS.put(PotionEffectType.WEAKNESS, PotionEffectType.INCREASE_DAMAGE);
	}

	public static class PotionInfo {
		public @Nullable PotionEffectType mType;
		public int mDuration;
		public int mAmplifier;
		public boolean mAmbient;
		public boolean mShowParticles;
		public boolean mShowIcon;
		public boolean mInfinite;

		public PotionInfo(PotionEffect effect) {
			mType = effect.getType();
			mDuration = effect.getDuration();
			mAmplifier = effect.getAmplifier();
			mAmbient = effect.isAmbient();
			mShowParticles = effect.hasParticles();
			mShowIcon = effect.hasIcon();
			mInfinite = isInfinite(effect.getDuration());
		}

		public PotionInfo(PotionInfo potionInfo) {
			mType = potionInfo.mType;
			mDuration = potionInfo.mDuration;
			mAmplifier = potionInfo.mAmplifier;
			mAmbient = potionInfo.mAmbient;
			mShowParticles = potionInfo.mShowParticles;
			mShowIcon = potionInfo.mShowIcon;
			mInfinite = potionInfo.mInfinite;
		}

		public PotionInfo(@Nullable PotionEffectType type, int duration, int amplifier, boolean ambient,
						  boolean showParticles, boolean showIcon) {
			mType = type;
			mDuration = duration;
			mAmplifier = amplifier;
			mAmbient = ambient;
			mShowParticles = showParticles;
			mShowIcon = showIcon;
			mInfinite = isInfinite(duration);
		}

		public JsonObject getAsJsonObject() {
			JsonObject potionInfoObject = new JsonObject();

			potionInfoObject.addProperty("type", mType == null ? null : mType.getKey().getKey());
			potionInfoObject.addProperty("duration", mDuration);
			potionInfoObject.addProperty("amplifier", mAmplifier);
			potionInfoObject.addProperty("ambient", mAmbient);
			potionInfoObject.addProperty("show_particles", mShowParticles);

			return potionInfoObject;
		}

		public PotionInfo(JsonObject object) throws Exception {
			mType = getTypeByKey(object.get("type").getAsString());
			mDuration = object.get("duration").getAsInt();
			mAmplifier = object.get("amplifier").getAsInt();
			mAmbient = object.get("ambient").getAsBoolean();
			mShowParticles = object.get("show_particles").getAsBoolean();
			mInfinite = isInfinite(mDuration);
		}
	}

	/*
	 * Dividend should be 1 for drink/splash, 4 for lingering potions, 8 for tipped arrows
	 */
	public static List<PotionInfo> getPotionInfoList(PotionType type, int dividend) {
		List<PotionInfo> infos = new ArrayList<>();
		List<PotionEffect> effects = type.getPotionEffects();
		for (PotionEffect effect : effects) {
			infos.add(new PotionInfo(effect.getType(), effect.getDuration() / dividend, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
		}
		return infos;
	}

	public static void apply(LivingEntity entity, PotionInfo info) {
		entity.addPotionEffect(new PotionEffect(info.mType, info.mDuration, info.mAmplifier, info.mAmbient, info.mShowParticles));
	}

	public static List<PotionEffect> getEffects(ItemStack item) {
		if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta) {
			return getEffects((PotionMeta) item.getItemMeta());
		}
		// Not a potion - return an empty list to simplify callers iterating the result
		return new ArrayList<>(0);
	}

	public static List<PotionEffect> getEffects(PotionMeta meta) {
		List<PotionEffect> effectsList = new ArrayList<>();

		PotionType potionType = meta.getBasePotionType();
		List<PotionInfo> infos = getPotionInfoList(potionType, 1);
		for (PotionInfo info : infos) {
			PotionEffect effect = new PotionEffect(info.mType, info.mDuration, info.mAmplifier, info.mAmbient,
				info.mShowParticles);
			effectsList.add(effect);
		}

		if (meta.hasCustomEffects()) {
			List<PotionEffect> effects = meta.getCustomEffects();
			effectsList.addAll(effects);
		}

		return effectsList;
	}

	public static boolean hasPositiveEffects(Collection<PotionEffect> effects) {
		for (PotionEffect effect : effects) {
			if (hasPositiveEffects(effect.getType())) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasPositiveEffects(PotionEffectType type) {
		return POSITIVE_EFFECTS.contains(type);
	}

	public static boolean hasNegativeEffects(ItemStack potionItem) {
		for (PotionEffect effect : getEffects(potionItem)) {
			if (hasNegativeEffects(effect.getType())) {
				if (effect.getDuration() > 30 || effect.getAmplifier() == 0 || effect.getType().equals(PotionEffectType.HARM)) {
					// The negative effect lasts longer than 1s or is only level 1
					// Probably not an antidote / other "good" negative potion
					return true;
				}
			}
		}

		return false;
	}

	public static boolean hasNegativeEffects(PotionEffectType type) {
		return NEGATIVE_EFFECTS.contains(type);
	}

	public static void clearNegatives(Plugin plugin, Player player) {
		boolean dolphin = player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
		for (PotionEffectType type : NEGATIVE_EFFECTS) {
			if (player.hasPotionEffect(type)) {
				if (PotionEffectType.SLOW.equals(type) && dolphin) {
					continue;
				}
				PotionEffect effect = player.getPotionEffect(type);
				if (effect != null && effect.getDuration() < Constants.THIRTY_MINUTES) {
					player.removePotionEffect(effect.getType());
					plugin.mPotionManager.removePotion(player, PotionID.APPLIED_POTION, type);
				}
			}
		}
		// This method removes vanilla effects regardless of source - may need to re-apply effects from non-potion sources afterwards
		plugin.mPotionManager.refreshEffects(player);
	}

	public static void clearNegatives(LivingEntity entity) {
		boolean dolphin = entity.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
		for (PotionEffectType type : NEGATIVE_EFFECTS) {
			if (entity.hasPotionEffect(type)) {
				if (type.equals(PotionEffectType.SLOW) && dolphin) {
					continue;
				}
				PotionEffect effect = entity.getPotionEffect(type);
				if (effect != null && effect.getDuration() < Constants.THIRTY_MINUTES) {
					entity.removePotionEffect(type);
				}
			}
		}
	}

	public static void reduceNegatives(Plugin plugin, Player player, double reduction) {
		plugin.mPotionManager.modifyPotionDuration(player, potionInfo -> {
			if (NEGATIVE_EFFECTS.contains(potionInfo.mType)) {
				return (int) (potionInfo.mDuration * reduction);
			} else {
				return potionInfo.mDuration;
			}
		});
		List<Effect> effects = EffectManager.getInstance().getAllEffects(player);
		for (Effect effect : effects) {
			if (effect.isDebuff()) {
				effect.setDuration((int) (effect.getDuration() * reduction));
			}
		}
	}

	public static void applyPotion(Plugin plugin, Player player, PotionEffect effect) {
		if (effect.getType().equals(PotionEffectType.HEAL)) {
			double health = player.getHealth();
			double healthToAdd = 4 * (effect.getAmplifier() + 1);

			health = Math.min(health + healthToAdd, EntityUtils.getMaxHealth(player));

			player.setHealth(health);
		} else {
			plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
		}
	}

	public static void applyPotion(@Nullable Entity applier, LivingEntity applied, PotionEffect effect) {
		if (applied.hasPotionEffect(effect.getType())) {
			PotionEffect targetPotionEffect = applied.getPotionEffect(effect.getType());
			if (targetPotionEffect == null) {
				return;
			}
			if (targetPotionEffect.getAmplifier() < effect.getAmplifier()
				|| (targetPotionEffect.getAmplifier() == effect.getAmplifier()
					&& compareDurations(effect, targetPotionEffect))
			) {
				PotionEffectApplyEvent event = new PotionEffectApplyEvent(applier, applied, effect);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					applied.addPotionEffect(event.getEffect());
				}
			}
		} else {
			PotionEffectApplyEvent event = new PotionEffectApplyEvent(applier, applied, effect);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				applied.addPotionEffect(event.getEffect());
			}
		}
	}

	public static void applyPotion(Plugin plugin, Player player, @Nullable PotionMeta meta) {
		//Do not run if null to avoid NullPointerException
		if (meta == null) {
			return;
		}

		if (meta.hasCustomEffects()) {
			for (PotionEffect effect : meta.getCustomEffects()) {
				if (Objects.equals(effect.getType(), PotionEffectType.HARM)) {
					//If 10+, kill, if below, deal normal instant damage
					//If instant healing, manually add health
					if (effect.getAmplifier() >= 9) {
						player.setHealth(0);
					} else {
						DamageUtils.damage(null, player, DamageType.MAGIC, 3 * Math.pow(2, effect.getAmplifier() + 1));
					}
				} else if (Objects.equals(effect.getType(), PotionEffectType.HEAL)) {
					PlayerUtils.healPlayer(plugin, player, 2 * Math.pow(2, effect.getAmplifier() + 1));
				}

				plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
			}
		} else {
			List<PotionInfo> infos = getPotionInfoList(meta.getBasePotionType(), 1);
			for (PotionInfo info : infos) {
				//If instant healing, manually add health, otherwise if instant damage, manually remove health, else add effect
				//Check then add health
				if (info.mType != null && info.mType.equals(PotionEffectType.HEAL)) {
					PlayerUtils.healPlayer(plugin, player, 2 * Math.pow(2, info.mAmplifier + 1));
				} else if (info.mType != null && info.mType.equals(PotionEffectType.HARM)) {
					DamageUtils.damage(null, player, DamageType.MAGIC, 3 * Math.pow(2, info.mAmplifier + 1));
				} else {
					plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
				}
			}
		}
	}

	public static List<PotionEffectType> getNegativeEffects(Plugin plugin, LivingEntity le) {
		List<PotionEffectType> types = new ArrayList<>();
		for (PotionEffect effect : le.getActivePotionEffects()) {
			if (NEGATIVE_EFFECTS.contains(effect.getType())) {
				types.add(effect.getType());
			}
		}

		NavigableSet<Effect> slows = plugin.mEffectManager.getEffects(le, EntityUtils.SLOW_EFFECT_NAME);
		if (slows != null && !types.contains(PotionEffectType.SLOW)) {
			types.add(PotionEffectType.SLOW);
		}

		return types;
	}

	public static void removePositiveEffects(PotionMeta potionMeta) {
		for (PotionEffectType type : POSITIVE_EFFECTS) {
			potionMeta.removeCustomEffect(type);
		}
	}

	public static @Nullable PotionEffectType getOppositeEffect(PotionEffectType type) {
		return OPPOSITE_EFFECTS.get(type);
	}

	// Duration is equal to -1 or greater than about 10 hours.
	public static boolean isInfinite(PotionEffect effect) {
		return effect.isInfinite();
	}

	public static boolean isInfinite(int duration) {
		return duration == PotionEffect.INFINITE_DURATION || duration > 1000000;
	}

	/**
	 * This directly applies the splash potion effect to the player and surrounding entities by using the PotionSplashEvent.
	 * Doesn't play splash potion break sound (Sound.ENTITY_SPLASH_POTION_BREAK) or particles
	 *
	 * @param player - The player that splashed the potion
	 * @param potion - The thrown potion entity to splash
	 */
	public static boolean mimicSplashPotionEffect(Player player, ThrownPotion potion) {
		// remove potion immediately after spawning to prevent "ghost" splash potions
		potion.remove();
		Map<LivingEntity, Double> affectedEntites = new HashMap<>();
		// adjust range of potion effects here
		// these values are from vanilla (ThrownPotion.java#L138 - applySplash function)
		List<Entity> nearbyEntities = player.getNearbyEntities(4.0d, 2.0d, 4.0d);
		for (Entity entity : nearbyEntities) {
			if (entity instanceof LivingEntity) {
				// intensity can be 1.0d because PotionSplashEvent listener in EntityListener already scales intensities by distance
				affectedEntites.put((LivingEntity) entity, 1.0d);
			}
		}
		// add the player to the list of affected entities otherwise the player who splashed the potion won't get any effects
		affectedEntites.put(player, 1.0d);

		PotionSplashEvent potionEvent = new PotionSplashEvent(potion, affectedEntites);
		Bukkit.getPluginManager().callEvent(potionEvent);

		// ignore if splash event if cancelled
		if (potionEvent.isCancelled()) {
			return false;
		}
		return true;
	}

	/**
	 * Plays the particle and sound effect of a potion splash
	 */
	public static void splashPotionParticlesAndSound(Player player, @Nullable Color color) {
		if (color == null) {
			color = Color.WHITE;
		}
		player.getWorld().playEffect(player.getLocation(), org.bukkit.Effect.POTION_BREAK, color.asRGB());
	}

	public static void splashPotionParticlesAndSound(Player player, @Nullable Color color, Location loc) {
		if (color == null) {
			color = Color.WHITE;
		}
		player.getWorld().playEffect(loc, org.bukkit.Effect.POTION_BREAK, color.asRGB());
	}

	public static void instantDrinkParticles(Player player, @Nullable Color color) {
		if (color != null) {
			double red = color.getRed() / 255D;
			double green = color.getGreen() / 255D;
			double blue = color.getBlue() / 255D;
			for (int i = 0; i < 30; i++) {
				double y = FastUtils.randomDoubleInRange(0.25, 1.75);
				new PartialParticle(Particle.SPELL_MOB, player.getLocation().add(0, y, 0), 1, red, green, blue, 1)
						.directionalMode(true).spawnAsPlayerActive(player);
			}
		} else {
			new PartialParticle(Particle.SPELL, player.getLocation().add(0, 0.75, 0), 30, 0, 0.45, 0, 1).spawnAsPlayerActive(player);
		}
	}

	private static boolean compareDurations(PotionEffect effect1, PotionEffect effect2) {
		return compareDurations(effect1.getDuration(), effect2.getDuration());
	}

	// Returns true if duration1 is strictly greater than duration 2, accounting for infinite effects
	public static boolean compareDurations(int duration1, int duration2) {
		if (duration2 == -1) {
			return false;
		} else if (duration1 == -1) {
			return true;
		} else {
			return duration1 > duration2;
		}
	}

	@SuppressWarnings("deprecation")
	public static @Nullable PotionEffectType getTypeByKey(String key) {
		return PotionEffectType.getByName(key);
	}
}
