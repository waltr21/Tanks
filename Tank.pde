public class Tank{
    //X value for the take and rotations
    private float x = width/2;
    //Y value for the tank and rotations
    private float y = height/2;
    //Width of the arm.
    private int armW = 80;
    //Height of the arm.
    private int armH = 8;
    //Angle to rotate the arm
    private float armAngle = 0;
    //Width of the body of the tank.
    private int bodyW = 120;
    //Height of the body of the tank.
    private int bodyH = 30;
    //Force of gravity in the game.
    private float gravity = 0.4;
    //Variable to keep track of the velocity in the game.
    private float velocity = 0;
    //Count to limit the jumps to one.
    private int count = 0;
    //Health for the player.
    private int health = 10;

    private long pastTime = 0;

    private PImage img = loadImage("tank1.png");

    public void setAngle(float a){
        armAngle = a;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public float getX(){
        return x-(bodyW/2);
    }

    public float getVelocity(){
        return velocity;
    }

    public float getY(){
        return y;
    }

    public float getArmX(){
        return x;
    }

    public float getArmY(){
        return y;
    }

    public float getArmW(){
        return armW;
    }

    public float getArmH(){
        return armH;
    }

    public int getHealth(){
        return health;
    }

    public int getTankH(){
        return bodyH;
    }

    public int getTankW(){
        return bodyW;
    }

    public boolean takeHit(){
        //System.out.println("HIT!");

        if (System.currentTimeMillis() - pastTime > 200){
            health--;
            pastTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public void move(int dir){
        x += dir;
    }

    public float getAngle(){
        return armAngle;
    }

    public void gravity(){
        velocity += gravity;
        y += velocity;
    }

    public void jump(){
        if (count < 2)
            velocity -= 10;
        count++;
    }

    public void resetCount(){
        count = 0;
    }

    public void displayDead(){
        if (health <= 0){
            x = -1000;
            y = -1000;
        }
    }

    public void bound(){
        if (y + bodyH > height){
            y = height - bodyH;
            resetVelocity();
            resetCount();
        }
        if (y < 0)
            y = 0;
        if (x - bodyW/2 < 0)
            x = 0 + bodyW/2;
        if (x + bodyW/2 > width)
            x = width - bodyW/2;
    }

    public void resetVelocity(){
        velocity = 0;
    }

    public void showArm(){
        displayDead();
        rotate(0);
        translate(x,y);
        rotate(armAngle);
        fill (106, 108, 0);
        rect(0, -armH/2, armW, armH);
    }

    public void showBody(){
        displayDead();
        rotate(0);
        image(img, x-(bodyW/2), y, bodyW, bodyH);
    }
}
