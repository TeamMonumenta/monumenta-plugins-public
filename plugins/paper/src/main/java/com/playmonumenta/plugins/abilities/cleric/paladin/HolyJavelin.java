package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.CrusadeEnhancementTag;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;



public class HolyJavelin extends Ability {
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 50), 1.0f);

	private static final double HITBOX_LENGTH = 0.75;
	private static final int RANGE = 12;
	private static final int UNDEAD_DAMAGE_1 = 18;
	private static final int UNDEAD_DAMAGE_2 = 32;
	private static final int DAMAGE_1 = 9;
	private static final int DAMAGE_2 = 12;
	private static final int FIRE_DURATION = 5 * 20;
	private static final int COOLDOWN = 10 * 20;

	private final double mDamage;
	private final double mUndeadDamage;

	public static final String CHARM_DAMAGE = "Holy Javelin Damage";
	public static final String CHARM_COOLDOWN = "Holy Javelin Cooldown";
	public static final String CHARM_RANGE = "Holy Javelin Range";

	public static final AbilityInfo<HolyJavelin> INFO =
		new AbilityInfo<>(HolyJavelin.class, "Holy Javelin", HolyJavelin::new)
			.linkedSpell(ClassAbility.HOLY_JAVELIN)
			.scoreboardId("HolyJavelin")
			.shorthandName("HJ")
			.descriptions(
				"While sprinting, left-clicking with a non-pickaxe throws a piercing spear of light, instantly travelling up to 12 blocks or until it hits a solid block. " +
					"It deals 18 magic damage to all enemies in a 0.75-block cube around it along its path, or 9 magic damage to non-undead, and sets them all on fire for 5s. Cooldown: 10s.",
				"Attacking an undead enemy with that left-click now transmits any passive Divine Justice and Luminous Infusion damage to other enemies pierced by the spear. " +
					"Damage is increased from 18 to 32, and from 9 to 18 against non-undead.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HolyJavelin::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false).sprinting(true)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(new ItemStack(Material.TRIDENT, 1))
			.priorityAmount(1001); // shortly after divine justice and luminous infusion

	private @Nullable Crusade mCrusade;
	private @Nullable DivineJustice mDivineJustice;
	private @Nullable LuminousInfusion mLuminousInfusion;

	public HolyJavelin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mUndeadDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? UNDEAD_DAMAGE_1 : UNDEAD_DAMAGE_2);

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
			mDivineJustice = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, DivineJustice.class);
			mLuminousInfusion = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, LuminousInfusion.class);
		});
	}

	public void cast() {
		execute(0, null);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			    && mCustomTriggers.get(0).check(mPlayer, AbilityTrigger.Key.LEFT_CLICK)) {
			double sharedPassiveDamage = 0;
			if (mLuminousInfusion != null) {
				sharedPassiveDamage += mLuminousInfusion.mLastPassiveMeleeDamage;
				sharedPassiveDamage += mLuminousInfusion.mLastPassiveDJDamage;
				mLuminousInfusion.mLastPassiveMeleeDamage = 0;
				mLuminousInfusion.mLastPassiveDJDamage = 0;
			}
			if (mDivineJustice != null) {
				sharedPassiveDamage += mDivineJustice.mLastPassiveDamage;
				mDivineJustice.mLastPassiveDamage = 0;
			}
			execute(sharedPassiveDamage, enemy);
		}
		return false;
	}

	public void execute(double bonusDamage,
	                    @Nullable LivingEntity triggeringEnemy) {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();
		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1, 1.75f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, 1, 0.9f);
		Location playerLoc = mPlayer.getEyeLocation();
		Location location = playerLoc.clone();
		Vector increment = location.getDirection();
		new PartialParticle(Particle.EXPLOSION_NORMAL, location.clone().add(increment), 10, 0, 0, 0, 0.125f).spawnAsPlayerActive(mPlayer);

		// Get a list of all the mobs this could possibly hit (that are within range of the player)
		List<LivingEntity> potentialTargets = EntityUtils.getNearbyMobs(location, range + HITBOX_LENGTH, mPlayer);
		BoundingBox box = BoundingBox.of(playerLoc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		for (double i = 0; i < range; i += HITBOX_LENGTH) {
			box.shift(increment);
			Location loc = box.getCenter().toLocation(world);
			new PartialParticle(Particle.REDSTONE, loc, 22, 0.25, 0.25, 0.25, COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0f, 0f, 0f, 0.025f).spawnAsPlayerActive(mPlayer);

			Iterator<LivingEntity> iterator = potentialTargets.iterator();
			while (iterator.hasNext()) {
				LivingEntity enemy = iterator.next();
				if (enemy.getBoundingBox().overlaps(box)) {
					double damage = (
						Crusade.enemyTriggersAbilities(enemy, mCrusade)
							? mUndeadDamage
							: mDamage
					);
					if (enemy != triggeringEnemy) {
						// Triggering enemy would've already received the melee damage from Luminous
						// Infusion
						damage += bonusDamage;
					}
					EntityUtils.applyFire(mPlugin, FIRE_DURATION, enemy, mPlayer);
					DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);
					if (Crusade.applyCrusadeToSlayer(enemy, mCrusade)) {
						mPlugin.mEffectManager.addEffect(enemy, "CrusadeSlayerTag", new CrusadeEnhancementTag(mCrusade.getEnhancementDuration()));
					}
					iterator.remove();
				}
			}

			if (!loc.isChunkLoaded() || loc.getBlock().getType().isSolid()) {
				loc.subtract(increment.multiply(0.5));
				new PartialParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f).spawnAsPlayerActive(mPlayer);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.65f);
				world.playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
				break;
			}
		}
	}
}
