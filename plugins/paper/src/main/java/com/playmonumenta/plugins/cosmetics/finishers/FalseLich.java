package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants.Note;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Objects;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.loot.Lootable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FalseLich implements EliteFinisher {
	public static final String NAME = "False Lich";

	public static final double RANGE = 32.0;
	public static final NamespacedKey EMPTY_LOOTTABLE
		= Objects.requireNonNull(NamespacedKey.fromString("minecraft:empty"));

	private enum Instrument {
		GRAND_PIANO(Sound.BLOCK_NOTE_BLOCK_BANJO, 0.50f),
		BIT(Sound.BLOCK_NOTE_BLOCK_BIT, -0.50f),
		BIT2(Sound.BLOCK_NOTE_BLOCK_BIT, 0.50f),
		HARP1(Sound.BLOCK_NOTE_BLOCK_HARP, 0.20f),
		HARP2(Sound.BLOCK_NOTE_BLOCK_HARP, 0.20f),
		HARP3(Sound.BLOCK_NOTE_BLOCK_HARP, 0.10f),
		GUITAR(Sound.BLOCK_NOTE_BLOCK_GUITAR, 0.50f),
		PLING1(Sound.BLOCK_NOTE_BLOCK_PLING, -0.20f),
		PLING2(Sound.BLOCK_NOTE_BLOCK_PLING, -0.20f),
		BASS(Sound.BLOCK_NOTE_BLOCK_BASS, 0.00f),
		BASE_DRUM(Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.00f),
		SNARE_DRUM(Sound.BLOCK_NOTE_BLOCK_SNARE, -0.20f),
		CLICK(Sound.BLOCK_NOTE_BLOCK_HAT, -0.20f);

		public final Sound mSound;
		public final float mPan;

		Instrument(Sound sound, float pan) {
			mSound = sound;
			mPan = pan;
		}
	}


	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		World world = killedMob.getWorld();
		Vector killedLookDir = killedMob.getLocation().getDirection();
		final Vector panDir = VectorUtils.rotateYAxis(killedLookDir, 90);

		new BukkitRunnable() {
			LivingEntity mMob = (LivingEntity) killedMob;
			final Location mLocation = killedMob.getLocation();
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % 2 == 0) {
					int quarterNote = mTicks >> 1;
					int bar = quarterNote >> 2;
					quarterNote &= 0x3;
					switch (bar) {
						case 0 -> {
							switch (quarterNote) {
								case 0 -> {
									new PartialParticle(Particle.EXPLOSION_HUGE, mLocation, 1).spawnAsPlayerActive(p);
									world.playSound(mLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1, 1);
									world.playSound(mLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);

									playNote(Instrument.GRAND_PIANO, Note.F4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.D5, 1.0f);
									playNote(Instrument.HARP1, Note.A3, 1.0f);
									playNote(Instrument.HARP2, Note.F4, 1.0f);
									playNote(Instrument.HARP3, Note.A4, 1.0f);
									playNote(Instrument.PLING1, Note.A4, 0.6f);
									playNote(Instrument.PLING2, Note.A3, 0.6f);
									playNote(Instrument.BASS, Note.D4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.A3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.F4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.D5, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.A3, 1.0f);
									playNote(Instrument.CLICK, Note.G4, 1.0f);
								}
								case 3 -> playNote(Instrument.BASS, Note.D5, 0.1f);
								default -> {
								}
							}
						}
						case 1 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.PLING2, Note.A3, 0.4f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.A4, 0.63f);
									playNote(Instrument.BASS, Note.FS4, 0.1f);
									playNote(Instrument.CLICK, Note.G4, 0.6f);
								}
								case 3 -> {
									playNote(Instrument.GRAND_PIANO, Note.D5, 0.91f);
									playNote(Instrument.BASS, Note.F4, 0.2f);
									playNote(Instrument.CLICK, Note.G4, 0.8f);
								}
								default -> {
								}
							}
						}
						case 2 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.GRAND_PIANO, Note.G4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.E5, 1.0f);
									playNote(Instrument.HARP1, Note.B3, 1.0f);
									playNote(Instrument.HARP2, Note.E4, 1.0f);
									playNote(Instrument.HARP3, Note.G4, 1.0f);
									playNote(Instrument.PLING1, Note.G4, 0.6f);
									playNote(Instrument.PLING2, Note.G3, 0.6f);
									playNote(Instrument.BASS, Note.E4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.B3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.B3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.G4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.E5, 1.0f);
									playNote(Instrument.PLING1, Note.G4, 1.0f);
									playNote(Instrument.PLING2, Note.G3, 1.0f);
									playNote(Instrument.BASS, Note.E4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.B3, 1.0f);
									playNote(Instrument.CLICK, Note.G4, 1.0f);
								}
								default -> {
								}
							}
						}
						case 3 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.GRAND_PIANO, Note.C5, 0.82f);
									playNote(Instrument.PLING2, Note.G3, 0.4f);
									playNote(Instrument.BASS, Note.B4, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.B3, 1.0f);
								}
								case 1 -> playNote(Instrument.GRAND_PIANO, Note.G4, 0.56f);
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.C5, 0.64f);
									playNote(Instrument.BASS, Note.D5, 0.3f);
									playNote(Instrument.CLICK, Note.G4, 0.6f);
								}
								case 3 -> {
									playNote(Instrument.GRAND_PIANO, Note.E5, 1.0f);
									playNote(Instrument.BASS, Note.G4, 0.5f);
									playNote(Instrument.CLICK, Note.G4, 0.8f);
								}
								default -> {
								}
							}
						}
						case 4 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.GRAND_PIANO, Note.G4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.F5, 1.0f);
									playNote(Instrument.HARP1, Note.AS3, 1.0f);
									playNote(Instrument.HARP2, Note.D4, 1.0f);
									playNote(Instrument.HARP3, Note.G4, 1.0f);
									playNote(Instrument.PLING1, Note.AS4, 0.6f);
									playNote(Instrument.PLING2, Note.AS3, 0.6f);
									playNote(Instrument.BASS, Note.G4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.AS3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.AS3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.G4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.F5, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.AS3, 1.0f);
									playNote(Instrument.CLICK, Note.AS3, 0.9f);
								}
								case 3 -> playNote(Instrument.BASS, Note.G4, 0.3f);
								default -> {
								}
							}
						}
						case 5 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.GRAND_PIANO, Note.D5, 0.97f);
									playNote(Instrument.PLING2, Note.AS3, 0.4f);
									playNote(Instrument.BASS, Note.AS4, 0.8f);
									playNote(Instrument.SNARE_DRUM, Note.AS3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.AS4, 0.35f);
									playNote(Instrument.BASS, Note.D5, 0.7f);
									playNote(Instrument.CLICK, Note.AS3, 0.9f);
								}
								default -> {
								}
							}
						}
						case 6 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.GRAND_PIANO, Note.A4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.D5, 1.0f);
									playNote(Instrument.HARP1, Note.A3, 1.0f);
									playNote(Instrument.HARP2, Note.CS4, 1.0f);
									playNote(Instrument.HARP3, Note.F4, 1.0f);
									playNote(Instrument.PLING1, Note.A4, 0.6f);
									playNote(Instrument.PLING2, Note.A3, 0.6f);
									playNote(Instrument.BASS, Note.A4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.A3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.D5, 0.68f);
									playNote(Instrument.CLICK, Note.AS3, 0.9f);
								}
								case 3 -> playNote(Instrument.BASS, Note.A4, 0.1f);
								default -> {
								}
							}
						}
						case 7 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.GRAND_PIANO, Note.A4, 1.0f);
									playNote(Instrument.GRAND_PIANO, Note.E5, 1.0f);
									playNote(Instrument.HARP3, Note.E4, 1.0f);
									playNote(Instrument.PLING2, Note.CS4, 0.4f);
									playNote(Instrument.BASS, Note.E4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.A3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.GRAND_PIANO, Note.E5, 1.0f);
									playNote(Instrument.BASS, Note.E4, 0.4f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
									playNote(Instrument.CLICK, Note.AS3, 0.6f);
								}
								case 3 -> {
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
									playNote(Instrument.CLICK, Note.AS3, 0.8f);
								}
								default -> {
								}
							}
						}

						// Second half
						case 8 -> {
							switch (quarterNote) {
								case 0 -> {
									mMob = EliteFinishers.createClonedMob(le, p);
									ScoreboardUtils.addEntityToTeam(mMob, "lichfinisher", NamedTextColor.LIGHT_PURPLE);
									if (mMob instanceof Lootable lootable) {
										lootable.setLootTable(Bukkit.getLootTable(EMPTY_LOOTTABLE));
									}
									mLocation.add(0.0, mMob.getHeight() + 0.5, 0.0);
									mMob.teleport(mLocation);
									new PartialParticle(Particle.CLOUD, mLocation, 50, 0.1, 0.1, 0.1, 0.1).spawnAsBoss();
									world.playSound(mLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1, 1);

									playNote(Instrument.BIT, Note.AS4, 0.96f);
									playNote(Instrument.BIT2, Note.AS4, 0.96f);
									playNote(Instrument.HARP1, Note.D5, 1.0f);
									playNote(Instrument.HARP2, Note.AS4, 1.0f);
									playNote(Instrument.GUITAR, Note.D4, 0.7f);
									playNote(Instrument.PLING1, Note.A4, 1.0f);
									playNote(Instrument.PLING2, Note.D4, 1.0f);
									playNote(Instrument.BASS, Note.G4, 0.8f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.8f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.AS4, 0.92f);
									playNote(Instrument.BIT2, Note.AS4, 0.92f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.AS4, 0.88f);
									playNote(Instrument.BIT2, Note.AS4, 0.88f);
									playNote(Instrument.GUITAR, Note.F4, 0.6f);
									playNote(Instrument.BASS, Note.D5, 0.82f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.82f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.AS4, 0.84f);
									playNote(Instrument.BIT2, Note.AS4, 0.84f);
								}
								default -> {
								}
							}
						}
						case 9 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.G4, 1.0f);
									playNote(Instrument.BIT2, Note.G4, 1.0f);
									playNote(Instrument.HARP1, Note.D5, 1.0f);
									playNote(Instrument.HARP2, Note.G4, 1.0f);
									playNote(Instrument.GUITAR, Note.G4, 0.65f);
									playNote(Instrument.BASS, Note.G4, 0.84f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.84f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.G4, 0.92f);
									playNote(Instrument.BIT2, Note.G4, 0.92f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.G4, 0.88f);
									playNote(Instrument.BIT2, Note.G4, 0.88f);
									playNote(Instrument.GUITAR, Note.AS4, 0.6f);
									playNote(Instrument.BASS, Note.D5, 0.86f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.86f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.G4, 0.84f);
									playNote(Instrument.BIT2, Note.G4, 0.84f);
								}
								default -> {
								}
							}
						}
						case 10 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.A4, 1.0f);
									playNote(Instrument.BIT2, Note.A4, 1.0f);
									playNote(Instrument.HARP1, Note.E5, 1.0f);
									playNote(Instrument.HARP2, Note.A4, 1.0f);
									playNote(Instrument.HARP3, Note.G4, 1.0f);
									playNote(Instrument.GUITAR, Note.D4, 0.7f);
									playNote(Instrument.BASS, Note.G4, 0.88f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.88f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.A4, 1.0f);
									playNote(Instrument.BIT2, Note.A4, 1.0f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.A4, 0.96f);
									playNote(Instrument.BIT2, Note.A4, 0.96f);
									playNote(Instrument.GUITAR, Note.F4, 0.6f);
									playNote(Instrument.BASS, Note.D5, 0.9f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.9f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.A4, 0.92f);
									playNote(Instrument.BIT2, Note.A4, 0.92f);
								}
								default -> {
								}
							}
						}
						case 11 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.F4, 1.0f);
									playNote(Instrument.BIT2, Note.F4, 1.0f);
									playNote(Instrument.HARP1, Note.D5, 1.0f);
									playNote(Instrument.HARP2, Note.F4, 1.0f);
									playNote(Instrument.GUITAR, Note.G4, 0.65f);
									playNote(Instrument.BASS, Note.G4, 0.92f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.92f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.F4, 0.92f);
									playNote(Instrument.BIT2, Note.F4, 0.92f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.F4, 0.88f);
									playNote(Instrument.BIT2, Note.F4, 0.88f);
									playNote(Instrument.GUITAR, Note.AS4, 0.6f);
									playNote(Instrument.BASS, Note.D5, 0.94f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.94f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.F4, 0.84f);
									playNote(Instrument.BIT2, Note.F4, 0.84f);
								}
								default -> {
								}
							}
						}
						case 12 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.G4, 1.0f);
									playNote(Instrument.BIT2, Note.G4, 1.0f);
									playNote(Instrument.HARP1, Note.D5, 1.0f);
									playNote(Instrument.HARP2, Note.G4, 1.0f);
									playNote(Instrument.GUITAR, Note.CS5, 0.7f);
									playNote(Instrument.PLING1, Note.AS4, 1.0f);
									playNote(Instrument.PLING2, Note.G4, 1.0f);
									playNote(Instrument.BASS, Note.A4, 0.96f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.96f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.G4, 0.92f);
									playNote(Instrument.BIT2, Note.G4, 0.92f);
									playNote(Instrument.GUITAR, Note.E4, 0.6f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.G4, 0.88f);
									playNote(Instrument.BIT2, Note.G4, 0.88f);
									playNote(Instrument.GUITAR, Note.G4, 0.65f);
									playNote(Instrument.BASS, Note.CS5, 0.98f);
									playNote(Instrument.BASE_DRUM, Note.G3, 0.98f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.G4, 0.84f);
									playNote(Instrument.BIT2, Note.G4, 0.84f);
									playNote(Instrument.GUITAR, Note.AS4, 0.6f);
								}
								default -> {
								}
							}
						}
						case 13 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.F4, 1.0f);
									playNote(Instrument.BIT2, Note.F4, 1.0f);
									playNote(Instrument.HARP1, Note.D5, 1.0f);
									playNote(Instrument.HARP2, Note.F4, 1.0f);
									playNote(Instrument.GUITAR, Note.CS4, 0.7f);
									playNote(Instrument.BASS, Note.A4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.G3, 1.0f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.F4, 0.92f);
									playNote(Instrument.BIT2, Note.F4, 0.92f);
									playNote(Instrument.GUITAR, Note.E4, 0.6f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.F4, 0.88f);
									playNote(Instrument.BIT2, Note.F4, 0.88f);
									playNote(Instrument.GUITAR, Note.G4, 0.65f);
									playNote(Instrument.BASS, Note.CS5, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.G3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 0.8f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.F4, 0.84f);
									playNote(Instrument.BIT2, Note.F4, 0.84f);
									playNote(Instrument.GUITAR, Note.AS4, 0.6f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 0.9f);
								}
								default -> {
								}
							}
						}
						case 14 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.E4, 0.99f);
									playNote(Instrument.BIT2, Note.E4, 0.99f);
									playNote(Instrument.HARP1, Note.CS5, 1.0f);
									playNote(Instrument.HARP2, Note.E4, 1.0f);
									playNote(Instrument.GUITAR, Note.CS4, 0.7f);
									playNote(Instrument.PLING1, Note.A4, 1.0f);
									playNote(Instrument.PLING2, Note.E4, 1.0f);
									playNote(Instrument.BASS, Note.A4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.G3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.E4, 0.92f);
									playNote(Instrument.BIT2, Note.E4, 0.92f);
									playNote(Instrument.GUITAR, Note.E4, 0.6f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.E4, 0.88f);
									playNote(Instrument.BIT2, Note.E4, 0.88f);
									playNote(Instrument.GUITAR, Note.G4, 0.65f);
									playNote(Instrument.BASS, Note.CS5, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.G3, 1.0f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.E4, 0.84f);
									playNote(Instrument.BIT2, Note.E4, 0.84f);
									playNote(Instrument.GUITAR, Note.AS4, 0.6f);
								}
								default -> {
								}
							}
						}
						case 15 -> {
							switch (quarterNote) {
								case 0 -> {
									playNote(Instrument.BIT, Note.CS4, 1.0f);
									playNote(Instrument.BIT2, Note.CS4, 1.0f);
									playNote(Instrument.HARP1, Note.E5, 1.0f);
									playNote(Instrument.HARP2, Note.CS4, 1.0f);
									playNote(Instrument.GUITAR, Note.CS4, 0.7f);
									playNote(Instrument.BASS, Note.A4, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.G3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 1 -> {
									playNote(Instrument.BIT, Note.CS4, 0.92f);
									playNote(Instrument.BIT2, Note.CS4, 0.92f);
									playNote(Instrument.GUITAR, Note.E4, 0.6f);
								}
								case 2 -> {
									playNote(Instrument.BIT, Note.CS4, 0.87f);
									playNote(Instrument.BIT2, Note.CS4, 0.87f);
									playNote(Instrument.GUITAR, Note.G4, 0.65f);
									playNote(Instrument.BASS, Note.CS5, 1.0f);
									playNote(Instrument.BASE_DRUM, Note.G3, 1.0f);
									playNote(Instrument.SNARE_DRUM, Note.A3, 1.0f);
								}
								case 3 -> {
									playNote(Instrument.BIT, Note.CS4, 0.77f);
									playNote(Instrument.BIT2, Note.CS4, 0.77f);
									playNote(Instrument.GUITAR, Note.AS4, 0.6f);
								}
								default -> {
								}
							}
						}
						case 16 -> {
							if (quarterNote == 0) {
								playNote(Instrument.BIT, Note.D5, 1.0f);
								playNote(Instrument.BIT2, Note.D4, 1.0f);
								playNote(Instrument.HARP1, Note.D5, 1.0f);
								playNote(Instrument.HARP2, Note.E4, 1.0f);
								playNote(Instrument.PLING1, Note.A4, 1.0f);
								playNote(Instrument.PLING2, Note.F4, 1.0f);
								playNote(Instrument.BASS, Note.D4, 1.0f);
								playNote(Instrument.SNARE_DRUM, Note.A4, 1.0f);
								mMob.setSilent(false);
								mMob.setInvulnerable(false);
								mMob.setHealth(0);
								this.cancel();
							}
						}
						default -> {
						}
					}
				}
				mTicks++;
			}

			public void playNote(Instrument instrument, Note note, float volume) {
				Location notePos = mLocation.clone().add(panDir.clone().multiply(instrument.mPan));
				world.playSound(notePos, instrument.mSound, SoundCategory.PLAYERS, volume, note.mPitch);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_SAND;
	}
}
