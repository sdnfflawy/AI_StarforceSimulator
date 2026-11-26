import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Random;


public class StarforceSimulator extends JFrame {

    static final int MAX_STAR = 30;


    static final double[] SUCCESS = {
            99.75, 94.50, 89.25, 89.25, 84.00, 78.75, 73.50, 68.25, 63.00, 57.75,
            52.50, 47.25, 42.00, 36.75, 31.50, 31.50, 31.50, 15.75, 15.75, 15.75,
            31.50, 15.75, 15.75, 10.50, 10.50, 10.50, 7.35, 5.25, 3.15, 1.05
    };

    static final double[] DESTROY = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 2.055, 2.055, 6.74, 6.74, 8.425,
            10.275, 12.6375, 16.85, 17.90, 17.90, 17.90, 18.49, 18.95, 19.37, 19.79
    };

    static double failRate(int s) {
        return 100.0 - SUCCESS[s] - DESTROY[s];
    }

    static String pct(double v) {
        double rounded = Math.round(v * 10000) / 10000.0;
        return BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString() + "%";
    }

    private final Random rnd = new SecureRandom();
    private int star = 0;
    private boolean destroyed = false;

    private final int[] tries = new int[MAX_STAR];
    private final int[] succ = new int[MAX_STAR];
    private final int[] fail = new int[MAX_STAR];
    private final int[] dest = new int[MAX_STAR];

    private final StarPanel starPanel = new StarPanel();
    private final ItemPanel itemPanel = new ItemPanel();
    private final JLabel starText = new JLabel("0성", SwingConstants.CENTER);
    private final JLabel resultLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel summaryLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JButton enhanceBtn = new JButton("강화");
    private final JButton restartBtn = new JButton("다시하기");
    private final JCheckBox adminCheck = new JCheckBox("관리자 모드");
    private final JSpinner adminSpinner = new JSpinner(new SpinnerNumberModel(29, 0, MAX_STAR, 1));
    private final JButton adminBtn = new JButton("해당 성 아이템 가져오기");
    private final StatTableModel tableModel = new StatTableModel();
    private final JTable table = new JTable(tableModel);

    public StarforceSimulator() {
        super("스타포스 강화 시뮬레이터");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(java.util.Arrays.asList(makeStarIcon(16), makeStarIcon(32), makeStarIcon(64)));
        setLayout(new BorderLayout(12, 0));

        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);

        enhanceBtn.addActionListener(e -> enhance());
        restartBtn.addActionListener(e -> restart());
        adminCheck.addActionListener(e -> updateAdminEnabled());
        adminBtn.addActionListener(e -> adminSetStar((Integer) adminSpinner.getValue()));
        getRootPane().setDefaultButton(enhanceBtn);

        updateAdminEnabled();
        refreshView();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 4));
        p.setPreferredSize(new Dimension(380, 560));

        JLabel title = new JLabel("스타포스 강화 시뮬레이터");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        starText.setFont(starText.getFont().deriveFont(Font.BOLD, 24f));
        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 15f));
        fixSize(starText, 350, 34);
        fixSize(resultLabel, 350, 24);
        fixSize(summaryLabel, 350, 20);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        enhanceBtn.setPreferredSize(new Dimension(120, 36));
        restartBtn.setPreferredSize(new Dimension(120, 36));
        buttons.add(enhanceBtn);
        buttons.add(restartBtn);

        JPanel admin = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        admin.add(adminCheck);
        admin.add(adminSpinner);
        admin.add(new JLabel("성"));
        JPanel adminBtnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        adminBtnRow.add(adminBtn);

        JButton resetStats = new JButton("기록 초기화");
        resetStats.addActionListener(e -> {
            for (int i = 0; i < MAX_STAR; i++) {
                tries[i] = 0;
                succ[i] = 0;
                fail[i] = 0;
                dest[i] = 0;
            }
            resultLabel.setText(" ");
            refreshView();
        });
        JPanel resetRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        resetRow.add(resetStats);

        for (JComponent c : new JComponent[]{title, starPanel, itemPanel, starText, resultLabel,
                buttons, summaryLabel, admin, adminBtnRow, resetRow}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        p.add(title);
        p.add(Box.createVerticalStrut(12));
        p.add(starPanel);
        p.add(Box.createVerticalStrut(6));
        p.add(itemPanel);
        p.add(starText);
        p.add(Box.createVerticalStrut(6));
        p.add(resultLabel);
        p.add(Box.createVerticalStrut(10));
        p.add(buttons);
        p.add(Box.createVerticalStrut(10));
        p.add(summaryLabel);
        p.add(Box.createVerticalGlue());
        p.add(admin);
        p.add(adminBtnRow);
        p.add(resetRow);
        return p;
    }

    private static void fixSize(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setPreferredSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
    }

    private JComponent buildRightPanel() {
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new StatRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(640, 560));
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 0, 16, 16), sp.getBorder()));
        return sp;
    }

    private void enhance() {
        if (destroyed || star >= MAX_STAR) return;

        int s = star;
        tries[s]++;
        double r = rnd.nextDouble() * 100.0;

        if (r < SUCCESS[s]) {
            succ[s]++;
            star++;
            resultLabel.setForeground(new Color(0, 110, 210));
            resultLabel.setText("강화 성공!  " + s + "성 → " + star + "성");
        } else if (r < SUCCESS[s] + DESTROY[s]) {
            dest[s]++;
            destroyed = true;
            resultLabel.setForeground(new Color(200, 30, 30));
            resultLabel.setText("장비가 파괴되었습니다... (" + s + "성에서 파괴)");
        } else {
            fail[s]++;
            resultLabel.setForeground(new Color(120, 120, 120));
            resultLabel.setText("강화 실패  (" + s + "성 유지)");
        }
        refreshView();
    }

    private void restart() {
        star = 0;
        destroyed = false;
        resultLabel.setForeground(Color.DARK_GRAY);
        resultLabel.setText("새 아이템을 받았습니다. 0성부터 다시 시작!");
        refreshView();
    }

    private void adminSetStar(int target) {
        star = target;
        destroyed = false;
        resultLabel.setForeground(new Color(120, 60, 180));
        resultLabel.setText("[관리자] " + target + "성 아이템을 지급했습니다.");
        refreshView();
    }

    private void updateAdminEnabled() {
        boolean on = adminCheck.isSelected();
        adminSpinner.setEnabled(on);
        adminBtn.setEnabled(on);
    }

    private void refreshView() {
        if (destroyed) {
            starText.setText("파괴됨");
        } else {
            starText.setText(star >= MAX_STAR ? star + "성 (최대)" : star + "성");
        }
        itemPanel.repaint();
        enhanceBtn.setEnabled(!destroyed && star < MAX_STAR);
        restartBtn.setEnabled(destroyed);

        int t = 0, sc = 0, f = 0, d = 0;
        for (int i = 0; i < MAX_STAR; i++) {
            t += tries[i];
            sc += succ[i];
            f += fail[i];
            d += dest[i];
        }
        summaryLabel.setText("총 시도 " + t + " | 성공 " + sc + " | 실패 " + f + " | 파괴 " + d);

        starPanel.repaint();
        tableModel.fireTableDataChanged();
        int row = Math.min(star, MAX_STAR - 1);
        table.scrollRectToVisible(table.getCellRect(row, 0, true));
    }

    private class StarPanel extends JPanel {
        private static final int R = 9, STEP = 20, GROUP_GAP = 10, ROW_H = 26;

        StarPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(5 * STEP * 3 + GROUP_GAP * 2 + 10, ROW_H * 2 + 8));
            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int groupW = 5 * STEP;
            int totalW = 3 * groupW + 2 * GROUP_GAP;
            int x0 = (getWidth() - totalW) / 2;

            for (int i = 0; i < MAX_STAR; i++) {
                int row = i / 15;
                int col = i % 15;
                int group = col / 5;
                int inGroup = col % 5;
                int cx = x0 + group * (groupW + GROUP_GAP) + inGroup * STEP + STEP / 2;
                int cy = 14 + row * ROW_H;
                boolean on = !destroyed && i < star;
                drawStar(g, cx, cy, on);
            }
            g.dispose();
        }

        private void drawStar(Graphics2D g, int cx, int cy, boolean on) {
            Path2D.Double path = new Path2D.Double();
            for (int k = 0; k < 10; k++) {
                double angle = -Math.PI / 2 + k * Math.PI / 5;
                double r = (k % 2 == 0) ? R : R * 0.42;
                double x = cx + r * Math.cos(angle);
                double y = cy + r * Math.sin(angle);
                if (k == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.closePath();
            g.setColor(on ? new Color(255, 205, 0) : new Color(95, 95, 95));
            g.fill(path);
            g.setColor(on ? new Color(200, 140, 0) : new Color(60, 60, 60));
            g.draw(path);
        }
    }

    static Path2D starShape(double cx, double cy, double r) {
        Path2D.Double path = new Path2D.Double();
        for (int k = 0; k < 10; k++) {
            double angle = -Math.PI / 2 + k * Math.PI / 5;
            double rr = (k % 2 == 0) ? r : r * 0.42;
            double x = cx + rr * Math.cos(angle);
            double y = cy + rr * Math.sin(angle);
            if (k == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.closePath();
        return path;
    }

    static Image makeStarIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Path2D star = starShape(size / 2.0, size / 2.0 + size * 0.03, size * 0.48);
        g.setPaint(new GradientPaint(0, 0, new Color(0xFFE066), 0, size, new Color(0xFFB800)));
        g.fill(star);
        g.setColor(new Color(0xC98A00));
        g.setStroke(new BasicStroke(Math.max(1f, size / 24f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(star);
        g.dispose();
        return img;
    }

    private class ItemPanel extends JPanel {
        ItemPanel() {
            setOpaque(false);
            Dimension d = new Dimension(150, 150);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(getWidth() / 2.0, getHeight() / 2.0);

            if (destroyed) {
                Graphics2D faded = (Graphics2D) g.create();
                faded.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                drawSword(faded);
                faded.dispose();
                drawBurst(g);
            } else {
                drawSword(g);
            }
            g.dispose();
        }

        private void drawSword(Graphics2D g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.rotate(Math.toRadians(45));

            // 칼날
            Path2D blade = new Path2D.Double();
            blade.moveTo(0, -70);
            blade.lineTo(9, -48);
            blade.lineTo(9, 18);
            blade.lineTo(-9, 18);
            blade.lineTo(-9, -48);
            blade.closePath();
            g.setPaint(new GradientPaint(-9, 0, new Color(0xF0F4FA), 9, 0, new Color(0x8F9CAD)));
            g.fill(blade);
            g.setColor(new Color(0x6E7B8C));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(blade);
            g.setColor(new Color(0xB9C4D2));
            g.fillRect(-1, -50, 2, 66);

            g.setPaint(new GradientPaint(0, 18, new Color(0xFFD84D), 0, 27, new Color(0xC9962A)));
            g.fill(new RoundRectangle2D.Double(-27, 18, 54, 9, 6, 6));

            g.setColor(new Color(0x7A4A22));
            g.fillRoundRect(-5, 27, 10, 28, 4, 4);
            g.setColor(new Color(0x5A3416));
            for (int y = 31; y < 54; y += 7) g.drawLine(-5, y, 5, y + 3);

            g.setPaint(new GradientPaint(0, 50, new Color(0xFFD84D), 0, 66, new Color(0xC9962A)));
            g.fill(new Ellipse2D.Double(-8, 50, 16, 16));
            g.dispose();
        }

        private void drawBurst(Graphics2D g) {
            Path2D.Double burst = new Path2D.Double();
            int points = 14;
            for (int k = 0; k < points * 2; k++) {
                double angle = -Math.PI / 2 + k * Math.PI / points;
                double r = (k % 2 == 0) ? 52 : 26;
                double x = r * Math.cos(angle);
                double y = r * Math.sin(angle);
                if (k == 0) burst.moveTo(x, y);
                else burst.lineTo(x, y);
            }
            burst.closePath();
            g.setPaint(new GradientPaint(0, -52, new Color(0xFFD23F), 0, 52, new Color(0xE8452C)));
            g.fill(burst);
            g.setColor(new Color(0xA82314));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(burst);
        }
    }

    private class StatTableModel extends AbstractTableModel {
        private final String[] cols = {"단계", "성공 확률", "실패 확률", "파괴 확률", "시도", "성공", "실패", "파괴"};

        @Override
        public int getRowCount() {
            return MAX_STAR;
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int c) {
            return cols[c];
        }

        @Override
        public Object getValueAt(int r, int c) {
            switch (c) {
                case 0: return r + "성 → " + (r + 1) + "성";
                case 1: return pct(SUCCESS[r]);
                case 2: return pct(failRate(r));
                case 3: return pct(DESTROY[r]);
                case 4: return tries[r];
                case 5: return succ[r];
                case 6: return fail[r];
                default: return dest[r];
            }
        }
    }

    private class StatRenderer extends DefaultTableCellRenderer {
        StatRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                       boolean focus, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, false, false, row, col);
            boolean current = !destroyed && star < MAX_STAR && row == star;
            c.setBackground(current ? new Color(255, 244, 190) : Color.WHITE);
            c.setForeground(col == 3 && DESTROY[row] > 0 ? new Color(190, 40, 40) : Color.BLACK);
            return c;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource("맑은 고딕", Font.PLAIN, 13));
            }
        }
        SwingUtilities.invokeLater(() -> new StarforceSimulator().setVisible(true));
    }
}
