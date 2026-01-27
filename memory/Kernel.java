import java.lang.Thread;
import java.io.*;
import java.util.*;

public class Kernel extends Thread
{
  private static int virtPageNum = 63;
  private String output = null;
  private static final String lineSeparator = System.getProperty("line.separator");
  private String command_file;
  private String config_file;
  private ControlPanel controlPanel ;
  private Vector memVector = new Vector();
  private Vector instructVector = new Vector();
  private String status;
  private boolean doStdoutLog = false;
  private boolean doFileLog = false;
  public int runs;
  public int runcycles;
  public long block = (int) Math.pow(2,12); 
  public static byte addressradix = 10;

  public void init( String commands , String config )  
  {
    File f = new File( commands );
    command_file = commands;
    config_file = config;
    String line;
    String tmp = null;
    String command = "";
    byte R = 0;
    byte M = 0;
    int i = 0;
    int j = 0;
    int id = 0;
    int physical = 0;
    int physical_count = 0;
    int inMemTime = 0;
    int lastTouchTime = 0;
    int map_count = 0;
    double power = 14;
    long high = 0;
    long low = 0;
    long addr = 0;
    long address_limit = (block * virtPageNum+1)-1;
  
    if ( config != null )
    {
      f = new File ( config );
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("numpages")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              virtPageNum = Common.s2i(st.nextToken()) - 1;
              if ( virtPageNum < 2 || virtPageNum > 63 )
              {
                System.out.println("MemoryManagement: numpages out of bounds.");
                System.exit(-1);
              }
              address_limit = (block * virtPageNum+1)-1;
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
      for (i = 0; i <= virtPageNum; i++) 
      {
        high = (block * (i + 1))-1;
        low = block * i;
        memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
      }
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("memset")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            while (st.hasMoreTokens()) 
            { 
              id = Common.s2i(st.nextToken());
              tmp = st.nextToken();
              if (tmp.startsWith("x")) 
              {
                physical = -1;
              } 
              else 
              {
                physical = Common.s2i(tmp);
              }
              if ((0 > id || id > virtPageNum) || (-1 > physical || physical > ((virtPageNum - 1) / 2)))
              {
                System.out.println("MemoryManagement: Invalid page value in " + config);
                System.exit(-1);
              }
              R = Common.s2b(st.nextToken());
              M = Common.s2b(st.nextToken());
              inMemTime = Common.s2i(st.nextToken());
              lastTouchTime = Common.s2i(st.nextToken());
              
              Page page = (Page) memVector.elementAt(id);
              page.physical = physical;
              page.R = R;
              page.M = M;
              page.inMemTime = inMemTime;
              page.lastTouchTime = lastTouchTime;
            }
          }
          if (line.startsWith("enable_logging")) 
          { 
             // ... (código logging igual) ...
             StringTokenizer st = new StringTokenizer(line);
             while (st.hasMoreTokens()) {
                if ( st.nextToken().startsWith( "true" ) ) doStdoutLog = true;
             }
          }
          if (line.startsWith("log_file")) 
          { 
             // ... (código log_file igual) ...
             StringTokenizer st = new StringTokenizer(line);
             while (st.hasMoreTokens()) tmp = st.nextToken();
             if ( tmp.startsWith( "log_file" ) ) { doFileLog = false; output = "tracefile"; }               
             else { doFileLog = true; doStdoutLog = false; output = tmp; }
          }
          if (line.startsWith("pagesize")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              if ( tmp.startsWith( "power" ) )
              {
                power = (double) Integer.parseInt(st.nextToken());
                block = (int) Math.pow(2,power);
              }
              else
              {
                block = Long.parseLong(tmp,10);             
              }
              address_limit = (block * virtPageNum+1)-1;
            }
            // ... (validación pagesize igual) ...
             for (i = 0; i <= virtPageNum; i++) {
              Page page = (Page) memVector.elementAt(i);
              page.high = (block * (i + 1))-1;
              page.low = block * i;
            }
          }
          if (line.startsWith("addressradix")) 
          { 
             // ... (validación radix igual) ...
             StringTokenizer st = new StringTokenizer(line);
             while (st.hasMoreTokens()) {
               tmp = st.nextToken(); tmp = st.nextToken(); addressradix = Byte.parseByte(tmp);
             }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
    }
    
    // --- LECTURA DE COMANDOS ---
    f = new File ( commands );
    try 
    {
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      while ((line = in.readLine()) != null) 
      {
        if (line.startsWith("READ") || line.startsWith("WRITE")) 
        {
          if (line.startsWith("READ")) command = "READ";
          if (line.startsWith("WRITE")) command = "WRITE";
          
          StringTokenizer st = new StringTokenizer(line);
          tmp = st.nextToken(); // COMANDO
          tmp = st.nextToken(); // "hex", "bin", etc, o numero

          long startAddr = 0;
          long endAddr = 0;

          if (tmp.startsWith("random")) 
          {
            instructVector.addElement(new Instruction(command,Common.randomLong( address_limit )));
          } 
          else 
          { 
            int radix = 10;
            String token = "";

            if ( tmp.startsWith( "bin" ) ) radix = 2;
            else if ( tmp.startsWith( "oct" ) ) radix = 8;
            else if ( tmp.startsWith( "hex" ) ) radix = 16;
            
            if (radix != 10) {
                token = st.nextToken(); 
            } else {
                token = tmp; 
            }

            if (token.contains("-")) {
                String[] parts = token.split("-");
                startAddr = Long.parseLong(parts[0], radix);
                endAddr = Long.parseLong(parts[1], radix);
                
                // --- VALIDACIÓN DE TAMAÑO (NUEVO) ---
                // Verifica que el rango no sea mayor al tamaño de una página (block)
                long rangeSize = endAddr - startAddr;
                if (rangeSize >= block) { // Si block es 4096, max dif permitida es 4095
                    System.out.println("MemoryManagement Error: El rango " + token + " excede el tamano de pagina (" + block + " bytes).");
                    System.exit(-1);
                }
                // ------------------------------------

                command = command + "-" + endAddr; 
                addr = startAddr;
            } else {
                addr = Long.parseLong(token, radix);
            }

            if (0 > addr || addr > address_limit)
            {
              System.out.println("MemoryManagement: " + addr + ", Address out of range in " + commands);
              System.exit(-1);
            }
            instructVector.addElement(new Instruction(command,addr));
          }
        }
      }
      in.close();
    } catch (IOException e) { /* Handle exceptions */ }
    
    // ... (resto de init igual: runcycles, map_count, controlPanel setup) ...
    runcycles = instructVector.size();
    if ( runcycles < 1 ) { System.out.println("Error: no instructions"); System.exit(-1); }
    if ( doFileLog ) { File trace = new File(output); trace.delete(); }
    runs = 0;
    // ... (map_count logic) ...
    for (i = 0; i < virtPageNum; i++) {
      Page page = (Page) memVector.elementAt(i);
      if ( page.physical != -1 ) map_count++;
      // ... (duplicate check) ...
      if ( map_count < ( virtPageNum +1 ) / 2 && page.physical == -1) { page.physical = i; map_count++; }
    }
    // ... (GUI update loop) ...
    for (i = 0; i < virtPageNum; i++) {
        Page page = (Page) memVector.elementAt(i);
        if(page.physical == -1) controlPanel.removePhysicalPage( i );
        else controlPanel.addPhysicalPage( i , page.physical );
    }
  } 

  // ... (setControlPanel, getPage, printLogFile, run IGUALES) ...
  public void setControlPanel(ControlPanel newControlPanel) { controlPanel = newControlPanel ; }
  public void getPage(int pageNum) { Page page = ( Page ) memVector.elementAt( pageNum ); controlPanel.paintPage( page ); }
  private void printLogFile(String message) { /* ... codigo igual ... */ }
  public void run() { step(); while (runs != runcycles) { try { Thread.sleep(2000); } catch(InterruptedException e) {} step(); } }

  // --- STEP MODIFICADO PARA RANGOS ---
  public void step()
  {
    Instruction instruct = ( Instruction ) instructVector.elementAt( runs );
    String fullCmd = instruct.inst;
    String realCmd = fullCmd;
    long startAddr = instruct.addr;
    long endAddr = startAddr; 

    if (fullCmd.contains("-")) {
        String[] parts = fullCmd.split("-");
        realCmd = parts[0];
        endAddr = Long.parseLong(parts[1]);
    }

    controlPanel.instructionValueLabel.setText( realCmd );
    if(startAddr != endAddr) 
        controlPanel.addressValueLabel.setText( Long.toString(startAddr, addressradix) + "-" + Long.toString(endAddr, addressradix) );
    else 
        controlPanel.addressValueLabel.setText( Long.toString(startAddr, addressradix) );

    long segmentSize = block / 4;
    TreeMap<Integer, TreeSet<Integer>> touched = new TreeMap<Integer, TreeSet<Integer>>();

    long current = startAddr;
    while (current <= endAddr) {
        int p = Virtual2Physical.pageNum(current, virtPageNum, block);
        long offset = current % block;
        int s = (int) (offset / segmentSize);

        if (!touched.containsKey(p)) touched.put(p, new TreeSet<Integer>());
        touched.get(p).add(s);

        Page page = (Page) memVector.elementAt(p);
        
        if (page.physical == -1) {
             if ( doFileLog ) printLogFile( realCmd + " " + Long.toString(current, addressradix) + " ... page fault" );
             if ( doStdoutLog ) System.out.println( realCmd + " " + Long.toString(current, addressradix) + " ... page fault" );
             PageFault.replacePage( memVector , virtPageNum , p , controlPanel );
             controlPanel.pageFaultValueLabel.setText( "YES" );
        }

        if (realCmd.startsWith("READ")) { page.R = 1; page.segR[s] = 1; } 
        else { page.M = 1; page.segM[s] = 1; }
        page.lastTouchTime = 0;

        long nextBoundary = (current - offset) + (s + 1) * segmentSize;
        if (nextBoundary > endAddr && current != endAddr) current = endAddr; 
        else current = nextBoundary; 
        
        if (current == nextBoundary && current <= endAddr) {} else { current++; }
    }

    String resultStr = "Resultado ";
    for (Map.Entry<Integer, TreeSet<Integer>> entry : touched.entrySet()) {
        int p = entry.getKey();
        resultStr += "(P" + p + ", ";
        int count = 0;
        for(Integer s : entry.getValue()) {
            resultStr += "S" + s;
            if (++count < entry.getValue().size()) resultStr += ",";
        }
        resultStr += ") ";
    }

    String logMsg = realCmd + " ";
    if (addressradix == 16) logMsg += "hex ";
    logMsg += Long.toString(startAddr, addressradix);
    if (startAddr != endAddr) logMsg += "-" + Long.toString(endAddr, addressradix);
    logMsg += " ... " + resultStr;

    if ( doFileLog ) printLogFile( logMsg );
    if ( doStdoutLog ) System.out.println( logMsg );

    if ( controlPanel.pageFaultValueLabel.getText() != "YES" ) controlPanel.pageFaultValueLabel.setText("NO");
    
    for ( int k = 0; k < virtPageNum; k++ ) 
    {
      Page p = ( Page ) memVector.elementAt( k );
      if ( p.R == 1 && p.lastTouchTime == 10 ) p.R = 0;
      if ( p.physical != -1 ) { p.inMemTime += 10; p.lastTouchTime += 10; }
    }
    runs++;
    controlPanel.timeValueLabel.setText( Integer.toString( runs*10 ) + " (ns)" );
  }

  public void reset() {
    memVector.removeAllElements(); instructVector.removeAllElements();
    controlPanel.statusValueLabel.setText( "STOP" ); controlPanel.timeValueLabel.setText( "0" );
    controlPanel.instructionValueLabel.setText( "NONE" ); controlPanel.addressValueLabel.setText( "NULL" );
    controlPanel.pageFaultValueLabel.setText( "NO" ); controlPanel.virtualPageValueLabel.setText( "x" );
    controlPanel.physicalPageValueLabel.setText( "0" ); controlPanel.RValueLabel.setText( "0" );
    controlPanel.MValueLabel.setText( "0" ); controlPanel.inMemTimeValueLabel.setText( "0" );
    controlPanel.lastTouchTimeValueLabel.setText( "0" ); controlPanel.lowValueLabel.setText( "0" );
    controlPanel.highValueLabel.setText( "0" );
    init( command_file , config_file );
  }
}