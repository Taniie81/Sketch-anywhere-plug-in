import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage; // AJOUTÉ
import java.io.ByteArrayOutputStream; // AJOUTÉ
import javax.imageio.ImageIO; // AJOUTÉ
import java.util.ArrayList;
import java.util.Base64; // AJOUTÉ
import java.util.List;

public class DrawingCanvas extends JPanel {
    private final List<List<Point>> lines = new ArrayList<>();

    public DrawingCanvas() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                List<Point> newLine = new ArrayList<>();
                newLine.add(e.getPoint());
                lines.add(newLine);
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!lines.isEmpty()) {
                    lines.get(lines.size() - 1).add(e.getPoint());
                    repaint();
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.BLACK);

        for (List<Point> line : lines) {
            for (int i = 1; i < line.size(); i++) {
                Point p1 = line.get(i - 1);
                Point p2 = line.get(i);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    // --- LA MÉTHODE QUI MANQUAIT EST ICI ---
    public String getBase64Image() {
        try {
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            this.paint(g2);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
}