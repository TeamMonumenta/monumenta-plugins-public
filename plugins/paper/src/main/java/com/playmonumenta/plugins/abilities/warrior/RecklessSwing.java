package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class RecklessSwing extends Ability {

	private static final int COOLDOWN = 60 * 20;

	public static final AbilityInfo<RecklessSwing> INFO =
		new AbilityInfo<>(RecklessSwing.class, "Reckless Swing", RecklessSwing::new)
			.linkedSpell(ClassAbility.RECKLESS_SWING)
			.scoreboardId("RecklessSwing")
			.shorthandName("RS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Swing recklessly")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RecklessSwing::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true).onGround(false)))
			.displayItem(Material.NETHERITE_AXE);

	public RecklessSwing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}
		putOnCooldown();
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
		mPlayer.setVelocity(NmsUtils.getVersionAdapter().getActualDirection(mPlayer));
		mPlayer.damage(5);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Location loc2 = mPlayer.getLocation();
			Hitbox hitbox = Hitbox.approximateCone(loc2, 5, 1.1);
			hitbox.getHitMobs().forEach(
				m -> {
					DamageUtils.damage(mPlayer, m, DamageEvent.DamageType.MELEE_SKILL, 4, ClassAbility.RECKLESS_SWING, true);
					MovementUtils.knockAway(loc2, m, 0.8f);
				}
			);
			Vector arcDir = loc2.getDirection().setY(0).normalize();
			double degree = VectorUtils.vectorToRotation(arcDir)[0] + 90;
			new PPCircle(Particle.SWEEP_ATTACK, loc2.clone().add(0, 0.15, 0), 5).delta(1).arcDegree(degree - Math.toDegrees(1.1), degree + Math.toDegrees(1.1)).countPerMeter(3).delta(0, 0.1, 0).spawnAsPlayerActive(mPlayer);
			new PPCircle(Particle.SWEEP_ATTACK, loc2.clone().add(0, 0.15, 0), 2).delta(1).arcDegree(degree - Math.toDegrees(1.1), degree + Math.toDegrees(1.1)).countPerMeter(3).delta(0, 0.1, 0).spawnAsPlayerActive(mPlayer);
			world.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
			world.playSound(loc2, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);
			mPlugin.mEffectManager.addEffect(mPlayer, "RecklessSwingBerserk", new PercentDamageDealt(10 * 20, 0.1) {
				@Override
				public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
					new PartialParticle(Particle.VILLAGER_ANGRY, mPlayer.getEyeLocation(), 2).delta(1).spawnAsPlayerBuff(mPlayer);
				}

				@Override
				public void onKill(EntityDeathEvent event, Player player) {
					if (!WalletUtils.tryToPayFromInventoryAndWallet(mPlayer, Objects.requireNonNull(InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString("epic:r3/items/currency/archos_ring"))), true, true)) {
						mPlayer.damage(1);
						mPlayer.sendMessage(Component.text("No archos ring found in wallet.", NamedTextColor.DARK_RED));
					}
				}
			});
		}, 10);

		return true;
	}

	private static Description<RecklessSwing> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<RecklessSwing> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addDashedLine();
	}

	private static Description<RecklessSwing> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addTrigger()
			.addDashedLine()
			.addLine("Player takes %d damage and lunges forward, dealing").statValues(stat(5))
			.addLine("%d melee damage to all mobs. Then, cause an earth").statValues(stat(120))
			.addLine("quake where you crash that deals %d melee damage to").statValues(stat(350))
			.addLine("nearby mobs and affects them with Tremble. You then")
			.addLine("go Berserk and gain killaura and auto aim for %t").statValues(stat(400))
			.addLine("and every mob you kill while Berserk puts %d Archos").statValues(stat(1))
			.addLine("Ring in your Bag of Hoarding. Berserk then goes")
			.addLine("Reckless and enables PvP and does a Reckless Swing")
			.addLine("again on a random nearby player which kills them.")
			.addDashedLine();
	}

}
