import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Classe Principal com Interface Gráfica e Editor de Microcódigo.
 * Implementa modos de Clock Manual e Automático.
 */
public class Main extends JFrame {
    private Mic1Simulador simulador;
    private PainelDatapath painelVisual;
    private JTextArea areaMicrocodigo;
    private JTextArea areaLog;
    private JButton btnStep;
    private JButton btnRunStop;
    private JButton btnReset; // Declaração corrigida
    private JSlider sliderSpeed;

    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    private final int INITIAL_DELAY_MS = 500; // 0.5 segundo

    public Main() {
        super("Simulador MIC-1 Editável - Grupo 6");
        
        simulador = new Mic1Simulador();
        painelVisual = new PainelDatapath(simulador);
        
        // --- Layout Principal (Split Pane) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.add(splitPane, BorderLayout.CENTER);

        // --- Painel Esquerdo: Editor ---
        JPanel painelEditor = new JPanel(new BorderLayout());
        
        // Código Default (O Ciclo de Busca + Instruções Básicas)
        String codigoDefault = 
            "// --- Ciclo de Busca (FETCH) ---\n" +
            "0: MAR = PC; rd; goto 1\n" +
            "1: PC = PC + 1; rd; goto 2\n" +
            "2: H = MBR; goto (MBR)\n" +
            "\n" +
            "// --- LODD (Opcode 0x0) ---\n" +
            "10: MAR = MBR; rd; goto 11\n" +
            "11: rd; goto 12\n" +
            "12: AC = MDR; goto 0\n" +
            "\n" +
            "// --- STOD (Opcode 0x1) ---\n" +
            "20: MAR = MBR; goto 21\n" +
            "21: MDR = AC; wr; goto 22\n" +
            "22: wr; goto 0\n" +
            "\n" +
            "// --- ADDD (Opcode 0x2) ---\n" +
            "30: MAR = MBR; rd; goto 31\n" +
            "31: rd; goto 32\n" +
            "32: H = AC; goto 33\n" +
            "33: AC = H + MDR; goto 0\n" +
            "\n" +
            "// --- SUBD (Opcode 0x3) --- Requer a lógica INV(MDR)\n" +
            "40: MAR = MBR; rd; goto 41\n" +
            "41: rd; goto 42\n" +
            "42: H = AC; goto 43\n" +
            "43: AC = H + INV(MDR); goto 44\n" +
            "44: AC = AC + 1; goto 0\n" +
            "\n" +
            "// --- JUMP (Opcode 0x6) ---\n" +
            "60: PC = MBR; goto 0";

        areaMicrocodigo = new JTextArea(codigoDefault);
        areaMicrocodigo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollEditor = new JScrollPane(areaMicrocodigo);
        painelEditor.add(new JLabel(" Editor de Microcódigo (MIC-1):"), BorderLayout.NORTH);
        painelEditor.add(scrollEditor, BorderLayout.CENTER);
        
        JButton btnCompilar = new JButton("Compilar & Carregar Microcódigo");
        painelEditor.add(btnCompilar, BorderLayout.SOUTH);
        
        splitPane.setLeftComponent(painelEditor);

        // --- Painel Direito: Visualização + Controles ---
        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.add(painelVisual, BorderLayout.CENTER);
        
        // Controles de Clock - INICIALIZAÇÃO CORRIGIDA
        JPanel controles = new JPanel(new FlowLayout());
        btnReset = new JButton("Reset"); // Inicialização correta
        btnStep = new JButton("Step (Manual)"); // Inicialização correta
        btnRunStop = new JButton("Run (Automático)"); // Inicialização correta
        
        // Slider para Velocidade
        sliderSpeed = new JSlider(JSlider.HORIZONTAL, 100, 1500, INITIAL_DELAY_MS);
        sliderSpeed.setMajorTickSpacing(500);
        sliderSpeed.setMinorTickSpacing(100);
        sliderSpeed.setPaintTicks(true);
        sliderSpeed.setPaintLabels(true);
        controles.add(new JLabel("Velocidade (ms):"));
        controles.add(sliderSpeed);
        
        controles.add(btnReset);
        controles.add(btnStep);
        controles.add(btnRunStop);
        
        areaLog = new JTextArea(3, 40);
        areaLog.setEditable(false);
        
        painelDireito.add(controles, BorderLayout.NORTH);
        painelDireito.add(new JScrollPane(areaLog), BorderLayout.SOUTH);
        
        splitPane.setRightComponent(painelDireito);
        splitPane.setDividerLocation(350);

        // --- Inicialização ---
        compilarCodigo(); // Compila o default ao abrir
        carregarProgramaExemplo();

        // --- Ações ---
        btnCompilar.addActionListener(e -> compilarCodigo());
        
        // Modo Manual (Single Step)
        btnStep.addActionListener(e -> stepSimulador());

        // Modo Automático (Run/Stop)
        btnRunStop.addActionListener(e -> toggleRunStop());
        
        // Ação do Reset (usa a variável de instância btnReset)
        btnReset.addActionListener(e -> {
            stopRunning();
            simulador.reg.reset();
            carregarProgramaExemplo(); 
            painelVisual.repaint();
            areaLog.setText("Resetado. Pronto para novo ciclo.");
        });

        this.setSize(1000, 700);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void stepSimulador() {
        // Atualiza log antes de executar
        String instrucaoAtual = simulador.codigoFonteMicro[simulador.reg.MPC];
        if(instrucaoAtual == null) instrucaoAtual = "NOP / Desconhecido";
        
        areaLog.setText("Executando MPC " + simulador.reg.MPC + ": " + instrucaoAtual);
        
        simulador.step();
        painelVisual.repaint();
    }
    
    private void toggleRunStop() {
        if (isRunning) {
            stopRunning();
        } else {
            startRunning();
        }
    }
    
    private void startRunning() {
        isRunning = true;
        btnRunStop.setText("Stop");
        btnStep.setEnabled(false); // Desativa manual no modo automático
        
        // Define o delay baseado no Slider
        int delay = sliderSpeed.getValue(); 
        
        // Usa um Scheduler para executar a tarefa repetidamente
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            // Este bloco executa a cada 'delay' milissegundos
            SwingUtilities.invokeLater(() -> {
                stepSimulador();
            });
        }, 0, delay, TimeUnit.MILLISECONDS);
    }
    
    private void stopRunning() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        isRunning = false;
        btnRunStop.setText("Run (Automático)");
        btnStep.setEnabled(true);
        areaLog.append("\nModo Automático Parado.");
    }


    private void compilarCodigo() {
        stopRunning(); // Para o modo automático se estiver rodando
        try {
            simulador.compilarMicrocodigo(areaMicrocodigo.getText());
            areaLog.setText("Microcódigo compilado com sucesso! MPC resetado para 0.");
            simulador.reg.MPC = 0; 
            painelVisual.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro no código: " + ex.getMessage());
        }
    }

    private void carregarProgramaExemplo() {
        // Programa MAC-1: LODD 5, SUBD 6, STOD 7
        int[] obj = Montador.montar("LODD 5\nSUBD 6\nSTOD 7");
        simulador.carregarPrograma(obj);
        
        // Dados na memória (10 - 4 = 6)
        simulador.memoria.put(5, 10); 
        simulador.memoria.put(6, 4);  
        simulador.memoria.put(7, 0);  
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
} 