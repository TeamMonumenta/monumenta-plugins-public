package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class DaggerThrow extends Ability {

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 4;
	private static final int DAGGER_THROW_2_DAMAGE = 8;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_SILENCE_DURATION = 2 * 20;
	private static final int DAGGER_THROW_DAGGERS = 3;
	private static final double DAGGER_THROW_1_VULN = 0.2;
	private static final double DAGGER_THROW_2_VULN = 0.4;
	private static final double DAGGER_THROW_VULN_ENHANCEMENT = 0.1;
	private static final double DAGGER_THROW_SPREAD = Math.toRadians(25);
	private static final Particle.DustOptions DAGGER_THROW_COLOR = new Particle.DustOptions(Color.fromRGB(64, 64, 64), 1);

	public static final String CHARM_DAMAGE = "Dagger Throw Damage";
	public static final String CHARM_COOLDOWN = "Dagger Throw Cooldown";
	public static final String CHARM_RANGE = "Dagger Throw Range";
	public static final String CHARM_VULN = "Dagger Throw Vulnerability Amplifier";
	public static final String CHARM_DAGGERS = "Dagger Throw Daggers";

	private final double mDamage;
	private final double mVulnAmplifier;

	public DaggerThrow(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Dagger Throw");
		mInfo.mLinkedSpell = ClassAbility.DAGGER_THROW;
		mInfo.mScoreboardId = "DaggerThrow";
		mInfo.mShorthandName = "DT";
		mInfo.mDescriptions.add("Sneak left click while holding two swords to throw three daggers which deal 4 melee damage and gives each target 20% Vulnerability for 10 seconds. Cooldown: 12s.");
		mInfo.mDescriptions.add("The damage is increased to 8 and the Vulnerability increased to 40%.");
		mInfo.mDescriptions.add("Targets are additionally silenced for 2s. Vulnerability is increased by 10%.");
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, DAGGER_THROW_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.WOODEN_SWORD, 1);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE);
		mVulnAmplifier = (isLevelOne() ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN) + (isEnhanced() ? DAGGER_THROW_VULN_ENHANCEMENT : 0) + CharmManager.getLevelPercentDecimal(player, CHARM_VULN);
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection();
		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, DAGGER_THROW_RANGE + 1, mPlayer);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.25f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.0f);

		int daggers = DAGGER_THROW_DAGGERS + (int) CharmManager.getLevel(mPlayer, CHARM_DAGGERS);

		for (int a = (daggers / 2) * -1; a <= (daggers / 2); a++) {
			double totalSpread = (DAGGER_THROW_SPREAD * DAGGER_THROW_DAGGERS);
			double individualSpread = totalSpread / daggers;
			double angle = a * individualSpread;
			Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY(), FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
			newDir.normalize();

			// Since we want some hitbox allowance, we use bounding boxes instead of a raycast
			BoundingBox box = BoundingBox.of(loc, 0.55, 0.55, 0.55);

			for (int i = 0; i <= CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, DAGGER_THROW_RANGE); i++) {
				box.shift(newDir);
				Location bLoc = box.getCenter().toLocation(world);
				new PPLine(Particle.REDSTONE, bLoc, newDir, 0.9).countPerMeter(10).delta(0.1).data(DAGGER_THROW_COLOR).spawnAsPlayerActive(mPlayer);

				for (LivingEntity mob : mobs) {
					if (mob.getBoundingBox().overlaps(box)
						&& MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
						bLoc.subtract(newDir.clone().multiply(0.5));
						new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.4f, 2.5f);

						DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.mLinkedSpell, true);
						EntityUtils.applyVulnerability(mPlugin, DAGGER_THROW_DURATION, mVulnAmplifier, mob);
						if (isEnhanced()) {
							EntityUtils.applySilence(mPlugin, DAGGER_THROW_SILENCE_DURATION, mob);
						}
						break;

					} else if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
						bLoc.subtract(newDir.clone().multiply(0.5));
						new PartialParticle(Particle.SWEEP_ATTACK, bLoc, 3, 0.3, 0.3, 0.3, 0.1).spawnAsPlayerActive(mPlayer);
						break;
					}
				}
			}
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > - 50 && InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}

}
