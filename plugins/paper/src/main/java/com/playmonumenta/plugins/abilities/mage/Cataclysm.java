package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.StarfallCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Cataclysm extends Ability {

	private static final int COOLDOWN = 80 * 20;

	private final StarfallCS mCosmetic;

	public static final AbilityInfo<Cataclysm> INFO =
		new AbilityInfo<>(Cataclysm.class, "Cataclysm", Cataclysm::new)
			.linkedSpell(ClassAbility.CATACLYSM)
			.scoreboardId("Cataclysm")
			.shorthandName("Ca")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Blow up everything.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Cataclysm::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).onGround(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.END_CRYSTAL);


	public Cataclysm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new StarfallCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location playerStartLocation = mPlayer.getLocation();

		World world = mPlayer.getWorld();
		BoundingBox movingPlayerBox = mPlayer.getBoundingBox();
		Vector vector = playerStartLocation.getDirection();
		LocationUtils.travelTillObstructed(
			world,
			movingPlayerBox,
			5,
			vector,
			0.1
		);
		Location playerEndLocation = movingPlayerBox
			.getCenter()
			.setY(movingPlayerBox.getMinY())
			.toLocation(world)
			.setDirection(vector);

		if (!playerEndLocation.getWorld().getWorldBorder().isInside(playerEndLocation)
			|| ZoneUtils.hasZoneProperty(playerEndLocation, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return true;
		}
		PlayerUtils.playerTeleport(mPlayer, playerEndLocation);
		mPlayer.getWorld().playSound(playerEndLocation, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.2f, 1f);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			new PartialParticle(Particle.GUST_EMITTER, playerEndLocation, 15, 5, 0, 5).spawnAsEntityActive(mPlayer);
			new PartialParticle(Particle.EXPLOSION_HUGE, playerEndLocation, 15, 5, 0, 5).spawnAsEntityActive(mPlayer);
			mPlayer.getWorld().playSound(playerEndLocation, Sound.BLOCK_END_PORTAL_SPAWN, 1f, 1f);
			mPlayer.getWorld().playSound(playerEndLocation, Sound.ITEM_TRIDENT_THUNDER, 1f, 1f);
			mPlayer.getWorld().playSound(playerEndLocation, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
			mPlayer.getWorld().playSound(playerEndLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);
			EntityUtils.getNearbyMobs(playerEndLocation, 7).forEach(e -> {
				DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.MAGIC, 5, ClassAbility.CATACLYSM, true);
				MovementUtils.knockAway(playerEndLocation, e, 0.8f);
			});

			// Code stolen from Starfall.java
			new BukkitRunnable() {
				double mT = 0;

				@Override
				public void run() {
					new BukkitRunnable() {
						double mT = 0;
						final Location mRandomLoc = playerEndLocation.clone().add(FastUtils.randomDoubleInRange(-6, 6), 25, FastUtils.randomDoubleInRange(-6, 6));
						final Location mOgLoc = mRandomLoc.clone().subtract(0, 25, 0);

						@Override
						public void run() {
							mT += 1;
							World world = mPlayer.getWorld();
							for (int i = 0; i < 8; i++) {
								mRandomLoc.subtract(0, 0.25, 0);
								if (!mRandomLoc.isChunkLoaded() || mRandomLoc.getBlock().getType().isSolid()) {
									if (mRandomLoc.getY() - mOgLoc.getY() <= 2) {
										mCosmetic.starfallLandEffect(world, mPlayer, mRandomLoc, playerEndLocation, 4);
										this.cancel();
										Hitbox hitbox = new Hitbox.SphereHitbox(mRandomLoc, 4);
										for (LivingEntity e : hitbox.getHitMobs()) {
											DamageUtils.damage(mPlayer, e, DamageEvent.DamageType.MAGIC, 2, ClassAbility.CATACLYSM, true);
											MovementUtils.knockAway(mRandomLoc, e, 0.4f, true);
										}
										break;
									}
								}
							}
							mCosmetic.starfallFallEffect(world, mPlayer, mRandomLoc, playerEndLocation, mOgLoc, mT);

							if (mT >= 50) {
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					mT += 8;
					if (mT >= 80) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 8);

		}, 6);
		putOnCooldown();
		return true;
	}

	private static Description<Cataclysm> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<Cataclysm> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<Cataclysm> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Teleport forward and send out a powerful Shockwave,")
			.addLine("which kills everything nearby and heals you.")
			.addLine()
			.addStat("Radius: %r").statValues(stat(20))
			.addStat("Damage: %d (s)").statValues(stat(450))
			.addStat("Healing: %p").statValues(stat(2))
			.addStat("Cooldown: %t").statValues(stat(COOLDOWN))
			.addLine()
			.addLine("Then, for the next %t, become a *Sorcerer* and").statValues(stat(80)).styles(Style.style(NamedTextColor.DARK_BLUE))
			.addLine("vomit *Starfall* everywhere every %t in a random").statValues(stat(8)).styles(UNDERLINED)
			.addLine("direction, ignoring cooldowns, which deal bonus")
			.addLine("damage and silence and stun to all hit mobs.")
			.addLine()
			.addStat("Starfall Damage Bonus: +%p").statValues(stat(3))
			.addStat("Effect: Silence for %t").statValues(stat(200))
			.addDashedLine();
	}

}
