package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Encore extends DepthsAbility {

	public static final String ABILITY_NAME = "Encore";
	public static final int[] COOLDOWN = {28 * 20, 26 * 20, 24 * 20, 22 * 20, 20 * 20, 16 * 20};
	public static final int DELAY = 5 * 20;

	public static final DepthsAbilityInfo<Encore> INFO =
		new DepthsAbilityInfo<>(Encore.class, ABILITY_NAME, Encore::new, DepthsTree.PRISMATIC, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.ENCORE)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Encore::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.JUKEBOX)
			.descriptions(Encore::getDescription);

	private boolean mActive = false;

	public Encore(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		mActive = true;
		putOnCooldown();

		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				Location loc = mPlayer.getLocation().add(0, 0.1, 0);
				World world = mPlayer.getWorld();

				switch (mTicks) {
					case 0 -> {
						new PPCircle(Particle.NOTE, loc, 1.75).countPerMeter(1.5).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.5f, Constants.NotePitches.GS2);
					}
					case 2 -> {
						new PPCircle(Particle.NOTE, loc, 1.25).countPerMeter(1.5).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.6f, Constants.NotePitches.F11);
					}
					case 4 -> {
						new PPCircle(Particle.NOTE, loc, 0.75).countPerMeter(1.5).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.7f, Constants.NotePitches.CS7);
					}
					default -> {
					}
				}

				mTicks++;
				if (mTicks > 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (!mActive) {
			return true;
		}

		ClassAbility spell = event.getSpell();
		if (spell == ClassAbility.ICE_BARRIER || spell == ClassAbility.ENCORE || spell == ClassAbility.RAPIDFIRE) {
			return true;
		}

		if (!(event.getAbility() instanceof DepthsAbility ability)) {
			return true;
		}

		abilityCastEventInner(ability, ability.getInfo(), spell);
		return true;
	}

	@SuppressWarnings("unused")
	private <T extends DepthsAbility> void abilityCastEventInner(DepthsAbility a, DepthsAbilityInfo<T> info, ClassAbility spell) {
		Class<T> clazz = info.getAbilityClass();
		if (a.getClass() != clazz) {
			// Should never happen
			return;
		}
		@SuppressWarnings("unchecked")
		T ability = (T) a;

		Runnable action;
		if (info.getDepthsTrigger() == DepthsTrigger.SHIFT_BOW) {
			action = () -> {
				boolean sneak = mPlayer.isSneaking();
				mPlayer.setSneaking(true);
				AbstractArrow arrow = mPlayer.getWorld().spawnArrow(mPlayer.getEyeLocation(), mPlayer.getEyeLocation().getDirection(), 3, 0);
				arrow.setShooter(mPlayer);
				arrow.setCritical(true);
				arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
				ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
				Bukkit.getPluginManager().callEvent(eventLaunch);
				mPlayer.setSneaking(sneak);
			};
		} else {
			List<AbilityTriggerInfo<T>> triggers = info.getTriggers();
			if (triggers.isEmpty()) {
				return;
			}
			Predicate<T> method = triggers.get(0).getAction();
			action = () -> method.test(ability);
		}

		// start the countdown!
		cancelOnDeath(new BukkitRunnable() {
			int mTicks = 0;
			Vector mRotation = VectorUtils.rotateYAxis(new Vector(1, 0, 0), mPlayer.getLocation().getYaw() + 90);
			@Override
			public void run() {
				Location loc = mPlayer.getLocation().add(0, 0.25, 0);
				World world = mPlayer.getWorld();

				if (mTicks < DELAY) {
					Location particleLoc = loc.clone().add(mRotation);
					double color = mTicks * 1.0 / DELAY;
					new PartialParticle(Particle.NOTE, particleLoc, 1, color, 0, 0, 1).directionalMode(true).spawnAsPlayerActive(mPlayer);
					mRotation = VectorUtils.rotateYAxis(mRotation, 18);
				}
				switch (mTicks) {
					case 20 -> mPlayer.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.75f, Constants.NotePitches.CS7);
					case 40 -> mPlayer.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.75f, Constants.NotePitches.F11);
					case 60 -> mPlayer.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.75f, Constants.NotePitches.GS14);
					case 80 -> mPlayer.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.75f, Constants.NotePitches.B17);

					case DELAY -> {
						new PPCircle(Particle.NOTE, loc, 1.5).countPerMeter(0.75).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.75f, Constants.NotePitches.GS14);
					}
					case DELAY + 1 -> {
						new PPCircle(Particle.NOTE, loc, 2.5).countPerMeter(0.75).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.75f, Constants.NotePitches.B17);
					}
					case DELAY + 2 -> {
						new PPCircle(Particle.NOTE, loc, 3.5).countPerMeter(0.75).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 0.85f, Constants.NotePitches.CS19);
					}
					case DELAY + 3 -> {
						new PPCircle(Particle.NOTE, loc, 4.5).countPerMeter(0.75).extra(1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.PLAYERS, 1f, Constants.NotePitches.F23);
					}
					default -> {
					}
				}

				mTicks++;
				if (mTicks > DELAY + 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		cancelOnDeath(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			int prevCooldown = mPlugin.mTimers.getCooldown(mPlayer.getUniqueId(), spell);
			mPlugin.mTimers.removeCooldown(mPlayer, spell, false);
			action.run();
			mPlugin.mTimers.addCooldown(mPlayer, spell, prevCooldown);
		}, DELAY));
		mActive = false;
	}

	private static Description<Encore> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Encore>(color)
			.add("Left click while sneaking and holding a weapon to cause the next active ability you cast to be recast at no cooldown cost ")
			.addDuration(DELAY)
			.add(" seconds later.")
			.addCooldown(COOLDOWN[rarity - 1], true);
	}
}
