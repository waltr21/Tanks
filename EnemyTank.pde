public class EnemyTank{
    private float x = -1000;
    private float y = -1000;
    private int bodyW = 120;
    private int bodyH = 30;
    private PImage img = loadImage("tank1.png");


    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public void showBody(){
        rotate(0);
        image(img, x-(bodyW/2), y, bodyW, bodyH);
    }
}
