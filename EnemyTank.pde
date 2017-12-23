public class EnemyTank{
    private float x = -1000;
    private float y = -1000;
    private int bodyW = 120;
    private int bodyH = 30;
    private int armW = 80;
    private int armH = 8;
    private float angle = 0;
    private PImage img = loadImage("tank1.png");

    public void setAngle(float a){
        angle = a;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getAngle(){
        return angle;
    }

    public void setY(float tempY){
        y = tempY;
    }


    public void showBody(){
        rotate(0);
        pushMatrix();
        //translate according to the position of the tank arm.
        translate(x+(bodyW/2), y);

        //Draw the body to the adjusted translate pos.
        image(img, -(bodyW/2), 0, bodyW, bodyH);

        rotate(angle);
        fill (106, 108, 0);
        //Draw the recentAngle slightly adjusted so it can
        //rotate from its center point.
        rect(0, -armH/2, armW, armH);
        popMatrix();
    }


}
