package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.effects.AbilityCooldownDecrease;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.hexfall.Reincarnation;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPRectPrism;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpellNightmarishCarvings extends SpellBaseAoE {
	private static final String SPELL_NAME = "Nightmarish Carvings (☠)";
	private final Location mCenter;
	private final IntruderBoss.Dialogue mDialogue;
	private List<Player> mPlayers;
	private final IntruderBoss.Narration mNarration;
	private final double mExtraPowerPerCast;
	private final ChargeUpManager mChargeUpManager;

	private final Map<Entity, CarvingType> mCarvings = new HashMap<>();
	private final List<TextDisplay> mTextDisplays = new ArrayList<>();
	private double mExtraPower = 0;

	private int mCastNumber = 0;
	private static final int BUFF_DURATION = 120 * 60 * 20;
	private static final int CHARGE_TIME = 15 * 20;

	public SpellNightmarishCarvings(Plugin plugin, LivingEntity boss, List<Player> players, Location center, IntruderBoss.Dialogue dialogue, IntruderBoss.Narration narration, double extraPowerPerCast) {
		super(plugin, boss, 25, CHARGE_TIME, 16 * 20, true, false, Sound.ITEM_TOTEM_USE, 0.12f, 1);
		mPlayers = List.copyOf(players);
		mCenter = center;
		mDialogue = dialogue;
		mNarration = narration;
		mExtraPowerPerCast = extraPowerPerCast;
		mChargeUpManager = new ChargeUpManager(mLauncher, mDuration, Component.text("Channeling ", NamedTextColor.GOLD).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS, IntruderBoss.DETECTION_RANGE);
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.END_ROD, loc)
			.count(25)
			.delta(mRadius)
			.spawnAsBoss();
	}

	@Override
	protected void chargeCircleAction(Location loc, double radius) {
		new PPCircle(Particle.DUST_COLOR_TRANSITION, mCenter, radius)
			.data(new Particle.DustTransition(Color.fromRGB(0x6b0000), Color.PURPLE, (float) (3 * (1.1 - radius / mRadius))))
			.count(100)
			.spawnAsBoss();
	}

	@Override
	protected void outburstAction(Location loc) {
		new PartialParticle(Particle.FLASH, loc).spawnAsBoss();
	}

	@Override
	protected void circleOutburstAction(Location loc, double radius) {
		new PPCircle(Particle.DUST_COLOR_TRANSITION, mCenter, 30)
			.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0x6b0000), 1.6f))
			.ringMode(false)
			.countPerMeter(1)
			.spawnAsBoss();
		new PartialParticle(Particle.FLASH, mCenter).minimumCount(1).spawnAsBoss();
	}

	@Override
	protected void dealDamageAction(Location loc) {
		mPlayers.forEach(player -> {
			player.playSound(mLauncher.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 0.5f, 0.5f);
			PlayerUtils.killPlayer(player, mLauncher, SPELL_NAME, true, true, true);
		});
		killDisplays();
		super.cancel();
	}

	@Override
	public void run() {
		super.run();
		mPlayers = IntruderBoss.playersInRange(mLauncher.getLocation());
		mChargeUpManager.setTime(0);

		mActiveTasks.add(new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUpManager.nextTick(2)) {
					mCarvings.keySet().forEach(Entity::remove);
					mCarvings.clear();
					mChargeUpManager.remove();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 2));
		EffectManager.getInstance().addEffect(mLauncher, "NightmarishCarvingsResistance", new PercentDamageReceived(15 * 20, -1.0));

		mDialogue.dialogue("ETERNALLY BOUND. IN DREAMS.");
		mCarvings.clear();
		mTextDisplays.clear();

		if (mCastNumber < 2) {
			mCarvings.put(summonCarving(mCenter.clone().add(new Vector(0, -2, 3)),
				"CarvingofWrath", CarvingType.DAMAGE), CarvingType.DAMAGE);
			mCarvings.put(summonCarving(mCenter.clone().add(new Vector(3, -2, -3)),
				"CarvingofFortitude", CarvingType.RESISTANCE), CarvingType.RESISTANCE);
			mCarvings.put(summonCarving(mCenter.clone().add(new Vector(-3, -2, -3)),
				"CarvingofOmen", CarvingType.SPEED), CarvingType.SPEED);

		} else {
			mCarvings.put(summonCarving(mCenter.clone().add(new Vector(0, -2, 3)),
				"CarvingofRestoration", CarvingType.HEALING), CarvingType.HEALING);
			mCarvings.put(summonCarving(mCenter.clone().add(new Vector(3, -2, -3)),
				"CarvingofPerspicacity", CarvingType.ABILITY_COOLDOWN), CarvingType.ABILITY_COOLDOWN);
			mCarvings.put(summonCarving(mCenter.clone().add(new Vector(-3, -2, -3)),
				"CarvingofResilience", CarvingType.REINCARNATION), CarvingType.REINCARNATION);

			mNarration.narration("The Carvings feel different this time.");
			mNarration.narration("Your mind rebells against the <obfuscated>lxxxxxxx</obfuscated>.");
		}
		Component carvingsText = mCarvings.values().stream().flatMap(carvingType -> Stream.of(Component.text("¤", carvingType.getColor())))
			.reduce((textComponent, textComponent2) -> textComponent.append(Component.text(" ")).append(textComponent2))
			.orElseGet(Component::empty);
		mPlayers.forEach(
			player ->
				player.showTitle(Title.title(carvingsText,
					Component.text("ONE. OF THREE.", IntruderBoss.TEXT_COLOR, TextDecoration.BOLD),
					Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(1500)))
				)
		);

		// Prevent accidents
		mCarvings.keySet().forEach(entity -> EffectManager.getInstance().addEffect(entity, "NightmarishCarvings", new PercentDamageReceived(10, -1.0)));

		new PPRectPrism(Particle.FLAME, mCenter.clone().subtract(new Vector(1, 0, 1)), mCenter.clone().add(new Vector(1, 0, 1)))
			.edgeMode(true)
			.countPerMeter(5)
			.extra(0.05)
			.spawnAsBoss();

		new PPRectPrism(Particle.FLAME, mCenter.clone().subtract(new Vector(2, 0, 2)), mCenter.clone().add(new Vector(2, 0, 2)))
			.edgeMode(true)
			.countPerMeter(5)
			.extra(0.05)
			.spawnAsBoss();
	}

	private Entity summonCarving(Location location, String name, CarvingType type) {
		Entity summon = Objects.requireNonNull(LibraryOfSoulsIntegration.summon(location, name));

		TextDisplay text = location.getWorld().spawn(location.clone().add(0, summon.getHeight() + 4, 0), TextDisplay.class);
		mTextDisplays.add(text);
		EntityUtils.setRemoveEntityOnUnload(text);
		text.setAlignment(TextDisplay.TextAlignment.CENTER);
		text.setBillboard(Display.Billboard.CENTER);
		text.setTransformation(new Transformation(
			new Vector3f(),
			new AxisAngle4f(),
			new Vector3f(2, 2, 2),
			new AxisAngle4f()
		));
		text.setBrightness(new Display.Brightness(15, 15));
		text.setBackgroundColor(Color.fromARGB(0x00000000));
		Component coloredName = summon.name().color(type.getColor());
		text.text(coloredName);

		mPlayers.forEach(player -> player.sendMessage(coloredName.hoverEvent(type.getMessage(mExtraPower))));

		return summon;
	}

	public enum CarvingType {
		DAMAGE(true, true,
			extra -> new PercentDamageDealt(BUFF_DURATION, 0.15 + extra),
			extra ->
				MessagingUtils.fromMiniMessage("<color:gray>The carving strengthens both you and the <obfuscated>lxxxxxxx</obfuscated>.</color>\n").append(
					Component.text(String.format("+%d%% Damage.", (int) (15 + extra * 100)), TextColor.color(0xb80217))
				),
			TextColor.color(0xb80217)),
		RESISTANCE(true, true,
			extra -> new PercentDamageReceived(BUFF_DURATION, -0.15 - extra / 2),
			extra ->
				MessagingUtils.fromMiniMessage("<color:gray>The carving fortifies both you and the <obfuscated>lxxxxxxx</obfuscated>.</color>\n").append(
					Component.text(String.format("+%d%% Resistance.", (int) (15 + extra * 50)), TextColor.color(0x7b0fff))
				),
			TextColor.color(0x7b0fff)),
		SPEED(true, true,
			extra -> new PercentSpeed(BUFF_DURATION, 0.15 + extra, "NightmarishCarvings"),
			extra ->
				MessagingUtils.fromMiniMessage("<color:gray>The carving accelerates both you and the <obfuscated>lxxxxxxx</obfuscated>.</color>\n").append(
					Component.text(String.format("+%d%% Speed.", (int) (15 + extra * 100)), TextColor.color(0x0fefff))
				),
			TextColor.color(0x0fefff)),

		// Last one
		HEALING(false, true,
			extra -> new PercentHeal(BUFF_DURATION, 0.25),
			extra ->
				Component.text("The carving revitalizes you.\n", NamedTextColor.GRAY).append(
					Component.text(String.format("+%d%% Healing.", 25), TextColor.color(0xfd245e))
				),
			TextColor.color(0xfd245e)),
		ABILITY_COOLDOWN(false, true,
			extra -> new AbilityCooldownDecrease(BUFF_DURATION, 0.25),
			extra ->
				Component.text("The carving hastens you.\n", NamedTextColor.GRAY).append(
					Component.text(String.format("-%d%% Ability Cooldown.", 25), TextColor.color(0x8bfe3e))
				),
			TextColor.color(0x8bfe3e)),
		REINCARNATION(false, true,
			extra -> new Reincarnation(BUFF_DURATION, 1),
			extra ->
				Component.text("The carving reinforces you.\n", NamedTextColor.GRAY).append(
					Component.text("You've gained Reincarnation against death...", TextColor.color(0xfe9e11))
				),
			TextColor.color(0xfe9e11));

		private final boolean mLauncherGetsBuff;
		private final boolean mPlayerGetsBuff;
		private final Function<Double, Effect> mBuff;
		private final Function<Double, Component> mMessage;
		private final TextColor mColor;

		CarvingType(boolean bossGetsBuff, boolean playerGetsBuff, Function<Double, Effect> buff, Function<Double, Component> message, TextColor color) {
			mLauncherGetsBuff = bossGetsBuff;
			mPlayerGetsBuff = playerGetsBuff;
			mBuff = buff;
			mMessage = message;
			mColor = color;
		}

		public boolean isLauncherGetsBuff() {
			return mLauncherGetsBuff;
		}

		public boolean isPlayerGetsBuff() {
			return mPlayerGetsBuff;
		}

		public Effect getBuff(double add) {
			return mBuff.apply(add);
		}

		public Component getMessage(double add) {
			return mMessage.apply(add);
		}

		public TextColor getColor() {
			return mColor;
		}
	}

	public void addEffect(Effect effect, Entity entity) {
		String name = effect.getDisplayedName();
		if (name == null) {
			name = "Buff" + FastUtils.RANDOM.nextInt();
		}
		EffectManager.getInstance().addEffect(entity, "NightmarishCarvings" + name.replace(" ", ""), effect.displaysTime(false).deleteOnLogout(true));
	}

	public void bossEntityDeathEvent(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (mCarvings.containsKey(entity)) {
			CarvingType chosenCarving = Objects.requireNonNull(mCarvings.get(entity));
			mPlayers.forEach(player -> player.sendMessage(chosenCarving.getMessage(mExtraPower)));

			if (chosenCarving.isPlayerGetsBuff()) {
				mPlayers.forEach(player -> {
					addEffect(chosenCarving.getBuff(mExtraPower), player);
				});
			}
			if (chosenCarving.isLauncherGetsBuff()) {
				addEffect(chosenCarving.getBuff(mExtraPower), mLauncher);
			}

			finishSpell();
		}
	}

	private void finishSpell() {
		EffectManager.getInstance().clearEffects(mLauncher, "NightmarishCarvingsResistance");

		mExtraPower += mExtraPowerPerCast;
		mCastNumber++;

		killDisplays();
		super.cancel();
	}

	public void killDisplays() {
		mCarvings.keySet().forEach(Entity::remove);
		mCarvings.clear();
		mTextDisplays.forEach(Entity::remove);
		mTextDisplays.clear();
		mChargeUpManager.remove();
	}

	@Override
	public void cancel() {
		// Will cause bugs if it casts another spell and CANCELS this one
	}

	@Override
	public int cooldownTicks() {
		return 5 * 20;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
