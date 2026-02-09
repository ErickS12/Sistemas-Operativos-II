/*
  Este archivo implementa el algoritmo de reemplazo por AGING (Envejecimiento).
  Es llamado por Kernel.java cuando ocurre un fallo de página (Page Fault).
*/

import java.util.*;

public class PageFault {

  /**
   * Algoritmo de reemplazo AGING.
   * Busca la página en memoria física que tenga el menor valor en su contador 'age'.
   * * @param mem Vector con todas las páginas.
   * @param virtPageNum Cantidad total de páginas virtuales.
   * @param replacePageNum El ID de la página nueva que quiere entrar (la que causó el fallo).
   * @param controlPanel La interfaz gráfica para actualizar los cambios.
   */
  public static void replacePage ( Vector mem , int virtPageNum , int replacePageNum , ControlPanel controlPanel ) 
  {
    int victim = -1;
    int minAge = Integer.MAX_VALUE; // Iniciamos con el valor más alto posible para encontrar el menor

    // 1. BUSCAR LA VÍCTIMA
    // Recorremos todas las páginas para encontrar la que tiene el menor 'age'
    for (int i = 0; i < virtPageNum; i++) 
    {
      Page page = ( Page ) mem.elementAt( i );
      
      // Solo nos interesan las páginas que actualmente ocupan un marco físico (physical != -1)
      if ( page.physical != -1 ) 
      {
        // En Aging, un valor menor significa que la página ha sido usada menos recientemente
        // (tiene más ceros a la izquierda o se desplazó más sin ser leída).
        if (page.age < minAge) 
        {
          minAge = page.age;
          victim = i;
        }
      }
    }

    // 2. REALIZAR EL INTERCAMBIO (SWAP)
    if (victim != -1) 
    {
      Page victimPage = ( Page ) mem.elementAt( victim );
      Page nextPage = ( Page ) mem.elementAt( replacePageNum );
      
      // A) Actualizar GUI: Quitar la página vieja de la visualización
      controlPanel.removePhysicalPage( victim );
      
      // B) Transferir el marco físico (physical frame) de la víctima a la nueva página
      nextPage.physical = victimPage.physical;
      
      // C) Actualizar GUI: Poner la nueva página en la visualización
      controlPanel.addPhysicalPage( nextPage.physical , replacePageNum );
      
      // D) Resetear COMPLETAMENTE la página expulsada
      victimPage.inMemTime = 0;
      victimPage.lastTouchTime = 0;
      victimPage.R = 0;
      victimPage.M = 0;
      victimPage.age = 0;       // ¡CRUCIAL! Reiniciar la edad para cuando vuelva a entrar en el futuro
      victimPage.physical = -1; // Marcar como fuera de memoria física
    }
  }
}