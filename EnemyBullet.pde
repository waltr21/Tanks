public class EnemyBullet{
    private float x;
    private float y;

    public EnemyBullet(float conX, float conY){
        x = conX;
        y = conY;
    }

    public void setX(float newX){
        x = newX;
    }

    public void setY(float newY){
        y = newY;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void showBullet(){
        fill(0);
        ellipse(x, y, 10, 10);
    }
}
