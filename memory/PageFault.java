/*
  Este archivo implementa el algoritmo de reemplazo por AGING (Envejecimiento).
  Es llamado por Kernel.java cuando ocurre un fallo de página (Page Fault).
*/

import java.util.*;

public class PageFault {

  
    // Constantes para AGING de 8 bits
  private static final int AGE_BITS = 8;
  private static final int AGE_MASK = (1 << AGE_BITS) - 1; // 0xFF

  /**
   * Algoritmo de reemplazo AGING.
   * Busca la página en memoria física que tenga el menor valor en su contador 'age'.
   * @param mem Vector con todas las páginas.
   * @param virtPageNum Cantidad total de páginas virtuales.
   * @param replacePageNum El ID de la página nueva que quiere entrar (la que causó el fallo).
   * @param controlPanel La interfaz gráfica para actualizar los cambios.
   * 
   */

  public static void replacePage ( Vector mem , int virtPageNum , int replacePageNum , ControlPanel controlPanel ) 
  {
    int victim = -1;
    int minAge = AGE_MASK; // 255 → máximo posible en 8 bits
    // 1. BUSCAR LA VÍCTIMA
    // Recorremos todas las páginas para encontrar la que tiene el menor 'age'
    // 1. SELECCIÓN DE VÍCTIMA (AGING)
    for (int i = 0; i < virtPageNum; i++) {
      Page page = (Page) mem.elementAt(i);

      // Solo páginas residentes en memoria física
      if (page.physical != -1) {
        if ((page.age & AGE_MASK) < minAge) {
          minAge = page.age & AGE_MASK;
          victim = i;
        }
      }
    }
// 2. REEMPLAZO
    if (victim == -1) {
      System.err.println("Error: no se encontró página víctima para reemplazo");
      System.exit(-1);
    }

    Page victimPage = (Page) mem.elementAt(victim);
    Page nextPage   = (Page) mem.elementAt(replacePageNum);

    // 2A. Si la página fue modificada, simulamos write-back
    if (victimPage.M == 1) {
      System.out.println("Página " + victim + " escrita a disco (dirty bit)");
    }
    

    // 2B. Actualizar GUI: remover víctima
    controlPanel.removePhysicalPage(victim);

    // 2C. Transferir el frame físico
    nextPage.physical = victimPage.physical;

    // 2D. Actualizar GUI: agregar nueva página
    controlPanel.addPhysicalPage(nextPage.physical, replacePageNum);

    // 2E. Resetear completamente la página expulsada
    victimPage.inMemTime = 0;
    victimPage.lastTouchTime = 0;
    victimPage.R = 0;
    victimPage.M = 0;
    victimPage.age = 0;           // importante para AGING
    victimPage.physical = -1;     // fuera de memoria
  }
}