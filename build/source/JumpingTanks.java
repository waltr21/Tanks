import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.*; 
import java.net.*; 
import java.nio.*; 
import java.nio.channels.*; 
import java.util.ArrayList; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class JumpingTanks extends PApplet {







Tank player;
EnemyTank enemy;
ArrayList<Bullet> bullets = new ArrayList<Bullet>();
ArrayList<EnemyBullet> enemyBullets = new ArrayList<EnemyBullet>();
float recentAngle = 30;
int dir = 0;
int gravity = 10;
boolean holdingR, holdingL;
DatagramChannel dc;
String address = "127.0.0.1";
int portNum = 8765;


public void setup(){
    
    frameRate(60);

    player = new Tank();
    enemy = new EnemyTank();
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));

    //Open the channel.
    try{
        dc = DatagramChannel.open();
    }
    catch(Exception e){
        System.out.println("Error in setup: " + e);
    }

    //Create a thread for handling packets coming in.
    Thread myThread = new Thread(new Runnable() {
        public void run() {
            runThread();
        }
    });
    myThread.start();
}

public void draw(){
    background(236,236,236);
    //System.out.println(frameRate);
    player.move(dir);
    player.gravity();
    player.bound();

    //Calculate the angle of the arm.
    recentAngle = calculateArmAngle();
    player.setAngle(recentAngle);

    //Show the arm and body of the tanks.
    showAndBoundBullets();
    showEnemyBullets();
    enemy.showBody();
    player.showBody();
    player.showArm();
    checkHit();

    //Pack the appropriate coordinates into strings and send them.
    String loc = player.getX() + "," + player.getY() + "," + player.getAngle();
    for (Bullet b : bullets){
        loc += "," + b.getX() + "/" + b.getY();
    }
    try{
        ByteBuffer buff = ByteBuffer.wrap(loc.getBytes());
        dc.send(buff, new InetSocketAddress(address, portNum));
    }
    catch(Exception e){
        System.out.println("Error in draw: " + e);
    }
}

public void showAndBoundBullets(){
    for (int i = 0; i < bullets.size(); i++){
        //Check to see if the bullet is out of bounds.
        if (bullets.get(i).getY() > height || bullets.get(i).getY() < 0){
            bullets.remove(i);
        }
        else if (bullets.get(i).getX() > width || bullets.get(i).getX() < 0){
            bullets.remove(i);
        }
        //Travel if in bounds.
        else{
            bullets.get(i).travel();
        }
    }
}

public void checkHit(){
    for (EnemyBullet b : enemyBullets){
        if (b.getX() > player.getX() && b.getX() < player.getX() + player.getTankW()){
            if (b.getY() > player.getY() && b.getY() < player.getY() + player.getArmH()){
                //System.out.println("HIT!");
                player.takeHit();
                break;
            }
        }
    }
}


public void showEnemyBullets(){
    for (EnemyBullet b : enemyBullets){
        //System.out.println(b.getX() + "-" + b.getY());
        b.showBullet();
    }
}

public float calculateArmAngle(){
    //Make a right triangle with the mouse and tank pos.
    float opp = (mouseY - player.getArmY());
    float adj = (mouseX - player.getArmX());
    //Handle divide by 0 errors.
    if (adj == 0)
        return recentAngle;

    //Take the arc tan of our angle (returns in radians)
    float newAngle = atan(opp/adj);

    //If we are in the 1 or 3 quadrant we have to add pi.
    if (mouseX < player.getArmX()){
        newAngle += PI;
    }
    return newAngle;
}

public void runThread(){
    try{
        System.out.println("Thread created.");
        while (true){
            ByteBuffer buffer = ByteBuffer.allocate(1024);
    		dc.receive(buffer);
            String message = new String(buffer.array());
            String[] coordinates = message.split(",");

            /*
             *[0] Tank X pos.
             *[1] Tank Y pos.
             *[2] Tank arm Angle pos.
             *[3-X] Bullet X and Y pos.
             */

            enemy.setX(Float.parseFloat(coordinates[0]));
            enemy.setY(Float.parseFloat(coordinates[1]));
            enemy.setAngle(Float.parseFloat(coordinates[2]));
            int bulletCount = 0;

            for (int i = 3; i < coordinates.length; i++){
                String[] bulletCoor = coordinates[i].split("/");
                float locX = Float.parseFloat(bulletCoor[0]);
                float locY = Float.parseFloat(bulletCoor[1]);
                enemyBullets.get(bulletCount).setX(locX);
                enemyBullets.get(bulletCount).setY(locY);
                bulletCount++;
            }

            for (int i = bulletCount; i < enemyBullets.size(); i++){
                enemyBullets.get(i).setY(-1000);
                enemyBullets.get(i).setX(-1000);
            }
        }
    }
    catch(Exception e){
        System.out.println("Exception in the tank thread: " +  e);
    }
}

public void keyReleased(){
    if (keyCode == RIGHT || key == 'd' || key == 'D')
        holdingR = false;

    if (keyCode == LEFT || key == 'a' || key == 'A')
        holdingL = false;

    //Check that we have both keys lifted up to stop moving.
    if (!holdingL && !holdingR)
        dir = 0;
}

public void keyPressed(){
    //Move the player left and right.
    if (keyCode == RIGHT || key == 'd' || key == 'D'){
        dir = 2;
        holdingR = true;
    }
    if (keyCode == LEFT || key == 'a' || key == 'A'){
        dir = -2;
        holdingL = true;
    }
    //Jump for the player.
    if (keyCode == UP || key == 'w' || key == 'W')
        player.jump();

    //Add a bullet to the ArrayList when the player fires.
    if (key == ' '){
        if (bullets.size() < 3){
            //Calculate the x and y coordinates of the bullet before
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();;
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();;
            bullets.add(new Bullet(newX, newY, recentAngle));
        }
    }
}
public class Bullet{
    private float x;
    private float y;
    private float angle;
    private PVector pos;
    private PVector velocity;

    public Bullet(float x, float y, float angle){
        this.x = x;
        this.y = y;
        this.angle = angle;
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
        fill(0);
        ellipse(pos.x, pos.y, 10, 10);
        pos.add(velocity.x * 15, velocity.y * 15);
    }

}
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
    private float gravity = 0.4f;
    //Variable to keep track of the velocity in the game.
    private float velocity = 0;
    //Count to limit the jumps to one.
    private int count = 0;
    //Health for the player.
    private int health = 10;

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

    public int getHealth(){
        return health;
    }

    public int getTankH(){
        return bodyH;
    }

    public int getTankW(){
        return bodyW;
    }

    public void takeHit(){
        health--;
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
        rotate(0);
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
    public void settings() {  size(800,800); }
    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "JumpingTanks" };
        if (passedArgs != null) {
          PApplet.main(concat(appletArgs, passedArgs));
        } else {
          PApplet.main(appletArgs);
        }
    }
}
