package com.playmonumenta.plugins.depths.abilities.earthbound;

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
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class BeastsClaw extends DepthsAbility {

	public static final String ABILITY_NAME = "Beast's Claw";
	private static final int COOLDOWN = 20 * 8;
	private static final int[] DAMAGE = {8, 10, 12, 14, 16, 24};
	private static final int CLAW_RADIUS = 3;
	private static final int[] STUN_DURATION = {15, 20, 25, 30, 35, 45};
	private static final double HITBOX_RADIUS = 1.1;
	private static final int CLAW_DELAY = 10;
	private static final double DISTANCE_PER_CLAW = 0.5;
	private static final double Y_OFFSET = 1.5;
	private static final int ARC_INC = 25;
	private static final int DEGREE_STEP = 5;
	private static final int START_ANGLE = 160;
	private static final int END_ANGLE = 360;
	private static final List<Material> MATERIALS = List.of(
		Material.PODZOL,
		Material.GRANITE,
		Material.IRON_ORE
	);

	public static final String CHARM_COOLDOWN = "Beast's Claw Cooldown";

	public static final DepthsAbilityInfo<BeastsClaw> INFO =
		new DepthsAbilityInfo<>(BeastsClaw.class, ABILITY_NAME, BeastsClaw::new, DepthsTree.EARTHBOUND, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.BEASTSCLAW)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", BeastsClaw::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.WOODEN_HOE)
			.descriptions(BeastsClaw::getDescription);

	private final double mDamage;
	private final double mVelocity;
	private final int mStunDuration;
	private final List<UUID> mHitMobs = new ArrayList<>();

	public BeastsClaw(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BEASTS_CLAW_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.BEASTS_CLAW_VELOCITY.mEffectName, 1);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.BEASTS_CLAW_STUN_DURATION.mEffectName, STUN_DURATION[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		mHitMobs.clear();

		World world = mPlayer.getWorld();

		// Dash
		world.playSound(mPlayer, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(mPlayer, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(mPlayer, Sound.ENTITY_STRIDER_DEATH, SoundCategory.PLAYERS, 0.8f, 0.6f);
		world.playSound(mPlayer, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(mPlayer, Sound.ENTITY_RAVAGER_HURT, SoundCategory.PLAYERS, 0.6f, 0.5f);
		world.playSound(mPlayer, Sound.ENTITY_SQUID_DEATH, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(mPlayer, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2f, 0.9f);
		Vector dir = mPlayer.getLocation().getDirection();
		mPlayer.setVelocity(dir.setY(dir.getY() * 0.1 + 0.35).multiply(mVelocity));

		Location eyeLoc = mPlayer.getEyeLocation();
		eyeLoc.subtract(eyeLoc.getDirection().multiply(0.15));
		ParticleUtils.drawParticleCircleExplosion(mPlayer, eyeLoc, 0, 0.85, 0, -90,
			10, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getEyeLocation(), 0, 0.85, 0, -90,
				10, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 2);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getEyeLocation(), 0, 0.85, 0, -90,
				10, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 4);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getEyeLocation(), 0, 0.85, 0, -90,
				10, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 6);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getEyeLocation(), 0, 0.85, 0, -90,
				10, 0.35f, false, 0, 0.1, Particle.EXPLOSION_NORMAL);
		}, 8);
		claw();

		return true;
	}

	private void claw() {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			World world = mPlayer.getWorld();
			world.playSound(mPlayer, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2f, 0.7f);
			world.playSound(mPlayer, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2f, 0.9f);
			world.playSound(mPlayer, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.5f, 0.5f);
			world.playSound(mPlayer, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.3f, 1.0f);
			world.playSound(mPlayer, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 0.6f);
			world.playSound(mPlayer, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.4f, 2.0f);
			world.playSound(mPlayer, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.9f);

			Location startLoc = mPlayer.getLocation().clone().add(0, Y_OFFSET, 0);
			startLoc.setPitch(Math.min(30, Math.max(0, -startLoc.getPitch())));
			for (int i = 0; i < 3; i++) {
				int finalI = i;
				ParticleUtils.drawCleaveArc(startLoc, CLAW_RADIUS, 160, START_ANGLE, END_ANGLE, 1, 180, 0, 0, ARC_INC,
					(Location l, int ring, double angleProgress) -> {
						spawnParticle(l.clone().add(0, -DISTANCE_PER_CLAW * finalI, 0), startLoc, true, finalI == 1, angleProgress);
					}, DEGREE_STEP);
				ParticleUtils.drawCleaveArc(startLoc, CLAW_RADIUS, 20, START_ANGLE, END_ANGLE, 1, 180, 0, 0, ARC_INC,
					(Location l, int ring, double angleProgress) -> {
						spawnParticle(l.clone().add(0, -DISTANCE_PER_CLAW * finalI, 0), startLoc, false, finalI == 1, angleProgress);
					}, DEGREE_STEP);
			}
		}, CLAW_DELAY);
	}

	private void spawnParticle(Location loc, Location startLoc, boolean rightHand, boolean centerClaw, double angleProgress) {
		// Make the particles move together with the player
		Location finalLoc = loc.clone().add(mPlayer.getLocation().clone().add(0, Y_OFFSET, 0).toVector().subtract(startLoc.toVector()));

		Vector dir = finalLoc.getDirection().clone().setY(0).normalize();
		Vector particleDir = dir.clone().rotateAroundY(rightHand ? Math.PI * -0.4 : Math.PI * 0.4);
		new PartialParticle(Particle.CRIT, finalLoc, 1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.BLOCK_CRACK, finalLoc.clone().add(dir.clone().multiply(-0.25)), 1)
			.data(MATERIALS.get(FastUtils.randomIntInRange(0, MATERIALS.size() - 1)).createBlockData())
			.extra(0.15)
			.delta(-particleDir.getX(), -particleDir.getY(), -particleDir.getZ()).directionalMode(true)
			.spawnAsPlayerActive(mPlayer);

		if (centerClaw && (angleProgress * END_ANGLE) % (DEGREE_STEP * 3) == 0) {
			List<LivingEntity> hitMobs = new Hitbox.SphereHitbox(finalLoc, HITBOX_RADIUS).getHitMobs();
			hitMobs.forEach(mob -> {
				if (!mHitMobs.contains(mob.getUniqueId())) {
					EntityUtils.applyStun(mPlugin, mStunDuration, mob);
					DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true, false);
					mHitMobs.add(mob.getUniqueId());
				}
			});
		}
	}


	private static Description<BeastsClaw> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to lunge forward and unleash a devastating claw swipe, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" melee damage and stunning mobs in front of you for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION[rarity - 1], false, true)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}


}
