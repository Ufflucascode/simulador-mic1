public class Registradores {
    // Registradores do Caminho de Dados (Data Path)
    public int MAR = 0; // Memory Address Register
    public int MDR = 0; // Memory Data Register
    public int PC  = 0; // Program Counter
    public int MBR = 0; // Memory Buffer Register 
    public int SP  = 0; // Stack Pointer
    public int LV  = 0; // Local Variable Pointer
    public int CPP = 0; // Constant Pool Pointer
    public int TOS = 0; // Top of Stack
    public int OPC = 0; // Old PC
    public int H   = 0; // Holding Register (Entrada A da ALU)
    
    // Acumulador (Para MAC-1 visual)
    public int AC = 0; 

    // Controle
    public int MPC = 0; // Micro Program Counter
    
    // Flags ALU
    public boolean N = false; // Negative
    public boolean Z = false; // Zero

    public void reset() {
        MAR = MDR = PC = MBR = SP = LV = CPP = TOS = OPC = H = MPC = AC = 0;
        N = Z = false;
    }
}