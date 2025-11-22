import java.util.HashMap;
import java.util.Map;

public class Mic1Simulador {
    public Registradores reg = new Registradores();
    public MicroInstrucao[] controlStore = new MicroInstrucao[512];
    public Map<Integer, Integer> memoria = new HashMap<>();
    
    // Para depuração visual: guarda o texto original de cada linha da Control Store
    public String[] codigoFonteMicro = new String[512];

    // Constantes B-Bus
    public static final int B_MDR=0, B_PC=1, B_MBR=2, B_MBRU=3, B_SP=4, B_LV=5, B_CPP=6, B_TOS=7, B_OPC=8;
    // Constantes C-Bus
    public static final int C_H=1, C_OPC=2, C_TOS=4, C_CPP=8, C_LV=16, C_SP=32, C_PC=64, C_MDR=128, C_MAR=256, C_AC=512; 

    public Mic1Simulador() {
        // Inicia vazio, o Main vai carregar o código padrão
    }

    // --- PARSER DE MICROCODIGO (O NOVO RECURSO) ---
    public void compilarMicrocodigo(String texto) throws Exception {
        // Limpa a Control Store
        for(int i=0; i<512; i++) { 
            controlStore[i] = null; 
            codigoFonteMicro[i] = "";
        }

        String[] linhas = texto.split("\n");
        
        // Passagem 1: Mapear Rótulos (Labels) -> Endereços (Não implementado neste exemplo simples, usaremos endereços explícitos '0:')
        // Passagem 2: Parsear linha a linha
        
        for(String linha : linhas) {
            linha = linha.trim();
            if(linha.isEmpty() || linha.startsWith("//")) continue;
            
            // Formato esperado: "ENDERECO: DESTINOS = OPERACAO; MEMORIA; GOTO PROXIMO"
            // Ex: "0: MAR = PC; rd; goto 1"
            
            try {
                // 1. Separar Endereço
                String[] partesDoisPontos = linha.split(":");
                int endereco = Integer.parseInt(partesDoisPontos[0].trim());
                String resto = partesDoisPontos[1].trim();
                
                codigoFonteMicro[endereco] = linha; // Guarda texto para mostrar na tela depois

                // 2. Separar por ponto e vírgula (Atribuição ; Memória ; Pulo)
                String[] comandos = resto.split(";");
                
                int cBus = 0;
                int bBus = 0; // Default MDR
                int alu = 0;  // Default A
                int mem = 0;
                int next = 0;
                int jam = 0;
                
                // Analisar cada parte do comando
                for(String cmd : comandos) {
                    cmd = cmd.trim().toUpperCase();
                    
                    // -- Lógica de GOTO (Next Address) --
                    if(cmd.startsWith("GOTO")) {
                        String dest = cmd.replace("GOTO", "").trim();
                        if(dest.equals("(MBR)")) {
                            jam = MicroInstrucao.JMPC; // Pulo dinâmico
                            next = 0;
                        } else {
                            next = Integer.parseInt(dest);
                        }
                    }
                    // -- Lógica de Memória --
                    else if(cmd.equals("RD")) mem |= MicroInstrucao.READ;
                    else if(cmd.equals("WR")) mem |= MicroInstrucao.WRITE;
                    else if(cmd.equals("FETCH")) mem |= MicroInstrucao.FETCH;
                    
                    // -- Lógica de ALU e Atribuição (=) --
                    else if(cmd.contains("=")) {
                        String[] assign = cmd.split("=");
                        String destinos = assign[0].trim();
                        String operacao = assign[1].trim();
                        
                        // Parse Destinos (C-Bus)
                        if(destinos.contains("H")) cBus |= C_H;
                        if(destinos.contains("OPC")) cBus |= C_OPC;
                        if(destinos.contains("TOS")) cBus |= C_TOS;
                        if(destinos.contains("CPP")) cBus |= C_CPP;
                        if(destinos.contains("LV")) cBus |= C_LV;
                        if(destinos.contains("SP")) cBus |= C_SP;
                        if(destinos.contains("PC")) cBus |= C_PC;
                        if(destinos.contains("MDR")) cBus |= C_MDR;
                        if(destinos.contains("MAR")) cBus |= C_MAR;
                        if(destinos.contains("AC")) cBus |= C_AC;
                        
                        // Parse Fonte/ALU
                        // Simplificação: Detecta qual registrador está na direita para definir o B-Bus
                        if(operacao.contains("MDR")) bBus = B_MDR;
                        else if(operacao.contains("PC")) bBus = B_PC;
                        else if(operacao.contains("MBR")) bBus = B_MBR;
                        else if(operacao.contains("SP")) bBus = B_SP;
                        else if(operacao.contains("LV")) bBus = B_LV;
                        else if(operacao.contains("CPP")) bBus = B_CPP;
                        else if(operacao.contains("TOS")) bBus = B_TOS;
                        else if(operacao.contains("OPC")) bBus = B_OPC;
                        
                        // Parse Operação ALU
                        if(operacao.equals("A")) alu = 0;
                        else if(operacao.equals("B") || operacao.equals("MDR") || operacao.equals("PC") || operacao.equals("MBR")) alu = 1;
                        else if(operacao.contains("+") && operacao.contains("1")) alu = 7; // B+1 (simplificado)
                        else if(operacao.contains("+")) alu = 4; // A+B
                        else if(operacao.contains("BAND")) alu = 11; // A & B
                        else if(operacao.contains("INV")) alu = 2; // ~A (Inverso)
                    }
                }
                
                controlStore[endereco] = new MicroInstrucao(next, jam, alu, cBus, mem, bBus);
                
            } catch (Exception e) {
                System.out.println("Erro ao compilar linha: " + linha + " -> " + e.getMessage());
            }
        }
    }

    // Executa um passo
    public void step() {
        MicroInstrucao ui = controlStore[reg.MPC];
        if(ui == null) {
            // Se não houver instrução, tenta resetar ou parar
            return;
        }

        // 1. Executar ALU
        int valA = reg.H;
        int valB = 0;
        switch(ui.bBus) {
            case B_MDR: valB = reg.MDR; break;
            case B_PC:  valB = reg.PC; break;
            case B_MBR: valB = reg.MBR; break;
            case B_SP:  valB = reg.SP; break;
            // ... outros ...
        }
        
        // Hack visual para o MAC-1
        if (ui.bBus == B_MBR && (ui.cBus & C_MDR) != 0) valB = reg.AC; // Se for salvar AC, usa valor AC

        int result = 0;
        switch(ui.alu) {
            case 0: result = valA; break;
            case 1: result = valB; break;
            case 2: result = ~valA; break;
            case 4: result = valA + valB; break;
            case 7: result = valB + 1; break;
            case 11: result = valA & valB; break;
        }
        
        reg.Z = (result == 0);
        reg.N = (result < 0);

        // 2. Escrever C-Bus
        if((ui.cBus & C_MAR) != 0) reg.MAR = result;
        if((ui.cBus & C_MDR) != 0) reg.MDR = result;
        if((ui.cBus & C_PC)  != 0) reg.PC  = result;
        if((ui.cBus & C_H)   != 0) reg.H   = result;
        if((ui.cBus & C_AC)  != 0) reg.AC  = result;
        if((ui.cBus & C_SP)  != 0) reg.SP  = result;

        // 3. Memória
        if((ui.mem & MicroInstrucao.READ) != 0) {
            reg.MDR = memoria.getOrDefault(reg.MAR, 0);
            reg.MBR = reg.MDR; // No MAC-1 simples, MBR recebe dados da leitura
        }
        if((ui.mem & MicroInstrucao.WRITE) != 0) {
            memoria.put(reg.MAR, reg.MDR);
        }

        // 4. Próximo Endereço
        int next = ui.nextAddress;
        if((ui.jam & MicroInstrucao.JMPC) != 0) {
            int opcode = (reg.MBR >> 12) & 0xF;
            // Mapeamento fixo para os exemplos
            if(opcode == 0) next = 10;      // LODD
            else if(opcode == 1) next = 20; // STOD
            else if(opcode == 2) next = 30; // ADDD
            else if(opcode == 3) next = 40; // SUBD (Adicione esta linha!)
            else if(opcode == 6) next = 60; // JUMP
        }
        reg.MPC = next;
    }
    
    public void carregarPrograma(int[] codigoMaquina) {
        for(int i=0; i<codigoMaquina.length; i++) memoria.put(i, codigoMaquina[i]);
    }
}