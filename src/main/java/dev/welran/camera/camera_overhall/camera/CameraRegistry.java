package dev.welran.camera.camera_overhall.camera;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraRegistry {
    private final Map<String, CameraEntry> camerasById = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, CameraEntry>> networkIndex = new ConcurrentHashMap<>();

    public void registerOrUpdate(CameraEntry entry) {
        if (entry == null || entry.networkId() == null || entry.networkId().isBlank() || entry.cameraIndex() < 0) {
            return;
        }

        Map<Integer, CameraEntry> camerasInNetwork = networkIndex.computeIfAbsent(
                entry.networkId(), ignored -> new ConcurrentHashMap<>()
        );
        CameraEntry previous = camerasInNetwork.put(entry.cameraIndex(), entry);
        if (previous != null && !previous.getUniqueId().equals(entry.getUniqueId())) {
            camerasById.remove(previous.getUniqueId(), previous);
        }
        camerasById.put(entry.getUniqueId(), entry);
    }

    public void remove(String uniqueId) {
        if (uniqueId == null) {
            return;
        }
        CameraEntry removed = camerasById.remove(uniqueId);
        if (removed == null) {
            return;
        }

        Map<Integer, CameraEntry> camerasInNetwork = networkIndex.get(removed.networkId());
        if (camerasInNetwork != null) {
            camerasInNetwork.remove(removed.cameraIndex(), removed);
            if (camerasInNetwork.isEmpty()) {
                networkIndex.remove(removed.networkId(), camerasInNetwork);
            }
        }
    }

    public Optional<CameraEntry> getCamera(String networkId, int cameraIndex) {
        Map<Integer, CameraEntry> camerasInNetwork = networkIndex.get(networkId);
        return camerasInNetwork == null
                ? Optional.empty()
                : Optional.ofNullable(camerasInNetwork.get(cameraIndex));
    }

    public Collection<CameraEntry> getCameras() {
        return List.copyOf(camerasById.values());
    }

    public List<CameraEntry> getCameras(String networkId) {
        Map<Integer, CameraEntry> camerasInNetwork = networkIndex.get(networkId);
        if (camerasInNetwork == null) {
            return List.of();
        }
        return camerasInNetwork.values().stream()
                .sorted(java.util.Comparator.comparingInt(CameraEntry::cameraIndex))
                .toList();
    }

    public void purgeStaleEntries(long currentTick, long maxAgeTicks) {
        for (CameraEntry entry : camerasById.values()) {
            if (currentTick - entry.lastSeenTick() > maxAgeTicks) {
                remove(entry.getUniqueId());
            }
        }
    }

    public void clear() {
        camerasById.clear();
        networkIndex.clear();
    }
}
