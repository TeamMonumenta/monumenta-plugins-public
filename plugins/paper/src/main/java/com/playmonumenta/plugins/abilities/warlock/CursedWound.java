package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import java.util.HashMap;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CursedWound extends Ability {

	private static final int CURSED_WOUND_DOT_DAMAGE = 1;
	private static final int CURSED_WOUND_DOT_PERIOD = 20;
	private static final int CURSED_WOUND_DURATION = 6 * 20;
	private static final int CURSED_WOUND_RADIUS = 3;
	private static final double CURSED_WOUND_DAMAGE = 0.05;
	private static final double CURSED_WOUND_1_CAP = 0.15;
	private static final double CURSED_WOUND_2_CAP = 0.3;
	private static final int CURSED_WOUND_EXTENDED_DURATION = 2 * 20;
	private static final String DOT_EFFECT_NAME = "CursedWoundDamageOverTimeEffect";
	private static final double DAMAGE_PER_EFFECT = 2;

	private static final Color LIGHT_COLOR = Color.fromRGB(217, 217, 217);
	private static final Color DARK_COLOR = Color.fromRGB(13, 13, 13);

	public static final String CHARM_DAMAGE = "Cursed Wound Damage Modifier";
	public static final String CHARM_RADIUS = "Cursed Wound Radius";
	public static final String CHARM_CAP = "Cursed Wound Damage Cap";
	public static final String CHARM_DOT = "Cursed Wound DoT";

	private double mCursedWoundCap;
	private @Nullable Collection<PotionEffect> mStoredPotionEffects;
	private @Nullable HashMap<String, Effect> mStoredCustomEffects;

	public CursedWound(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Cursed Wound");
		mInfo.mScoreboardId = "CursedWound";
		mInfo.mShorthandName = "CW";
		mInfo.mDescriptions.add("Attacking an enemy with a critical scythe attack passively afflicts it and all enemies in a 3-block cube around it with 1 damage every second for 6s. Your melee attacks passively deal 3% more damage per ability on cooldown, capped at +15% damage.");
		mInfo.mDescriptions.add("Critical attacks now also extend all enemies' debuffs (except Stun, Silence, and Paralysis) by 2s. Damage cap is increased from 15% to 30%.");
		mInfo.mDescriptions.add("When you kill a mob with a melee scythe attack, all debuffs on the mob get stored in your scythe. Then, on your next melee scythe attack, all mobs within 3 blocks of the target are inflicted with the effects stored in your scythe, as well as 2 magic damage per effect.");
		mInfo.mLinkedSpell = ClassAbility.CURSED_WOUND;
		mDisplayItem = new ItemStack(Material.GOLDEN_SWORD, 1);
		mCursedWoundCap = CharmManager.getLevelPercentDecimal(player, CHARM_CAP) + (isLevelOne() ? CURSED_WOUND_1_CAP : CURSED_WOUND_2_CAP);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			World world = mPlayer.getWorld();
			BlockData fallingDustData = Material.ANVIL.createBlockData();

			if (isEnhanced() && mStoredPotionEffects != null && mStoredCustomEffects != null) {
				double damage = DAMAGE_PER_EFFECT * (mStoredPotionEffects.size() + mStoredCustomEffects.size());
				double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, CURSED_WOUND_RADIUS);
				if (damage > 0) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), radius)) {
						 mStoredPotionEffects.forEach(mob::addPotionEffect);
						 mStoredCustomEffects.forEach((source, effect) -> mPlugin.mEffectManager.addEffect(mob, source, effect));
						 DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, true);
					}

					mStoredPotionEffects = null;
					mStoredCustomEffects = null;

					world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.75f);
					new PartialParticle(Particle.FALLING_DUST, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 25,
						radius / 2, radius / 3, radius / 2, fallingDustData)
						.spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_MOB, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 35,
						radius / 2, radius / 2, radius / 2, 0, new Particle.DustOptions(DARK_COLOR, 1.4f))
						.spawnAsPlayerActive(mPlayer);
				}
			}


			if (EntityUtils.isHostileMob(enemy)) {
				new PartialParticle(Particle.FALLING_DUST, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 3,
					(enemy.getWidth() / 2) + 0.1, enemy.getHeight() / 3, (enemy.getWidth() / 2) + 0.1, fallingDustData)
					.spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_MOB, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 6,
					(enemy.getWidth() / 2) + 0.1, enemy.getHeight() / 3, (enemy.getWidth() / 2) + 0.1, 0, new Particle.DustOptions(DARK_COLOR, 1.4f))
					.spawnAsPlayerActive(mPlayer);

				int cooldowns = 0;
				for (Integer ability : mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId())) {
					if (ability > 0) {
						cooldowns++;
					}
				}

				event.setDamage(event.getDamage() * (1 + Math.min(cooldowns * (CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE) + CURSED_WOUND_DAMAGE), mCursedWoundCap)));
			}

			if (PlayerUtils.isFallingAttack(mPlayer)) {
				world.playSound(mPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.75f);
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), CURSED_WOUND_RADIUS, mPlayer)) {
					new PartialParticle(Particle.FALLING_DUST, mob.getLocation().add(0, mob.getHeight() / 2, 0), 3,
						(mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, fallingDustData)
						.spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_MOB, mob.getLocation().add(0, mob.getHeight() / 2, 0), 6,
						(mob.getWidth() / 2) + 0.1, mob.getHeight() / 3, (mob.getWidth() / 2) + 0.1, 0, new Particle.DustOptions(DARK_COLOR, 1.4f))
						.spawnAsPlayerActive(mPlayer);
					mPlugin.mEffectManager.addEffect(mob, DOT_EFFECT_NAME, new CustomDamageOverTime(CURSED_WOUND_DURATION, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT, CURSED_WOUND_DOT_DAMAGE), CURSED_WOUND_DOT_PERIOD, mPlayer, null));
					if (isLevelTwo()) {
						//Bleed interaction
						if (EntityUtils.isBleeding(mPlugin, mob)) {
							EntityUtils.setBleedTicks(mPlugin, mob, EntityUtils.getBleedTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						//Custom slow effect interaction
						if (EntityUtils.isSlowed(mPlugin, mob)) {
							EntityUtils.setSlowTicks(mPlugin, mob, EntityUtils.getSlowTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						//Custom weaken interaction
						if (EntityUtils.isWeakened(mPlugin, mob)) {
							EntityUtils.setWeakenTicks(mPlugin, mob, EntityUtils.getWeakenTicks(mPlugin, mob) + CURSED_WOUND_EXTENDED_DURATION);
						}
						for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, mob)) {
							PotionEffect effect = mob.getPotionEffect(effectType);
							if (effect != null) {
								mob.removePotionEffect(effectType);
								// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
								mob.addPotionEffect(new PotionEffect(effectType, effect.getDuration() + CURSED_WOUND_EXTENDED_DURATION, effect.getAmplifier()));
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		World world = event.getEntity().getWorld();
		Location loc = mPlayer.getLocation();
		LivingEntity entity = event.getEntity();
		EntityDamageEvent entityDamageEvent = entity.getLastDamageCause();
		if (isEnhanced() && entityDamageEvent != null && entityDamageEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {

			mStoredPotionEffects = entity.getActivePotionEffects();
			mStoredPotionEffects.removeIf(effect -> PotionUtils.hasPositiveEffects(effect.getType()));

			mStoredCustomEffects = new HashMap<>();
			HashMap<String, Effect> customEffects = mPlugin.mEffectManager.getPriorityEffects(entity);
			for (String source : customEffects.keySet()) {
				Effect effect = customEffects.get(source);
				if (effect.isDebuff()) {
					mStoredCustomEffects.put(source, effect);
				}
			}

			if (!mStoredPotionEffects.isEmpty() || !mStoredCustomEffects.isEmpty()) {
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.65f);
				createOrb(new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75),
					FastUtils.randomDoubleInRange(1, 1.5),
					FastUtils.randomDoubleInRange(-0.75, 0.75)), mPlayer.getLocation().add(0, 1, 0), mPlayer, entity, null);
			}
		}
	}

	private void createOrb(Vector dir, Location loc, Player mPlayer, LivingEntity target, Location optLoc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = target.getLocation().clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = optLoc != null ? optLoc : LocationUtils.getHalfHeightLocation(mPlayer);

				for (int i = 0; i < 4; i++) {
					if (mT <= 2) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.085;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.2) {
						mD.normalize().multiply(0.2);
					}

					mL.add(mD);

					for (int j = 0; j < 2; j++) {
						Color c = FastUtils.RANDOM.nextBoolean() ? DARK_COLOR : LIGHT_COLOR;
						double red = c.getRed() / 255D;
						double green = c.getGreen() / 255D;
						double blue = c.getBlue() / 255D;
						new PartialParticle(Particle.SPELL_MOB,
							mL.clone().add(FastUtils.randomDoubleInRange(-0.05, 0.05),
								FastUtils.randomDoubleInRange(-0.05, 0.05),
								FastUtils.randomDoubleInRange(-0.05, 0.05)),
							1, red, green, blue, 1)
							.directionalMode(true).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
					}
					Color c = FastUtils.RANDOM.nextBoolean() ? DARK_COLOR : LIGHT_COLOR;
					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0,
						new Particle.DustOptions(c, 1.4f))
						.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(mPlayer.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1, 0.8f);
						new PartialParticle(Particle.SPELL, loc.add(0, 1, 0), 20, 0.4f, 0.4f, 0.4f, 0.6F)
							.spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.FALLING_DUST, mL, 45, 0, 0, 0, 0.75F, Material.ANVIL.createBlockData())
							.minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand());
	}
}
