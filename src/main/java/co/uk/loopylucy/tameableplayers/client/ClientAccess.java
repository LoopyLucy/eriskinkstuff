package co.uk.loopylucy.tameableplayers.client;

import co.uk.loopylucy.tameableplayers.client.model.MittensModel;

/**
 * A utility class providing global access to client-side mod components.
 * This is primarily used to store static references to models initialized 
 * during client setup.
 */
public class ClientAccess {
    /** Static reference to the baked MittensModel. */
    public static MittensModel MITTENS_MODEL;
}