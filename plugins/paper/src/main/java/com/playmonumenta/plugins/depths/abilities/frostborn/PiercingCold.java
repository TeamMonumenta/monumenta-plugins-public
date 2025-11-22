package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.HashSet;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

public class PiercingCold extends DepthsAbility {

	public static final String ABILITY_NAME = "Piercing Cold";
	public static final double[] DAMAGE = {18, 21, 24, 27, 30, 36};
	public static final int ICE_TICKS = 8 * 20;
	public static final int COOLDOWN = 11 * 20;
	private static final Particle.DustOptions ENCHANTED_ARROW_COLOR = new Particle.DustOptions(Color.fromRGB(80, 32, 140), 2.0f);
	private static final Particle.DustOptions ENCHANTED_ARROW_FRINGE_COLOR = new Particle.DustOptions(Color.fromRGB(168, 255, 252), 2.0f);
	private static final int MAX_DIST = 50;

	public static final String CHARM_COOLDOWN = "Piercing Cold Cooldown";

	public static final DepthsAbilityInfo<PiercingCold> INFO =
		new DepthsAbilityInfo<>(PiercingCold.class, ABILITY_NAME, PiercingCold::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.PIERCING_COLD)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.actionBarColor(TextColor.color(80, 32, 140))
			.displayItem(Material.PRISMARINE_SHARD)
			.descriptions(PiercingCold::getDescription);

	private final double mDamage;
	private final int mIceDuration;

	public PiercingCold(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.PIERCING_COLD_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.PIERCING_COLD_ICE_DURATION.mEffectName, ICE_TICKS);
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!mPlayer.isSneaking()
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, false)
			|| isOnCooldown()) {
			return true;
		}
		projectile.remove();
		mPlugin.mProjectileEffectTimers.removeEntity(projectile);
		putOnCooldown((int) (getModifiedCooldown() * BowAspect.getCooldownReduction(mPlayer)));

		World world = mPlayer.getWorld();
		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection().normalize();

		world.playSound(startLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 1.4f);
		world.playSound(startLoc, Sound.ENTITY_PUFFER_FISH_DEATH, SoundCategory.PLAYERS, 0.7f, 0.8f);
		world.playSound(startLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1f, 1.5f);
		world.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(startLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(startLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1f, 0.75f);

		Location endLoc = startLoc.clone();
		Location checkLoc = startLoc.clone();
		for (int i = 0; i < 90; i++) {
			if (startLoc.distance(checkLoc) > MAX_DIST) {
				endLoc = checkLoc.clone();
				break;
			}
			if ((checkLoc.getBlock().isSolid() && !DepthsUtils.isIce(checkLoc.getBlock().getType()))) {
				endLoc = checkLoc.clone();

				// if we hit a solid (non ice block, also play particles too
				new PartialParticle(Particle.SMOKE_LARGE, endLoc, 80, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, endLoc, 80, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, endLoc, 50, 0.1, 0.1, 0.1, 0.3).spawnAsPlayerActive(mPlayer);
				world.playSound(endLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0.85f);

				break;
			}

			checkLoc.add(dir.clone().multiply(0.5));
		}

		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
			new PartialParticle(Particle.CRIT_MAGIC, mob.getLocation().add(0, 1, 0), 15, 0.1, 0.2, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, mob.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.1, 0.2, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
		}

		new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, startLoc.clone().add(dir), 20, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, startLoc.clone().add(dir), 10, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);

		Location pLoc = mPlayer.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
		pVec = pVec.normalize();

		HashSet<Block> blocksToIce = new HashSet<>();

		// Ice blocks + travel particles
		Location currLoc = startLoc.clone();
		for (int i = 0; i < MAX_DIST; i++) {
			currLoc.add(dir);
			new PartialParticle(Particle.SMOKE_NORMAL, currLoc, 5, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FALLING_DUST, currLoc, 5, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData("light_blue_glazed_terracotta")).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, currLoc, 6, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_NORMAL, currLoc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, currLoc, 3, 0.1, 0.1, 0.1, 0, ENCHANTED_ARROW_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, currLoc, 5, 0.15, 0.15, 0.15, 0.4).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, currLoc.clone().add(pVec), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, currLoc.clone().add(pVec), 1, 0, 0, 0, 0.02).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, currLoc.clone().add(dir.clone().multiply(0.5)).add(pVec.clone().rotateAroundAxis(dir, Math.PI / 12)), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR).spawnAsPlayerActive(mPlayer);

			// Replace blocks at bottom with ice
			Block block = currLoc.getBlock();
			Location checkingLoc = currLoc.clone();
			int count = 0;
			while (!world.getBlockAt(checkingLoc).isSolid() && world.getBlockAt(checkingLoc).getType() != Material.WATER && count < 15) {
				checkingLoc = checkingLoc.clone().add(0, -1, 0);
				count++;
			}
			if (count < 15) {
				Block centerBlock = world.getBlockAt(checkingLoc);
				blocksToIce.add(centerBlock);
				blocksToIce.add(centerBlock.getRelative(BlockFace.NORTH));
				blocksToIce.add(centerBlock.getRelative(BlockFace.EAST));
				blocksToIce.add(centerBlock.getRelative(BlockFace.SOUTH));
				blocksToIce.add(centerBlock.getRelative(BlockFace.WEST));
			}

			// allow it to pass through ice
			if ((block.getType().isSolid() || block.getType() == Material.WATER) && !DepthsUtils.isIce(block.getType())) {
				break;
			}

			pVec.rotateAroundAxis(dir, Math.PI / 6);
		}

		// ice all blocks
		for (Block b : blocksToIce) {
			DepthsUtils.iceExposedBlock(b, mIceDuration, mPlayer);
		}

		return true;
	}

	private static Description<PiercingCold> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Shooting a projectile while sneaking instead shoots an enchanted beam of frost that deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage and leaves a trail of ice below it that lasts for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS)
			.add(" seconds. The beam can pass through ice blocks.")
			.addCooldown(COOLDOWN);
	}


}

