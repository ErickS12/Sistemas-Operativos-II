import java.util.*;

public class PageFault {

  /**
   * Algoritmo de reemplazo por AGING (Envejecimiento).
   * Busca la página con el menor valor en 'age' (la menos usada recientemente).
   */
  public static void replacePage ( Vector mem , int virtPageNum , int replacePageNum , ControlPanel controlPanel ) 
  {
    int victim = -1;
    int minAge = Integer.MAX_VALUE; // Iniciamos con el valor más alto posible

    // 1. Buscar la víctima (Menor edad)
    for (int i = 0; i < virtPageNum; i++) 
    {
      Page page = ( Page ) mem.elementAt( i );
      
      // Solo revisamos páginas que estén ocupando un marco físico
      if ( page.physical != -1 ) 
      {
        // En Aging, un valor numérico menor significa que los bits 1 están más a la derecha (pasado)
        // o que tiene puros 0s.
        if (page.age < minAge) 
        {
          minAge = page.age;
          victim = i;
        }
      }
    }

    // 2. Realizar el intercambio (Swap)
    if (victim != -1) 
    {
      Page victimPage = ( Page ) mem.elementAt( victim );
      Page nextPage = ( Page ) mem.elementAt( replacePageNum );
      
      // Actualizar GUI: Quitar página vieja
      controlPanel.removePhysicalPage( victim );
      
      // Asignar el marco físico a la nueva página
      nextPage.physical = victimPage.physical;
      
      // Actualizar GUI: Poner página nueva
      controlPanel.addPhysicalPage( nextPage.physical , replacePageNum );
      
      // Resetear valores de la página expulsada
      victimPage.inMemTime = 0;
      victimPage.lastTouchTime = 0;
      victimPage.R = 0;
      victimPage.M = 0;
      victimPage.age = 0;       // Importante: Resetear edad al salir
      victimPage.physical = -1; // Marcar como fuera de memoria
    }
  }
}