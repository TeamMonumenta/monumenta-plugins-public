package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.EndGateway;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class FalseSpiritPortal extends BossAbilityGroup {
	public static final String identityTag = "boss_falsespiritportal";
	public static final String SUMMON_CEILING_TAG = "CeilingGoHSummon";

	private static final List<String> mobs = Arrays.asList("IncompleteSpirit", "ImperfectKeterCore", "HalludHornet", "BarklessEnt", "PiercingEnt", "SpiritoftheValley");
	private static final List<String> ceilingMobs = Arrays.asList("IncompleteSpirit", "ImperfectKeterCore", "BarklessEnt", "PiercingEnt", "SpiritoftheValley");
	private static final String delveMiniboss = "IncompleteAberration";
	private static final String TRIDENT_TAG = "GoHTrident";
	private static final String SUMMON_TAG = "GoHSummon";
	private final List<LivingEntity> mTridentStands = new ArrayList<>();
	private final List<ArmorStand> mGates = new ArrayList<>(2);
	private final List<Player> mWarned = new ArrayList<>();
	private int mMobsKilled = 0;
	private int mPlayerCount;
	private @Nullable ItemStack mTrident = null;
	private @Nullable String mTridentName = null;
	//WARNING: Ceiling Gate also requires normal SUMMON_TAG as well as SUMMON_CEILING_TAG
	private @Nullable LivingEntity mCeilingGate = null;
	private String mPortalNumTag = "";
	//If delves is enabled in the instance
	private boolean mDelve = false;

	public FalseSpiritPortal(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		World world = mBoss.getWorld();
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, PercentDamageReceived.GENERIC_NAME,
			new PercentDamageReceived(20 * 60 * 27, -0.8));

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), FalseSpirit.detectionRange, true);
		mPlayerCount = players.size();
		if (mPlayerCount < 1) {
			mPlayerCount = 1;
		} else if (DelvesUtils.getPlayerTotalDelvePoint(null, players.get(0), ServerProperties.getShardName()) > 0) {
			mDelve = true;
		}

		//Wait until scoreboard tag is assigned first
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (String s : mBoss.getScoreboardTags()) {
				if (s.startsWith("PortalNum")) {
					mPortalNumTag = s;
				}
			}

			NamespacedKey key = NamespacedKeyUtils.fromString("epic:r2/dungeons/forum/ex_nihilo");
			switch (mPortalNumTag) {
				case "PortalNum1" -> key = NamespacedKeyUtils.fromString("epic:r2/dungeons/forum/ex_nihilo_hallud");
				case "PortalNum2" -> key = NamespacedKeyUtils.fromString("epic:r2/dungeons/forum/ex_nihilo_chasom");
				case "PortalNum3" -> key = NamespacedKeyUtils.fromString("epic:r2/dungeons/forum/ex_nihilo_midat");
				case "PortalNum4" -> key = NamespacedKeyUtils.fromString("epic:r2/dungeons/forum/ex_nihilo_daath");
				case "PortalNum5" -> key = NamespacedKeyUtils.fromString("epic:r2/dungeons/forum/ex_nihilo_keter");
				default -> {
				}
			}

			try {
				mTrident = InventoryUtils.getItemFromLootTable(mBoss, key);
				if (mTrident == null) {
					throw new Exception("[False Spirit] Failed to get trident from loot table! False Spirit will be impossible to defeat.");
				}
				mTridentName = MessagingUtils.plainText(mTrident.displayName());
				if (mTridentName == null) {
					throw new Exception("[False Spirit] Failed to set trident display name! False Spirit will be impossible to defeat.");
				}

				Damageable dm = (Damageable) mTrident.getItemMeta();
				dm.setDamage(248);
				mTrident.setItemMeta(dm);
			} catch (Exception exception) {
				// Thank you, Java for not having a default message for NPEs
				if (exception.getMessage() != null) {
					MMLog.warning(exception.getMessage());
				}
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), exception);
			}
		}, 1);

		Collection<ArmorStand> stands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), 20);
		for (ArmorStand as : stands) {
			Set<String> tags = as.getScoreboardTags();
			for (String tag : tags) {
				switch (tag) {
					case SUMMON_TAG -> mGates.add(as);
					case SUMMON_CEILING_TAG -> mCeilingGate = as;
					default -> {
					}
				}
			}
		}

		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), FalseSpirit.detectionRange, EnumSet.of(EntityType.ARMOR_STAND))) {
			Set<String> tags = e.getScoreboardTags();
			for (String tag : tags) {
				if (tag.equals(TRIDENT_TAG)) {
					mTridentStands.add(e);
				}
			}
		}

		for (ArmorStand gate : mGates) {
			for (int x = -5; x <= 5; x++) {
				for (int y = -5; y <= 5; y++) {
					for (int z = -5; z <= 5; z++) {
						Block b = gate.getLocation().add(x, y, z).getBlock();
						if (b.getType() == Material.BLACK_CONCRETE && FastUtils.RANDOM.nextInt(2) == 0) {
							b.setType(Material.PURPLE_CONCRETE);
						}
					}
				}
			}
		}

		List<Entity> mMobs = new ArrayList<>();

		if (mDelve) {
			ArmorStand as = mGates.get(FastUtils.RANDOM.nextInt(mGates.size()));
			Entity mob = LibraryOfSoulsIntegration.summon(as.getLocation().add(FastUtils.randomDoubleInRange(-1, 1), 0, FastUtils.randomDoubleInRange(-1, 1)), delveMiniboss);
			mMobs.add(mob);

			new PartialParticle(Particle.SPELL_WITCH, as.getLocation(), 30, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(boss);
			new PartialParticle(Particle.SMOKE_LARGE, as.getLocation(), 12, 0, 0.45, 0, 0.125).spawnAsEntityActive(boss);
			world.playSound(as.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 3f, 0.7f);

			world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 2);
		} else {
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 10, 2);
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				//Scales with player count to summon mobs
				//No more than 15 mobs from one portal can be out at a time
				//Custom spawn rate for delves, 50% faster for normal portals and 25% faster for the ceiling portal
				if (((!mDelve && mTicks % (100 / mPlayerCount) == 0) || (mDelve && mCeilingGate != null && mTicks % (80 / mPlayerCount) == 0) || (mDelve && mCeilingGate == null && mTicks % (67 / mPlayerCount) == 0))
					&& mMobs.size() <= 15) {
					ArmorStand as = mGates.get(FastUtils.RANDOM.nextInt(mGates.size()));
					String mobName;

					//Uses a different list for the ceiling gate
					if (mCeilingGate == null) {
						mobName = mobs.get(FastUtils.RANDOM.nextInt(mobs.size()));
					} else {
						mobName = ceilingMobs.get(FastUtils.RANDOM.nextInt(ceilingMobs.size()));
					}

					Entity mob = LibraryOfSoulsIntegration.summon(as.getLocation().add(FastUtils.randomDoubleInRange(-1, 1), 0, FastUtils.randomDoubleInRange(-1, 1)), mobName);
					//Gives slow falling to ceiling mobs
					if (mCeilingGate != null && mob instanceof LivingEntity) {
						((LivingEntity) mob).addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 10, 0));
					}
					mMobs.add(mob);

					new PartialParticle(Particle.SPELL_WITCH, as.getLocation(), 30, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, as.getLocation(), 12, 0, 0.45, 0, 0.125).spawnAsEntityActive(boss);
					world.playSound(as.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 3f, 0.7f);

					//After 60 seconds, kill mob
					new BukkitRunnable() {
						@Override
						public void run() {
							mMobs.remove(mob);

							if (mob != null && (!mob.isDead() || mob.isValid())) {
								List<Entity> passengers = mob.getPassengers();

								//Passengers do not teleport, have to be manually removed
								for (Entity passenger : passengers) {
									passenger.remove();
								}

								mob.remove();
							}
						}
					}.runTaskLater(mPlugin, 20 * 60);
				}

				// Spawns trident and resets kills (to spawn trident again)
				// If mTrident is null, the try/catch block at the top of this constructor complains about it
				if (mTrident != null && ((!mDelve && mMobsKilled >= 5 * mPlayerCount) || (mDelve && mMobsKilled >= 6 * mPlayerCount))) {
					mMobsKilled = 0;

					Location tridentLoc = mTridentStands.get(FastUtils.RANDOM.nextInt(mTridentStands.size())).getLocation();
					Item item = world.dropItem(tridentLoc, mTrident);
					item.setTicksLived(1);
					item.setPickupDelay(0);
					item.setCanMobPickup(false);
					item.setCanPlayerPickup(true);
					item.setGlowing(true);

					world.playSound(item.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 15, 0);
					new PartialParticle(Particle.CRIT, item.getLocation(), 20, 0.1, 0.1, 0.1).spawnAsEntityActive(boss);

					for (Player player : PlayerUtils.playersInRange(item.getLocation(), FalseSpirit.detectionRange, true)) {
						MessagingUtils.sendNPCMessage(player, "Bhairavi", "Quickly! On the hanging bookshelves! The Spear has formed! Take it and throw it at the gate!");
					}

					Location beaconLoc = tridentLoc.clone().add(0, 3, 0);

					// Spawns an end gateway to have a beacon laser to indicate location
					Block b = beaconLoc.getBlock();
					b.setType(Material.END_GATEWAY);
					if (b.getState() instanceof EndGateway eg) {
						eg.setAge(2400);
						eg.update();
					}

					// End gateways have their age "frozen" on this server, so this runnable doesn't need to change the block.
					new BukkitRunnable() {

						@Override
						public void run() {
							if (item.isDead() || !item.isValid() || mBoss.isDead() || !mBoss.isValid()) {
								b.setType(Material.AIR);
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 2);
				}

				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 20 * 5, 1);

		//Teleports mobs out of lower area
		new BukkitRunnable() {
			@Override
			public void run() {
				//Removes once portal and all the mobs are gone
				if (mMobs.isEmpty() && (mBoss.isDead() || !mBoss.isValid())) {
					this.cancel();
				}

				Iterator<Entity> mobs = mMobs.iterator();
				while (mobs.hasNext()) {
					Entity e = mobs.next();

					if (e == null || e.isDead() || !e.isValid()) {
						mobs.remove();
						mMobsKilled++;
					}

					//Teleport bees back that go too high
					if (e instanceof Bee && (e.getLocation().getY() - mBoss.getLocation().getY() > 3 || e.getLocation().getY() - mBoss.getLocation().getY() < -5)) {
						Location loc = mGates.get(FastUtils.RANDOM.nextInt(mGates.size())).getLocation();
						e.teleport(loc);
					}

					//If in "null space", teleport back to gate
					if (e instanceof LivingEntity && e.getLocation().getY() <= 7) {
						Location loc = mGates.get(FastUtils.RANDOM.nextInt(mGates.size())).getLocation();
						List<Entity> passengers = e.getPassengers();

						//Passengers do not teleport, have to be manually removed
						for (Entity passenger : passengers) {
							passenger.remove();
						}

						e.teleport(loc);
					}
				}
			}
		}.runTaskTimer(mPlugin, 20 * 5, 20 * 5);
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		if (event.getEntity() instanceof Trident trident && equalsTrident(trident.getItemStack())) {
			closePortal();
			trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);
			deleteTridents();
		}
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (damager instanceof Player player) {
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			if (equalsTrident(mainhand)) {
				event.setCancelled(true);
				MessagingUtils.sendNPCMessage(player, "Bhairavi", "It needs to be travelling faster than that! Throw the Spear!");
			} else if (!mWarned.contains(player)) {
				MessagingUtils.sendNPCMessage(player, "Bhairavi", "Don't attack the portal with your weapons! Kill the creatures, then hit the portal with the charged spear!");
				mWarned.add(player);
			}
		}
	}

	// Deletes all the tridents used to close the portals/gates so you can not hoard them
	private void deleteTridents() {
		for (Entity e : mBoss.getLocation().getNearbyEntities(100, 100, 100)) {
			if (e instanceof Item item) {
				if (equalsTrident(item.getItemStack())) {
					item.remove();
				}
			} else if (e instanceof Player player && mTridentName != null) {
				InventoryUtils.removeNamedItems(player, mTridentName);
			} else if (e instanceof Trident trident) {
				if (equalsTrident(trident.getItemStack())) {
					trident.remove();
				}
			}
		}
	}

	private void closePortal() {
		for (ArmorStand as : mGates) {
			for (int x = -5; x <= 5; x++) {
				for (int y = -5; y <= 5; y++) {
					for (int z = -5; z <= 5; z++) {
						Block b = as.getLocation().add(x, y, z).getBlock();
						if ((b.getType() == Material.BLACK_CONCRETE || b.getType() == Material.PURPLE_CONCRETE) && FastUtils.RANDOM.nextInt(2) == 0) {
							b.setType(Material.MAGMA_BLOCK);
						}
					}
				}
			}
			World world = mBoss.getWorld();
			new PartialParticle(Particle.EXPLOSION_HUGE, as.getLocation(), 1, 0, 0, 0).minimumCount(1).spawnAsEntityActive(mBoss);
			world.playSound(as.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 1);

			BoundingBox box = BoundingBox.of(as.getLocation(), 3, 3, 3);
			for (Player p : PlayerUtils.playersInRange(as.getLocation(), 5, true)) {
				if (box.overlaps(p.getBoundingBox())) {
					BossUtils.blockableDamage(mBoss, p, DamageType.MAGIC, 30);
				}
			}
		}

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), FalseSpirit.detectionRange, true)) {
			MessagingUtils.sendNPCMessage(player, "Bhairavi", "That's it! Head back to the fight!");
		}

		mBoss.remove();
	}

	//Checks if the given ItemStack equals the mTrident variable by name
	private boolean equalsTrident(@Nullable ItemStack item) {
		if (item != null) {
			return MessagingUtils.plainText(item.displayName()).equals(mTridentName);
		}
		return false;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		closePortal();
	}
}
