package jp.evolvegame.core;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Experimental third-person camera for the Monster.
 * A server-side invisible camera entity is moved behind/above the Monster body,
 * and ProtocolLib's CAMERA packet makes only the controller render from it.
 *
 * This intentionally lives outside the renderer so it can later be reused when
 * the vanilla test body is replaced by a Model Engine model.
 */
public final class MonsterCameraController {
    private final ProjectEvolvePlugin plugin;
    private final MatchManager match;
    private final TestMonsterController monsterRenderer;
    private final ProtocolManager protocol;
    private final Map<UUID, ArmorStand> cameras = new HashMap<>();
    private int taskId = -1;

    public MonsterCameraController(ProjectEvolvePlugin plugin, MatchManager match, TestMonsterController monsterRenderer) {
        this.plugin = plugin;
        this.match = match;
        this.monsterRenderer = monsterRenderer;
        this.protocol = ProtocolLibrary.getProtocolManager();
        startTask();
    }

    public void enableFor(Player player) {
        if (!plugin.getConfig().getBoolean("monster.camera.enabled", true)) return;
        ArmorStand camera = cameras.computeIfAbsent(player.getUniqueId(), id -> createCamera(player));
        updateCamera(player, camera);
        sendCamera(player, camera);
    }

    public void refresh(Player player) {
        if (!monsterRenderer.isControlled(player)) return;
        removeCameraEntity(player.getUniqueId());
        enableFor(player);
    }

    public void disableFor(Player player) {
        sendCamera(player, player);
        removeCameraEntity(player.getUniqueId());
    }

    public boolean isEnabledFor(Player player) {
        return cameras.containsKey(player.getUniqueId());
    }

    private ArmorStand createCamera(Player player) {
        Location loc = player.getLocation().clone();
        ArmorStand camera = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        camera.setInvisible(true);
        camera.setMarker(true);
        camera.setGravity(false);
        camera.setInvulnerable(true);
        camera.setSilent(true);
        camera.setPersistent(false);
        camera.setCollidable(false);
        camera.addScoreboardTag("project_evolve_monster_camera");
        return camera;
    }

    private void startTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            UUID monsterId = match.getMonsterId();
            if (monsterId == null) return;
            Player player = Bukkit.getPlayer(monsterId);
            if (player == null || !player.isOnline() || !monsterRenderer.isControlled(player)) return;

            ArmorStand camera = cameras.get(monsterId);
            if (camera == null || !camera.isValid()) {
                enableFor(player);
                return;
            }
            updateCamera(player, camera);
        }, 1L, 1L);
    }

    private void updateCamera(Player player, ArmorStand camera) {
        if (!camera.getWorld().equals(player.getWorld())) {
            removeCameraEntity(player.getUniqueId());
            enableFor(player);
            return;
        }

        int stage = match.getMonsterStage();
        String key = "monster.camera.stage-" + stage;
        double distance = plugin.getConfig().getDouble(key + ".distance", 4.0 + stage * 1.5);
        double height = plugin.getConfig().getDouble(key + ".height", 1.5 + stage);

        Location playerLoc = player.getLocation();
        Vector forward = playerLoc.getDirection().clone();
        forward.setY(0);
        if (forward.lengthSquared() < 0.0001) forward = new Vector(0, 0, 1);
        forward.normalize();

        Location anchor = playerLoc.clone().add(0, height, 0);
        Vector backwards = forward.clone().multiply(-1);
        double actualDistance = distance;

        if (plugin.getConfig().getBoolean("monster.camera.wall-collision", true)) {
            RayTraceResult hit = player.getWorld().rayTraceBlocks(anchor, backwards, distance);
            if (hit != null && hit.getHitPosition() != null) {
                double hitDistance = hit.getHitPosition().distance(anchor.toVector());
                actualDistance = Math.max(0.6, hitDistance - 0.35);
            }
        }

        Location desired = anchor.clone().add(backwards.multiply(actualDistance));
        // Keep the camera orientation tied to the controller's mouse direction.
        desired.setYaw(playerLoc.getYaw());
        desired.setPitch(playerLoc.getPitch());
        camera.teleport(desired);
    }

    private void sendCamera(Player viewer, Entity cameraEntity) {
        try {
            PacketContainer packet = protocol.createPacket(PacketType.Play.Server.CAMERA);
            packet.getIntegers().write(0, cameraEntity.getEntityId());
            protocol.sendServerPacket(viewer, packet);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Failed to switch camera for " + viewer.getName() + ": " + ex.getMessage());
        }
    }

    private void removeCameraEntity(UUID playerId) {
        ArmorStand camera = cameras.remove(playerId);
        if (camera != null && camera.isValid()) camera.remove();
    }

    public void shutdown() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        for (UUID id : cameras.keySet().toArray(UUID[]::new)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) sendCamera(player, player);
            removeCameraEntity(id);
        }
    }
}
