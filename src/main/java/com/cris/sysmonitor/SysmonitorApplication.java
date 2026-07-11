package com.cris.sysmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.URI;

@SpringBootApplication
public class SysmonitorApplication {

    public static void main(String[] args) {
        // Start Spring Boot in background thread
        Thread serverThread = new Thread(new Runnable() {
            public void run() {
                SpringApplication.run(SysmonitorApplication.class, args);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // Launch Swing GUI on main thread
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showWindow();
            }
        });
    }

    private static void showWindow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        final JFrame frame = new JFrame("CRIS SysMonitor V2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(440, 300);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel("CRIS SysMonitor V2");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(0x0d0d0d));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Centre for Railway Information Systems");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitle.setForeground(new Color(0x888888));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(0xeeeeee));

        final JLabel statusLabel = new JLabel("⏳  Starting server...");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(0x888888));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JLabel ipLabel = new JLabel(" ");
        ipLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ipLabel.setForeground(new Color(0x444444));
        ipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JButton openBtn = new JButton("Open Dashboard");
        openBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        openBtn.setBackground(new Color(0x0d0d0d));
        openBtn.setForeground(Color.WHITE);
        openBtn.setFocusPainted(false);
        openBtn.setBorderPainted(false);
        openBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        openBtn.setMaximumSize(new Dimension(200, 40));
        openBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        openBtn.setEnabled(false);

        final JButton stopBtn = new JButton("Stop Server");
        stopBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stopBtn.setBackground(new Color(0xf5f5f5));
        stopBtn.setForeground(new Color(0x888888));
        stopBtn.setFocusPainted(false);
        stopBtn.setBorderPainted(false);
        stopBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        stopBtn.setMaximumSize(new Dimension(200, 32));
        stopBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        stopBtn.setEnabled(false);

        main.add(title);
        main.add(Box.createRigidArea(new Dimension(0, 6)));
        main.add(subtitle);
        main.add(Box.createRigidArea(new Dimension(0, 20)));
        main.add(sep);
        main.add(Box.createRigidArea(new Dimension(0, 20)));
        main.add(statusLabel);
        main.add(Box.createRigidArea(new Dimension(0, 6)));
        main.add(ipLabel);
        main.add(Box.createRigidArea(new Dimension(0, 24)));
        main.add(openBtn);
        main.add(Box.createRigidArea(new Dimension(0, 10)));
        main.add(stopBtn);

        frame.add(main);
        frame.setVisible(true);

        // Poll until server is up
        Thread poller = new Thread(new Runnable() {
            public void run() {
                int attempts = 0;
                while (attempts < 30) {
                    try {
                        Thread.sleep(1000);
                        attempts++;
                        String ip = InetAddress.getLocalHost().getHostAddress();

                        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS");
                        sc.init(null, new javax.net.ssl.TrustManager[]{
                            new javax.net.ssl.X509TrustManager() {
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                            }
                        }, new java.security.SecureRandom());
                        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                                new javax.net.ssl.HostnameVerifier() {
                                    public boolean verify(String h, javax.net.ssl.SSLSession s) { return true; }
                                });

                        java.net.URL url = new java.net.URL("https://localhost:8443/api/usb");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(1000);
                        conn.connect();

                        final String serverIp = ip;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                statusLabel.setText("✅  Server is running");
                                statusLabel.setForeground(new Color(0x2e7d32));
                                ipLabel.setText("https://localhost:8443");
                                openBtn.setEnabled(true);
                                stopBtn.setEnabled(true);
                            }
                        });
                        break;

                    } catch (Exception e) {
                        final int a = attempts;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                statusLabel.setText("⏳  Starting server... (" + a + "s)");
                            }
                        });
                    }
                }
            }
        });
        poller.setDaemon(true);
        poller.start();

        openBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    String ip = InetAddress.getLocalHost().getHostAddress();
                    Desktop.getDesktop().browse(new URI("https://localhost:8443"));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Could not open browser.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stopBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Stop the server and exit?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) System.exit(0);
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
