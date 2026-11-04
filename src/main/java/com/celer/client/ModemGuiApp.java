package com.celer.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ModemGuiApp extends JFrame {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private SerialModemDriver driver;
    private final VotingService votingService = new VotingService("OPCAO_A", "OPCAO_B");
    private final UssdService ussdService = new UssdService(2.00);
    private final RetryPolicy retryPolicy = new RetryPolicy(3, 500);

    private final JTextField portField = new JTextField("/tmp/ttyB", 12);
    private final JButton connectButton = new JButton("Conectar");
    private final JLabel connStatusLabel = new JLabel("● DESCONECTADO");

    private final JLabel fsmStatusLabel = new JLabel("Desconhecido");
    private final JLabel operatorLabel = new JLabel("---");
    private final JLabel signalLabel = new JLabel("CSQ: -- (--- dBm)");
    private final JProgressBar signalBar = new JProgressBar(0, 31);
    private final JLabel balanceLabel = new JLabel("--.-- EUR");

    private final JLabel votingTallyLabel = new JLabel("OPCAO_A: 0  |  OPCAO_B: 0  |  Total: 0");

    private final JTextField votePhoneField = new JTextField("+351912345678", 12);
    private final JComboBox<String> voteChoiceBox = new JComboBox<>(new String[]{"OPCAO_A", "OPCAO_B"});

    private final JTextField smsDestField = new JTextField("+351912345678", 10);
    private final JTextField smsMsgField = new JTextField("Mensagem de teste", 16);

    private final JTextField customCmdField = new JTextField(18);
    private final JTextArea consoleArea = new JTextArea();

    public ModemGuiApp(String defaultPort) {
        super("Cellular Modem Platform - Dashboard de Controlo");
        if (defaultPort != null && !defaultPort.isBlank()) {
            portField.setText(defaultPort);
        }
        initUi();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 720);
        setMinimumSize(new Dimension(850, 600));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainPanel.add(buildTopBar(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.add(buildKpiAndActionsPanel(), BorderLayout.NORTH);
        centerPanel.add(buildConsolePanel(), BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);

        setupListeners();
        updateConnectionState(false);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        bar.setBorder(BorderFactory.createEtchedBorder());

        bar.add(new JLabel("Porta Série Virtual:"));
        bar.add(portField);
        bar.add(connectButton);

        connStatusLabel.setFont(connStatusLabel.getFont().deriveFont(Font.BOLD));
        connStatusLabel.setForeground(Color.RED);
        bar.add(connStatusLabel);

        return bar;
    }

    private JPanel buildKpiAndActionsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(buildDiagnosticsPanel());
        panel.add(buildOperationsPanel());
        return panel;
    }

    private JPanel buildDiagnosticsPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 6, 6));
        panel.setBorder(new TitledBorder("Estado do Modem e Rede (Telemetria)"));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(new JLabel("Registo na Rede: "));
        fsmStatusLabel.setFont(fsmStatusLabel.getFont().deriveFont(Font.BOLD));
        p1.add(fsmStatusLabel);
        panel.add(p1);

        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p2.add(new JLabel("Operadora (PLMN): "));
        operatorLabel.setFont(operatorLabel.getFont().deriveFont(Font.BOLD));
        p2.add(operatorLabel);
        panel.add(p2);

        JPanel p3 = new JPanel(new BorderLayout(5, 0));
        p3.add(signalLabel, BorderLayout.WEST);
        signalBar.setStringPainted(true);
        signalBar.setValue(0);
        p3.add(signalBar, BorderLayout.CENTER);
        panel.add(p3);

        JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p4.add(new JLabel("Saldo Atual (USSD): "));
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(Font.BOLD));
        p4.add(balanceLabel);
        panel.add(p4);

        JPanel p5 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p5.add(new JLabel("VAS Votações: "));
        votingTallyLabel.setFont(votingTallyLabel.getFont().deriveFont(Font.BOLD));
        p5.add(votingTallyLabel);
        panel.add(p5);

        return panel;
    }

    private JPanel buildOperationsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 6, 6));
        panel.setBorder(new TitledBorder("Operações do Modem e Serviços"));

        JPanel pModem = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton btnUnlock = new JButton("Desbloquear PIN");
        JButton btnRefresh = new JButton("Verificar Rede");
        JButton btnBalance = new JButton("Consultar Saldo");
        btnUnlock.addActionListener(e -> executeAsync("AT+CPIN=0000"));
        btnRefresh.addActionListener(e -> refreshNetworkStatus());
        btnBalance.addActionListener(e -> executeUssdBalance());
        pModem.add(btnUnlock);
        pModem.add(btnRefresh);
        pModem.add(btnBalance);
        panel.add(pModem);

        JPanel pSmsCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton btnListSms = new JButton("Listar Mensagens");
        JButton btnClearSms = new JButton("Limpar SIM (CMGD=1)");
        btnListSms.addActionListener(e -> executeAsync("AT+CMGL=\"ALL\""));
        btnClearSms.addActionListener(e -> executeAsync("AT+CMGD=1"));
        pSmsCtrl.add(btnListSms);
        pSmsCtrl.add(btnClearSms);
        panel.add(pSmsCtrl);

        JPanel pVote = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pVote.add(new JLabel("Voto:"));
        pVote.add(votePhoneField);
        pVote.add(voteChoiceBox);
        JButton btnVote = new JButton("Injetar Voto");
        btnVote.addActionListener(e -> processInboundVote());
        pVote.add(btnVote);
        panel.add(pVote);

        JPanel pSendSms = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        pSendSms.add(new JLabel("SMS:"));
        pSendSms.add(smsDestField);
        pSendSms.add(smsMsgField);
        JButton btnSendSms = new JButton("Enviar");
        btnSendSms.addActionListener(e -> sendManualSms());
        pSendSms.add(btnSendSms);
        panel.add(pSendSms);

        return panel;
    }

    private JPanel buildConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("Consola de Tráfego AT e Respostas em Tempo Real"));

        consoleArea.setEditable(false);
        consoleArea.setBackground(new Color(24, 24, 24));
        consoleArea.setForeground(new Color(220, 220, 220));
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        panel.add(new JScrollPane(consoleArea), BorderLayout.CENTER);

        JPanel bottomCmd = new JPanel(new BorderLayout(5, 5));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.add(new JLabel("Comando Manual:"));
        left.add(customCmdField);
        JButton btnSendCmd = new JButton("Enviar");
        btnSendCmd.addActionListener(e -> sendCustomCommand());
        customCmdField.addActionListener(e -> sendCustomCommand());
        left.add(btnSendCmd);
        bottomCmd.add(left, BorderLayout.WEST);

        JButton btnClear = new JButton("Limpar Consola");
        btnClear.addActionListener(e -> consoleArea.setText(""));
        bottomCmd.add(btnClear, BorderLayout.EAST);

        panel.add(bottomCmd, BorderLayout.SOUTH);
        return panel;
    }

    private void setupListeners() {
        connectButton.addActionListener(e -> toggleConnection());
    }

    private void toggleConnection() {
        if (driver != null && driver.isOpen()) {
            disconnectDriver();
        } else {
            connectDriver();
        }
    }

    private void connectDriver() {
        String port = portField.getText().trim();
        try {
            driver = new SerialModemDriver(port);
            driver.open();
            updateConnectionState(true);
            logInfo("Ligado com sucesso à porta virtual " + port);
            executeAsync("AT");
            executeAsync("AT+CPIN?");
        } catch (Exception ex) {
            logInfo("Erro ao ligar a " + port + ": " + ex.getMessage());
            updateConnectionState(false);
        }
    }

    private void disconnectDriver() {
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception ignored) {}
        }
        updateConnectionState(false);
        logInfo("Porta série desconectada.");
    }

    private void updateConnectionState(boolean connected) {
        connectButton.setText(connected ? "Desconectar" : "Conectar");
        portField.setEnabled(!connected);
        if (connected) {
            connStatusLabel.setText("● CONECTADO");
            connStatusLabel.setForeground(new Color(0, 150, 0));
        } else {
            connStatusLabel.setText("● DESCONECTADO");
            connStatusLabel.setForeground(Color.RED);
            fsmStatusLabel.setText("Desconhecido");
            operatorLabel.setText("---");
            signalLabel.setText("CSQ: -- (--- dBm)");
            signalBar.setValue(0);
        }
    }

    private void executeAsync(String cmd) {
        if (driver == null || !driver.isOpen()) {
            logInfo("Erro: Porta não está aberta.");
            return;
        }
        worker.submit(() -> {
            logTx(cmd);
            CommandResult result = driver.execute(cmd);
            logRx(result.response(), result.latencyMs(), result.success());
            handleResponseSideEffects(cmd, result.response());
        });
    }

    private void refreshNetworkStatus() {
        worker.submit(() -> {
            executeAndInspect("AT+CREG?");
            executeAndInspect("AT+COPS?");
            executeAndInspect("AT+CSQ");
        });
    }

    private void executeUssdBalance() {
        worker.submit(() -> {
            CommandResult res = executeAndInspect("AT+CUSD=1,\"*#100#\",15");
            if (res.success()) {
                double bal = ussdService.parseBalance(res.response());
                SwingUtilities.invokeLater(() -> balanceLabel.setText(String.format("%.2f EUR", bal)));
            }
        });
    }

    private void processInboundVote() {
        String voter = votePhoneField.getText().trim();
        String choice = (String) voteChoiceBox.getSelectedItem();
        worker.submit(() -> {
            executeAndInspect("AT+INJECT=\"" + voter + "\",\"" + choice + "\"");
            executeAndInspect("AT+CMGL=\"ALL\"");
            executeAndInspect("AT+CMGR=1");

            String decision = votingService.processVote(voter, choice);
            logInfo("VAS Decisão de Voto [" + voter + "]: " + decision);

            executeAndInspect("AT+CMGD=1");

            CommandResult smsRes = retryPolicy.execute(() -> driver.sendSms(voter, decision));
            logInfo("Confirmação SMS enviada: " + smsRes.success() + " (" + smsRes.latencyMs() + "ms)");

            SwingUtilities.invokeLater(this::updateVotingDisplay);
        });
    }

    private void sendManualSms() {
        String dest = smsDestField.getText().trim();
        String msg = smsMsgField.getText().trim();
        worker.submit(() -> {
            logTx("AT+CMGS=\"" + dest + "\" -> " + msg);
            CommandResult res = retryPolicy.execute(() -> driver.sendSms(dest, msg));
            logRx(res.response(), res.latencyMs(), res.success());
        });
    }

    private void sendCustomCommand() {
        String cmd = customCmdField.getText().trim();
        if (!cmd.isEmpty()) {
            customCmdField.setText("");
            executeAsync(cmd);
        }
    }

    private CommandResult executeAndInspect(String cmd) {
        logTx(cmd);
        CommandResult result = driver.execute(cmd);
        logRx(result.response(), result.latencyMs(), result.success());
        handleResponseSideEffects(cmd, result.response());
        return result;
    }

    private void handleResponseSideEffects(String cmd, String resp) {
        SwingUtilities.invokeLater(() -> {
            if (cmd.startsWith("AT+CREG?")) {
                if (resp.contains("0,1")) {
                    fsmStatusLabel.setText("REGISTADO (HOME NETWORK)");
                    fsmStatusLabel.setForeground(new Color(0, 140, 0));
                } else if (resp.contains("0,2")) {
                    fsmStatusLabel.setText("À PROCURA DE REDE (SEARCHING)");
                    fsmStatusLabel.setForeground(new Color(200, 120, 0));
                } else {
                    fsmStatusLabel.setText(resp);
                }
            } else if (cmd.startsWith("AT+COPS?")) {
                if (resp.contains("\"")) {
                    int first = resp.indexOf('\"');
                    int last = resp.lastIndexOf('\"');
                    if (last > first) {
                        operatorLabel.setText(resp.substring(first + 1, last));
                    }
                }
            } else if (cmd.startsWith("AT+CSQ")) {
                parseCsq(resp);
            }
        });
    }

    private void parseCsq(String resp) {
        if (resp.contains("+CSQ:")) {
            try {
                String sub = resp.substring(resp.indexOf("+CSQ:") + 5).trim();
                String rssiStr = sub.split(",")[0].trim();
                int rssi = Integer.parseInt(rssiStr);
                int dbm = (2 * rssi) - 113;
                signalLabel.setText("CSQ: " + rssi + " (" + dbm + " dBm)");
                signalBar.setValue(rssi);
                if (rssi >= 15) {
                    signalBar.setForeground(new Color(0, 160, 0));
                } else if (rssi >= 10) {
                    signalBar.setForeground(new Color(220, 160, 0));
                } else {
                    signalBar.setForeground(Color.RED);
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateVotingDisplay() {
        var res = votingService.getResults();
        int total = votingService.getTotalVotes();
        votingTallyLabel.setText("OPCAO_A: " + res.getOrDefault("OPCAO_A", 0) +
                "  |  OPCAO_B: " + res.getOrDefault("OPCAO_B", 0) +
                "  |  Total: " + total);
    }

    private void logTx(String cmd) {
        appendLog("[TX] " + cmd, new Color(100, 180, 255));
    }

    private void logRx(String resp, long latencyMs, boolean ok) {
        String clean = resp.replace("\r\n", " ").trim();
        appendLog("[RX] " + clean + "  (" + latencyMs + " ms)", ok ? new Color(120, 230, 120) : new Color(255, 120, 120));
    }

    private void logInfo(String info) {
        appendLog("[INFO] " + info, new Color(220, 220, 100));
    }

    private void appendLog(String line, Color color) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalTime.now().format(TIME_FMT);
            consoleArea.append("[" + timestamp + "] " + line + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String port = (args != null && args.length > 0) ? args[0] : "/tmp/ttyB";
        SwingUtilities.invokeLater(() -> {
            ModemGuiApp app = new ModemGuiApp(port);
            app.setVisible(true);
        });
    }
}
