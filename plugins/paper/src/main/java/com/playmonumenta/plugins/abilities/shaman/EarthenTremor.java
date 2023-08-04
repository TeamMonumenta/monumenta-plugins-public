package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;


public class EarthenTremor extends Ability {

	private static final int COOLDOWN = 10 * 20;
	private static final int SILENCE_DURATION = 30;
	private static final int RANGE = 8;
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 13;
	private static final double KNOCKBACK = 0.8;

	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);

	public static final String CHARM_COOLDOWN = "Earthen Tremor Cooldown";
	public static final String CHARM_DAMAGE = "Earthen Tremor Damage";
	public static final String CHARM_RADIUS = "Earthen Tremor Radius";
	public static final String CHARM_SILENCE_DURATION = "Earthen Tremor Silence Duration";
	public static final String CHARM_KNOCKBACK = "Earthen Tremor Knockback";

	private double mDamage;
	private final double mRadius;
	private final int mSilenceDuration;
	private final float mKnockback;

	public static final AbilityInfo<EarthenTremor> INFO =
		new AbilityInfo<>(EarthenTremor.class, "Earthen Tremor", EarthenTremor::new)
			.linkedSpell(ClassAbility.EARTHEN_TREMOR)
			.scoreboardId("EarthenTremor")
			.shorthandName("ET")
			.descriptions(
				String.format("Press swap with a weapon while sneaking to summon a earthen tremor on your position. Deals %s magic damage to mobs within %s blocks and pushes them away. Cooldown: %ss.",
					DAMAGE_1,
					RANGE,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Damage increased to %s and silences targets for %ss.",
					DAMAGE_2,
					StringUtils.ticksToSeconds(SILENCE_DURATION))
			)
			.simpleDescription("Summons a earthen tremor on your location, dealing damage and knocking mobs away.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EarthenTremor::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.DIRT);

	public EarthenTremor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, SILENCE_DURATION);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		for (LivingEntity mob : EntityUtils.getNearbyMobsInSphere(mPlayer.getLocation(), mRadius, null)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer, mob, mKnockback);
				if (isLevelTwo()) {
					EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
				}
			}
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation().add(0, 0.1, 0);

		for (Material mat : List.of(Material.PODZOL, Material.GRANITE, Material.IRON_ORE)) {
			ParticleUtils.explodingRingEffect(mPlugin, loc, mRadius, 0.3, 5, 0.075, l -> new PartialParticle(Particle.BLOCK_CRACK, loc, 30, mRadius / 2, 0.25, mRadius / 2, 0.1, mat.createBlockData()).spawnAsPlayerActive(mPlayer));
		}
		ParticleUtils.explodingRingEffect(mPlugin, loc, mRadius, 0.3, 5, 0.05, l -> new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 6, mRadius / 2, 0.1, mRadius / 2, 0.1).spawnAsPlayerActive(mPlayer));

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2, 0.6f);
		world.playSound(loc, Sound.ITEM_AXE_WAX_OFF, 0.4f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.25f, 0.5f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, 0.4f, 2.0f);

	}
}
