public class EnemyTank{
    private float x = -1000;
    private float y = -1000;
    private int bodyW = 120;
    private int bodyH = 30;
    private int armW = 80;
    private int armH = 8;
    private float angle = 0;
    private boolean shield = false;
    private PImage img = loadImage("tank2.png");

    public void setAngle(float a){
        angle = a;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setShield(boolean b){
        shield = b;
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

        if (shield){
            noFill();
            strokeWeight(3);
            stroke(0,0,200);
            ellipse(0, 0, 140, 140);
        }
        noStroke();

        //Draw the body to the adjusted translate pos.
        image(img, -(bodyW/2), 0, bodyW, bodyH);

        rotate(angle);
        fill (119, 8, 0);
        //Draw the recentAngle slightly adjusted so it can
        //rotate from its center point.
        rect(0, -armH/2, armW, armH);

        popMatrix();
    }


}
