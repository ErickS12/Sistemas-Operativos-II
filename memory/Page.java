public class Page 
{
  public int id;
  public int physical;
  public byte R;
  public byte M;
  public int inMemTime;
  public int lastTouchTime;
  public long high;
  public long low;
  // --- NUEVOS CAMPOS PARA SEGMENTOS ---
  public byte[] segR = new byte[4]; // Bit R para cada uno de los 4 segmentos
  public byte[] segM = new byte[4]; // Bit M para cada uno de los 4 segmentos
  // ------------------------------------
  // --- VARIABLE PARA ENVEJECIMIENTO ---
  public int age; // Registro de historial de 32 bits
  // -------------------------------------

  public Page( int id, int physical, byte R, byte M, int inMemTime, int lastTouchTime, long high, long low ) 
  {
    this.id = id;
    this.physical = physical;
    this.R = R;
    this.M = M;
    this.inMemTime = inMemTime;
    this.lastTouchTime = lastTouchTime;
    this.high = high;
    this.low = low;
    // Inicializar bits de segmentos en 0
    for(int i = 0; i < 4; i++) {
        this.segR[i] = 0;
        this.segM[i] = 0;
    }
  } 	
}
