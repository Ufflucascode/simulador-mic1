import javax.swing.*;
import java.awt.*;

public class PainelDatapath extends JPanel {
    private Mic1Simulador sim;

    public PainelDatapath(Mic1Simulador sim) {
        this.sim = sim;
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Configurações visuais
        g2.setStroke(new BasicStroke(2));
        
        // --- Layout no estilo "U" do Data Path ---
        
        int xBase = 100;
        int yTop = 50;
        
        // 1. Registradores Superiores
        desenharRegistrador(g2, "PC", sim.reg.PC, xBase, yTop);
        desenharRegistrador(g2, "SP", sim.reg.SP, xBase + 100, yTop);
        desenharRegistrador(g2, "MDR", sim.reg.MDR, xBase + 200, yTop);
        desenharRegistrador(g2, "MAR", sim.reg.MAR, xBase + 300, yTop);
        desenharRegistrador(g2, "AC", sim.reg.AC, xBase + 400, yTop); 

        // 2. ALU e Registradores Inferiores
        int yBottom = 400;
        desenharRegistrador(g2, "H", sim.reg.H, xBase + 150, yBottom - 100);
        
        // Desenho da ALU (Trapézio)
        g2.setColor(new Color(255, 200, 200));
        Polygon aluShape = new Polygon();
        aluShape.addPoint(xBase + 100, yBottom);
        aluShape.addPoint(xBase + 300, yBottom);
        aluShape.addPoint(xBase + 250, yBottom + 80);
        aluShape.addPoint(xBase + 150, yBottom + 80);
        
        g2.fillPolygon(aluShape);
        g2.setColor(Color.BLACK); // Verifique se esta linha tem o ponto e vírgula
        g2.drawPolygon(aluShape);
        
        g2.drawString("ALU", xBase + 190, yBottom + 40);
        
        // Flags
        g2.drawString("N: " + (sim.reg.N?"1":"0"), xBase + 320, yBottom + 20);
        g2.drawString("Z: " + (sim.reg.Z?"1":"0"), xBase + 320, yBottom + 40);

        // 3. Barramentos (Linhas)
        g2.setColor(Color.GRAY);
        // Barramento B (entrando na ALU pela direita/topo)
        g2.drawLine(xBase + 300, yTop + 40, xBase + 300, yBottom); 
        // Barramento C (saindo da ALU)
        g2.drawLine(xBase + 200, yBottom + 80, xBase + 200, yBottom + 120); // Saída ALU
        g2.drawLine(xBase - 20, yBottom + 120, xBase + 500, yBottom + 120); // Linha horizontal inferior
        
        // Informações de Controle
        g2.setColor(new Color(0, 0, 100));
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.drawString("MPC: " + sim.reg.MPC, 600, 50);
        g2.drawString("Instrução Atual (MAC-1)", 600, 100);
        
        // Evitar erro se MDR for negativo ou muito grande na decodificação visual
        int op = (sim.reg.MDR >> 12) & 0xF;
        String mnemonico = getMnemonico(op);
        g2.drawString(mnemonico + " (Op: "+op+")", 600, 120);
    }

    private void desenharRegistrador(Graphics2D g, String nome, int valor, int x, int y) {
        g.setColor(Color.CYAN);
        g.fillRect(x, y, 80, 40);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 80, 40);
        g.drawString(nome, x + 5, y + 15);
        g.drawString(String.valueOf(valor), x + 5, y + 35);
    }
    
    private String getMnemonico(int op) {
        switch(op) {
            case 0: return "LODD";
            case 1: return "STOD";
            case 2: return "ADDD";
            case 6: return "JUMP";
            default: return "???";
        }
    }
}