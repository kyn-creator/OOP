package snake;

import java.awt.*;

public class Food {
    private int x, y;

    public Food(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(Graphics g, int tileSize) {
        g.setColor(Color.RED);
        g.fillOval(x * tileSize, y * tileSize, tileSize, tileSize);
    }

    public int getX() { return x; }
    public int getY() { return y; }
}