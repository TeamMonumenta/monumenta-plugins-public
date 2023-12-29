package com.playmonumenta.plugins.depths.bosses.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.VoidCorruption;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VesperidysBlockPlacerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_vesperidysblockplacer";

	private final Vesperidys mVesperidys;
	private final Plugin mMonuPlugin;
	private final int mCorruptionCleanse;

	public static @Nullable VesperidysBlockPlacerBoss deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return construct(plugin, boss);
	}

	public static @Nullable VesperidysBlockPlacerBoss construct(Plugin plugin, LivingEntity boss) {
		// Get nearest entity called Vesperidys.
		Vesperidys vesperidys = null;
		List<LivingEntity> witherSkeletons = EntityUtils.getNearbyMobs(boss.getLocation(), 100, EnumSet.of(EntityType.WITHER_SKELETON));
		for (LivingEntity mob : witherSkeletons) {
			if (mob.getScoreboardTags().contains(Vesperidys.identityTag)) {
				vesperidys = BossUtils.getBossOfClass(mob, Vesperidys.class);
				break;
			}
		}
		if (vesperidys == null) {
			MMLog.warning("VesperidysBlockPlacerBoss: Vesperidys wasn't found! (This is a bug)");
			return null;
		}
		return new VesperidysBlockPlacerBoss(plugin, boss, vesperidys);
	}

	public VesperidysBlockPlacerBoss(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		this(plugin, boss, vesperidys, 5);
	}

	public VesperidysBlockPlacerBoss(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, int corruptionCleanse) {
		super(plugin, identityTag, boss);
		mMonuPlugin = plugin;
		mVesperidys = vesperidys;
		mCorruptionCleanse = corruptionCleanse;

		List<Spell> spells = List.of(
			new Spell() {
				private final Mob mMob = (Mob) mBoss;

				@Override
				public void run() {
					if (mMob.getLocation().getY() < mVesperidys.mSpawnLoc.getY() - 5) {
						// Teleport back up
						Location newLoc = mVesperidys.mPlatformList.getRandomPlatform(null).getCenter().add(0, 1, 0);
						if (newLoc != null) {
							Location bossLoc = mBoss.getLocation();

							for (int i = 0; i < 50; i++) {
								Vector vec = LocationUtils.getDirectionTo(newLoc, bossLoc);
								Location pLoc = mBoss.getEyeLocation();
								pLoc.add(vec.multiply(i * 0.5));
								if (pLoc.distance(mBoss.getEyeLocation()) > newLoc.distance(bossLoc)) {
									break;
								}
								new PartialParticle(Particle.VILLAGER_ANGRY, pLoc, 1, 0, 0, 0, 0).spawnAsBoss();
							}

							mBoss.getWorld().playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);
							mBoss.getWorld().playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 2.0f, 1.0f);

							mMob.teleport(newLoc);
							mMob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 5 * 20, 0));
						}
					}

					// Get Y level for platform blocks.
					double yLevel = mVesperidys.mSpawnLoc.getY() - 1;

					if (Math.abs(mMob.getLocation().getY() - yLevel) < 3) {
						for (int i = -1; i <= 1; i++) {
							for (int j = -1; j <= 1; j++) {
								Location loc = mMob.getLocation().clone().add(i, 0, j);
								loc.setY(yLevel);

								Block block = loc.getBlock();
								if (block.getType() == Material.AIR || block.getType() == Material.LIGHT) {
									loc.getWorld().playSound(loc, Sound.BLOCK_NETHER_BRICKS_PLACE, SoundCategory.HOSTILE, 1f, 0.7f);
									block.setType(Material.TINTED_GLASS);
									mVesperidys.mAnticheese.mIgnored.add(block);

									new BukkitRunnable() {
										@Override public void run() {
											mVesperidys.mAnticheese.mIgnored.remove(block);
										}
									}.runTaskLater(mMonuPlugin, 5 * 20);
								}
							}
						}
					}
				}

				@Override
				public int cooldownTicks() {
					return 0;
				}
			}
		);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, Vesperidys.detectionRange);
		super.constructBoss(SpellManager.EMPTY, spells, Vesperidys.detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true)) {
			NavigableSet<VoidCorruption> corruptionSet = mMonuPlugin.mEffectManager.getEffects(player, VoidCorruption.class);
			if (corruptionSet.size() > 0) {
				corruptionSet.last().addCorruption(-mCorruptionCleanse);
			}
		}
	}
}
