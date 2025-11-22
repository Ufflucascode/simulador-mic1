import java.util.HashMap;
import java.util.Map;

public class Montador {
    public static int[] montar(String codigoFonte) {
        String[] linhas = codigoFonte.split("\n");
        int[] memoria = new int[256];
        int end = 0;
        
        for(String linha : linhas) {
            linha = linha.trim().toUpperCase();
            // Ignora linhas vazias ou comentários
            if(linha.isEmpty() || linha.startsWith("//") || linha.startsWith(";")) continue;
            
            // Divide por espaços (ex: "LODD 5")
            String[] partes = linha.split("\\s+");
            if(partes.length == 0) continue;

            String mnemonico = partes[0];
            int arg = 0;
            
            // Se tiver argumento, tenta converter
            if(partes.length > 1) {
                try {
                    arg = Integer.parseInt(partes[1]);
                } catch (NumberFormatException e) {
                    arg = 0; // Se falhar, assume 0
                }
            }
            
            int opcode = 0;
            boolean instrucaoValida = true;

            // Mapeamento MAC-1 (Baseado no PDF)
            switch(mnemonico) {
                case "LODD": opcode = 0x0; break; // 0000
                case "STOD": opcode = 0x1; break; // 0001
                case "ADDD": opcode = 0x2; break; // 0010
                case "SUBD": opcode = 0x3; break; // 0011
                case "JPOS": opcode = 0x4; break; // 0100
                case "JZER": opcode = 0x5; break; // 0101
                case "JUMP": opcode = 0x6; break; // 0110
                case "LOCO": opcode = 0x7; break; // 0111
                case "LODL": opcode = 0x8; break; // 1000
                case "STOL": opcode = 0x9; break; // 1001
                case "ADDL": opcode = 0xA; break; // 1010
                case "SUBL": opcode = 0xB; break; // 1011
                case "JNEG": opcode = 0xC; break; // 1100
                case "JNZE": opcode = 0xD; break; // 1101
                case "CALL": opcode = 0xE; break; // 1110
                case "HALT": opcode = 0xF; break; // Usaremos F como Halt/Syscalls
                default: instrucaoValida = false;
            }
            
            if(instrucaoValida) {
                // Junta o Opcode (4 bits superiores) com o Argumento (12 bits inferiores)
                int instrucao = (opcode << 12) | (arg & 0xFFF);
                memoria[end++] = instrucao;
            }
        }
        return memoria;
    }
}