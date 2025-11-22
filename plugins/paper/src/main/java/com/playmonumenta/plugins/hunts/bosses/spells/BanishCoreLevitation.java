package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BanishCoreLevitation extends Spell implements CoreElemental.CoreElementalBase, Listener {
	// For how long the players have to hit the boss within
	private static final int DURATION = 13 * 20;
	// Maximum distance from the boss that the players can receive levitation effect
	private static final int LEVITATION_RANGE = 14;
	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final CoreElemental mQuarry;
	private final Location mStartLoc;
	private final Vector[][] mFissureVector;
	private final PassiveCoreInstability mCoreInstability;
	private final List<Block> mPlacedBlocks = new ArrayList<>();
	private List<Player> mAffectedPlayers = new ArrayList<>();
	private final List<Player> mLevitatedPlayer = new ArrayList<>();

	public BanishCoreLevitation(Plugin plugin, LivingEntity boss, CoreElemental quarry, Location startLoc, Vector[][] fissureVector, PassiveCoreInstability coreInstability) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
		mStartLoc = startLoc;
		mFissureVector = fissureVector;
		mCoreInstability = coreInstability;
	}

	@Override
	public void run() {
		mQuarry.mIsCastingBanish = true;
		mCoreInstability.castAbility(this);
		// Register events, for removing blocks placed by players
		mPlugin.getServer().getPluginManager().registerEvents(this, mPlugin);

		// Teleport Core to centre
		mBoss.teleport(mStartLoc.clone().add(0, 5, 0));
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, DURATION, 5, false, false));

		mPlacedBlocks.clear();
		mAffectedPlayers = new ArrayList<>(mQuarry.getPlayers());
		mAffectedPlayers.forEach(player -> player.sendMessage(Component.text("The core is ascending...", NamedTextColor.DARK_RED)));
		// Effects
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2f, 1.45f);
		mActiveTasks.add(new BukkitRunnable() {
			int mTick = 0;

			@Override
			public void run() {
				mTick++;
				if (mTick % 20 == 0) {
					// Particles
					new PartialParticle(Particle.REDSTONE, mStartLoc)
						.count(100)
						.delta(12, 1, 12)
						.extra(2)
						.data(new Particle.DustOptions(Color.fromRGB(204, 204, 204), 2))
						.spawnAsBoss();
					ParticleUtils.drawSphere(mBoss.getEyeLocation(), 10, LEVITATION_RANGE,
						(loc, t) -> {
							Vector direction = LocationUtils.getDirectionTo(mBoss.getEyeLocation(), loc);
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc)
								.count(2)
								.delta(direction.getX(), direction.getY(), direction.getZ())
								.extra(0.3)
								.directionalMode(true)
								.spawnAsBoss();
						});
					new PPCircle(Particle.REDSTONE, mBoss.getLocation(), LEVITATION_RANGE)
						.count(40)
						.data(new Particle.DustOptions(Color.fromRGB(204, 204, 204), 3))
						.delta(0.3)
						.spawnAsEntityActive(mBoss);
					for (Vector[] vectors : mFissureVector) {
						Location startLoc = mStartLoc.clone().add(0, 0.5, 0);
						for (Vector vector : vectors) {
							new PPLine(Particle.CAMPFIRE_COSY_SMOKE, startLoc, vector.clone().normalize(), vector.length())
								.countPerMeter(FastUtils.randomDoubleInRange(0.1, 0.5))
								.delta(0, FastUtils.randomDoubleInRange(0.01, 0.1), 0)
								.directionalMode(true)
								.extra(3)
								.spawnAsEntityActive(mBoss);
							startLoc = startLoc.add(vector);
						}
					}
					// Give nearby players levitation
					if (mTick > 20 * 3) {
						for (Player player : PlayerUtils.playersInRange(mBoss.getEyeLocation(), LEVITATION_RANGE, true)) {
							if (mAffectedPlayers.contains(player) && player.getLocation().getY() < mBoss.getLocation().getY()) {
								PotionUtils.applyPotion(mBoss, player, new PotionEffect(PotionEffectType.LEVITATION, 20, 1, false, false, false));
								if (!mLevitatedPlayer.contains(player)) {
									mLevitatedPlayer.add(player);
									player.sendMessage(Component.text("The heat from the Core causes you to float. Get closer to it!", CoreElemental.COLOR));
								}
							}
						}
					}
				}
				if (mTick % 10 == 0) {
					// Sounds
					for (Player player : mAffectedPlayers) {
						player.playSound(player, Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 3f, 0.5f + (float) mTick / DURATION * 1.5f);
						player.playSound(player, Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 3f, 0.5f + (float) mTick / DURATION * 1);
					}
				}
				if (mTick == 30) {
					mBoss.setInvulnerable(false);
				}
				if (mTick < 20 * 3) {
					mBoss.teleport(mBoss.getLocation().add(0, 0.15, 0));
					EntityUtils.setSize(mBoss, 3 + (int) Math.floor(mTick / 10d));
				}
				mBoss.setRotation(mTick * 15, 0);
				if (mTick > DURATION) {
					// Banish players who haven't struck the boss
					this.cancel();
					for (Player player : mAffectedPlayers) {
						player.sendMessage(Component.text("The Core has melted and fragmented, never to be seen again.", NamedTextColor.DARK_RED));
						player.removePotionEffect(PotionEffectType.LEVITATION);
						mQuarry.banish(player);
					}
					mAffectedPlayers.clear();
					mLevitatedPlayer.clear();
					EntityUtils.setSize(mBoss, 3);
					mBoss.setAI(true);
					fall();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void fall() {
		mQuarry.mIsCastingBanish = false;
		// Unregister the events because they are not used again
		HandlerList.unregisterAll(this);
		// Effects
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getEyeLocation())
			.count(20)
			.delta(0.1)
			.extra(0.4)
			.spawnAsBoss();
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid()) {
					this.cancel();
					return;
				}
				for (Block block : new ArrayList<>(mPlacedBlocks)) {
					if (block.getY() > mBoss.getLocation().getY() - 2) {
						new PartialParticle(Particle.BLOCK_CRACK, block.getLocation())
							.count(20)
							.delta(0.5)
							.data(block.getType().createBlockData())
							.spawnAsBoss();
						block.setType(Material.AIR);
						mPlacedBlocks.remove(block);
					}
				}

				if (mPlacedBlocks.isEmpty()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		// Remove player from banish
		if (event.getSource() instanceof Player player
			&& mAffectedPlayers.contains(player)) {
			if (event.getType() == DamageEvent.DamageType.MELEE) {
				mAffectedPlayers.remove(player);
				player.removePotionEffect(PotionEffectType.LEVITATION);
				ChargeUpManager chargeUp = mQuarry.mCoreInstability.getChargeUp(this);
				if (chargeUp != null) {
					chargeUp.excludePlayer(player);
				}
				// Effect
				new PPCircle(Particle.REDSTONE, mBoss.getBoundingBox().getCenter().toLocation(mBoss.getWorld()), ((MagmaCube) mBoss).getSize() + 3)
					.count(30)
					.data(new Particle.DustOptions(Color.RED, 1))
					.spawnAsBoss();
				player.sendMessage(Component.text("The heat has escaped from the Core, cooling it down.", TextColor.color(255, 204, 71)));
			} else {
				event.setCancelled(true);
				player.sendMessage(Component.text("Your attacks dissipated before they can reach the Core. Get closer!", NamedTextColor.DARK_RED));
				player.playSound(player.getEyeLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 2f, 2f);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return DURATION + 3 * 20;
	}

	@Override
	public boolean persistOnPhaseChange() {
		return true;
	}

	@Override
	public String getSpellName() {
		return String.format("Asthenospheric Plunder (%s)", Quarry.BANISH_CHARACTER);
	}

	@Override
	public String getSpellChargePrefix() {
		return "Incoming";
	}

	@Override
	public int getChargeDuration() {
		return DURATION;
	}

	@Override
	public int getSpellDuration() {
		return 0;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Block block = event.getBlockPlaced();
		if (mQuarry.getPlayers().contains(event.getPlayer()) && block.isSolid() && !BlockUtils.isMechanicalBlock(block.getType()) && !BlockUtils.isValuableBlock(block.getType())) {
			mPlacedBlocks.add(block);
		}
	}
}
