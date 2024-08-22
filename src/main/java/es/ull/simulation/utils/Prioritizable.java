package es.ull.simulation.utils;

/**
 * A prioritizable object has a priority and can be used in a Prioritized Table.
 * @author Iván Castilla Rodríguez
 */
public interface Prioritizable {
  /**
   * Returns the priority of the object.
   * @return the priority of the object.
   */
  int getPriority();
}
