package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DepthsAdvancingShadows extends DepthsAbility {

	public static final String ABILITY_NAME = "Advancing Shadows";
	public static final double[] DAMAGE = {0.16, 0.20, 0.24, 0.28, 0.32, 0.4};

	private static final int ADVANCING_SHADOWS_RANGE = 12;
	private static final double ADVANCING_SHADOWS_OFFSET = 2.7;
	private static final int COOLDOWN = 18 * 20;
	private static final int DAMAGE_DURATION = 5 * 20;

	public static final String CHARM_COOLDOWN = "Advancing Shadows Cooldown";

	public static final DepthsAbilityInfo<DepthsAdvancingShadows> INFO =
		new DepthsAbilityInfo<>(DepthsAdvancingShadows.class, ABILITY_NAME, DepthsAdvancingShadows::new, DepthsTree.SHADOWDANCER, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.ADVANCING_SHADOWS_DEPTHS)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DepthsAdvancingShadows::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.WITHER_SKELETON_SKULL)
			.descriptions(DepthsAdvancingShadows::getDescription);

	private final double mRange;
	private final double mDamage;
	private final int mDamageDuration;

	public DepthsAdvancingShadows(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.ADVANCING_SHADOWS_RANGE.mEffectName, ADVANCING_SHADOWS_RANGE);
		mDamage = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ADVANCING_SHADOWS_DAMAGE_MULTIPLIER.mEffectName);
		mDamageDuration = CharmManager.getDuration(mPlayer, CharmEffects.ADVANCING_SHADOWS_DURATION.mEffectName, DAMAGE_DURATION);

	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		LivingEntity entity = EntityUtils.getHostileEntityAtCursor(mPlayer, mRange);

		if (entity == null) {
			return false;
		}

		double origDistance = mPlayer.getLocation().distance(entity.getLocation());
		if (origDistance <= mRange && !entity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			Vector dir = LocationUtils.getDirectionTo(entity.getLocation(), mPlayer.getLocation());
			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			while (loc.distance(entity.getLocation()) > ADVANCING_SHADOWS_OFFSET) {
				loc.add(dir.clone().multiply(0.3333));
				new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 4, 0.3, 0.5, 0.3, 1.0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.025).spawnAsPlayerActive(mPlayer);
				if (loc.distance(entity.getLocation()) < ADVANCING_SHADOWS_OFFSET) {
					double multiplier = ADVANCING_SHADOWS_OFFSET - loc.distance(entity.getLocation());
					loc.subtract(dir.clone().multiply(multiplier));
					break;
				}
			}
			loc.add(0, 1, 0);

			// Just in case the player's teleportation loc is in a block.
			int count = 0;
			while (count < 5 && loc.getBlock().getType().isSolid()) {
				count++;
				loc.subtract(dir.clone().multiply(1.15));
			}

			// If still solid, something is wrong.
			if (loc.getBlock().getType().isSolid()) {
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.8f);
				return false;
			}

			// Prevent the player from teleporting over void
			if (loc.getY() < 8) {
				boolean safe = false;
				for (int y = 0; y < loc.getY() - 1; y++) {
					Location tempLoc = loc.clone();
					tempLoc.setY(y);
					if (!tempLoc.getBlock().isPassable()) {
						safe = true;
						break;
					}
				}

				// Maybe void - not worth it
				if (!safe) {
					world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.8f);
					return false;
				}

				// Don't teleport players below y = 1.1 to avoid clipping into oblivion
				loc.setY(Math.max(1.1, loc.getY()));
			}

			// Extra safeguard to prevent bizarro teleports
			if (mPlayer.getLocation().distance(loc) > mRange) {
				world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.8f);
				return false;
			}

			Location playerLoc = mPlayer.getLocation();

			new PartialParticle(Particle.SPELL_WITCH, playerLoc.clone().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_LARGE, playerLoc.clone().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.1f);

			if (!(mPlayer.getInventory().getItemInOffHand().getType() == Material.SHIELD) && (loc.distance(entity.getLocation()) <= origDistance)) {
				mPlayer.teleport(loc);
			}
			playerLoc = mPlayer.getLocation();

			EffectManager.getInstance().addEffect(mPlayer, ABILITY_NAME, new PercentDamageDealt(mDamageDuration, mDamage, DamageEvent.DamageType.getAllMeleeTypes()));

			new PartialParticle(Particle.SPELL_WITCH, playerLoc.clone().add(0, 1.1, 0), 50, 0.35, 0.5, 0.35, 1.0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_LARGE, playerLoc.clone().add(0, 1.1, 0), 12, 0.35, 0.5, 0.35, 0.05).spawnAsPlayerActive(mPlayer);
			world.playSound(playerLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 1.1f);
			putOnCooldown();
			return true;
		}

		return false;
	}

	private static Description<DepthsAdvancingShadows> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsAdvancingShadows>(color)
			.add("Right click while looking directly at a hostile mob within ")
			.add(a -> a.mRange, ADVANCING_SHADOWS_RANGE)
			.add(" blocks to teleport to it and gain ")
			.addPercent(a -> a.mDamage, DAMAGE[rarity - 1], false, true)
			.add(" melee damage for ")
			.addDuration(a -> a.mDamageDuration, DAMAGE_DURATION)
			.add(" seconds. If you are holding a shield in your offhand, you will gain the damage buff but not be teleported.")
			.addCooldown(COOLDOWN);
	}


}

