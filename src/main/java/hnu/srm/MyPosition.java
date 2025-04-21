package hnu.srm;

public class MyPosition {
    int x, y;
    public MyPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public String toString() {
        return "X：" + this.x + " Y: " + this.y;
    }

    public static MyPosition add(MyPosition p1, MyPosition p2) {
        return new MyPosition(p1.x+p2.x, p1.y+p2.y);
    }

    public MyPosition reverse() {
        return new MyPosition(-1 * this.x, -1 * this.y);
    }
}
