package snake;

import java.awt.*;
import java.util.LinkedList;

public class Snake {

    private LinkedList<Point> body = new LinkedList<>();
    private Direction direction = Direction.RIGHT;

    public Snake(int startX, int startY) {
        body.add(new Point(startX, startY)); // head
    }

    public void setDirection(Direction d) {
        this.direction = d;
    }

    public Direction getDirection() {
        return direction;
    }

    public LinkedList<Point> getBody() {
        return body;
    }

    public void move(boolean growing) {
        Point head = body.getFirst();
        int x = head.x;
        int y = head.y;

        switch (direction) {
            case UP -> y--;
            case DOWN -> y++;
            case LEFT -> x--;
            case RIGHT -> x++;
        }

        // Wrap horizontally
        if (x < 0) x = GamePanel.COLS - 1;
        if (x >= GamePanel.COLS) x = 0;

        // Wrap vertically
        if (y < 0) y = GamePanel.ROWS - 1;
        if (y >= GamePanel.ROWS) y = 0;

        // Add new head
        body.addFirst(new Point(x, y));

        if (!growing) {
            body.removeLast();
        }
    }

    public boolean isSelfCollision() {
        Point head = body.getFirst();
        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i))) {
                return true;
            }
        }
        return false;
    }
}