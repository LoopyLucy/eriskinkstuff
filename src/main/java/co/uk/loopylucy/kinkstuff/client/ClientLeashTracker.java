package co.uk.loopylucy.kinkstuff.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A client-side tracker for player leashed states.
 * This class mirrors the leashed player data from the server, allowing the 
 * client to know which players are tied together for rope rendering.
 */
public class ClientLeashTracker {
    /** Maps a leashed player's UUID to their holder's UUID on the client. */
    private static final Map<UUID, UUID> CLIENT_MAP = new HashMap<>();

    /**
     * Updates the tracked leash state for a player.
     * 
     * @param target The leashed player's UUID.
     * @param holder The holder's UUID, or null to remove the leash.
     */
    public static void update(UUID target, UUID holder) {
        if (holder == null) {
            CLIENT_MAP.remove(target);
        } else {
            CLIENT_MAP.put(target, holder);
        }
    }

    /**
     * Retrieves the holder's UUID for a given leashed player.
     * 
     * @param target The leashed player's UUID.
     * @return The holder's UUID, or null if not leashed.
     */
    public static UUID getHolderFor(UUID target) {
        return CLIENT_MAP.get(target);
    }
}
