package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.enchantments.Bleeding;
import com.playmonumenta.plugins.itemstats.enchantments.Decay;
import com.playmonumenta.plugins.itemstats.enchantments.FireAspect;
import com.playmonumenta.plugins.itemstats.enchantments.IceAspect;
import com.playmonumenta.plugins.itemstats.enchantments.ThunderAspect;
import com.playmonumenta.plugins.itemstats.enchantments.WindAspect;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class ArcaneStrike extends Ability {

	private static final Particle.DustOptions COLOR_1 = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);
	private static final Particle.DustOptions COLOR_2 = new Particle.DustOptions(Color.fromRGB(217, 122, 255), 1.0f);

	private static final float RADIUS = 4.0f;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 7;
	private static final int BONUS_DAMAGE_1 = 2;
	private static final int BONUS_DAMAGE_2 = 3;
	private static final int COOLDOWN = 5 * 20;
	public static final ClassAbility ABILITY = ClassAbility.ARCANE_STRIKE;
	public static final String CHARM_DAMAGE = "Arcane Strike Damage";
	public static final String CHARM_RANGE = "Arcane Strike Range";
	public static final String CHARM_BONUS = "Arcane Strike Bonus Damage";
	public static final String CHARM_COOLDOWN = "Arcane Strike Cooldown";

	private final float mDamageBonus;
	private final float mDamageBonusAffected;

	public ArcaneStrike(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Arcane Strike");
		mInfo.mLinkedSpell = ClassAbility.ARCANE_STRIKE;
		mInfo.mScoreboardId = "ArcaneStrike";
		mInfo.mShorthandName = "AS";
		mInfo.mDescriptions.add(
			String.format("When you attack an enemy with a wand, you unleash an arcane explosion dealing %s magic damage to all mobs in a %s block radius around the target. Enemies that are on fire or slowed take %s extra damage. Arcane strike can not trigger Spellshock's static. Cooldown: %ss.",
				DAMAGE_1,
				(int)RADIUS,
				BONUS_DAMAGE_1,
				COOLDOWN / 20
			));
		mInfo.mDescriptions.add(
			String.format("The damage is increased to %s. Mobs that are on fire or slowed take %s additional damage.",
				DAMAGE_2,
				BONUS_DAMAGE_2));
		mInfo.mDescriptions.add("Your enchantment on-hit effects are now also applied to all other enemies hit in the radius.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN);
		mDisplayItem = new ItemStack(Material.GOLDEN_SWORD, 1);
		mDamageBonus = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamageBonusAffected = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_BONUS, isLevelOne() ? BONUS_DAMAGE_1 : BONUS_DAMAGE_2);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && mPlayer != null && mPlayer.getCooledAttackStrength(0) == 1) {
			putOnCooldown();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RADIUS), mPlayer)) {
				float dmg = SpellPower.getSpellDamage(mPlugin, mPlayer, mDamageBonus);

				// Arcane Strike extra damage if on fire or slowed (but effect not applied this tick)
				if (EntityUtils.isSlowed(mPlugin, mob) || (mob.hasPotionEffect(PotionEffectType.SLOW)
					&& !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_SLOWED_NONCE_METAKEY, 0))
					|| (mob.getFireTicks() > 0
					&& !MetadataUtils.happenedThisTick(mob, Constants.ENTITY_COMBUST_NONCE_METAKEY, 0))) {
					dmg += SpellPower.getSpellDamage(mPlugin, mPlayer, mDamageBonusAffected);
				}

				if (isEnhanced()) {
					ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
					int fire = ItemStatUtils.getEnchantmentLevel(mainHand, ItemStatUtils.EnchantmentType.FIRE_ASPECT);
					if (fire > 0) {
						FireAspect.apply(mPlugin, mPlayer, fire, mob);
					}
					int ice = ItemStatUtils.getEnchantmentLevel(mainHand, ItemStatUtils.EnchantmentType.ICE_ASPECT);
					if (ice > 0) {
						IceAspect.apply(mPlugin, mPlayer, ice, IceAspect.ICE_ASPECT_DURATION, mob, true);
						dmg += IceAspect.getBonusDamage(mob);
					}
					int thunder = ItemStatUtils.getEnchantmentLevel(mainHand, ItemStatUtils.EnchantmentType.THUNDER_ASPECT);
					if (thunder > 0) {
						ThunderAspect.apply(mPlugin, mPlayer, thunder, mob);
						dmg += ThunderAspect.getBonusDamage(mob);
					}
					int decay = ItemStatUtils.getEnchantmentLevel(mainHand, ItemStatUtils.EnchantmentType.DECAY);
					if (decay > 0) {
						Decay.apply(mPlugin, mob, Decay.DURATION, decay, mPlayer);
					}
					int bleed = ItemStatUtils.getEnchantmentLevel(mainHand, ItemStatUtils.EnchantmentType.BLEEDING);
					if (bleed > 0) {
						Bleeding.apply(mPlugin, mPlayer, bleed, Bleeding.DURATION, mob);
					}
					int wind = ItemStatUtils.getEnchantmentLevel(mainHand, ItemStatUtils.EnchantmentType.WIND_ASPECT);
					if (wind > 0) {
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
						WindAspect.launch(mob, wind);
					}
				}

				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, dmg, mInfo.mLinkedSpell, true, true);

			}

			Location locD = enemy.getLocation().add(0, 1, 0);
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.DRAGON_BREATH, locD, 75, 0, 0, 0, 0.25).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, locD, 35, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_WITCH, locD, 150, 2.5, 1, 2.5, 0.001).spawnAsPlayerActive(mPlayer);
			world.playSound(locD, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.75f, 1.5f);

			Location loc = mPlayer.getLocation().add(mPlayer.getLocation().getDirection().multiply(0.5));
			world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.75f, 1.65f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
			new BukkitRunnable() {
				double mD = 30;

				@Override
				public void run() {
					Vector vec;
					for (double degree = mD; degree < mD + 30; degree += 8) {
						double radian1 = Math.toRadians(degree);
						double cos = FastUtils.cos(radian1);
						double sin = FastUtils.sin(radian1);
						for (double r = 1; r < 4; r += 0.5) {
							vec = new Vector(cos * r, 1, sin * r);
							vec = VectorUtils.rotateXAxis(vec, loc.getPitch());
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

							Location l = loc.clone().add(vec);
							new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_1).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, COLOR_2).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
						}
					}
					mD += 30;
					if (mD >= 150) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return ItemUtils.isWand(mainHand);
	}
}
