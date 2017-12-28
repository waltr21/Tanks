public class Bullet{
    private float x;
    private float y;
    private float angle;
    private PVector pos;
    private PVector velocity;
    private int speed;
    private boolean fast;

    public Bullet(float x, float y, float angle, boolean s){
        this.x = x;
        this.y = y;
        this.angle = angle;
        fast = s;
        if (s)
            speed = 30;
        else
            speed = 15;
        pos = new PVector(x,y);
        velocity = PVector.fromAngle(angle);
    }

    public float getY(){
        return pos.y;
    }

    public float getX(){
        return pos.x;
    }

    public void travel(){
        pushMatrix();
        if(fast)
            fill(255, 145, 12);
        else
            fill(0);
        ellipse(pos.x, pos.y, 10, 10);
        pos.add(velocity.x * speed, velocity.y * speed);
        popMatrix();
    }

}
