package net.anubis.client.module.impl.misc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.anubis.client.chat.ModuleChat;
import net.anubis.client.event.EventHandler;
import net.anubis.client.event.impl.PacketReceiveNettyEvent;
import net.anubis.client.event.impl.PacketSendEvent;
import net.anubis.client.event.impl.TickEvent;
import net.anubis.client.gui.StaffGhost;
import net.anubis.client.module.ClientModule;
import net.anubis.client.module.ModuleCategory;
import net.anubis.client.module.ModuleRegistry;
import net.anubis.client.util.CompletionChannel;
import net.anubis.client.util.NameFilter;
import net.anubis.client.util.antivanish.AntiVanishHeuristics;
import net.anubis.client.util.antivanish.AntiVanishText;
import net.anubis.client.util.staff.ShardTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1109;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1657;
import net.minecraft.class_1676;
import net.minecraft.class_1934;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2588;
import net.minecraft.class_2596;
import net.minecraft.class_2664;
import net.minecraft.class_2675;
import net.minecraft.class_2680;
import net.minecraft.class_2703;
import net.minecraft.class_2739;
import net.minecraft.class_2741;
import net.minecraft.class_2765;
import net.minecraft.class_2767;
import net.minecraft.class_2824;
import net.minecraft.class_2846;
import net.minecraft.class_2879;
import net.minecraft.class_2885;
import net.minecraft.class_2886;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_640;
import net.minecraft.class_7439;
import net.minecraft.class_7828;
import net.minecraft.class_7923;
import net.minecraft.class_8042;
import net.minecraft.class_2703.class_2705;
import net.minecraft.class_2703.class_5893;

@Environment(EnvType.CLIENT)
public final class AntiVanishModule extends ClientModule {
   private static final class_310 MC = class_310.method_1551();
   private static final String PROBE_COMMAND = "msg";
   private static final int PROBE_CYCLE_TICKS = 100;
   private static final int PROBE_GAP_TICKS = 10;
   private static final long PROBE_TIMEOUT_MS = 5000L;
   private static final int PROBE_FLOOD_LIMIT = 3;
   private static final int SENSOR_RANGE = 64;
   private static final long SIGNAL_WINDOW_MS = 15000L;
   private static final long DETECTION_TTL_MS = 20000L;
   private static final long CRITICAL_COOLDOWN_MS = 12000L;
   private static final long ANNOUNCE_COOLDOWN_MS = 1500L;
   private static final long ANNOUNCE_WINDOW_MS = 4000L;
   private static final int MAX_ANNOUNCE_PER_WINDOW = 6;
   private static final int MAX_OBSERVATIONS_PER_TICK = 512;
   private static final int CRITICAL_SCORE = 35;
   private static final long SELF_PLACE_TTL_MS = 6000L;
   private static final long CONTAINER_SELF_GRACE_MS = 2500L;
   private static final long SPELL_SETTLE_MS = 1000L;
   private final ConcurrentLinkedQueue<AntiVanishModule.Observation> observations = new ConcurrentLinkedQueue<>();
   private final Map<UUID, AntiVanishModule.KnownPlayer> knownPlayers = new HashMap<>();
   private final Map<String, AntiVanishModule.Detection> detections = new LinkedHashMap<>();
   private final Map<String, Long> signalCooldowns = new HashMap<>();
   private final Map<String, Long> announceCooldowns = new HashMap<>();
   private final Deque<Long> announceTimes = new ArrayDeque<>();
   private final Map<Long, Long> selfPlacedBlocks = new ConcurrentHashMap<>();
   private final Map<UUID, Long> confirmedDepartures = new HashMap<>();
   private final Map<Long, Long> automatedMechanisms = new HashMap<>();
   private final Map<String, Deque<Long>> weakParticleBursts = new HashMap<>();
   private final Deque<AntiVanishModule.Signal> signals = new ArrayDeque<>();
   private final Deque<AntiVanishModule.ExplosionEvent> recentExplosions = new ArrayDeque<>();
   private final Deque<AntiVanishModule.PendingVanish> pendingVanishes = new ArrayDeque<>();
   private final Map<String, AntiVanishModule.HiddenSpell> hiddenSpells = new LinkedHashMap<>();
   private final Deque<AntiVanishModule.RecentMessage> recentMessages = new ArrayDeque<>();
   private boolean serverSendsLeaveMessages;
   private final ArrayDeque<String> probeQueue = new ArrayDeque<>();
   private final Map<Integer, AntiVanishModule.Probe> probesInFlight = new HashMap<>();
   private final List<String> probeHits = new ArrayList<>();
   private int lastProbeTick;
   private static final long RECENT_MESSAGE_TTL_MS = 8000L;
   private static final long TRUSTED_LISTED_MS = 1500L;
   private final Map<UUID, Long> listedSinceMs = new HashMap<>();
   private final ShardTracker shard = new ShardTracker();
   private int tickCounter;
   private volatile long lastLocalActionMs;
   private long lastContainerActivityMs;
   private long lastCriticalMs;
   private long criticalUntilMs;
   private String criticalWatcher = "";
   private final Set<UUID> pendingChimes = new HashSet<>();
   private UUID ghostId;
   private String ghostName;

   public AntiVanishModule() {
      super("anti-vanish", "Anti Vanish", "Detects vanished players.", ModuleCategory.DONUT);
      this.usesNotifications();
   }

   @Override
   protected void onEnable() {
      this.resetRuntime();
      if (MC.field_1724 != null && MC.field_1687 != null) {
         this.trackListedPlayers();
      }
   }

   @Override
   protected void onDisable() {
      this.resetRuntime();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (MC.field_1724 != null && MC.field_1687 != null && MC.method_1562() != null) {
         if (this.shard.moved(MC)) {
            this.resetRuntime();
         }

         this.playPendingChime();
         this.tickCounter++;
         if (MC.field_1724.field_7512 != MC.field_1724.field_7498) {
            this.lastContainerActivityMs = System.currentTimeMillis();
         }

         this.drainObservations();
         this.processPendingVanishes();
         this.stepStaffProbes();
         if (this.tickCounter % 10 == 0) {
            this.trackListedPlayers();
            this.reviewSpells();
         }

         if (this.tickCounter % 5 == 0) {
            this.scanInvisiblePlayers();
         }

         this.pruneState();
      } else {
         this.resetRuntime();
      }
   }

   @EventHandler
   public void onPacketSend(PacketSendEvent event) {
      this.handleOutbound(event.packet);
   }

   private void handleOutbound(class_2596<?> packet) {
      if (packet != null) {
         if (packet instanceof class_2885 useOn) {
            this.lastLocalActionMs = System.currentTimeMillis();
            if (useOn.method_12543() != null && useOn.method_12543().method_17777() != null) {
               long until = System.currentTimeMillis() + 6000L;
               class_2338 hit = useOn.method_12543().method_17777();
               class_2338 adjacent = hit.method_10093(useOn.method_12543().method_17780());
               this.selfPlacedBlocks.put(hit.method_10063(), until);
               this.selfPlacedBlocks.put(adjacent.method_10063(), until);
               String itemPath = "";
               if (MC.field_1724 != null && useOn.method_12546() != null) {
                  class_2960 itemId = class_7923.field_41178.method_10221(MC.field_1724.method_5998(useOn.method_12546()).method_7909());
                  itemPath = AntiVanishHeuristics.path(itemId == null ? "" : itemId.toString());
               }

               markSelfMultiBlockFootprint(hit, itemPath, this.selfPlacedBlocks, until);
               markSelfMultiBlockFootprint(adjacent, itemPath, this.selfPlacedBlocks, until);
            }
         } else {
            if (packet instanceof class_2846 || packet instanceof class_2886 || packet instanceof class_2824 || packet instanceof class_2879) {
               this.lastLocalActionMs = System.currentTimeMillis();
            }
         }
      }
   }

   static void markSelfMultiBlockFootprint(class_2338 pos, String path, Map<Long, Long> targets, long until) {
      if (pos != null && targets != null) {
         targets.put(pos.method_10063(), until);
         String id = path == null ? "" : path;
         boolean vertical = id.endsWith("_door") && !id.endsWith("trapdoor")
            || id.contains("sunflower")
            || id.contains("lilac")
            || id.contains("rose_bush")
            || id.contains("peony")
            || id.contains("tall_grass")
            || id.contains("large_fern")
            || id.contains("pitcher_plant");
         if (vertical) {
            targets.put(pos.method_10084().method_10063(), until);
            targets.put(pos.method_10074().method_10063(), until);
         }

         if (id.endsWith("_bed")) {
            targets.put(pos.method_10069(1, 0, 0).method_10063(), until);
            targets.put(pos.method_10069(-1, 0, 0).method_10063(), until);
            targets.put(pos.method_10069(0, 0, 1).method_10063(), until);
            targets.put(pos.method_10069(0, 0, -1).method_10063(), until);
         }
      }
   }

   @EventHandler
   public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
      if (this.handleInbound(event.packet)) {
         event.setCancelled(true);
      }
   }

   private boolean handleInbound(class_2596<?> packet) {
      if (packet instanceof class_8042 bundle) {
         for (class_2596<?> part : bundle.method_48324()) {
            this.handleInbound(part);
         }

         return false;
      } else {
         if (packet instanceof class_7828 remove) {
            if (remove.comp_1105().size() < 4) {
               for (UUID id : remove.comp_1105()) {
                  this.observations.offer(AntiVanishModule.Observation.tabRemove(id));
               }
            }
         } else if (packet instanceof class_2703 info) {
            if (info.method_46327().contains(class_5893.field_40700)) {
               int unlisted = 0;

               for (class_2705 entry : info.method_46329()) {
                  if (entry != null && !entry.comp_1108()) {
                     unlisted++;
                  }
               }

               if (unlisted > 0 && unlisted < 4) {
                  for (class_2705 entry : info.method_46329()) {
                     if (entry != null && !entry.comp_1108()) {
                        this.observations.offer(AntiVanishModule.Observation.tabHide(entry.comp_1106()));
                     }
                  }
               }
            }

            if (info.method_46327().contains(class_5893.field_29137)) {
               this.queueGamemodeUpdates(info);
            }
         } else if (packet instanceof class_7439 chat) {
            String text = chat.comp_763() == null ? "" : chat.comp_763().getString();
            if (!text.isBlank()) {
               this.observations.offer(AntiVanishModule.Observation.systemChat(text));
            }

            String departedName = departedPlayerName(chat.comp_763());
            if (!departedName.isBlank()) {
               this.observations.offer(AntiVanishModule.Observation.playerLeft(departedName));
            }
         } else if (packet instanceof class_2739 metadata) {
            this.observations.offer(AntiVanishModule.Observation.entityMetadata(metadata.comp_1127()));
         } else if (packet instanceof class_2664 explosion) {
            this.observations.offer(AntiVanishModule.Observation.explosion(explosion.comp_2883(), explosion.comp_4594()));
         } else if (packet instanceof class_2765 sound) {
            this.observations
               .offer(AntiVanishModule.Observation.entitySound(sound.method_11883(), soundId(((class_3414)sound.method_11882().comp_349()).comp_3319())));
         } else if (packet instanceof class_2675 particles) {
            class_2960 id = class_7923.field_41180.method_10221(particles.method_11551().method_10295());
            this.observations
               .offer(
                  AntiVanishModule.Observation.position(
                     AntiVanishModule.ObservationType.PARTICLE,
                     particles.method_11544(),
                     particles.method_11547(),
                     particles.method_11546(),
                     id == null ? "" : id.toString()
                  )
               );
         } else if (packet instanceof class_2767 sound) {
            this.onSoundPacket(sound);
         }

         return false;
      }
   }

   private void onSoundPacket(class_2767 packet) {
      if (packet != null && packet.method_11894() != null && packet.method_11894().comp_349() != null) {
         String id = soundId(((class_3414)packet.method_11894().comp_349()).comp_3319());
         this.observations
            .offer(
               AntiVanishModule.Observation.position(
                  AntiVanishModule.ObservationType.POSITIONAL_SOUND, packet.method_11890(), packet.method_11889(), packet.method_11893(), id
               )
            );
      }
   }

   public static boolean shouldShowHud() {
      AntiVanishModule module = instance();
      return module != null && module.isEnabled() && module.hasHudContent();
   }

   private boolean hasHudContent() {
      long now = System.currentTimeMillis();
      if (now < this.criticalUntilMs) {
         return true;
      }

      for (AntiVanishModule.Detection detection : this.detections.values()) {
         if (detection.expiresAt > now && detectionWorthShowing(detection)) {
            return true;
         }
      }

      return false;
   }

   private static boolean detectionWorthShowing(AntiVanishModule.Detection detection) {
      if (detection == null) {
         return false;
      }

      String name = detection.name == null ? "" : detection.name.trim();
      return !name.isBlank() && !"Unknown".equalsIgnoreCase(name) && !"You".equalsIgnoreCase(name) && !"CRITICAL".equalsIgnoreCase(name);
   }

   public static String hudTag(AntiVanishModule.HudEntry entry) {
      if (entry == null) {
         return "WATCH";
      } else if ("CRITICAL".equalsIgnoreCase(entry.name())) {
         return "ALERT";
      } else {
         String lower = entry.reason() == null ? "" : entry.reason().toLowerCase(Locale.ROOT);
         if (lower.startsWith("vanish event")) {
            return "VANISH";
         } else if (lower.startsWith("invisible entity")) {
            return "INVIS";
         } else if (lower.startsWith("suspicious sound")) {
            return "SOUND";
         } else {
            return lower.startsWith("ghost particle") ? "PARTICLE" : "WATCH";
         }
      }
   }

   public static String hudValue(AntiVanishModule.HudEntry entry) {
      if (entry == null) {
         return "Staff";
      }

      if ("CRITICAL".equalsIgnoreCase(entry.name())) {
         return shortName(entry.reason());
      }

      String name = entry.name() == null ? "" : entry.name().trim();
      return !name.isBlank() && !"Unknown".equalsIgnoreCase(name) && !"You".equalsIgnoreCase(name) ? shortName(name) : "Staff";
   }

   private static String shortName(String name) {
      String shown = name == null ? "" : NameFilter.apply(name.trim());
      return shown.length() <= 16 ? shown : shown.substring(0, 16);
   }

   public static List<AntiVanishModule.HudEntry> hudEntries() {
      AntiVanishModule module = instance();
      return module != null && shouldShowHud() ? module.hudSnapshot() : List.of();
   }

   private static AntiVanishModule instance() {
      return ModuleRegistry.get(AntiVanishModule.class);
   }

   private void drainObservations() {
      for (int i = 0; i < 512; i++) {
         AntiVanishModule.Observation observation = this.observations.poll();
         if (observation == null) {
            break;
         }

         this.processObservation(observation);
      }

      while (this.observations.size() > 4096) {
         this.observations.poll();
      }
   }

   private void processObservation(AntiVanishModule.Observation observation) {
      switch (observation.type) {
         case TAB_REMOVE:
            this.handleTabRemoval(observation.profileId);
            break;
         case TAB_HIDE:
            this.handleTabHidden(observation.profileId);
            break;
         case PLAYER_LEFT:
            this.serverSendsLeaveMessages = true;
            this.confirmDeparture(observation.detail);
            break;
         case SYSTEM_CHAT:
            this.cacheRecentMessage(observation.detail);
            break;
         case ENTITY_METADATA:
            this.inspectInvisibleEntity(observation.entityId);
            break;
         case POSITIONAL_SOUND:
            this.inspectPositionalSound(observation);
            break;
         case ENTITY_SOUND:
            this.inspectEntitySound(observation);
            break;
         case PARTICLE:
            this.inspectParticle(observation);
            break;
         case EXPLOSION:
            this.rememberExplosion(observation);
            break;
         case GAMEMODE:
            this.announceGamemode(observation.profileId, observation.detail);
      }
   }

   private void trackListedPlayers() {
      if (MC.method_1562() != null) {
         long now = System.currentTimeMillis();

         for (class_640 info : MC.method_1562().method_45732()) {
            if (info != null && info.method_2966() != null && info.method_2966().id() != null) {
               UUID uuid = info.method_2966().id();
               if (MC.field_1724 == null || !MC.field_1724.method_5667().equals(uuid)) {
                  this.listedSinceMs.putIfAbsent(uuid, now);
                  String name = info.method_2966().name();
                  if (name != null && !this.knownPlayers.containsKey(uuid)) {
                     this.knownPlayers.put(uuid, new AntiVanishModule.KnownPlayer(uuid, name));
                  }

                  if (name != null && !this.hiddenSpells.isEmpty()) {
                     this.endSpell(name, false, 1000L);
                  }
               }
            }
         }

         if (this.listedSinceMs.size() > 1024 || this.knownPlayers.size() > 1024) {
            Set<UUID> connected = new HashSet<>();

            for (class_640 info : MC.method_1562().method_2880()) {
               if (info != null && info.method_2966() != null) {
                  connected.add(info.method_2966().id());
               }
            }

            this.listedSinceMs.keySet().retainAll(connected);
            this.knownPlayers.keySet().retainAll(connected);
         }
      }
   }

   private boolean trustedListed(UUID uuid) {
      Long since = uuid == null ? null : this.listedSinceMs.get(uuid);
      return since != null && System.currentTimeMillis() - since >= 1500L;
   }

   private boolean credibleSubject(UUID uuid, String name) {
      return uuid != null && AntiVanishText.isUsername(name) && StaffListModule.isStaffName(name) && this.trustedListed(uuid);
   }

   private void handleTabRemoval(UUID uuid) {
      if (uuid != null && !MC.field_1724.method_5667().equals(uuid)) {
         String name = this.knownName(uuid);
         if (this.credibleSubject(uuid, name) && AntiVanishText.isPlausiblePlayerName(name)) {
            this.pendingVanishes.removeIf(pending -> pending.uuid.equals(uuid));
            this.pendingVanishes.addLast(new AntiVanishModule.PendingVanish(uuid, name, this.tickCounter + 20));
         }
      }
   }

   private void handleTabHidden(UUID uuid) {
      if (uuid != null && !MC.field_1724.method_5667().equals(uuid)) {
         AntiVanishModule.KnownPlayer known = this.knownPlayers.get(uuid);
         String name = known != null && known.name != null ? known.name : this.knownName(uuid);
         if (this.credibleSubject(uuid, name) && AntiVanishText.isPlausiblePlayerName(name)) {
            if (!this.recentMessageNames(name)) {
               this.reportHidden(uuid, name, false, "Vanish Event: hidden from TAB", "hidden from TAB", 100);
            }
         }
      }
   }

   private String knownName(UUID uuid) {
      AntiVanishModule.KnownPlayer known = this.knownPlayers.get(uuid);
      if (known != null && known.name != null && !known.name.isBlank()) {
         return known.name;
      }

      if (MC.method_1562() != null) {
         class_640 info = MC.method_1562().method_2871(uuid);
         if (info != null && info.method_2966() != null) {
            return info.method_2966().name();
         }
      }

      return null;
   }

   private UUID knownUuid(String name) {
      for (AntiVanishModule.KnownPlayer known : this.knownPlayers.values()) {
         if (known.name.equalsIgnoreCase(name)) {
            return known.uuid;
         }
      }

      return null;
   }

   private void confirmDeparture(String displayedName) {
      long now = System.currentTimeMillis();

      for (AntiVanishModule.KnownPlayer known : this.knownPlayers.values()) {
         if (AntiVanishText.containsPlayerName(displayedName, known.name)) {
            this.confirmedDepartures.put(known.uuid, now + 5000L);
            this.pendingVanishes.removeIf(pending -> pending.uuid.equals(known.uuid));
            this.hiddenSpells.remove(spellKey(known.name));
            AntiVanishModule.Detection detection = this.detections.get(known.uuid.toString());
            if (detection != null && tabDepartureReason(detection.reason)) {
               this.detections.remove(known.uuid.toString());
            }

            this.signals
               .removeIf(
                  signal -> signal.type == AntiVanishModule.SignalType.VANISH
                     && signal.subject.equalsIgnoreCase(known.name)
                     && tabDepartureReason(signal.reason)
               );
         }
      }
   }

   private void processPendingVanishes() {
      while (!this.pendingVanishes.isEmpty() && this.pendingVanishes.peekFirst().dueTick <= this.tickCounter) {
         AntiVanishModule.PendingVanish pending = this.pendingVanishes.removeFirst();
         if (MC.method_1562().method_2871(pending.uuid) == null) {
            long now = System.currentTimeMillis();
            if (this.confirmedDepartures.getOrDefault(pending.uuid, 0L) <= now) {
               if (this.recentMessageNames(pending.name)) {
                  this.serverSendsLeaveMessages = true;
               } else {
                  class_1657 remaining = MC.field_1687.method_18470(pending.uuid);
                  if (remaining != null && !remaining.method_31481()) {
                     this.reportHidden(pending.uuid, pending.name, false, "Vanish Event: entity remained", "off TAB, still nearby", 100);
                  } else {
                     String reason = "Vanish Event: silent TAB disappearance";
                     int score = silentTabRemovalScore(this.serverSendsLeaveMessages);
                     if (this.serverSendsLeaveMessages) {
                        this.reportHidden(pending.uuid, pending.name, false, reason, "left TAB silently", score);
                     } else {
                        AntiVanishModule.RegionProof proof = this.regionProof(pending.uuid);
                        if (proof != null) {
                           this.signals.addLast(new AntiVanishModule.Signal(AntiVanishModule.SignalType.VANISH, pending.name, proof, reason, score, now));
                           this.evaluateCritical();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static int silentTabRemovalScore(boolean serverSendsLeaveMessages) {
      return serverSendsLeaveMessages ? 100 : 30;
   }

   static boolean tabDepartureReason(String reason) {
      if (reason == null) {
         return false;
      }

      String lower = reason.toLowerCase(Locale.ROOT);
      return lower.startsWith("vanish event:") && (lower.contains("tab") || lower.contains("entity remained") || lower.contains("no leave packet"));
   }

   private void cacheRecentMessage(String text) {
      if (text != null && !text.isBlank()) {
         long now = System.currentTimeMillis();
         this.recentMessages.addLast(new AntiVanishModule.RecentMessage(text, now));

         while (!this.recentMessages.isEmpty() && (now - this.recentMessages.peekFirst().atMs > 8000L || this.recentMessages.size() > 64)) {
            this.recentMessages.removeFirst();
         }
      }
   }

   private boolean recentMessageNames(String name) {
      if (name != null && !name.isBlank()) {
         long now = System.currentTimeMillis();

         for (AntiVanishModule.RecentMessage message : this.recentMessages) {
            if (now - message.atMs <= 8000L && AntiVanishText.looksLikeLeaveMessage(message.text, name)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void stepStaffProbes() {
      long now = System.currentTimeMillis();
      Iterator<Entry<Integer, AntiVanishModule.Probe>> it = this.probesInFlight.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Integer, AntiVanishModule.Probe> entry = it.next();
         List<String> names = CompletionChannel.INSTANCE.poll(entry.getKey());
         if (names != null || now - entry.getValue().atMs() >= 5000L) {
            CompletionChannel.INSTANCE.forget(entry.getKey());
            it.remove();
            if (names != null && suggests(names, entry.getValue().name())) {
               this.probeHits.add(entry.getValue().name());
            }
         }
      }

      boolean idle = this.probeQueue.isEmpty() && this.probesInFlight.isEmpty();
      if (idle && !this.probeHits.isEmpty()) {
         this.reportProbeHits();
      }

      if (idle && this.tickCounter % 100 == 0) {
         this.queueStaffProbes();
      }

      if (!this.probeQueue.isEmpty() && this.tickCounter - this.lastProbeTick >= 10) {
         String name = this.probeQueue.pollFirst();
         this.lastProbeTick = this.tickCounter;
         int key = CompletionChannel.INSTANCE.fire(this, "msg " + name, CompletionChannel.Form.SLASH);
         if (key >= 0) {
            this.probesInFlight.put(key, new AntiVanishModule.Probe(name, now));
         }
      }
   }

   private void queueStaffProbes() {
      if (MC.method_1562() != null && MC.field_1724 != null) {
         Set<String> tabNames = this.tabNames();
         String self = MC.field_1724.method_5477().getString();

         for (String name : StaffListModule.DONUT_STAFF) {
            if (!name.equalsIgnoreCase(self) && !tabNames.contains(name.toLowerCase(Locale.ROOT))) {
               this.probeQueue.addLast(name);
            }
         }
      }
   }

   private static boolean suggests(List<String> names, String name) {
      String wanted = CompletionChannel.core(name);

      for (String suggestion : names) {
         if (CompletionChannel.core(suggestion).equals(wanted)) {
            return true;
         }
      }

      return false;
   }

   private void reportProbeHits() {
      List<String> hits = new ArrayList<>(this.probeHits);
      this.probeHits.clear();
      if (MC.method_1562() != null && MC.field_1724 != null) {
         Set<String> tabNames = this.tabNames();
         List<String> hidden = new ArrayList<>();

         for (String name : hits) {
            if (AntiVanishText.isPlausiblePlayerName(name) && !tabNames.contains(name.toLowerCase(Locale.ROOT)) && !this.recentMessageNames(name)) {
               hidden.add(name);
            }
         }

         if (!hidden.isEmpty() && hidden.size() <= 3) {
            for (String name : hidden) {
               this.reportHidden(this.knownUuid(name), name, false, "Vanish Event: hidden but targetable", "off TAB, still targetable", 90);
            }
         }
      }
   }

   private Set<String> tabNames() {
      Set<String> names = new HashSet<>();

      for (class_640 info : MC.method_1562().method_2880()) {
         if (info != null && info.method_2966() != null && info.method_2966().name() != null) {
            names.add(info.method_2966().name().toLowerCase(Locale.ROOT));
         }
      }

      return names;
   }

   private static String departedPlayerName(class_2561 component) {
      if (component != null && component.method_10851() instanceof class_2588 translated && "multiplayer.player.left".equals(translated.method_11022())) {
         Object[] args = translated.method_11023();
         if (args.length != 0 && args[0] != null) {
            return args[0] instanceof class_2561 name ? name.getString() : String.valueOf(args[0]);
         } else {
            return "";
         }
      } else {
         return "";
      }
   }

   private void reportHidden(UUID uuid, String name, boolean spectator, String reason, String detail, int score) {
      this.upsertDetection(detectionKey(uuid, name), name, reason, score, System.currentTimeMillis() + 20000L);
      AntiVanishModule.HiddenSpell spell = this.hiddenSpells.get(spellKey(name));
      if (spell == null || spell.spectator != spectator) {
         this.hiddenSpells.put(spellKey(name), new AntiVanishModule.HiddenSpell(uuid, name, spectator, detail, this.regionProof(uuid), this.tickCounter));
      }
   }

   private void reviewSpells() {
      Iterator<AntiVanishModule.HiddenSpell> it = this.hiddenSpells.values().iterator();

      while (it.hasNext()) {
         AntiVanishModule.HiddenSpell spell = it.next();
         if (spell.spectator && MC.method_1562().method_2871(spell.uuid) == null) {
            it.remove();
         } else if (!spell.watched && spell.sinceTick < this.tickCounter) {
            AntiVanishModule.RegionProof proof = this.regionProof(spell.uuid);
            if (proof == null && !spell.told) {
               proof = spell.proof;
            }

            if (proof != null) {
               spell.told = true;
               spell.watched = true;
               this.watchingYou(spell.name, proof);
            } else if (!spell.told) {
               spell.told = true;
               this.news(spell.name, spell.how(), spell.detail);
            }
         }
      }
   }

   private boolean endSpell(String name, boolean spectator, long settleMs) {
      AntiVanishModule.HiddenSpell spell = this.hiddenSpells.get(spellKey(name));
      if (spell != null && spell.spectator == spectator && System.currentTimeMillis() - spell.sinceMs >= settleMs) {
         this.hiddenSpells.remove(spellKey(name));
         this.detections.remove(detectionKey(spell.uuid, spell.name));
         if (spell.told) {
            this.news(spell.name, ModuleChat.body().text("is ").good("visible again").build(), "");
         }

         return true;
      } else {
         return false;
      }
   }

   private AntiVanishModule.RegionProof regionProof(UUID uuid) {
      if (uuid == null) {
         return null;
      } else {
         class_1657 body = MC.field_1687.method_18470(uuid);
         if (body != null && body != MC.field_1724) {
            return new AntiVanishModule.RegionProof(uuid, Math.round(body.method_5739(MC.field_1724)));
         } else {
            return StaffListModule.isInYourRegion(uuid) ? new AntiVanishModule.RegionProof(uuid, -1) : null;
         }
      }
   }

   private static String spellKey(String name) {
      return name.toLowerCase(Locale.ROOT);
   }

   private static String detectionKey(UUID uuid, String name) {
      return uuid != null ? uuid.toString() : name;
   }

   private void scanInvisiblePlayers() {
      double rangeSq = this.sensorRangeSq();

      for (class_1657 player : MC.field_1687.method_18456()) {
         if (player != null && player != MC.field_1724 && player.method_5767() && !(player.method_5858(MC.field_1724) > rangeSq) && this.realPlayer(player)) {
            this.triggerSensor(AntiVanishModule.SignalType.INVISIBLE, player, "Invisible Entity: metadata flag", 35, 5000L);
         }
      }
   }

   private void inspectInvisibleEntity(int entityId) {
      if (MC.field_1687.method_8469(entityId) instanceof class_1657 player && player != MC.field_1724 && player.method_5767()) {
         if (!(player.method_5858(MC.field_1724) > this.sensorRangeSq())) {
            if (this.realPlayer(player)) {
               this.triggerSensor(AntiVanishModule.SignalType.INVISIBLE, player, "Invisible Entity: metadata flag", 35, 5000L);
            }
         }
      }
   }

   private boolean realPlayer(class_1657 player) {
      return this.credibleSubject(player.method_5667(), profileName(player));
   }

   private static String profileName(class_1657 player) {
      return player.method_7334() == null ? player.method_5477().getString() : player.method_7334().name();
   }

   private void inspectPositionalSound(AntiVanishModule.Observation observation) {
      if (AntiVanishHeuristics.suspiciousSound(observation.detail)) {
         class_243 source = observation.position();
         long now = System.currentTimeMillis();
         if (this.nearPlayer(source)
            && !this.hasVisibleCause(source)
            && !this.isExplosionRelated(source)
            && !this.isPoweredMechanism(source, observation.detail)
            && now - this.lastLocalActionMs >= 1000L
            && !this.recentlySelfPlaced(source)
            && !this.villagerToggledDoor(observation.detail, source)
            && (!this.selfContainerActive() || !isContainerSignal(observation.detail))) {
            this.triggerSensor(AntiVanishModule.SignalType.SOUND, this.locatedSubject(source), "Suspicious Sound: " + shortId(observation.detail), 14, 3000L);
         }
      }
   }

   private void inspectEntitySound(AntiVanishModule.Observation observation) {
      if (AntiVanishHeuristics.suspiciousSound(observation.detail)) {
         if (MC.field_1687.method_8469(observation.entityId) instanceof class_1657 player
            && player != MC.field_1724
            && player.method_5767()
            && player.method_5858(MC.field_1724) <= this.sensorRangeSq()
            && this.realPlayer(player)) {
            this.triggerSensor(AntiVanishModule.SignalType.SOUND, player, "Suspicious Sound: invisible source", 16, 3000L);
         }
      }
   }

   private void inspectParticle(AntiVanishModule.Observation observation) {
      if (AntiVanishHeuristics.suspiciousParticle(observation.detail)) {
         class_243 source = observation.position();
         long now = System.currentTimeMillis();
         if (this.nearPlayer(source)
            && !this.hasVisibleCause(source)
            && !this.isExplosionRelated(source)
            && now - this.lastLocalActionMs >= 900L
            && !this.hasAmbientParticleSource(source, observation.detail)) {
            String particle = shortId(observation.detail);
            if (!particle.equals("block") && !particle.contains("smoke") || !this.nearSelf(source, 6.25)) {
               if (!particle.equals("block") && !particle.contains("smoke") || this.particleBurstReady(particle, now)) {
                  this.triggerSensor(
                     AntiVanishModule.SignalType.PARTICLE, this.locatedSubject(source), "Ghost Particle: " + shortId(observation.detail), 16, 3000L
                  );
               }
            }
         }
      }
   }

   private boolean villagerToggledDoor(String blockId, class_243 source) {
      String path = AntiVanishHeuristics.path(blockId);
      boolean doorLike = path.contains("door") && !path.contains("trapdoor") || path.contains("fence_gate");
      if (doorLike && source != null && MC.field_1687 != null) {
         for (class_1297 entity : MC.field_1687.method_18112()) {
            if (entity.method_5864() == class_1299.field_6077 && entity.method_73189().method_1025(source) <= 9.0) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean recentlySelfPlaced(class_243 source) {
      if (source == null) {
         return false;
      }

      Long until = this.selfPlacedBlocks.get(class_2338.method_49638(source).method_10063());
      return until != null && until > System.currentTimeMillis();
   }

   private void rememberExplosion(AntiVanishModule.Observation observation) {
      long now = System.currentTimeMillis();
      this.recentExplosions
         .addLast(new AntiVanishModule.ExplosionEvent(observation.position(), Math.max(2.0, parseDouble(observation.detail, 4.0) + 4.0), now));

      while (this.recentExplosions.size() > 8) {
         this.recentExplosions.removeFirst();
      }
   }

   private boolean particleBurstReady(String particle, long now) {
      Deque<Long> burst = this.weakParticleBursts.computeIfAbsent(particle, ignored -> new ArrayDeque<>());
      burst.addLast(now);

      while (!burst.isEmpty() && now - burst.peekFirst() > 2000L) {
         burst.removeFirst();
      }

      if (burst.size() < 2) {
         return false;
      }

      burst.clear();
      return true;
   }

   private void triggerSensor(AntiVanishModule.SignalType type, String where, String reason, int weight, long cooldownMs) {
      this.addSignal(type, where, null, reason, weight, cooldownMs);
   }

   private void triggerSensor(AntiVanishModule.SignalType type, class_1657 player, String reason, int weight, long cooldownMs) {
      this.addSignal(type, profileName(player), this.regionProof(player.method_5667()), reason, weight, cooldownMs);
   }

   private void addSignal(AntiVanishModule.SignalType type, String subject, AntiVanishModule.RegionProof proof, String reason, int weight, long cooldownMs) {
      long now = System.currentTimeMillis();
      String cooldownKey = type.name() + "|" + subject.toLowerCase(Locale.ROOT);
      long last = this.signalCooldowns.getOrDefault(cooldownKey, 0L);
      if (now - last >= cooldownMs) {
         this.signalCooldowns.put(cooldownKey, now);
         this.signals.addLast(new AntiVanishModule.Signal(type, subject, proof, reason, weight, now));
         this.upsertDetection("signal:" + cooldownKey, subject, reason, weight, now + 20000L);
         this.announceTrigger(cooldownKey, subject, proof != null, reason);
         this.evaluateCritical();
      }
   }

   private void evaluateCritical() {
      long now = System.currentTimeMillis();
      this.pruneSignals(now);
      EnumMap<AntiVanishModule.SignalType, Integer> strongest = new EnumMap<>(AntiVanishModule.SignalType.class);
      AntiVanishModule.Signal watcher = null;

      for (AntiVanishModule.Signal signal : this.signals) {
         strongest.merge(signal.type, signal.weight, Math::max);
         if (signal.proof != null) {
            watcher = signal;
         }
      }

      int score = strongest.values().stream().mapToInt(Integer::intValue).sum();
      if (watcher != null && strongest.size() >= 2 && score >= 35) {
         if (now - this.lastCriticalMs >= 12000L) {
            this.watchingYou(watcher.subject, watcher.proof);
         }
      }
   }

   private void watchingYou(String name, AntiVanishModule.RegionProof proof) {
      long now = System.currentTimeMillis();
      this.lastCriticalMs = now;
      this.criticalUntilMs = now + 7000L;
      this.criticalWatcher = name;
      this.pendingChimes.add(proof.id());
      this.ghostId = proof.id();
      this.ghostName = name;
   }

   private void playPendingChime() {
      if (!this.pendingChimes.isEmpty()) {
         boolean sirenCovers = this.pendingChimes.stream().allMatch(StaffListModule::alarmedRecently);
         this.pendingChimes.clear();
         if (!sirenCovers) {
            if (MC.method_1483() != null) {
               MC.method_1483().method_4873(class_1109.method_4757(class_3417.field_15203, 1.0F, 1.0F));
            }

            StaffGhost.watching(this.ghostId, this.ghostName, 0);
         }
      }
   }

   private void news(String name, class_2561 what, String detail) {
      ModuleChat.Body line = ModuleChat.body().name(NameFilter.apply(name)).text(" ").append(what);
      if (!detail.isEmpty()) {
         line.text(" (" + detail + ")");
      }

      ModuleChat.send(this, line);
   }

   private void queueGamemodeUpdates(class_2703 info) {
      if (!info.method_46327().contains(class_5893.field_29136)) {
         for (class_2705 entry : info.method_46329()) {
            if (entry != null && entry.comp_1110() != null && entry.comp_1106() != null) {
               this.observations.offer(AntiVanishModule.Observation.gamemode(entry.comp_1106(), entry.comp_1110().method_8381()));
            }
         }
      }
   }

   private void announceGamemode(UUID uuid, String gameMode) {
      if (uuid != null && (MC.field_1724 == null || !MC.field_1724.method_5667().equals(uuid))) {
         AntiVanishModule.KnownPlayer known = this.knownPlayers.get(uuid);
         if (known != null && StaffListModule.isStaffName(known.name)) {
            if (class_1934.field_9219.method_8381().equals(gameMode)) {
               if (this.credibleSubject(uuid, known.name)) {
                  this.reportHidden(uuid, known.name, true, "Vanish Event: switched to spectator", "", 100);
                  return;
               }
            } else if (this.endSpell(known.name, true, 0L)) {
               return;
            }

            ModuleChat.send(this, StaffListModule.switchedTo(ModuleChat.body().name(NameFilter.apply(known.name)).text(" "), gameMode));
         }
      }
   }

   private void announceTrigger(String eventKey, String subject, boolean named, String reason) {
      long now = System.currentTimeMillis();
      Long last = this.announceCooldowns.get(eventKey);
      if (last == null || now - last >= 1500L) {
         while (!this.announceTimes.isEmpty() && now - this.announceTimes.peekFirst() > 4000L) {
            this.announceTimes.removeFirst();
         }

         if (this.announceTimes.size() < 6) {
            this.announceCooldowns.put(eventKey, now);
            this.announceTimes.addLast(now);
            int colon = reason.indexOf(": ");
            ModuleChat.Body line = colon < 0
               ? ModuleChat.body().warn(reason)
               : ModuleChat.body().warn(reason.substring(0, colon)).text(reason.substring(colon));
            boolean located = subject != null && !subject.isBlank() && !"Unknown".equalsIgnoreCase(subject) && !"CRITICAL".equalsIgnoreCase(subject);
            if (located) {
               line.text(" (");
               if (named) {
                  line.name(NameFilter.apply(subject));
               } else {
                  line.value(subject);
               }

               line.text(")");
            }

            ModuleChat.send(this, line);
         }
      }
   }

   private void upsertDetection(String key, String name, String reason, int score, long expiresAt) {
      AntiVanishModule.Detection existing = this.detections.get(key);
      if (existing == null) {
         this.detections.put(key, new AntiVanishModule.Detection(name, reason, score, expiresAt));
      } else {
         existing.name = name;
         existing.expiresAt = Math.max(existing.expiresAt, expiresAt);
         if (score >= existing.score) {
            existing.reason = reason;
            existing.score = score;
         }
      }
   }

   private List<AntiVanishModule.HudEntry> hudSnapshot() {
      long now = System.currentTimeMillis();
      List<AntiVanishModule.HudEntry> out = new ArrayList<>();
      if (now < this.criticalUntilMs) {
         out.add(new AntiVanishModule.HudEntry("CRITICAL", this.criticalWatcher));
      }

      Set<String> seenRows = new HashSet<>();
      this.detections
         .values()
         .stream()
         .filter(detection -> detection.expiresAt > now)
         .filter(AntiVanishModule::detectionWorthShowing)
         .sorted(
            Comparator.<AntiVanishModule.Detection>comparingInt(detection -> detection.score)
               .reversed()
               .thenComparing(detection -> detection.name, String.CASE_INSENSITIVE_ORDER)
         )
         .map(detection -> new AntiVanishModule.HudEntry(detection.name, detection.reason))
         .filter(entry -> seenRows.add(hudTag(entry) + "\u0000" + hudValue(entry)))
         .limit(4L)
         .forEach(out::add);
      return List.copyOf(out);
   }

   private void pruneState() {
      long now = System.currentTimeMillis();
      this.detections.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
      this.signalCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 60000L);
      this.announceCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 60000L);
      this.selfPlacedBlocks.entrySet().removeIf(entry -> entry.getValue() <= now);
      this.confirmedDepartures.entrySet().removeIf(entry -> entry.getValue() <= now);
      this.automatedMechanisms.entrySet().removeIf(entry -> entry.getValue() <= now);
      this.weakParticleBursts.values().removeIf(burst -> {
         while (!burst.isEmpty() && now - burst.peekFirst() > 2000L) {
            burst.removeFirst();
         }

         return burst.isEmpty();
      });

      while (!this.recentExplosions.isEmpty() && now - this.recentExplosions.peekFirst().timeMs > 3000L) {
         this.recentExplosions.removeFirst();
      }

      this.pruneSignals(now);
   }

   private void pruneSignals(long now) {
      while (!this.signals.isEmpty() && now - this.signals.peekFirst().timeMs > 15000L) {
         this.signals.removeFirst();
      }
   }

   private boolean nearPlayer(class_243 source) {
      return source != null && source.method_1025(MC.field_1724.method_73189()) <= this.sensorRangeSq();
   }

   private boolean nearSelf(class_243 source, double maxDistSq) {
      return source != null && MC.field_1724 != null && source.method_1025(MC.field_1724.method_73189()) < maxDistSq;
   }

   private boolean selfContainerActive() {
      return System.currentTimeMillis() - this.lastContainerActivityMs < 2500L;
   }

   private static boolean isContainerSignal(String id) {
      String path = AntiVanishHeuristics.path(id);
      return path.contains("chest") || path.contains("barrel") || path.contains("shulker");
   }

   private String locatedSubject(class_243 source) {
      if (source != null && MC.field_1724 != null) {
         class_243 me = MC.field_1724.method_73189();
         double dx = source.field_1352 - me.field_1352;
         double dy = source.field_1351 - me.field_1351;
         double dz = source.field_1350 - me.field_1350;
         long dist = Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
         String dir = compass(dx, dz);
         return dir.isEmpty() ? dist + "m" : dist + "m " + dir;
      } else {
         return "nearby";
      }
   }

   private static String compass(double dx, double dz) {
      String ns = dz < -1.0 ? "N" : (dz > 1.0 ? "S" : "");
      String ew = dx > 1.0 ? "E" : (dx < -1.0 ? "W" : "");
      return ns + ew;
   }

   private boolean hasVisibleCause(class_243 source) {
      if (source == null) {
         return true;
      }

      for (class_1297 entity : MC.field_1687.method_18112()) {
         if (entity != MC.field_1724) {
            if (entity instanceof class_1657 player) {
               if (!player.method_5767() && entity.method_73189().method_1025(source) <= 16.0) {
                  return true;
               }
            } else if (entity instanceof class_1676 && entity.method_73189().method_1025(source) <= 16.0) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isExplosionRelated(class_243 source) {
      if (source == null) {
         return false;
      }

      long now = System.currentTimeMillis();

      while (!this.recentExplosions.isEmpty() && now - this.recentExplosions.peekFirst().timeMs > 3000L) {
         this.recentExplosions.removeFirst();
      }

      for (AntiVanishModule.ExplosionEvent explosion : this.recentExplosions) {
         if (explosion.center.method_1025(source) <= explosion.radius * explosion.radius) {
            return true;
         }
      }

      return false;
   }

   private boolean hasAmbientParticleSource(class_243 source, String particleId) {
      if (source != null && shortId(particleId).contains("smoke")) {
         class_2338 center = class_2338.method_49638(source);

         for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
               for (int dz = -2; dz <= 2; dz++) {
                  class_2338 pos = center.method_10069(dx, dy, dz);
                  class_2960 id = class_7923.field_41175.method_10221(MC.field_1687.method_8320(pos).method_26204());
                  String path = shortId(id == null ? "" : id.toString());
                  if (path.contains("campfire")
                     || path.contains("furnace")
                     || path.contains("smoker")
                     || path.contains("torch")
                     || path.contains("fire")
                     || path.contains("candle")
                     || path.contains("respawn_anchor")) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isPoweredMechanism(class_243 source, String soundId) {
      String path = shortId(soundId);
      if (!path.contains("door") && !path.contains("trapdoor")) {
         return false;
      }

      class_2338 center = class_2338.method_49638(source);
      long now = System.currentTimeMillis();

      for (int dy = -1; dy <= 1; dy++) {
         class_2338 pos = center.method_10069(0, dy, 0);
         class_2680 state = MC.field_1687.method_8320(pos);
         long key = pos.method_10063();
         boolean powered = state.method_28498(class_2741.field_12484) && (Boolean)state.method_11654(class_2741.field_12484);
         if (powered || MC.field_1687.method_49803(pos)) {
            this.automatedMechanisms.put(key, now + 5000L);
            return true;
         }

         if (this.automatedMechanisms.getOrDefault(key, 0L) > now) {
            return true;
         }
      }

      return false;
   }

   private double sensorRangeSq() {
      double r = 64.0;
      return r * r;
   }

   private static double parseDouble(String value, double fallback) {
      try {
         return Double.parseDouble(value);
      } catch (Exception ignored) {
         return fallback;
      }
   }

   private static String soundId(class_2960 id) {
      return id == null ? "" : id.toString();
   }

   private static String shortId(String id) {
      return AntiVanishHeuristics.path(id);
   }

   private void resetRuntime() {
      this.observations.clear();
      this.knownPlayers.clear();
      this.detections.clear();
      this.signalCooldowns.clear();
      this.announceCooldowns.clear();
      this.announceTimes.clear();
      this.selfPlacedBlocks.clear();
      this.confirmedDepartures.clear();
      this.automatedMechanisms.clear();
      this.weakParticleBursts.clear();
      this.signals.clear();
      this.recentExplosions.clear();
      this.pendingVanishes.clear();
      this.hiddenSpells.clear();
      this.shard.clear();
      this.tickCounter = 0;
      this.lastLocalActionMs = 0L;
      this.lastContainerActivityMs = 0L;
      this.lastCriticalMs = 0L;
      this.criticalUntilMs = 0L;
      this.criticalWatcher = "";
      this.pendingChimes.clear();
      this.ghostId = null;
      this.ghostName = null;
      this.recentMessages.clear();
      this.listedSinceMs.clear();
      this.serverSendsLeaveMessages = false;
      this.probeQueue.clear();
      this.probesInFlight.clear();
      this.probeHits.clear();
      this.lastProbeTick = 0;
      CompletionChannel.INSTANCE.clear(this);
   }

   @Environment(EnvType.CLIENT)
   private static final class Detection {
      String name;
      String reason;
      int score;
      long expiresAt;

      Detection(String name, String reason, int score, long expiresAt) {
         this.name = name;
         this.reason = reason;
         this.score = score;
         this.expiresAt = expiresAt;
      }
   }

   @Environment(EnvType.CLIENT)
   private record ExplosionEvent(class_243 center, double radius, long timeMs) {
   }

   @Environment(EnvType.CLIENT)
   private static final class HiddenSpell {
      final UUID uuid;
      final String name;
      final boolean spectator;
      final String detail;
      final AntiVanishModule.RegionProof proof;
      final int sinceTick;
      final long sinceMs = System.currentTimeMillis();
      boolean told;
      boolean watched;

      HiddenSpell(UUID uuid, String name, boolean spectator, String detail, AntiVanishModule.RegionProof proof, int sinceTick) {
         this.uuid = uuid;
         this.name = name;
         this.spectator = spectator;
         this.detail = detail;
         this.proof = proof;
         this.sinceTick = sinceTick;
      }

      class_2561 how() {
         return this.spectator
            ? StaffListModule.switchedTo(ModuleChat.body(), class_1934.field_9219.method_8381()).build()
            : ModuleChat.body().danger("vanished").build();
      }
   }

   @Environment(EnvType.CLIENT)
   public record HudEntry(String name, String reason) {
   }

   @Environment(EnvType.CLIENT)
   private record KnownPlayer(UUID uuid, String name) {
   }

   @Environment(EnvType.CLIENT)
   private record Observation(AntiVanishModule.ObservationType type, UUID profileId, int entityId, double x, double y, double z, String detail) {
      static AntiVanishModule.Observation tabRemove(UUID id) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.TAB_REMOVE, id, -1, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation tabHide(UUID id) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.TAB_HIDE, id, -1, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation gamemode(UUID id, String mode) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.GAMEMODE, id, -1, 0.0, 0.0, 0.0, mode);
      }

      static AntiVanishModule.Observation playerLeft(String name) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.PLAYER_LEFT, null, -1, 0.0, 0.0, 0.0, name);
      }

      static AntiVanishModule.Observation systemChat(String text) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.SYSTEM_CHAT, null, -1, 0.0, 0.0, 0.0, text);
      }

      static AntiVanishModule.Observation entityMetadata(int id) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.ENTITY_METADATA, null, id, 0.0, 0.0, 0.0, "");
      }

      static AntiVanishModule.Observation explosion(class_243 center, float radius) {
         return new AntiVanishModule.Observation(
            AntiVanishModule.ObservationType.EXPLOSION, null, -1, center.field_1352, center.field_1351, center.field_1350, Float.toString(radius)
         );
      }

      static AntiVanishModule.Observation entitySound(int id, String sound) {
         return new AntiVanishModule.Observation(AntiVanishModule.ObservationType.ENTITY_SOUND, null, id, 0.0, 0.0, 0.0, sound);
      }

      static AntiVanishModule.Observation position(AntiVanishModule.ObservationType type, double x, double y, double z, String detail) {
         return new AntiVanishModule.Observation(type, null, -1, x, y, z, detail);
      }

      class_243 position() {
         return new class_243(this.x, this.y, this.z);
      }
   }

   @Environment(EnvType.CLIENT)
   private enum ObservationType {
      TAB_REMOVE,
      TAB_HIDE,
      PLAYER_LEFT,
      SYSTEM_CHAT,
      ENTITY_METADATA,
      POSITIONAL_SOUND,
      ENTITY_SOUND,
      PARTICLE,
      EXPLOSION,
      GAMEMODE;
   }

   @Environment(EnvType.CLIENT)
   private record PendingVanish(UUID uuid, String name, int dueTick) {
   }

   @Environment(EnvType.CLIENT)
   private record Probe(String name, long atMs) {
   }

   @Environment(EnvType.CLIENT)
   private record RecentMessage(String text, long atMs) {
   }

   @Environment(EnvType.CLIENT)
   private record RegionProof(UUID id, int distance) {
   }

   @Environment(EnvType.CLIENT)
   private record Signal(AntiVanishModule.SignalType type, String subject, AntiVanishModule.RegionProof proof, String reason, int weight, long timeMs) {
   }

   @Environment(EnvType.CLIENT)
   private enum SignalType {
      VANISH,
      INVISIBLE,
      PARTICLE,
      SOUND;
   }
}
