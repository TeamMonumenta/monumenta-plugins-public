package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.HexbreakerPassive;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SoothsayerPassive;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FlameTotem extends TotemAbility {

	private static final int COOLDOWN = 20 * 20;
	private static final int TOTEM_DURATION = 10 * 20;
	private static final int AOE_RANGE = 6;
	private static final int FIRE_DURATION = 2 * 20;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 5;
	public static final String TOTEM_NAME = "Flame Totem";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	private double mDamage;

	public static final AbilityInfo<FlameTotem> INFO =
		new AbilityInfo<>(FlameTotem.class, "Flame Totem", FlameTotem::new)
			.linkedSpell(ClassAbility.FLAME_TOTEM)
			.scoreboardId("FlameTotem")
			.shorthandName("FT")
			.descriptions(
				String.format("Press right click with a melee weapon to summon a flame totem. Mobs within %s blocks of this totem are dealt %s magic damage and set on " +
						"fire, without inferno damage, for %s seconds every second. Duration: %ss. Cooldown: %ss.",
					AOE_RANGE,
					DAMAGE_1,
					FIRE_DURATION / 20,
					TOTEM_DURATION / 20,
					COOLDOWN / 20
				),
				String.format("The totem deals %s magic damage per hit.",
					DAMAGE_2)
			)
			.simpleDescription("Summon a totem that deals damage and sets mobs on fire within its range.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FlameTotem::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.MAGMA_BLOCK);

	public FlameTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Flame Totem Projectile", "FlameTotem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mDamage *= HexbreakerPassive.damageBuff(mPlayer);
		mDamage *= SoothsayerPassive.damageBuff(mPlayer);
	}

	@Override
	public int getTotemDuration() {
		return TOTEM_DURATION;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		PPCircle fireRing = new PPCircle(Particle.FLAME, standLocation, AOE_RANGE).ringMode(true).count(50).delta(0);
		fireRing.spawnAsPlayerActive(mPlayer);
		if (ticks % 20 == 0) {
			pulse(standLocation, world, stats);
		}
		if (mMobStuckWithEffect != null && mMobStuckWithEffect.isDead()) {
			pulse(standLocation, world, stats);
			mMobStuckWithEffect = null;
		}
	}

	private void pulse(Location standLocation, World world, ItemStatManager.PlayerItemStats stats) {
		Set<LivingEntity> affectedMobs = new HashSet<>(EntityUtils.getNearbyMobsInSphere(standLocation, AOE_RANGE, null));

		for (LivingEntity mob : affectedMobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats), mDamage, true, false, false);
			EntityUtils.applyFire(mPlugin, FIRE_DURATION, mob, null);
		}

		PPCircle fireArea = new PPCircle(Particle.FLAME, standLocation, AOE_RANGE).ringMode(false).count(50).delta(0.01).extra(0.05);
		fireArea.spawnAsPlayerActive(mPlayer);

		world.playSound(standLocation, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.3f, 0.5f);
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.REDSTONE, standLocation, 45, 0.2, 1.1, 0.2, 0.1, COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, standLocation, 40, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.5f);
	}
}
