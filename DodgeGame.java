
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class DodgeGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dodge the Blocks");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new GamePanel());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

abstract class GameObject {
    private double x;
    private double y;
    private int width;
    private int height;

    public GameObject(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    protected void setX(double x) { this.x = x; }
    protected void setY(double y) { this.y = y; }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    public abstract void update();
    public abstract void draw(Graphics2D g);
}

class Player extends GameObject {
    private static final int SPEED = 7;
    private boolean movingLeft;
    private boolean movingRight;

    public Player(int x, int y) {
        super(x, y, 42, 42);
    }

    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }

    @Override
    public void update() {
        if (movingLeft) {
            setX(Math.max(0, getX() - SPEED));
        }

        if (movingRight) {
            setX(Math.min(
                GamePanel.WIDTH - getWidth(),
                getX() + SPEED
            ));
        }
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(new Color(40, 145, 255));
        g.fillRoundRect(
            (int) getX(), (int) getY(),
            getWidth(), getHeight(), 12, 12
        );

        g.setColor(Color.WHITE);
        g.fillOval((int) getX() + 9, (int) getY() + 10, 7, 7);
        g.fillOval((int) getX() + 26, (int) getY() + 10, 7, 7);

        g.setColor(new Color(10, 55, 115));
        g.drawArc((int) getX() + 11, (int) getY() + 19, 20, 12, 200, 140);
    }
}

class FallingBlock extends GameObject {
    private final int speed;
    private final Color color;

    public FallingBlock(int x, int y, int size, int speed, Color color) {
        super(x, y, size, size);
        this.speed = speed;
        this.color = color;
    }

    @Override
    public void update() {
        setY(getY() + speed);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(color);
        g.fillRoundRect(
            (int) getX(), (int) getY(),
            getWidth(), getHeight(), 8, 8
        );

        g.setColor(color.brighter());
        g.drawRoundRect(
            (int) getX() + 4, (int) getY() + 4,
            getWidth() - 8, getHeight() - 8, 5, 5
        );
    }
}

class GamePanel extends JPanel {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final List<GameObject> objects = new ArrayList<>();
    private final Random random = new Random();
    private final Timer timer;

    private Player player;
    private int score;
    private int spawnCounter;
    private boolean gameOver;
    private boolean started;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(18, 25, 42));
        setFocusable(true);

        setupKeyBindings();
        resetGame();

        timer = new Timer(16, e -> {
            if (started && !gameOver) {
                updateGame();
            }
            repaint();
        });

        timer.start();
    }

    private void setupKeyBindings() {
        bindKey("LEFT", "leftPressed",
            () -> player.setMovingLeft(true));

        bindKey("released LEFT", "leftReleased",
            () -> player.setMovingLeft(false));

        bindKey("RIGHT", "rightPressed",
            () -> player.setMovingRight(true));

        bindKey("released RIGHT", "rightReleased",
            () -> player.setMovingRight(false));

        bindKey("A", "aPressed",
            () -> player.setMovingLeft(true));

        bindKey("released A", "aReleased",
            () -> player.setMovingLeft(false));

        bindKey("D", "dPressed",
            () -> player.setMovingRight(true));

        bindKey("released D", "dReleased",
            () -> player.setMovingRight(false));

        bindKey("SPACE", "startOrRestart", () -> {
            if (!started || gameOver) {
                resetGame();
                started = true;
            }
        });
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(key), name);

        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    action.run();
                } catch (RuntimeException ex) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
    }

    private void resetGame() {
        objects.clear();

        player = new Player(WIDTH / 2 - 21, HEIGHT - 75);
        objects.add(player);

        score = 0;
        spawnCounter = 0;
        gameOver = false;
        started = false;
    }

    private void updateGame() {
        for (GameObject object : objects) {
            object.update();
        }

        spawnCounter++;

        int spawnRate = Math.max(18, 48 - score / 4);

        if (spawnCounter >= spawnRate) {
            spawnBlock();
            spawnCounter = 0;
        }

        Iterator<GameObject> iterator = objects.iterator();

        while (iterator.hasNext()) {
            GameObject object = iterator.next();

            if (object instanceof FallingBlock) {
                if (object.getBounds().intersects(player.getBounds())) {
                    gameOver = true;
                    player.setMovingLeft(false);
                    player.setMovingRight(false);
                    return;
                }

                if (object.getY() > HEIGHT) {
                    iterator.remove();
                    score++;
                }
            }
        }
    }

    private void spawnBlock() {
        int size = 24 + random.nextInt(24);
        int x = random.nextInt(WIDTH - size);
        int speed = 3 + Math.min(6, score / 8) + random.nextInt(2);

        Color[] colors = {
            new Color(255, 82, 82),
            new Color(255, 166, 77),
            new Color(255, 213, 79)
        };

        objects.add(new FallingBlock(
            x, -size, size, speed,
            colors[random.nextInt(colors.length)]
        ));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setColor(new Color(55, 68, 92));

        for (int i = 0; i < 35; i++) {
            int x = (i * 97 + 23) % WIDTH;
            int y = (i * 61 + 17) % HEIGHT;
            g.fillOval(x, y, 2, 2);
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString("DODGE THE BLOCKS", 20, 32);

        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.setColor(new Color(120, 220, 255));
        g.drawString("Score: " + score, WIDTH - 110, 32);

        for (GameObject object : objects) {
            object.draw(g);
        }

        if (!started && !gameOver) {
            drawOverlay(
                g,
                "DODGE THE BLOCKS",
                "Press SPACE to start",
                "Move with LEFT / RIGHT arrows or A / D"
            );
        } else if (gameOver) {
            drawOverlay(
                g,
                "GAME OVER",
                "Final score: " + score,
                "Press SPACE to play again"
            );
        }

        g.dispose();
    }

    private void drawOverlay(
        Graphics2D g, String title, String line1, String line2
    ) {
        g.setColor(new Color(5, 10, 22, 205));
        g.fillRoundRect(85, 145, WIDTH - 170, 190, 22, 22);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));

        FontMetrics titleMetrics = g.getFontMetrics();

        g.drawString(
            title,
            (WIDTH - titleMetrics.stringWidth(title)) / 2,
            198
        );

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));

        FontMetrics metrics = g.getFontMetrics();

        g.setColor(new Color(160, 225, 255));

        g.drawString(
            line1,
            (WIDTH - metrics.stringWidth(line1)) / 2,
            242
        );

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        metrics = g.getFontMetrics();

        g.setColor(new Color(220, 225, 235));

        g.drawString(
            line2,
            (WIDTH - metrics.stringWidth(line2)) / 2,
            278
        );
    }
}
