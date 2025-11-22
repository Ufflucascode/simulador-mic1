public class MicroInstrucao {
    public int nextAddress; // 9 bits: Próximo endereço na Control Store
    public int jam;         // 3 bits: JMPC(1), JAMN(2), JAMZ(4)
    public int alu;         // 8 bits: SLL8, SRA1, F0, F1, ENA, ENB, INVA, INC
    public int cBus;        // 9 bits: Máscara de quem recebe dados (H, OPC, TOS, CPP, LV, SP, PC, MDR, MAR)
    public int mem;         // 3 bits: Read, Write, Fetch
    public int bBus;        // 4 bits: Quem coloca dados no bus B (MDR, PC, MBR, etc.)

    public MicroInstrucao(int nextAddress, int jam, int alu, int cBus, int mem, int bBus) {
        this.nextAddress = nextAddress;
        this.jam = jam;
        this.alu = alu;
        this.cBus = cBus;
        this.mem = mem;
        this.bBus = bBus;
    }
    

    public static final int JAMZ = 4, JAMN = 2, JMPC = 1;
    public static final int WRITE = 2, READ = 1, FETCH = 4;
}