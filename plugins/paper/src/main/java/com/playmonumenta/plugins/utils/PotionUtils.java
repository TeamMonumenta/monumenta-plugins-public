package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ColoredGlowingEffect;
import com.playmonumenta.plugins.effects.Effect;
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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;


public class PotionUtils {
	private static final int SECONDS_1 = 20;
	private static final int SECONDS_22_HALF = (int) (22.5 * SECONDS_1);
	private static final int SECONDS_30 = 30 * SECONDS_1;
	private static final int SECONDS_45 = 45 * SECONDS_1;
	private static final int MINUTES_1 = 60 * SECONDS_1;
	private static final int MINUTES_1_HALF = MINUTES_1 + SECONDS_30;
	private static final int MINUTES_3 = MINUTES_1 * 3;
	private static final int MINUTES_5 = MINUTES_1 * 5;
	private static final int MINUTES_8 = MINUTES_1 * 8;

	private static final ImmutableSet<PotionEffectType> POSITIVE_EFFECTS = ImmutableSet.of(
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
			PotionEffectType.UNLUCK
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
		public static final PotionInfo HEALING = new PotionInfo(PotionEffectType.HEAL, 0, 0, false, true, true);
		public static final PotionInfo HEALING_STRONG = new PotionInfo(PotionEffectType.HEAL, 0, 1, false, true, true);

		public static final PotionInfo REGENERATION = new PotionInfo(PotionEffectType.REGENERATION, SECONDS_45, 0, false, true, true);
		public static final PotionInfo REGENERATION_LONG = new PotionInfo(PotionEffectType.REGENERATION, MINUTES_1_HALF, 0, false, true, true);
		public static final PotionInfo REGENERATION_STRONG = new PotionInfo(PotionEffectType.REGENERATION, SECONDS_22_HALF, 1, false, true, true);

		public static final PotionInfo SWIFTNESS = new PotionInfo(PotionEffectType.SPEED, MINUTES_3, 0, false, true, true);
		public static final PotionInfo SWIFTNESS_LONG = new PotionInfo(PotionEffectType.SPEED, MINUTES_8, 0, false, true, true);
		public static final PotionInfo SWIFTNESS_STRONG = new PotionInfo(PotionEffectType.SPEED, MINUTES_1_HALF, 1, false, true, true);

		public static final PotionInfo STRENGTH = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_3, 0, false, true, true);
		public static final PotionInfo STRENGTH_LONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_8, 0, false, true, true);
		public static final PotionInfo STRENGTH_STRONG = new PotionInfo(PotionEffectType.INCREASE_DAMAGE, MINUTES_1_HALF, 1, false, true, true);

		public static final PotionInfo LEAPING = new PotionInfo(PotionEffectType.JUMP, MINUTES_3, 0, false, true, true);
		public static final PotionInfo LEAPING_LONG = new PotionInfo(PotionEffectType.JUMP, MINUTES_8, 0, false, true, true);
		public static final PotionInfo LEAPING_STRONG = new PotionInfo(PotionEffectType.JUMP, MINUTES_1_HALF, 1, false, true, true);

		public static final PotionInfo NIGHT_VISION = new PotionInfo(PotionEffectType.NIGHT_VISION, MINUTES_3, 0, false, true, true);
		public static final PotionInfo NIGHT_VISION_LONG = new PotionInfo(PotionEffectType.NIGHT_VISION, MINUTES_8, 0, false, true, true);

		public static final PotionInfo FIRE_RESISTANCE = new PotionInfo(PotionEffectType.FIRE_RESISTANCE, MINUTES_3, 0, false, true, true);
		public static final PotionInfo FIRE_RESISTANCE_LONG = new PotionInfo(PotionEffectType.FIRE_RESISTANCE, MINUTES_8, 0, false, true, true);

		public static final PotionInfo LUCK = new PotionInfo(PotionEffectType.LUCK, MINUTES_5, 0, false, true, true);

		public @Nullable PotionEffectType mType;
		public int mDuration;
		public int mAmplifier;
		public boolean mAmbient;
		public boolean mShowParticles;
		public boolean mShowIcon;
		public boolean mInfinite;
		public int mHeavenlyBoonExtensions;

		public PotionInfo() {
		}

		public PotionInfo(PotionEffect effect) {
			mType = effect.getType();
			mDuration = effect.getDuration();
			mAmplifier = effect.getAmplifier();
			mAmbient = effect.isAmbient();
			mShowParticles = effect.hasParticles();
			mShowIcon = effect.hasIcon();
			mInfinite = isInfinite(effect.getDuration());
			mHeavenlyBoonExtensions = 0;
		}

		public PotionInfo(@Nullable PotionEffectType type, int duration, int amplifier, boolean ambient,
		                  boolean showParticles, boolean showIcon) {
			this(type, duration, amplifier, ambient, showParticles, showIcon, 0);
		}

		public PotionInfo(@Nullable PotionEffectType type, int duration, int amplifier, boolean ambient,
						  boolean showParticles, boolean showIcon, int heavenlyBoonExtensions) {
			mType = type;
			mDuration = duration;
			mAmplifier = amplifier;
			mAmbient = ambient;
			mShowParticles = showParticles;
			mShowIcon = showIcon;
			mInfinite = isInfinite(duration);
			mHeavenlyBoonExtensions = heavenlyBoonExtensions;
		}

		public JsonObject getAsJsonObject() {
			JsonObject potionInfoObject = new JsonObject();

			potionInfoObject.addProperty("type", mType == null ? null : mType.getName());
			potionInfoObject.addProperty("duration", mDuration);
			potionInfoObject.addProperty("amplifier", mAmplifier);
			potionInfoObject.addProperty("ambient", mAmbient);
			potionInfoObject.addProperty("show_particles", mShowParticles);
			potionInfoObject.addProperty("heavenly_boon_extensions", mHeavenlyBoonExtensions);

			return potionInfoObject;
		}

		public void loadFromJsonObject(JsonObject object) throws Exception {
			mType = PotionEffectType.getByName(object.get("type").getAsString());
			mDuration = object.get("duration").getAsInt();
			mAmplifier = object.get("amplifier").getAsInt();
			mAmbient = object.get("ambient").getAsBoolean();
			mShowParticles = object.get("show_particles").getAsBoolean();
			mInfinite = isInfinite(mDuration);
			if (object.has("heavenly_boon_extensions")) {
				mHeavenlyBoonExtensions = object.get("heavenly_boon_extensions").getAsInt();
			} else {
				mHeavenlyBoonExtensions = 0;
			}
		}
	}

	/*
	 * Dividend should be 1 for drink/splash, 4 for lingering potions, 8 for tipped arrows
	 * NOTE: This may return NULL for some broken potions!
	 */
	public static @Nullable PotionInfo getPotionInfo(PotionData data, int dividend) {
		PotionInfo newInfo;
		PotionType type = data.getType();
		boolean isExtended = data.isExtended();
		boolean isUpgraded = data.isUpgraded();

		/* Some bugged potion types don't actually have types... */
		if (type == null || type.getEffectType() == null) {
			return null;
		}

		if (type.isInstant()) {
			if (isUpgraded) {
				newInfo = new PotionInfo(type.getEffectType(), 0, 1, false, true, true);
			} else {
				newInfo = new PotionInfo(type.getEffectType(), 0, 0, false, true, true);
			}
		} else {
			if (type == PotionType.REGEN || type == PotionType.POISON) {
				if (isExtended) {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_1_HALF / dividend, 0, false, true, true);
				} else if (isUpgraded) {
					newInfo = new PotionInfo(type.getEffectType(), SECONDS_22_HALF / dividend, 1, false, true, true);
				} else {
					newInfo = new PotionInfo(type.getEffectType(), SECONDS_45 / dividend, 0, false, true, true);
				}
			} else if (type == PotionType.LUCK) {
				newInfo = new PotionInfo(type.getEffectType(), MINUTES_5 / dividend, 0, false, true, true);
			} else {
				if (isExtended) {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_8 / dividend, 0, false, true, true);
				} else if (isUpgraded) {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_1_HALF / dividend, 1, false, true, true);
				} else {
					newInfo = new PotionInfo(type.getEffectType(), MINUTES_3 / dividend, 0, false, true, true);
				}
			}
		}

		return newInfo;
	}

	public static void apply(LivingEntity entity, PotionInfo info) {
		entity.addPotionEffect(new PotionEffect(info.mType, info.mDuration, info.mAmplifier, info.mAmbient, info.mShowParticles));
	}

	public static List<PotionEffect> getEffects(ItemStack item) {
		if (item != null && item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta) {
			return getEffects((PotionMeta) item.getItemMeta());
		}
		// Not a potion - return an empty list to simplify callers iterating the result
		return new ArrayList<PotionEffect>(0);
	}

	public static List<PotionEffect> getEffects(PotionMeta meta) {
		List<PotionEffect> effectsList = new ArrayList<PotionEffect>();

		PotionData data = meta.getBasePotionData();
		if (data != null) {
			PotionUtils.PotionInfo info = PotionUtils.getPotionInfo(data, 1);
			if (info != null) {
				PotionEffect effect = new PotionEffect(info.mType, info.mDuration, info.mAmplifier, info.mAmbient,
						info.mShowParticles);
				effectsList.add(effect);
			}
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
				if ("SLOW".equals(type.getName()) && dolphin) {
					continue;
				}
				PotionEffect effect = entity.getPotionEffect(type);
				if (effect != null && effect.getDuration() < Constants.THIRTY_MINUTES) {
					entity.removePotionEffect(type);
				}
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
			if (
					targetPotionEffect != null && (
							targetPotionEffect.getAmplifier() < effect.getAmplifier()
									|| (
									targetPotionEffect.getAmplifier() == effect.getAmplifier()
											&& targetPotionEffect.getDuration() < effect.getDuration()
							))
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
		} else if (meta.hasCustomEffects()) {
			for (PotionEffect effect : meta.getCustomEffects()) {
				if (effect.getType() != null) {
					if (effect.getType().equals(PotionEffectType.HARM)) {
						//If 10+, kill, if below, deal normal instant damage
						//If instant healing, manually add health
						if (effect.getAmplifier() >= 9) {
							player.setHealth(0);
						} else {
							DamageUtils.damage(null, player, DamageType.MAGIC, 3 * Math.pow(2, effect.getAmplifier() + 1));
						}
					} else if (effect.getType().equals(PotionEffectType.HEAL)) {
						PlayerUtils.healPlayer(plugin, player, 2 * Math.pow(2, effect.getAmplifier() + 1));
					}
				}

				plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, effect);
			}
		} else {
			PotionInfo info = PotionUtils.getPotionInfo(meta.getBasePotionData(), 1);

			//If instant healing, manually add health, otherwise if instant damage, manually remove health, else add effect
			//Check then add health
			if (info != null && info.mType != null && info.mType.equals(PotionEffectType.HEAL)) {
				PlayerUtils.healPlayer(plugin, player, 2 * Math.pow(2, info.mAmplifier + 1));
			} else if (info != null && info.mType != null && info.mType.equals(PotionEffectType.HARM)) {
				DamageUtils.damage(null, player, DamageType.MAGIC, 3 * Math.pow(2, info.mAmplifier + 1));
			} else {
				plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, info);
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

	/**
	 * Apply the glowing effect along with a color to a given entity.
	 */
	public static void applyColoredGlowing(String source, Entity entity, @Nullable NamedTextColor color, int duration) {
		Plugin.getInstance().mEffectManager.addEffect(entity, source, new ColoredGlowingEffect(duration, color));
	}
}
