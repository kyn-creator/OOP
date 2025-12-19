package snake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Random;



public class GamePanel extends JPanel implements ActionListener, KeyListener {

    public static final int TILE_SIZE = 25;
    public static final int ROWS = 20;
    public static final int COLS = 20;

    private Snake snake;
    private Food food;
    private Timer timer;
    private boolean gameOver = false;
    private int score = 0;
    private int highScore = 0;

    // Bot / cheat
    private boolean botEnabled = false;
    private int lastKey = -1;   // for "6" then "9" secret code

    public GamePanel() {
        setPreferredSize(new Dimension(COLS * TILE_SIZE, ROWS * TILE_SIZE + 50));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadHighScore();
        startNewGame();

        timer = new Timer(150, this);
    }

    public void startGame() {

        timer.start();
    }

    private void startNewGame() {
        snake = new Snake(5, 5);
        spawnFood();
        score = 0;
        gameOver = false;
        botEnabled = false;
    }

    private void loadHighScore() {
        try {
            File file = new File("highscore.txt");
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(file));
                highScore = Integer.parseInt(br.readLine());
                br.close();
            }
        } catch (Exception ignored) {}
    }

    private void saveHighScore() {
        try {
            if (score > highScore) {
                FileWriter fw = new FileWriter("highscore.txt");
                fw.write(String.valueOf(score));
                fw.close();
                highScore = score;
            }
        } catch (Exception ignored) {}
    }

    private void spawnFood() {
        Random r = new Random();
        int x, y;
        boolean valid;

        do {
            x = r.nextInt(COLS);
            y = r.nextInt(ROWS);
            valid = true;

            for (Point p : snake.getBody()) {
                if (p.x == x && p.y == y) {
                    valid = false;
                    break;
                }
            }
        } while (!valid);

        food = new Food(x, y);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            repaint();
            return;
        }

        // If bot is enabled, let AI choose direction before moving
        if (botEnabled) {
            Direction botDir = findBestDirectionBFS();
            if (botDir != null) {
                snake.setDirection(botDir);
            }
        }

        // Decide if the next move will eat the food
        Point head = snake.getBody().getFirst();
        Direction dir = snake.getDirection();
        int nx = head.x;
        int ny = head.y;

        switch (dir) {
            case UP -> ny--;
            case DOWN -> ny++;
            case LEFT -> nx--;
            case RIGHT -> nx++;
        }

        // Apply wrapping for preview position
        if (nx < 0) nx = COLS - 1;
        if (nx >= COLS) nx = 0;
        if (ny < 0) ny = ROWS - 1;
        if (ny >= ROWS) ny = 0;

        boolean growing = false;
        if (nx == food.getX() && ny == food.getY()) {
            growing = true;
            score += 10;
            spawnFood();
        }

        // Move the snake once
        snake.move(growing);

        // Self-collision check
        if (snake.isSelfCollision()) {
            gameOver = true;
            timer.stop();
            saveHighScore();
        }

        repaint();
    }

    // BFS pathfinding from head to food, avoiding the snake body
    private Direction findBestDirectionBFS() {
        LinkedList<Point> body = snake.getBody();
        if (body.isEmpty()) return null;

        Point head = body.getFirst();
        int hx = head.x;
        int hy = head.y;

        int fx = food.getX();
        int fy = food.getY();

        // Block snake body (except head)
        boolean[][] blocked = new boolean[COLS][ROWS];
        for (int i = 1; i < body.size(); i++) {
            Point p = body.get(i);
            blocked[p.x][p.y] = true;
        }

        boolean[][] visited = new boolean[COLS][ROWS];
        Point[][] parent = new Point[COLS][ROWS];

        ArrayDeque<Point> q = new ArrayDeque<>();
        q.add(new Point(hx, hy));
        visited[hx][hy] = true;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        boolean found = false;

        while (!q.isEmpty()) {
            Point cur = q.removeFirst();
            if (cur.x == fx && cur.y == fy) {
                found = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nx = cur.x + dx[i];
                int ny = cur.y + dy[i];

                // wrap
                if (nx < 0) nx = COLS - 1;
                if (nx >= COLS) nx = 0;
                if (ny < 0) ny = ROWS - 1;
                if (ny >= ROWS) ny = 0;

                if (!visited[nx][ny] && !blocked[nx][ny]) {
                    visited[nx][ny] = true;
                    parent[nx][ny] = cur;
                    q.addLast(new Point(nx, ny));
                }
            }
        }

        if (!found) {
            // No path found, fallback to greedy
            return greedyDirection();
        }

        // Reconstruct the path from food back to head
        Point step = new Point(fx, fy);
        Point prev = parent[step.x][step.y];

        // Walk back until the parent is the head
        while (prev != null && !(prev.x == hx && prev.y == hy)) {
            step = prev;
            prev = parent[step.x][step.y];
        }

        // step is now the next position after head
        return directionFromTo(head, step);
    }

    // Fallback: simple greedy move toward food
    private Direction greedyDirection() {
        Point head = snake.getBody().getFirst();
        int hx = head.x;
        int hy = head.y;
        int fx = food.getX();
        int fy = food.getY();

        Direction current = snake.getDirection();

        if (fx > hx && current != Direction.LEFT) return Direction.RIGHT;
        if (fx < hx && current != Direction.RIGHT) return Direction.LEFT;
        if (fy > hy && current != Direction.UP) return Direction.DOWN;
        if (fy < hy && current != Direction.DOWN) return Direction.UP;

        return current;
    }

    // Compute direction from point a (head) to point b (next step), with wrapping
    private Direction directionFromTo(Point a, Point b) {
        int hx = a.x;
        int hy = a.y;
        int nx = b.x;
        int ny = b.y;

        // horizontal
        if ((hx + 1) % COLS == nx && hy == ny) return Direction.RIGHT;
        if ((hx - 1 + COLS) % COLS == nx && hy == ny) return Direction.LEFT;

        // vertical
        if ((hy + 1) % ROWS == ny && hx == nx) return Direction.DOWN;
        if ((hy - 1 + ROWS) % ROWS == ny && hx == nx) return Direction.UP;

        return snake.getDirection(); // fallback
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(10, 10, 30),
                getWidth(), getHeight(), new Color(30, 60, 120)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());


        // Grid
        int hudY = ROWS * TILE_SIZE + 30;
        g.setFont(new Font("Consolas", Font.BOLD, 18));

        // shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString("Score: " + score, 11, hudY);
        g.drawString("Highscore: " + highScore, 200, hudY);

        // foreground
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, hudY - 2);
        g.drawString("Highscore: " + highScore, 199, hudY - 2);
        g.setColor(Color.DARK_GRAY);
        for (int i = 0; i < ROWS; i++) {
            g.drawLine(0, i * TILE_SIZE, COLS * TILE_SIZE, i * TILE_SIZE);
        }
        for (int j = 0; j < COLS; j++) {
            g.drawLine(j * TILE_SIZE, 0, j * TILE_SIZE, ROWS * TILE_SIZE);
        }

        // Food
        boolean pulse = (System.currentTimeMillis() / 300) % 2 == 0;
        g.setColor(pulse ? new Color(255, 80, 80) : new Color(255, 140, 0));
        g.fillOval(food.getX() * TILE_SIZE, food.getY() * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // Snake with glow effect
        LinkedList<Point> body = snake.getBody();
        for (int i = 0; i < body.size(); i++) {
            Point p = body.get(i);

            if (i == 0) {
                // Snake head (round with eyes)
                g.setColor(new Color(50, 220, 50));
                g.fillOval(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                // Eyes
                g.setColor(Color.WHITE);
                g.fillOval(p.x * TILE_SIZE + 5, p.y * TILE_SIZE + 5, 6, 6);
                g.fillOval(p.x * TILE_SIZE + TILE_SIZE - 11, p.y * TILE_SIZE + 5, 6, 6);

                g.setColor(Color.BLACK);
                g.fillOval(p.x * TILE_SIZE + 7, p.y * TILE_SIZE + 7, 3, 3);
                g.fillOval(p.x * TILE_SIZE + TILE_SIZE - 9, p.y * TILE_SIZE + 7, 3, 3);
            } else {
                // Snake body with rounded segments
                g.setColor(new Color(0, 180, 120)); // teal body
                g.fillRoundRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE, 8, 8);

                // Subtle inner highlight (no X)
                g.setColor(new Color(0, 255, 180));
                g.drawRoundRect(p.x * TILE_SIZE + 3, p.y * TILE_SIZE + 3,
                        TILE_SIZE - 6, TILE_SIZE - 6, 6, 6);
            }
        }




        if (gameOver) {
            // Dark overlay
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Centered GAME OVER text
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            FontMetrics fm = g.getFontMetrics();
            String msg = "GAME OVER";
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            int y = (getHeight() / 2) - fm.getHeight();
            g.drawString(msg, x, y);

            // Centered retry text
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            fm = g.getFontMetrics();
            String retry = "Press R to Retry";
            int rx = (getWidth() - fm.stringWidth(retry)) / 2;
            int ry = (getHeight() / 2) + fm.getHeight();
            g.drawString(retry, rx, ry);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

        // Retry on R if game over
        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) {
            startNewGame();
            timer.start();
            return;
        }

        // SECRET CHEAT TOGGLE: "~" (backquote)
        if (e.getKeyCode() == KeyEvent.VK_BACK_QUOTE) {
            botEnabled = !botEnabled;
            System.out.println("BOT MODE: " + botEnabled);
        }



        // Normal player control (no 180-degree reversal)
        Direction d = snake.getDirection();

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> {
                if (d != Direction.DOWN) snake.setDirection(Direction.UP);
            }
            case KeyEvent.VK_DOWN -> {
                if (d != Direction.UP) snake.setDirection(Direction.DOWN);
            }
            case KeyEvent.VK_LEFT -> {
                if (d != Direction.RIGHT) snake.setDirection(Direction.LEFT);
            }
            case KeyEvent.VK_RIGHT -> {
                if (d != Direction.LEFT) snake.setDirection(Direction.RIGHT);
            }
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}