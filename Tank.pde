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

    private PImage img = loadImage("tank1.png");

    public void setAngle(float a){
        armAngle = a;
    }

    public float getX(){
        return x-(bodyW/2);
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
        if (count < 1)
            velocity -= 10;
        count++;
    }

    public void resetCount(){
        count = 0;
    }

    public void bound(){
        if (y + bodyH > height){
            y = height - bodyH;
            velocity = 0;
            resetCount();
        }
        if (y < 0)
            y = 0;
        if (x - bodyW/2 < 0)
            x = 0 + bodyW/2;
        if (x + bodyW/2 > width)
            x = width - bodyW/2;
    }


    public void showArm(){
        translate(x,y);
        rotate(armAngle);
        fill (106, 108, 0);
        rect(0, -armH/2, armW, armH);
    }

    public void showBody(){
        rotate(0);
        image(img, x-(bodyW/2), y, bodyW, bodyH);
    }
}
