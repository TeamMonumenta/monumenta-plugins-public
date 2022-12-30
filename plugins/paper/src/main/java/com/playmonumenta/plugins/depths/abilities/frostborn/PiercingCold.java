package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class PiercingCold extends DepthsAbility {

	public static final String ABILITY_NAME = "Piercing Cold";
	public static final int[] DAMAGE = {14, 17, 20, 23, 26, 32};
	public static final int ICE_TICKS = 8 * 20;
	public static final int COOLDOWN = 11 * 20;
	private static final Particle.DustOptions ENCHANTED_ARROW_COLOR = new Particle.DustOptions(Color.fromRGB(80, 32, 140), 2.0f);
	private static final Particle.DustOptions ENCHANTED_ARROW_FRINGE_COLOR = new Particle.DustOptions(Color.fromRGB(168, 255, 252), 2.0f);
	private static final int MAX_DIST = 50;

	public static final DepthsAbilityInfo<PiercingCold> INFO =
		new DepthsAbilityInfo<>(PiercingCold.class, ABILITY_NAME, PiercingCold::new, DepthsTree.FROSTBORN, DepthsTrigger.SHIFT_BOW)
			.linkedSpell(ClassAbility.PIERCING_COLD)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.PRISMARINE_SHARD))
			.descriptions(PiercingCold::getDescription, MAX_RARITY);

	public PiercingCold(Plugin plugin, Player player) {
		super(plugin, player, INFO);
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

		BoundingBox box = BoundingBox.of(mPlayer.getEyeLocation(), 1.5, 1.5, 1.5);

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		Vector dir = loc.getDirection().normalize();

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 1.4f);
		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_DEATH, 0.7f, 0.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.5f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.75f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.75f);

		new PartialParticle(Particle.CRIT, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc.clone().add(dir), 20, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(dir), 10, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getEyeLocation(), MAX_DIST, mPlayer);

		Location pLoc = mPlayer.getEyeLocation();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
		pVec = pVec.normalize();

		for (int i = 0; i < MAX_DIST; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 5, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.FALLING_DUST, bLoc, 5, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData("light_blue_glazed_terracotta")).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, bLoc, 6, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 3, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, bLoc, 3, 0.1, 0.1, 0.1, 0, ENCHANTED_ARROW_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, bLoc, 5, 0.15, 0.15, 0.15, 0.4).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, bLoc.clone().add(pVec), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, bLoc.clone().add(pVec), 1, 0, 0, 0, 0.02).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, bLoc.clone().add(dir.clone().multiply(0.5)).add(pVec.clone().rotateAroundAxis(dir, Math.PI / 12)), 1, 0, 0, 0, 0, ENCHANTED_ARROW_FRINGE_COLOR).spawnAsPlayerActive(mPlayer);
			Iterator<LivingEntity> iterator = mobs.iterator();
			while (iterator.hasNext()) {
				LivingEntity mob = iterator.next();
				if (mob.getBoundingBox().overlaps(box)) {
					new PartialParticle(Particle.CRIT_MAGIC, mob.getLocation().add(0, 1, 0), 15, 0.1, 0.2, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.SPELL_INSTANT, mob.getLocation().add(0, 1, 0), 20, 0.1, 0.2, 0.1, 0.15).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0), 10, 0.1, 0.2, 0.1, 0.1).spawnAsPlayerActive(mPlayer);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true);
					/* Prevent mob from being hit twice in one shot */
					iterator.remove();
				}
			}
			Block block = bLoc.getBlock();

			// Replace blocks at bottom with ice
			Location checkingLoc = bLoc.clone();
			int count = 0;
			while (!world.getBlockAt(checkingLoc).isSolid() && world.getBlockAt(checkingLoc).getType() != Material.WATER && count < 15) {
				checkingLoc = checkingLoc.clone().add(0, -1, 0);
				count++;
			}
			if (count < 15) {
				Block centerBlock = world.getBlockAt(checkingLoc);
				ArrayList<Block> blocksToIce = new ArrayList<>();
				blocksToIce.add(centerBlock);
				blocksToIce.add(centerBlock.getRelative(BlockFace.NORTH));
				blocksToIce.add(centerBlock.getRelative(BlockFace.EAST));
				blocksToIce.add(centerBlock.getRelative(BlockFace.SOUTH));
				blocksToIce.add(centerBlock.getRelative(BlockFace.WEST));

				for (Block b : blocksToIce) {
					DepthsUtils.iceExposedBlock(b, ICE_TICKS, mPlayer);
				}
			}

			if (block.getType().isSolid() || block.getType() == Material.WATER) {
				new PartialParticle(Particle.SMOKE_LARGE, bLoc, 80, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, bLoc, 80, 0.1, 0.1, 0.1, 0.2).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, bLoc, 50, 0.1, 0.1, 0.1, 0.3).spawnAsPlayerActive(mPlayer);
				world.playSound(bLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.85f);
				break;
			}

			pVec.rotateAroundAxis(dir, Math.PI / 6);
		}

		return true;
	}

	private static String getDescription(int rarity) {
		return "Shooting a projectile while sneaking instead shoots an enchanted beam of frost that deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage and leaves a trail of ice below it that lasts for " + ICE_TICKS / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s.";
	}


}

