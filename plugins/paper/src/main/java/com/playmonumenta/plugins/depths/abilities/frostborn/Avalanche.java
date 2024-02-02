package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Avalanche extends DepthsAbility {

	public static final String ABILITY_NAME = "Avalanche";
	public static final double[] DAMAGE = {30, 35, 40, 45, 50, 60};
	public static final int COOLDOWN_TICKS = 20 * 20;
	public static final int SLOW_DURATION = 2 * 20;
	public static final double SLOW_MODIFIER = 1;
	public static final int RADIUS = 8;
	public static final int NUM_PULSES = 4;
	private static final Particle.DustOptions ICE_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(200, 225, 255), 1.0f);

	public static final String CHARM_COOLDOWN = "Avalanche Cooldown";

	public static final DepthsAbilityInfo<Avalanche> INFO =
		new DepthsAbilityInfo<>(Avalanche.class, ABILITY_NAME, Avalanche::new, DepthsTree.FROSTBORN, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.AVALANCHE)
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.actionBarColor(TextColor.color(91, 187, 255))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Avalanche::cast, DepthsTrigger.SWAP))
			.displayItem(Material.SNOW_BLOCK)
			.descriptions(Avalanche::getDescription);

	private final double mRadius;
	private final double mDamage;
	private final int mDuration;

	public Avalanche(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.AVALANCHE_RANGE.mEffectName, RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.AVALANCHE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.AVALANCHE_ROOT_DURATION.mEffectName, SLOW_DURATION);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		Set<Location> checkIce = getNearbyIce(loc, mRadius);
		if (checkIce.size() == 0) {
			return false;
		}

		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		new BukkitRunnable() {
			int mPulses = 0;
			final List<LivingEntity> mHitMobs = new ArrayList<>();
			Set<Location> mIceToBreak = new HashSet<>();
			@Override
			public void run() {
				// re-obtain nearby ice every pulse in case ice disappears in the middle of casting
				mIceToBreak = getNearbyIce(loc, mRadius);
				mHitMobs.clear();
				for (Location l : mIceToBreak) {
					Location aboveLoc = l.clone().add(0.5, 1, 0.5);

					//Damage and root mobs
					for (LivingEntity mob : EntityUtils.getNearbyMobs(aboveLoc, 2, 5.0, 2)) {
						if (!mHitMobs.contains(mob)) {
							double damage = mDamage * (mPulses < 3 ? 0.15 : 0.55);
							DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, false, false);
							EntityUtils.applySlow(mPlugin, mDuration, SLOW_MODIFIER, mob);
							mHitMobs.add(mob);
						}
					}

					new PartialParticle(Particle.REDSTONE, aboveLoc.clone().add(0, 0.25, 0), 3, 0.3, 0.3, 0.3, ICE_PARTICLE_COLOR).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.BLOCK_CRACK, aboveLoc.clone().add(0, 0.25, 0), 3, 0.3, 0.3, 0.3, 0, Material.ICE.createBlockData()).spawnAsPlayerActive(mPlayer);
				}

				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
				world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 1.4f);
				world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 1.2f);

				mPulses++;
				if (mPulses >= NUM_PULSES) {
					for (Location l : mIceToBreak) {
						Block b = l.getBlock();
						if (b.getType() == Permafrost.PERMAFROST_ICE_MATERIAL) {
							//If special permafrost ice, set to normal ice instead of destroying
							b.setType(DepthsUtils.ICE_MATERIAL);
						} else {
							b.setBlockData(DepthsUtils.iceActive.get(l));
							DepthsUtils.iceActive.remove(l);
						}

						Location aboveLoc = l.clone().add(0.5, 1, 0.5);
						new PartialParticle(Particle.REDSTONE, aboveLoc.clone().add(0, 0.6, 0), 7, 0.3, 0.5, 0.3, ICE_PARTICLE_COLOR).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, aboveLoc.clone().add(0, 0.6, 0), 7, 0.3, 0.5, 0.3, 0, Material.ICE.createBlockData()).spawnAsPlayerActive(mPlayer);
					}

					ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getLocation(), 0, 1, -mPlayer.getLocation().getYaw(), -mPlayer.getLocation().getPitch(), 125,
						0.7f, true, 0, 0.1, Particle.EXPLOSION_NORMAL);
					ParticleUtils.drawParticleCircleExplosion(mPlayer, mPlayer.getLocation().add(0, 0.25, 0), 0, 1, -mPlayer.getLocation().getYaw(), -mPlayer.getLocation().getPitch(), 125,
						0.85f, true, 0, 0.1, Particle.EXPLOSION_NORMAL);

					world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 0.5f);
					world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 2.0f, 1.0f);
					world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 0.5f);
					world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.7f);
					world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.6f, 1.5f);
					world.playSound(loc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 1.0f, 0.5f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);
					world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.0f, 1.2f);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 3);

		return true;
	}

	private Set<Location> getNearbyIce(Location loc, double radius) {
		Set<Location> nearbyIce = new HashSet<>(DepthsUtils.iceActive.keySet());
		nearbyIce.removeIf(l -> !l.isWorldLoaded() || l.getWorld() != loc.getWorld() || l.distance(loc) > radius || !DepthsUtils.isIce(l.getBlock().getType()));
		return nearbyIce;
	}

	private static Description<Avalanche> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Avalanche>(color)
			.add("Swap hands to begin shattering all ice blocks within a radius of ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks, dealing a total of ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage in 4 pulses over 0.5s to enemies above the ice. The final pulse deals significantly more damage and removes the ice. Affected enemies are rooted for ")
			.addDuration(a -> a.mDuration, SLOW_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN_TICKS);
	}


}

