import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends JFrame {
    private Mic1Simulador simulador;
    private PainelDatapath painelVisual;
    private JTextArea areaMicrocodigo;
    private JTextArea areaAssembly;
    private JTextArea areaLog;
    private JButton btnStep, btnRunStop, btnReset;
    private JSlider sliderSpeed;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;

    public Main() {
        super("Simulador MIC-1/MAC-1 Profissional - Grupo 6");
        
        simulador = new Mic1Simulador();
        painelVisual = new PainelDatapath(simulador);
        
        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.add(splitMain, BorderLayout.CENTER);

        JTabbedPane abasEditores = new JTabbedPane();
        
        JPanel painelMicro = new JPanel(new BorderLayout());
        String codigoDefault = getMicrocodigoDefault();
        areaMicrocodigo = new JTextArea(codigoDefault);
        areaMicrocodigo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        painelMicro.add(new JScrollPane(areaMicrocodigo), BorderLayout.CENTER);
        JButton btnCompilarMicro = new JButton("1. Compilar Microcodigo (Hardware)");
        painelMicro.add(btnCompilarMicro, BorderLayout.SOUTH);
        abasEditores.addTab("Microcodigo (Controle MIC-1)", null, painelMicro, "Define o comportamento do hardware");

        JPanel painelAssembly = new JPanel(new BorderLayout());
        
        // --- EXEMPLO COMPLETO: SOMA REGRESSIVA (LOOP) ---
        String assemblyDefault = 
            "// Programa: Soma Regressiva (Loop)\n" +
            "// Objetivo: Somar 5 + 4 + 3 + 2 + 1\n" +
            "// Resultado esperado no endereco 500: 15\n" +
            "\n" +
            "// --- INICIALIZACAO ---\n" +
            "LODD 501   // Carrega o contador (5) no AC\n" +
            "JZER 10    // Se for zero, pula para o fim (linha 10)\n" +
            "\n" +
            "// --- LOOP ---\n" +
            "LODD 500   // Carrega o total atual\n" +
            "ADDD 501   // Soma o contador ao total\n" +
            "STOD 500   // Salva o novo total\n" +
            "\n" +
            "LODD 501   // Carrega o contador novamente\n" +
            "SUBD 502   // Subtrai 1 do contador\n" +
            "STOD 501   // Salva o novo contador\n" +
            "\n" +
            "JUMP 1     // Volta para o teste (JZER) no inicio\n" +
            "\n" +
            "// --- FIM ---\n" +
            "JUMP 10    // Loop infinito final (HALT)\n" +
            "\n" +
            "// --- DADOS ---\n" +
            "DW 500 0   // Variavel 'Total' (Inicia com 0)\n" +
            "DW 501 5   // Variavel 'Contador' (Inicia com 5)\n" +
            "DW 502 1   // Constante '1' (Para subtrair)";
            
        areaAssembly = new JTextArea(assemblyDefault);
        areaAssembly.setFont(new Font("Monospaced", Font.PLAIN, 14));
        areaAssembly.setForeground(new Color(0, 0, 100));
        painelAssembly.add(new JScrollPane(areaAssembly), BorderLayout.CENTER);
        
        JPanel panelBotoesAssembly = new JPanel(new GridLayout(1, 2));
        JButton btnCarregarAssembly = new JButton("2. Montar & Carregar RAM");
        panelBotoesAssembly.add(btnCarregarAssembly);
        painelAssembly.add(panelBotoesAssembly, BorderLayout.SOUTH);
        
        abasEditores.addTab("Programa (Assembly MAC-1)", null, painelAssembly, "Escreva o programa do usuario");
        
        splitMain.setLeftComponent(abasEditores);

        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.add(painelVisual, BorderLayout.CENTER);
        
        JPanel controles = new JPanel(new FlowLayout());
        btnReset = new JButton("Reiniciar");
        btnStep = new JButton("Passo");
        btnRunStop = new JButton("Executar");
        
        sliderSpeed = new JSlider(100, 1500, 500);
        controles.add(new JLabel("Atraso (ms):"));
        controles.add(sliderSpeed);
        controles.add(btnReset);
        controles.add(btnStep);
        controles.add(btnRunStop);
        
        areaLog = new JTextArea(4, 40);
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        painelDireito.add(controles, BorderLayout.NORTH);
        painelDireito.add(new JScrollPane(areaLog), BorderLayout.SOUTH);
        
        splitMain.setRightComponent(painelDireito);
        splitMain.setDividerLocation(400);

        compilarMicrocodigo(); 
        
        btnCompilarMicro.addActionListener(e -> compilarMicrocodigo());
        
        btnCarregarAssembly.addActionListener(e -> {
            try {
                int[] obj = Montador.montar(areaAssembly.getText());
                simulador.carregarPrograma(obj);
                areaLog.append("\nPrograma montado e carregado (Dados via DW incluidos).");
                painelVisual.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro no Assembly.");
            }
        });

        btnStep.addActionListener(e -> step());
        btnRunStop.addActionListener(e -> toggleRun());
        btnReset.addActionListener(e -> {
            stopRunning();
            simulador.reg.reset();
            simulador.cache.reset();
            areaLog.setText("Sistema Zerado. Clique em 'Montar' para recarregar o programa.");
            painelVisual.repaint();
        });

        this.setSize(1200, 750);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void compilarMicrocodigo() {
        try {
            simulador.compilarMicrocodigo(areaMicrocodigo.getText());
            areaLog.setText("Microcodigo compilado.");
            simulador.reg.MPC = 0;
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getMicrocodigoDefault() {
        return 
            "// --- Busca ---\n0: MAR = PC; rd; goto 1\n1: PC = PC + 1; rd; goto 2\n2: H = MBR; goto (MBR)\n\n" +
            "// --- LODD ---\n10: MAR = MBR; rd; goto 11\n11: rd; goto 12\n12: AC = MDR; goto 0\n\n" +
            "// --- STOD ---\n20: MAR = MBR; goto 21\n21: MDR = AC; wr; goto 22\n22: wr; goto 0\n\n" +
            "// --- ADDD ---\n30: MAR = MBR; rd; goto 31\n31: rd; goto 32\n32: H = AC; goto 33\n33: AC = H + MDR; goto 0\n\n" +
            "// --- SUBD ---\n40: MAR = MBR; rd; goto 41\n41: rd; goto 42\n42: H = AC; goto 43\n43: AC = H + INV(MDR); goto 44\n44: AC = AC + 1; goto 0\n\n" +
            "// --- JUMP ---\n60: PC = MBR; goto 0\n\n" +
            "// --- JZER (Se AC=0 pula, senao continua) ---\n" +
            "50: MAR = MBR; rd; goto 51 // Busca endereco de pulo\n" +
            "51: rd; goto 52\n" +
            "52: H = AC; goto 53 // Testa AC\n" +
            "53: H = H; if Z then goto 60 else goto 0 // Se Zero, vai pro JUMP(60), senao fetch(0)";
    }

    private void step() {
        String instr = simulador.codigoFonteMicro[simulador.reg.MPC];
        if(instr == null) instr = "Aguardando/NOP";
        areaLog.setText("MPC: " + simulador.reg.MPC + " | " + instr + "\nCache: " + simulador.cache.ultimoStatus);
        simulador.step();
        painelVisual.repaint();
    }

    private void toggleRun() {
        if(isRunning) stopRunning(); else startRunning();
    }

    private void startRunning() {
        isRunning = true;
        btnRunStop.setText("Parar");
        btnStep.setEnabled(false);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::step), 
            0, sliderSpeed.getValue(), TimeUnit.MILLISECONDS);
    }

    private void stopRunning() {
        if(scheduler != null) scheduler.shutdownNow();
        isRunning = false;
        btnRunStop.setText("Executar");
        btnStep.setEnabled(true);
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(Main::new); }
}