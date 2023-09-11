package com.playmonumenta.plugins.bosses;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.*;
import com.playmonumenta.plugins.bosses.bosses.abilities.AbilityMarkerEntityBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.DummyDecoyBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.MetalmancyBoss;
import com.playmonumenta.plugins.bosses.bosses.abilities.RestlessSoulsBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeDaggerCraftingBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTargetNPCBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTurretBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.DropShardBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.LavaCannonBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.Samwell;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayBookSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayDemonSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayGolemSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GrayScarabSummoner;
import com.playmonumenta.plugins.bosses.bosses.gray.GraySummoned;
import com.playmonumenta.plugins.bosses.bosses.lich.LichAlchBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichClericBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichConquestBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichCurseBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichDemiseBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichJudgementBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichKeyGlowBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichMageBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichRogueBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichScoutBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichShamanBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichShieldBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichStrifeBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichWarlockBoss;
import com.playmonumenta.plugins.bosses.bosses.lich.LichWarriorBoss;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.chunk.ChunkFullLoadEvent;
import com.playmonumenta.plugins.chunk.ChunkPartialUnloadEvent;
import com.playmonumenta.plugins.delves.mobabilities.DreadfulSummonBoss;
import com.playmonumenta.plugins.delves.mobabilities.SpectralSummonBoss;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.delves.mobabilities.TwistedMiniBoss;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.bosses.GalleryMobRisingBoss;
import com.playmonumenta.plugins.gallery.bosses.GallerySummonMobBoss;
import com.playmonumenta.plugins.parrots.RainbowParrot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation") // we have to use deprecated boss tags here still
public class BossManager implements Listener {

	/********************************************************************************
	 * Classes/Interfaces
	 *******************************************************************************/

	@FunctionalInterface
	public interface StatelessBossConstructor {
		@Nullable BossAbilityGroup construct(Plugin plugin, LivingEntity entity) throws Exception;
	}

	@FunctionalInterface
	public interface StatefulBossConstructor {
		@Nullable BossAbilityGroup construct(Plugin plugin, LivingEntity entity, Location spawnLoc, Location endLoc);
	}

	@FunctionalInterface
	public interface BossDeserializer {
		@Nullable BossAbilityGroup deserialize(Plugin plugin, LivingEntity entity) throws Exception;
	}

	/********************************************************************************
	 * Static Fields
	 *******************************************************************************/

	/*
	 * Holds a static reference to the most recently instantiated boss manager
	 */
	private static @MonotonicNonNull BossManager INSTANCE = null;

	private static final Map<String, StatelessBossConstructor> mStatelessBosses = new HashMap<>();
	private static final Map<String, StatefulBossConstructor> mStatefulBosses = new HashMap<>();
	private static final Map<String, BossDeserializer> mBossDeserializers = new HashMap<>();
	public static final Map<String, BossParameters> mBossParameters = new HashMap<>();

	static {
		/* Stateless bosses are those that have no end location set where a redstone block would be spawned when they die */
		registerStatelessBoss(BulletHellSurvivalBoss.identityTag, BulletHellSurvivalBoss::new, new BulletHellSurvivalBoss.Parameters());
		registerStatelessBoss(BlockLockBoss.identityTag, BlockLockBoss::new, new BlockLockBoss.Parameters());
		registerStatelessBoss(MessageBoss.identityTag, MessageBoss::new, new MessageBoss.Parameters());
		registerStatelessBoss(ImmortalPassengerBoss.identityTag, ImmortalPassengerBoss::new, new ImmortalPassengerBoss.Parameters());
		registerStatelessBoss(ChestLockBoss.identityTag, ChestLockBoss::new, new ChestLockBoss.Parameters());
		registerStatelessBoss(UnyieldingBoss.identityTag, UnyieldingBoss::new, new UnyieldingBoss.Parameters());
		registerStatelessBoss(ToughBoss.identityTag, ToughBoss::new, new ToughBoss.Parameters());
		registerStatelessBoss(UnseenBoss.identityTag, UnseenBoss::new, new UnseenBoss.Parameters());
		registerStatelessBoss(GenericBoss.identityTag, GenericBoss::new, new GenericBoss.Parameters());
		registerStatelessBoss(HiddenBoss.identityTag, HiddenBoss::new);
		registerStatelessBoss(InvisibleBoss.identityTag, InvisibleBoss::new);
		registerStatelessBoss(FireResistantBoss.identityTag, FireResistantBoss::new);
		registerStatelessBoss(HungerCloudBoss.identityTag, HungerCloudBoss::new);
		registerStatelessBoss(BlockBreakBoss.identityTag, BlockBreakBoss::new, new BlockBreakBoss.Parameters());
		registerStatelessBoss(PulseLaserBoss.identityTag, PulseLaserBoss::new, new PulseLaserBoss.Parameters());
		registerStatelessBoss(ArcaneLaserBoss.identityTag, ArcaneLaserBoss::new, new ArcaneLaserBoss.Parameters());
		registerStatelessBoss(WeaponSwitchBoss.identityTag, WeaponSwitchBoss::new);
		registerStatelessBoss(ShieldSwitchBoss.identityTag, ShieldSwitchBoss::new, new ShieldSwitchBoss.Parameters());
		registerStatelessBoss(ChargerBoss.identityTag, ChargerBoss::new, new ChargerBoss.Parameters());
		registerStatelessBoss(BlastResistBoss.identityTag, BlastResistBoss::new);
		registerStatelessBoss(InfestedBoss.identityTag, InfestedBoss::new);
		registerStatelessBoss(FireballBoss.identityTag, FireballBoss::new);
		registerStatelessBoss(TpBehindBoss.identityTag, TpBehindBoss::new, new TpBehindBoss.Parameters());
		registerStatelessBoss(TpBehindTargetedBoss.identityTag, TpBehindTargetedBoss::new);
		registerStatelessBoss(TpSwapBoss.identityTag, TpSwapBoss::new, new TpSwapBoss.Parameters());
		registerStatelessBoss(FlameNovaBoss.identityTag, FlameNovaBoss::new, new FlameNovaBoss.Parameters());
		registerStatelessBoss(PlayerTargetBoss.identityTag, PlayerTargetBoss::new);
		registerStatelessBoss(DamageReducedBoss.identityTag, DamageReducedBoss::new);
		registerStatelessBoss(WinterSnowmanEventBoss.identityTag, WinterSnowmanEventBoss::new);
		registerStatelessBoss(TrainingDummyBoss.identityTag, TrainingDummyBoss::new);
		registerStatelessBoss(FestiveTesseractSnowmanBoss.identityTag, FestiveTesseractSnowmanBoss::new);
		registerStatelessBoss(CrowdControlImmunityBoss.identityTag, CrowdControlImmunityBoss::new);
		registerStatelessBoss(FloatBoss.identityTag, FloatBoss::new);
		registerStatelessBoss(FrostNovaBoss.identityTag, FrostNovaBoss::new, new FrostNovaBoss.Parameters());
		registerStatelessBoss(DebuffHitBoss.identityTag, DebuffHitBoss::new);
		registerStatelessBoss(IceAspectBoss.identityTag, IceAspectBoss::new, new IceAspectBoss.Parameters());
		registerStatelessBoss(TsunamiChargerBoss.identityTag, TsunamiChargerBoss::new);
		registerStatelessBoss(BombTossBoss.identityTag, BombTossBoss::new);
		registerStatelessBoss(BombTossNoBlockBreakBoss.identityTag, BombTossNoBlockBreakBoss::new);
		registerStatelessBoss(RejuvenationBoss.identityTag, RejuvenationBoss::new, new RejuvenationBoss.Parameters());
		registerStatelessBoss(HandSwapBoss.identityTag, HandSwapBoss::new);
		registerStatelessBoss(UnstableBoss.identityTag, UnstableBoss::new, new UnstableBoss.Parameters());
		registerStatelessBoss(BerserkerBoss.identityTag, BerserkerBoss::new);
		registerStatelessBoss(SnowballDamageBoss.identityTag, SnowballDamageBoss::new, new SnowballDamageBoss.Parameters());
		registerStatelessBoss(CorruptInfestedBoss.identityTag, CorruptInfestedBoss::new);
		registerStatelessBoss(FlameLaserBoss.identityTag, FlameLaserBoss::new, new FlameLaserBoss.Parameters());
		registerStatelessBoss(SpecterParticleBoss.identityTag, SpecterParticleBoss::new);
		registerStatelessBoss(DreadnaughtParticleBoss.identityTag, DreadnaughtParticleBoss::new);
		registerStatelessBoss(DreadlingBoss.identityTag, DreadlingBoss::new);
		registerStatelessBoss(ProjectileDeflectionBoss.identityTag, ProjectileDeflectionBoss::new);
		registerStatelessBoss(LivingBladeBoss.identityTag, LivingBladeBoss::new);
		registerStatelessBoss(PrimordialElementalKaulBoss.identityTag, PrimordialElementalKaulBoss::new);
		registerStatelessBoss(ImmortalElementalKaulBoss.identityTag, ImmortalElementalKaulBoss::new);
		registerStatelessBoss(CyanSummonBoss.identityTag, CyanSummonBoss::new);
		registerStatelessBoss(WitherHitBoss.identityTag, WitherHitBoss::new);
		registerStatelessBoss(VolatileBoss.identityTag, VolatileBoss::new);
		registerStatelessBoss(SwapOnDismountBoss.identityTag, SwapOnDismountBoss::new);
		registerStatelessBoss(PlayerDamageOnlyBoss.identityTag, PlayerDamageOnlyBoss::new);
		registerStatelessBoss(GrayDemonSummoner.identityTag, GrayDemonSummoner::new);
		registerStatelessBoss(GrayGolemSummoner.identityTag, GrayGolemSummoner::new);
		registerStatelessBoss(GrayScarabSummoner.identityTag, GrayScarabSummoner::new);
		registerStatelessBoss(GrayBookSummoner.identityTag, GrayBookSummoner::new);
		registerStatelessBoss(GraySummoned.identityTag, GraySummoned::new);
		registerStatelessBoss(IceBreakBoss.identityTag, IceBreakBoss::new);
		registerStatelessBoss(PunchResistBoss.identityTag, PunchResistBoss::new);
		registerStatelessBoss(HalloweenCreeperBoss.identityTag, HalloweenCreeperBoss::new);
		registerStatelessBoss(NoExperienceBoss.identityTag, NoExperienceBoss::new);
		registerStatelessBoss(FocusFireBoss.identityTag, FocusFireBoss::new);
		registerStatelessBoss(ForceBoss.identityTag, ForceBoss::new, new ForceBoss.Parameters());
		registerStatelessBoss(AvengerBoss.identityTag, AvengerBoss::new, new AvengerBoss.Parameters());
		registerStatelessBoss(RageBoss.identityTag, RageBoss::new, new RageBoss.Parameters());
		registerStatelessBoss(EarthshakeBoss.identityTag, EarthshakeBoss::new, new EarthshakeBoss.Parameters());
		registerStatelessBoss(MagicArrowBoss.identityTag, MagicArrowBoss::new, new MagicArrowBoss.Parameters());
		registerStatelessBoss(SeekingProjectileBoss.identityTag, SeekingProjectileBoss::new, new SeekingProjectileBoss.Parameters());
		registerStatelessBoss(TrackingProjectileBoss.identityTag, TrackingProjectileBoss::new);
		registerStatelessBoss(WrathBoss.identityTag, WrathBoss::new, new WrathBoss.Parameters());
		registerStatelessBoss(LeapBoss.identityTag, LeapBoss::new);
		registerStatelessBoss(BarrierBoss.identityTag, BarrierBoss::new, new BarrierBoss.Parameters());
		registerStatelessBoss(CrowdControlResistanceBoss.identityTag, CrowdControlResistanceBoss::new, new CrowdControlResistanceBoss.Parameters());
		registerStatelessBoss(MeteorSlamBoss.identityTag, MeteorSlamBoss::new, new MeteorSlamBoss.Parameters());
		registerStatelessBoss(SwingBoss.identityTag, SwingBoss::new, new SwingBoss.Parameters());
		registerStatelessBoss(MistMob.identityTag, MistMob::new);
		registerStatelessBoss(HookBoss.identityTag, HookBoss::new, new HookBoss.Parameters());
		registerStatelessBoss(FrostGiantIcicle.identityTag, FrostGiantIcicle::new);
		registerStatelessBoss(SpellSlingerBoss.identityTag, SpellSlingerBoss::new);
		registerStatelessBoss(VindictiveBoss.identityTag, VindictiveBoss::new);
		registerStatelessBoss(ShadowTrailBoss.identityTag, ShadowTrailBoss::new);
		registerStatelessBoss(KineticProjectileBoss.identityTag, KineticProjectileBoss::new);
		registerStatelessBoss(FlameTrailBoss.identityTag, FlameTrailBoss::new, new FlameTrailBoss.Parameters());
		registerStatelessBoss(ShadeParticleBoss.identityTag, ShadeParticleBoss::new);
		registerStatelessBoss(FireBombTossBoss.identityTag, FireBombTossBoss::new, new FireBombTossBoss.Parameters());
		registerStatelessBoss(CommanderBoss.identityTag, CommanderBoss::new, new CommanderBoss.Parameters());
		registerStatelessBoss(ShadePossessedBoss.identityTag, ShadePossessedBoss::new);
		registerStatelessBoss(TwistedDespairBoss.identityTag, TwistedDespairBoss::new);
		registerStatelessBoss(CoordinatedAttackBoss.identityTag, CoordinatedAttackBoss::new);
		registerStatelessBoss(AbilitySilenceBoss.identityTag, AbilitySilenceBoss::new, new AbilitySilenceBoss.Parameters());
		registerStatelessBoss(ShiftingBoss.identityTag, ShiftingBoss::new);
		registerStatelessBoss(BulletHellBoss.identityTag, BulletHellBoss::new, new BulletHellBoss.Parameters());
		registerStatelessBoss(CarapaceBoss.identityTag, CarapaceBoss::new);
		registerStatelessBoss(KamikazeBoss.identityTag, KamikazeBoss::new);
		registerStatelessBoss(TinyBombTossBoss.identityTag, TinyBombTossBoss::new);
		registerStatelessBoss(AntiRangeBoss.identityTag, AntiRangeBoss::new, new AntiRangeBoss.Parameters());
		registerStatelessBoss(AntiMeleeBoss.identityTag, AntiMeleeBoss::new, new AntiMeleeBoss.Parameters());
		registerStatelessBoss(AntiSuffocationBoss.identityTag, AntiSuffocationBoss::new);
		registerStatelessBoss(DamageCapBoss.identityTag, DamageCapBoss::new, new DamageCapBoss.Parameters());
		registerStatelessBoss(ImmortalMountBoss.identityTag, ImmortalMountBoss::new, new ImmortalMountBoss.Parameters());
		registerStatelessBoss(SilenceOnHitBoss.identityTag, SilenceOnHitBoss::new);
		registerStatelessBoss(FalseSpiritPortal.identityTag, FalseSpiritPortal::new);
		registerStatelessBoss(TffBookSummonBoss.identityTag, TffBookSummonBoss::new);
		registerStatelessBoss(ArcaneProjectileBoss.identityTag, ArcaneProjectileBoss::new, new ArcaneProjectileBoss.Parameters());
		registerStatelessBoss(JumpBoss.identityTag, JumpBoss::new, new JumpBoss.Parameters());
		registerStatelessBoss(RebornBoss.identityTag, RebornBoss::new, new RebornBoss.Parameters());
		registerStatelessBoss(NoFireBoss.identityTag, NoFireBoss::new);
		registerStatelessBoss(DistanceCloserBoss.identityTag, DistanceCloserBoss::new);
		registerStatelessBoss(WeakHookBoss.identityTag, WeakHookBoss::new, new WeakHookBoss.Parameters());
		registerStatelessBoss(AuraEffectBoss.identityTag, AuraEffectBoss::new, new AuraEffectBoss.Parameters());
		registerStatelessBoss(DummyDecoyBoss.identityTag, DummyDecoyBoss::new);
		registerStatelessBoss(LaserBoss.identityTag, LaserBoss::new, new LaserBoss.Parameters());
		registerStatelessBoss(SpinBoss.identityTag, SpinBoss::new, new SpinBoss.Parameters());
		registerStatelessBoss(OnHitBoss.identityTag, OnHitBoss::new, new OnHitBoss.Parameters());
		registerStatelessBoss(NovaBoss.identityTag, NovaBoss::new, new NovaBoss.Parameters());
		registerStatelessBoss(ProjectileBoss.identityTag, ProjectileBoss::new, new ProjectileBoss.Parameters());
		registerStatelessBoss(RainbowParrot.identityTag, RainbowParrot::new);
		registerStatelessBoss(SpawnMobsBoss.identityTag, SpawnMobsBoss::new, new SpawnMobsBoss.Parameters());
		registerStatelessBoss(LandSlowBoss.identityTag, LandSlowBoss::new, new LandSlowBoss.Parameters());
		registerStatelessBoss(PounceBoss.identityTag, PounceBoss::new, new PounceBoss.Parameters());
		registerStatelessBoss(NoAbilityDamageBoss.identityTag, NoAbilityDamageBoss::new);
		registerStatelessBoss(NoGlowingBoss.identityTag, NoGlowingBoss::new);
		registerStatelessBoss(GenericTargetBoss.identityTag, GenericTargetBoss::new, new GenericTargetBoss.Parameters());
		registerStatelessBoss(MobRisingBoss.identityTag, MobRisingBoss::new, new MobRisingBoss.Parameters());
		registerStatelessBoss(GrenadeLauncherBoss.identityTag, GrenadeLauncherBoss::new, new GrenadeLauncherBoss.Parameters());
		registerStatelessBoss(SizeChangerBoss.identityTag, SizeChangerBoss::new, new SizeChangerBoss.Parameters());
		registerStatelessBoss(DelveScalingBoss.identityTag, DelveScalingBoss::new, new DelveScalingBoss.Parameters());
		registerStatelessBoss(DeathSummonBoss.identityTag, DeathSummonBoss::new, new DeathSummonBoss.Parameters());
		registerStatelessBoss(SummonOnExplosionBoss.identityTag, SummonOnExplosionBoss::new, new SummonOnExplosionBoss.Parameters());
		registerStatelessBoss(HostileBoss.identityTag, HostileBoss::new, new HostileBoss.Parameters());
		registerStatelessBoss(StarfallBoss.identityTag, StarfallBoss::new, new StarfallBoss.Parameters());
		registerStatelessBoss(ShatterBoss.identityTag, ShatterBoss::new, new ShatterBoss.Parameters());
		registerStatelessBoss(StatMultiplierBoss.identityTag, StatMultiplierBoss::new, new StatMultiplierBoss.Parameters());
		registerStatelessBoss(SpectralSummonBoss.identityTag, SpectralSummonBoss::new, new SpectralSummonBoss.Parameters());
		registerStatelessBoss(DreadfulSummonBoss.identityTag, DreadfulSummonBoss::new, new DreadfulSummonBoss.Parameters());
		registerStatelessBoss(FriendlyBoss.identityTag, FriendlyBoss::new, new FriendlyBoss.Parameters());
		registerStatelessBoss(MageCosmicMoonbladeBoss.identityTag, MageCosmicMoonbladeBoss::new, new MageCosmicMoonbladeBoss.Parameters());
		registerStatelessBoss(WarriorShieldWallBoss.identityTag, WarriorShieldWallBoss::new, new WarriorShieldWallBoss.Parameters());
		registerStatelessBoss(DodgeBoss.identityTag, DodgeBoss::new, new DodgeBoss.Parameters());
		registerStatelessBoss(BlockPlacerBoss.identityTag, BlockPlacerBoss::new);
		registerStatelessBoss(ScoutVolleyBoss.identityTag, ScoutVolleyBoss::new, new ScoutVolleyBoss.Parameters());
		registerStatelessBoss(WarlockAmpHexBoss.identityTag, WarlockAmpHexBoss::new, new WarlockAmpHexBoss.Parameters());
		registerStatelessBoss(LimitedLifespanBoss.identityTag, LimitedLifespanBoss::new, new LimitedLifespanBoss.Parameters());
		registerStatelessBoss(BlueFireBoss.identityTag, BlueFireBoss::new);
		registerStatelessBoss(BlueEarthBoss.identityTag, BlueEarthBoss::new);
		registerStatelessBoss(BlueAirBoss.identityTag, BlueAirBoss::new);
		registerStatelessBoss(BlueWaterBoss.identityTag, BlueWaterBoss::new);
		registerStatelessBoss(DropShardBoss.identityTag, DropShardBoss::new);
		registerStatelessBoss(BlueStrikeDaggerCraftingBoss.identityTag, BlueStrikeDaggerCraftingBoss::construct);
		registerStatelessBoss(BlueStrikeTargetNPCBoss.identityTag, BlueStrikeTargetNPCBoss::new);
		registerStatelessBoss(BlueStrikeTurretBoss.identityTag, BlueStrikeTurretBoss::new);
		registerStatelessBoss(LavaCannonBoss.identityTag, LavaCannonBoss::construct);
		registerStatelessBoss(PhasesManagerBoss.identityTag, PhasesManagerBoss::new, new PhasesManagerBoss.Parameters());
		registerStatelessBoss(SoundBoss.identityTag, SoundBoss::new, new SoundBoss.Parameters());
		registerStatelessBoss(MusicBoss.identityTag, MusicBoss::new, new MusicBoss.Parameters());
		registerStatelessBoss(RedstoneBoss.identityTag, RedstoneBoss::new, new RedstoneBoss.Parameters());
		registerStatelessBoss(SlashAttackBoss.identityTag, SlashAttackBoss::new, new SlashAttackBoss.Parameters());
		registerStatelessBoss(DashBoss.identityTag, DashBoss::new, new DashBoss.Parameters());
		registerStatelessBoss(WormBoss.identityTag, WormBoss::new, new WormBoss.Parameters());
		registerStatelessBoss(ResistanceBoss.identityTag, ResistanceBoss::new, new ResistanceBoss.Parameters());
		registerStatelessBoss(LichMageBoss.identityTag, LichMageBoss::new);
		registerStatelessBoss(LichRogueBoss.identityTag, LichRogueBoss::new);
		registerStatelessBoss(LichClericBoss.identityTag, LichClericBoss::new);
		registerStatelessBoss(LichWarlockBoss.identityTag, LichWarlockBoss::new);
		registerStatelessBoss(LichAlchBoss.identityTag, LichAlchBoss::new);
		registerStatelessBoss(LichScoutBoss.identityTag, LichScoutBoss::new);
		registerStatelessBoss(LichWarriorBoss.identityTag, LichWarriorBoss::new);
		registerStatelessBoss(LichShamanBoss.identityTag, LichShamanBoss::new);
		registerStatelessBoss(LichConquestBoss.identityTag, LichConquestBoss::new);
		registerStatelessBoss(LichDemiseBoss.identityTag, LichDemiseBoss::new);
		registerStatelessBoss(LichJudgementBoss.identityTag, LichJudgementBoss::new);
		registerStatelessBoss(LichStrifeBoss.identityTag, LichStrifeBoss::new);
		registerStatelessBoss(LichCurseBoss.identityTag, LichCurseBoss::new);
		registerStatelessBoss(LichShieldBoss.identityTag, LichShieldBoss::new);
		registerStatelessBoss(LichKeyGlowBoss.identityTag, LichKeyGlowBoss::new);
		registerStatelessBoss(FestiveTessUpgradeSnowmenBoss.identityTag, FestiveTessUpgradeSnowmenBoss::new, new FestiveTessUpgradeSnowmenBoss.Parameters());
		registerStatelessBoss(MetalmancyBoss.identityTag, MetalmancyBoss::new);
		registerStatelessBoss(RestlessSoulsBoss.identityTag, RestlessSoulsBoss::new);
		registerStatelessBoss(AlchemicalAberrationBoss.identityTag, AlchemicalAberrationBoss::new);
		registerStatelessBoss(AbilityMarkerEntityBoss.identityTag, AbilityMarkerEntityBoss::new);
		registerStatelessBoss(ThrowSummonBoss.identityTag, ThrowSummonBoss::new, new ThrowSummonBoss.Parameters());
		registerStatelessBoss(PotionThrowBoss.identityTag, PotionThrowBoss::new, new PotionThrowBoss.Parameters());
		registerStatelessBoss(TwistedMiniBoss.identityTag, TwistedMiniBoss::new);
		registerStatelessBoss(BrownPositiveBoss.identityTag, BrownPositiveBoss::new, new BrownPositiveBoss.Parameters());
		registerStatelessBoss(BrownNegativeBoss.identityTag, BrownNegativeBoss::new, new BrownNegativeBoss.Parameters());
		registerStatelessBoss(BrownMagnetSwapBoss.identityTag, BrownMagnetSwapBoss::new, new BrownMagnetSwapBoss.Parameters());
		registerStatelessBoss(ParadoxSwapBoss.identityTag, ParadoxSwapBoss::new);
		registerStatelessBoss(TemporalShieldBoss.identityTag, TemporalShieldBoss::new);
		registerStatelessBoss(GalleryMobRisingBoss.identityTag, GalleryMobRisingBoss::new, new GalleryMobRisingBoss.Parameters());
		registerStatelessBoss(GallerySummonMobBoss.identityTag, GallerySummonMobBoss::new, new GallerySummonMobBoss.Parameters());
		registerStatelessBoss(TagScalingBoss.identityTag, TagScalingBoss::new, new TagScalingBoss.Parameters());
		registerStatelessBoss(CancelDamageBoss.identityTag, CancelDamageBoss::new);
		registerStatelessBoss(ParticleRingBoss.identityTag, ParticleRingBoss::new, new ParticleRingBoss.Parameters());
		registerStatelessBoss(ShieldStunBoss.identityTag, ShieldStunBoss::new, new ShieldStunBoss.Parameters());
		registerStatelessBoss(RiftBoss.identityTag, RiftBoss::new, new RiftBoss.Parameters());
		registerStatelessBoss(LacerateBoss.identityTag, LacerateBoss::new, new LacerateBoss.Parameters());
		registerStatelessBoss(DamageTransferBoss.identityTag, DamageTransferBoss::new, new DamageTransferBoss.Parameters());
		registerStatelessBoss(ShockwaveBoss.identityTag, ShockwaveBoss::new, new ShockwaveBoss.Parameters());
		registerStatelessBoss(OmenBoss.identityTag, OmenBoss::new, new OmenBoss.Parameters());
		registerStatelessBoss(FlareBoss.identityTag, FlareBoss::new, new FlareBoss.Parameters());

		/* Stateful bosses have a remembered spawn location and end location where a redstone block is set when they die */
		registerStatefulBoss(CAxtal.identityTag, CAxtal::new);
		registerStatefulBoss(Masked.identityTag, Masked::new);
		registerStatefulBoss(Virius.identityTag, Virius::new);
		registerStatefulBoss(Orangyboi.identityTag, Orangyboi::new);
		registerStatefulBoss(Azacor.identityTag, Azacor::new);
		registerStatefulBoss(CShura.identityTag, CShura::new);
		registerStatefulBoss(SwordsageRichter.identityTag, SwordsageRichter::new);
		registerStatefulBoss(Kaul.identityTag, Kaul::new);
		registerStatefulBoss(TCalin.identityTag, TCalin::new);
		registerStatefulBoss(CrownbearerBoss.identityTag, CrownbearerBoss::new);
		registerStatefulBoss(RabbitGodBoss.identityTag, RabbitGodBoss::new);
		registerStatefulBoss(OldLabsBoss.identityTag, OldLabsBoss::new);
		registerStatefulBoss(HeadlessHorsemanBoss.identityTag, HeadlessHorsemanBoss::new);
		registerStatefulBoss(Varcosa.identityTag, Varcosa::new);
		registerStatefulBoss(FrostGiant.identityTag, FrostGiant::new);
		registerStatefulBoss(TealQuestBoss.identityTag, TealQuestBoss::new);
		registerStatefulBoss(VarcosaSummonerBoss.identityTag, VarcosaSummonerBoss::new);
		registerStatefulBoss(VarcosasLastBreathBoss.identityTag, VarcosasLastBreathBoss::new);
		registerStatefulBoss(VarcosaLingeringWillBoss.identityTag, VarcosaLingeringWillBoss::new);
		registerStatefulBoss(MimicQueen.identityTag, MimicQueen::new);
		registerStatefulBoss(FalseSpirit.identityTag, FalseSpirit::new);
		registerStatefulBoss(SnowSpirit.identityTag, SnowSpirit::new);
		registerStatefulBoss(Lich.identityTag, Lich::new);
		registerStatefulBoss(Hedera.identityTag, Hedera::new);
		registerStatefulBoss(Davey.identityTag, Davey::new);
		registerStatefulBoss(Nucleus.identityTag, Nucleus::new);
		registerStatefulBoss(Ghalkor.identityTag, Ghalkor::new);
		registerStatefulBoss(Svalgot.identityTag, Svalgot::new);
		registerStatefulBoss(BeastOfTheBlackFlame.identityTag, BeastOfTheBlackFlame::new);
		registerStatefulBoss(RKitxet.identityTag, RKitxet::new);
		registerStatefulBoss(VerdantMinibossBoss.identityTag, VerdantMinibossBoss::new);
		registerStatefulBoss(PortalBoss.identityTag, PortalBoss::new);
		registerStatefulBoss(ImperialConstruct.identityTag, ImperialConstruct::new);
		registerStatefulBoss(Samwell.identityTag, Samwell::new);
		registerStatefulBoss(TealSpirit.identityTag, TealSpirit::new);

	}

	private static void registerStatelessBoss(String identityTag, StatelessBossConstructor constructor) {
		registerStatelessBoss(identityTag, constructor, constructor::construct);
	}

	private static void registerStatelessBoss(String identityTag, StatelessBossConstructor constructor, BossDeserializer deserializer) {
		registerStatelessBoss(identityTag, constructor, deserializer, null);
	}

	private static void registerStatelessBoss(String identityTag, StatelessBossConstructor constructor, @Nullable BossParameters parameters) {
		registerStatelessBoss(identityTag, constructor, constructor::construct, parameters);
	}

	private static void registerStatelessBoss(String identityTag, StatelessBossConstructor constructor, BossDeserializer deserializer, @Nullable BossParameters parameters) {
		mStatelessBosses.put(identityTag, constructor);
		mBossDeserializers.put(identityTag, deserializer);
		if (parameters != null) {
			mBossParameters.put(identityTag, parameters);
		}
	}

	private static void registerStatefulBoss(String identityTag, StatefulBossConstructor constructor) {
		registerStatefulBoss(identityTag, constructor, (plugin, boss) -> SerializationUtils.statefulBossDeserializer(boss, identityTag, (startLoc, endLoc) -> constructor.construct(plugin, boss, startLoc, endLoc)));
	}

	private static void registerStatefulBoss(String identityTag, StatefulBossConstructor constructor, BossDeserializer deserializer) {
		mStatefulBosses.put(identityTag, constructor);
		mBossDeserializers.put(identityTag, deserializer);
	}

	/********************************************************************************
	 * Member Variables
	 *******************************************************************************/
	private final Plugin mPlugin;
	private final Map<UUID, Boss> mBosses;
	private boolean mNearbyEntityDeathEnabled = false;
	private boolean mNearbyBlockBreakEnabled = false;
	private boolean mNearbyPlayerDeathEnabled = false;

	public BossManager(Plugin plugin) {
		INSTANCE = this;
		mPlugin = plugin;
		mBosses = new HashMap<>();

		/* When starting up, look for bosses in all current world entities */
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (!(entity instanceof LivingEntity)) {
					continue;
				}

				processEntity((LivingEntity) entity);
			}
		}

	}

	/********************************************************************************
	 * Event Handlers
	 *******************************************************************************/

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity living
			    && !living.getScoreboardTags().isEmpty()) {
			// EntityAddToWorldEvent is called at an inconvenient time in Minecraft's code, which can cause deadlocks,
			// so we delay initialisation of boss data slightly to be outside the entity loading code.
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (living.isValid()) {
					processEntity(living);
				}
			});
		}
	}

	// Creature spawn event is also listened to in order to set up boss data for initial spawn at the moment the mob is summoned,
	// which allows to immediately use the boss after summoning it.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void creatureSpawnEvent(CreatureSpawnEvent event) {
		processEntity(event.getEntity());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
		if (event.getEntity() instanceof LivingEntity living) {
			unload(living, false);
		}
	}

	// Some entities seem to persist despite their chunk being unloaded,
	// so we manually load/unload their boss data when the chunk is loaded/unloaded.

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkFullLoadEvent(ChunkFullLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof LivingEntity living) {
				processEntity(living);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkPartialUnloadEvent(ChunkPartialUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof LivingEntity living) {
				unload(living, false);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();

		if (mNearbyEntityDeathEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(entity.getLocation(), 12.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyEntityDeath(event);
				}
			}
		}

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.death(event);
			if (entity.getHealth() <= 0) {
				unload(boss, false);
				mBosses.remove(entity.getUniqueId());

				/*
				 * Remove special serialization data from drops. Should not be
				 * necessary since loaded bosses already have this data stripped
				 */
				SerializationUtils.stripSerializationDataFromDrops(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (mNearbyBlockBreakEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(event.getBlock().getLocation(), 62.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyBlockBreak(event);
				}
			}
		}
	}

	// EntityExplodeEvent is used by various spells like SpellBlockBreak to conditionally destroy blocks.
	// We don't want those to trigger death events though. So we listen to the ExplosionPrimeEvent event
	// which precedes every real creeper explosion and only handle explosions after a prime event.
	// Handling the death in the prime event itself doesn't quite work, as the creeper is still alive and
	// will explode after the event, damaging any mobs the death handler spawned for example.
	// Delaying by a tick doesn't work either, as the creeper will be discarded just after the explosion.
	private @Nullable Creeper mLastPrimedCreeper = null;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void explosionPrimeEvent(ExplosionPrimeEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Creeper creeper) {
			mLastPrimedCreeper = creeper;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (entity == mLastPrimedCreeper) {
			Boss boss = mBosses.remove(entity.getUniqueId());
			if (boss != null) {
				boss.death(null);
				unload(boss, false);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		if (shooter instanceof LivingEntity) {
			Boss boss = mBosses.get(((LivingEntity) shooter).getUniqueId());
			if (boss != null) {
				// May cancel the event
				boss.bossLaunchedProjectile(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		if (shooter instanceof LivingEntity livingShooter) {
			Boss boss = mBosses.get(livingShooter.getUniqueId());
			if (boss != null) {
				boss.bossProjectileHit(event);
			}
		}
		if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity hit) {
			Boss boss = mBosses.get(hit.getUniqueId());
			if (boss != null) {
				boss.bossHitByProjectile(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void areaEffectCloudApplyEvent(AreaEffectCloudApplyEvent event) {
		// Make a copy, so it can be iterated while bosses modify the actual list
		for (LivingEntity entity : new ArrayList<>(event.getAffectedEntities())) {
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.areaEffectAppliedToBoss(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void potionSplashEvent(PotionSplashEvent event) {
		// Make a copy, so it can be iterated while bosses modify the actual list
		for (LivingEntity entity : new ArrayList<>(event.getAffectedEntities())) {
			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.splashPotionAppliedToBoss(event);
			}
		}

		if (event.getEntity().getShooter() instanceof LivingEntity le) {
			Boss boss = mBosses.get(le.getUniqueId());
			if (boss != null) {
				boss.bossSplashPotion(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());
		if (boss != null) {
			boss.entityPotionEffectEvent(event);
		}
	}

	@SuppressWarnings("unchecked")
	public @Nullable <T extends BossAbilityGroup> T getBoss(Entity entity, Class<T> cls) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			for (BossAbilityGroup ability : boss.getAbilities()) {
				if (cls.isInstance(ability)) {
					return (T) ability;
				}
			}
		}

		return null;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHurt(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		Boss boss = mBosses.get(damagee.getUniqueId());
		if (boss != null) {
			// May cancel the event
			boss.onHurt(event);
			if (damager != null) {
				boss.onHurtByEntity(event, damager);
				if (source != null) {
					boss.onHurtByEntityWithSource(event, damager, source);
				}
			}
			boss.setLastHitBy(source);
		}

		Entity vehicle = damagee.getVehicle();
		if (vehicle instanceof LivingEntity mount) {
			Boss mountBoss = mBosses.get(mount.getUniqueId());
			if (mountBoss != null) {
				mountBoss.onPassengerHurt(event);
			}
		}
	}

	// Must be before player items and abilities, which use priority HIGH
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onDamage(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		LivingEntity source = event.getSource();
		if (source != null) {
			Boss boss = mBosses.get(source.getUniqueId());
			if (boss != null) {
				// May cancel the event
				boss.onDamage(event, damagee);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void spellCastEvent(SpellCastEvent event) {
		LivingEntity boss = event.getBoss();
		Boss b = mBosses.get(boss.getUniqueId());
		if (b != null && !EntityUtils.isSilenced(boss)) {
			b.bossCastAbility(event);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void entityPathfindEvent(EntityPathfindEvent event) {
		if (event.getEntity() instanceof Mob entity) {

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.bossPathfind(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityTargetEvent(EntityTargetEvent event) {
		if (event.getEntity() instanceof Mob entity) {

			Boss boss = mBosses.get(entity.getUniqueId());
			if (boss != null) {
				boss.bossChangedTarget(event);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void slimeSplitEvent(SlimeSplitEvent event) {
		if (event.getEntity().getScoreboardTags().contains("boss_nosplit")) {
			/*
			 * This annoying special-case boss won't work with the normal system because
			 * the Boss that needs to be matched unloads before this event gets called
			 */
			event.setCancelled(true);
		}
	}

	/* Not actually an event handler - must be called by EntityUtils applyStun() */
	/* TODO: Probably make this an actual event? */
	public void entityStunned(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossStunned();
		}
	}

	/* Not actually an event handler - must be called by EntityUtils applySilence() */
	/* TODO: Probably make this an actual event? */
	public void entitySilenced(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossSilenced();
		}
	}

	public void entityKnockedAway(Entity entity, float speed) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.bossKnockedAway(speed);
		}
	}

	/* Kind of a weird one - not hooked to bosses but used for snowman killer */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (player.hasMetadata(WinterSnowmanEventBoss.deathMetakey)) {
			Entity snowman = Bukkit.getEntity(UUID.fromString(player.getMetadata(WinterSnowmanEventBoss.deathMetakey).get(0).asString()));
			Component snowmanName = snowman != null ? snowman.customName() : null;
			Component deathMessage = Component.text("")
				                         .append(Component.selector(player.getName()))
				                         .append(Component.text(" was snowballed by "))
				                         .append(snowmanName != null ? snowmanName : Component.text("a snowman"));
			event.deathMessage(deathMessage);
			player.removeMetadata(WinterSnowmanEventBoss.deathMetakey, mPlugin);
		}

		if (mNearbyPlayerDeathEnabled) {
			/* For performance reasons this check is only enabled when there is a loaded
			 * boss that is using this feature
			 */
			for (LivingEntity m : EntityUtils.getNearbyMobs(player.getLocation(), 75.0)) {
				Boss boss = mBosses.get(m.getUniqueId());
				if (boss != null) {
					boss.nearbyPlayerDeath(event);
				}
			}
		}
	}

	/* Another weird one - used for exorcism potion */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void lingeringPotionSplashEvent(LingeringPotionSplashEvent event) {
		if (event.getEntityType() == EntityType.SPLASH_POTION) {
			ThrownPotion potEntity = event.getEntity();
			ItemStack potItem = potEntity.getItem();
			if (potItem.getType() == Material.LINGERING_POTION) {
				if (InventoryUtils.testForItemWithLore(potItem, "Exorcism")) {
					AreaEffectCloud cloud = event.getAreaEffectCloud();
					cloud.setMetadata("MonumentaBossesGrayExorcism", new FixedMetadataValue(mPlugin, 1));
					cloud.setRadius(6.5f);
					cloud.setDurationOnUse(0);
					cloud.setRadiusOnUse(0);
					cloud.setRadiusPerTick(-0.004f);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerLeashEntityEvent(PlayerLeashEntityEvent event) {
		if (mBosses.get(event.getEntity().getUniqueId()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());

		if (boss != null) {
			boss.customEffectAppliedToBoss(event);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void bossExploded(EntityExplodeEvent event) {
		Boss boss = mBosses.get(event.getEntity().getUniqueId());

		if (boss != null) {
			boss.bossExploded(event);
		}
	}

	// Only acts on fire applied by the plugin, called in EntityUtils
	public void bossIgnited(Entity entity, int ticks) {
		Boss boss = mBosses.get(entity.getUniqueId());

		if (boss != null) {
			boss.bossIgnited(ticks);
		}
	}

	/********************************************************************************
	 * Static public methods
	 *******************************************************************************/
	public static BossManager getInstance() {
		if (INSTANCE == null) {
			throw new RuntimeException("BossManager used before it was loaded");
		}
		return INSTANCE;
	}

	/********************************************************************************
	 * Public Methods
	 *******************************************************************************/

	public List<BossAbilityGroup> getAbilities(Entity entity) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			return boss.getAbilities();
		}
		return Collections.emptyList();
	}

	/*
	 * Every way to unload a boss needs to bounce through this function to ensure
	 * state is updated correctly!
	 */
	public void unload(Boss boss, boolean shuttingDown) {
		if (!shuttingDown) {
			checkDisablePerformanceEvents(boss);
		}
		boss.unload(shuttingDown);
	}

	public void unload(LivingEntity entity, boolean shuttingDown) {
		Boss boss = mBosses.remove(entity.getUniqueId());
		if (boss != null) {
			unload(boss, shuttingDown);
		}
	}

	public void unloadAll(boolean shuttingDown) {
		for (Map.Entry<UUID, Boss> entry : mBosses.entrySet()) {
			unload(entry.getValue(), shuttingDown);
		}
		mBosses.clear();
	}

	public void removeAbility(LivingEntity entity, String identityTag) {
		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss != null) {
			boss.removeAbility(identityTag);
		}
	}

	public static void createBoss(@Nullable CommandSender sender, LivingEntity targetEntity, String requestedTag) throws Exception {
		if (INSTANCE == null) {
			throw new Exception("BossManager is not loaded");
		}

		StatelessBossConstructor stateless = mStatelessBosses.get(requestedTag);
		if (stateless != null) {
			BossAbilityGroup bossAbilityGroup = stateless.construct(INSTANCE.mPlugin, targetEntity);
			if (bossAbilityGroup == null) {
				if (sender != null) {
					sender.sendMessage("Could not give '" + requestedTag + "' to mob!");
				}
				return;
			}
			INSTANCE.createBossInternal(targetEntity, bossAbilityGroup);
			if (sender != null) {
				sender.sendMessage("Successfully gave '" + requestedTag + "' to mob");
			}
		} else {
			if (mStatefulBosses.get(requestedTag) != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.GOLD + "There is a boss with the tag '" +
						                   ChatColor.GREEN + requestedTag + ChatColor.GOLD +
						                   "' but it requires positional arguments");
					sender.sendMessage(Component.text("Try again with some ending location coordinates", NamedTextColor.GOLD));
				}
			} else {
				if (sender != null) {
					sender.sendMessage(Component.text("No boss found with the tag '" + requestedTag + "'", NamedTextColor.RED));
				}
			}
		}
	}

	public static void createBoss(@Nullable CommandSender sender, LivingEntity targetEntity, String requestedTag, Location endLoc) throws Exception {
		if (INSTANCE == null) {
			throw new Exception("BossManager is not loaded");
		}

		StatefulBossConstructor stateful = mStatefulBosses.get(requestedTag);
		if (stateful != null) {
			BossAbilityGroup bossAbilityGroup = stateful.construct(INSTANCE.mPlugin, targetEntity, targetEntity.getLocation(), endLoc);
			if (bossAbilityGroup == null) {
				if (sender != null) {
					sender.sendMessage("Could not give '" + requestedTag + "' to mob!");
				}
				return;
			}
			INSTANCE.createBossInternal(targetEntity, bossAbilityGroup);
			if (sender != null) {
				sender.sendMessage("Successfully gave '" + requestedTag + "' to mob");
			}
		} else {
			if (mStatelessBosses.get(requestedTag) != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.GOLD + "There is a boss with the tag '" +
						                   ChatColor.GREEN + requestedTag + ChatColor.GOLD +
						                   "' but it does not take positional arguments");
					sender.sendMessage(Component.text("Try again without the coordinates", NamedTextColor.GOLD));
				}
			} else {
				if (sender != null) {
					sender.sendMessage(Component.text("No boss found with the tag '" + requestedTag + "'", NamedTextColor.RED));
				}
			}
		}
	}

	/* Machine-readable list */
	public String[] listBosses() {
		Set<String> allBossTags = new HashSet<>(mStatelessBosses.keySet());
		allBossTags.addAll(mStatefulBosses.keySet());
		return allBossTags.toArray(String[]::new);
	}

	/* Machine-readable list */
	public String[] listStatelessBosses() {
		return mStatelessBosses.keySet().toArray(String[]::new);
	}

	/********************************************************************************
	 * Private Methods
	 *******************************************************************************/

	/*
	 * This function should be called whenever an ability is loaded to enable events
	 * that are more performance sensitive (if applicable)
	 */
	private void checkEnablePerformanceEvents(BossAbilityGroup ability) {
		if (ability.hasNearbyEntityDeathTrigger()) {
			mNearbyEntityDeathEnabled = true;
		}

		if (ability.hasNearbyBlockBreakTrigger()) {
			mNearbyBlockBreakEnabled = true;
		}

		if (ability.hasNearbyPlayerDeathTrigger()) {
			mNearbyPlayerDeathEnabled = true;
		}
	}

	/*
	 * This function should be called whenever a boss is unloaded to disable events
	 * that are more performance sensitive (if applicable)
	 */
	private void checkDisablePerformanceEvents(Boss boss) {
		if (boss.hasNearbyEntityDeathTrigger()) {
			if (!mNearbyEntityDeathEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyEntityDeathTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyEntityDeathTrigger)) {
				mNearbyEntityDeathEnabled = false;
			}
		}

		if (boss.hasNearbyBlockBreakTrigger()) {
			if (!mNearbyBlockBreakEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyBlockBreakTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyBlockBreakTrigger)) {
				mNearbyBlockBreakEnabled = false;
			}
		}

		if (boss.hasNearbyPlayerDeathTrigger()) {
			if (!mNearbyBlockBreakEnabled) {
				mPlugin.getLogger().log(Level.WARNING, "Unloaded Boss with hasNearbyPlayerDeathTrigger but feature was not enabled. Definitely a bug!");
			}

			/*
			 * This boss was at least contributing to keeping this feature enabled
			 *
			 * Need to check all other loaded bosses to see if it still needs to be enabled
			 */
			if (mBosses.values().stream().noneMatch(Boss::hasNearbyPlayerDeathTrigger)) {
				mNearbyPlayerDeathEnabled = false;
			}
		}
	}

	public void createBossInternal(LivingEntity targetEntity, BossAbilityGroup ability) {
		/* Set up boss health / armor / etc */
		ability.init();

		checkEnablePerformanceEvents(ability);

		Boss boss = mBosses.get(targetEntity.getUniqueId());
		if (boss == null) {
			boss = new Boss(mPlugin, ability);
		} else {
			boss.add(ability);
		}

		mBosses.put(targetEntity.getUniqueId(), boss);
	}

	private void processEntity(LivingEntity entity) {
		if (mBosses.containsKey(entity.getUniqueId())) {
			// already loaded
			return;
		}

		Set<String> tags = entity.getScoreboardTags();
		if (!tags.isEmpty()) {
			Boss boss = null;
			/*
			 * Note - it is important to make a copy here to avoid concurrent modification exception
			 * which happens while iterating and the boss constructor changes the tags on the mob
			 */
			for (String tag : new ArrayList<>(tags)) {
				BossDeserializer deserializer = mBossDeserializers.get(tag);
				if (deserializer != null) {
					BossAbilityGroup ability;
					try {
						ability = deserializer.deserialize(mPlugin, entity);
						if (ability == null) {
							continue;
						}
					} catch (Exception ex) {
						mPlugin.getLogger().log(Level.SEVERE, "Failed to load boss!", ex);
						continue;
					}

					checkEnablePerformanceEvents(ability);

					if (boss == null) {
						boss = new Boss(mPlugin, ability);
						mBosses.put(entity.getUniqueId(), boss);
					} else {
						boss.add(ability);
					}
				}
			}
		}
	}

	public void sendBossDebugInfo(CommandSender sender) {
		sender.sendMessage("");
		sender.sendMessage("Total number of loaded bosses: " + mBosses.size());
		sender.sendMessage("mNearbyEntityDeathEnabled: " + mNearbyEntityDeathEnabled);
		sender.sendMessage("mNearbyBlockBreakEnabled: " + mNearbyBlockBreakEnabled);
		sender.sendMessage("mNearbyPlayerDeathEnabled: " + mNearbyPlayerDeathEnabled);

		Map<String, Integer> bossCounts = new HashMap<>();
		for (Boss boss : mBosses.values()) {
			String tags = String.join(" ", boss.getIdentityTags());
			Integer count = bossCounts.get(tags);
			if (count == null) {
				count = 1;
			} else {
				count += 1;
			}
			bossCounts.put(tags, count);
		}

		List<Map.Entry<String, Integer>> list = new ArrayList<>(bossCounts.entrySet());
		list.sort(Map.Entry.comparingByValue());

		for (Map.Entry<String, Integer> entry : list) {
			sender.sendMessage("  " + entry.getKey() + ": " + entry.getValue());
		}
	}

	public void manuallyRegisterBoss(LivingEntity entity, BossAbilityGroup ability) {
		checkEnablePerformanceEvents(ability);

		Boss boss = mBosses.get(entity.getUniqueId());
		if (boss == null) {
			boss = new Boss(mPlugin, ability);
			mBosses.put(entity.getUniqueId(), boss);
		} else {
			boss.add(ability);
		}
	}

}
