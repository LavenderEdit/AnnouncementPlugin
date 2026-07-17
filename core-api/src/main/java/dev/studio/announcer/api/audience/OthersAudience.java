package dev.studio.announcer.api.audience;

/**
 * Representa a todos los jugadores conectados excepto al actor que desencadenó el evento.
 * <p>
 * Nota: Si no hay un actor definido (es decir, actorId es nulo o vacío), esta audiencia
 * resuelve de forma implícita a "todos los jugadores" (AllAudience) ya que no se excluye a nadie.
 */
public final class OthersAudience implements Audience {
    public static final OthersAudience INSTANCE = new OthersAudience();
    private OthersAudience() {}
}
