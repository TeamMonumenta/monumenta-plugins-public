package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DecayedTotem extends TotemAbility {

	private static final int COOLDOWN = 25 * 20;
	private static final int AOE_RANGE = 8;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int DAMAGE = 4;
	private static final double SLOWNESS_PERCENT = 0.4;
	public static final String TOTEM_NAME = "Decayed Totem";
	private static final int TARGETS = 3;

	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(5, 120, 5), 1.0f);

	private final int mDuration;
	private final int mTickSpeed;
	private final List<LivingEntity> mTargets = new ArrayList<>();

	public static final AbilityInfo<DecayedTotem> INFO =
		new AbilityInfo<>(DecayedTotem.class, "Decayed Totem", DecayedTotem::new)
			.linkedSpell(ClassAbility.DECAYED_TOTEM)
			.scoreboardId("DecayedTotem")
			.shorthandName("DT")
			.descriptions(
				String.format("Press swap while holding a melee weapon to fire a projectile that summons a Decayed Totem. The totem anchors up to %s targets within %s blocks of the totem, " +
						"inflicting %s magic damage per second and %s%% Slowness (%ss duration, %ss cooldown)",
					TARGETS,
					AOE_RANGE,
					DAMAGE,
					(int) (SLOWNESS_PERCENT * 100),
					DURATION_1 / 20,
					COOLDOWN / 20
				),
				String.format("Damage now ticks every half second, and duration is increased to %ss.",
					DURATION_2 / 20)
			)
			.simpleDescription("Summons a totem, dealing damage and heavily slowing 3 mobs within range.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DecayedTotem::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.WITHER_ROSE);

	public DecayedTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Decayed Totem Projectile", "DecayedTotem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDuration = isLevelOne() ? DURATION_1 : DURATION_2;
		mTickSpeed = isLevelOne() ? 20 : 10;
	}

	@Override
	public int getTotemDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks == 0) {
			stand.getWorld().playSound(stand, Sound.BLOCK_CONDUIT_AMBIENT, 20.0f, 1.2f);
			stand.getWorld().playSound(stand, Sound.ENTITY_SKELETON_HURT, 0.6f, 0.3f);
			stand.getWorld().playSound(stand, Sound.ENTITY_PHANTOM_DEATH, 0.5f, 0.2f);
		}
		mTargets.removeIf(mob -> standLocation.distance(mob.getLocation()) >= AOE_RANGE || mob.isDead());
		if (mTargets.size() < TARGETS) {
			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobs(standLocation, AOE_RANGE);
			Collections.shuffle(affectedMobs);

			for (LivingEntity mob : affectedMobs) {
				if (mTargets.contains(mob) || standLocation.distance(mob.getLocation()) >= AOE_RANGE) {
					continue;
				}
				impactMob(mob, mTickSpeed + 5, false);
				mTargets.add(mob);
				if (mTargets.size() >= TARGETS) {
					break;
				}
			}
		}

		if (ticks % 5 == 0) {
			for (LivingEntity target : mTargets) {
				new PPLine(Particle.REDSTONE, stand.getEyeLocation(), target.getLocation()).countPerMeter(8).delta(0.03).data(BLACK).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, stand.getEyeLocation(), target.getLocation()).countPerMeter(8).delta(0.03).data(GREEN).spawnAsPlayerActive(mPlayer);
			}
		}
		if (ticks % mTickSpeed == 0) {
			for (LivingEntity target : mTargets) {
				impactMob(target, mTickSpeed + 20, true);
			}
		}
	}

	private void impactMob(LivingEntity target, int duration, boolean dealDamage) {
		if (dealDamage) {
			DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC, DAMAGE, ClassAbility.DECAYED_TOTEM, true);
		}
		EntityUtils.applySlow(mPlugin, duration, SLOWNESS_PERCENT, target);
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.SQUID_INK, standLocation, 5, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, 0.7f, 0.5f);
	}
}
