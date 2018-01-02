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
Platforms plats;
HealthBar bar;
Power power;
ArrayList<MenuButton> buttons = new ArrayList<MenuButton>();
ArrayList<Bullet> bullets = new ArrayList<Bullet>();
ArrayList<EnemyBullet> enemyBullets = new ArrayList<EnemyBullet>();
float recentAngle = 30;
int dir = 0;
int gravity = 10;
boolean holdingR, holdingL, playGame, displayIncreasing;
float powerDisplayW = 30;
boolean firstClient = false;
DatagramChannel dc;
String address = "127.0.0.1";
int portNum = 8765;
int speedCount = 0;
int scene = 0;
PImage titleText, controlImage, powerImage;



public void setup(){
    
    
    titleText  = loadImage("Title.png");
    controlImage = loadImage("Controls.png");
    powerImage = loadImage("PowerUps.png");

    buttons.add(new MenuButton(width/2, height/2, "Play"));
    buttons.add(new MenuButton(width/2, height/2 + 50, "Controls"));
    buttons.add(new MenuButton(width/2, height/2 + 100, "Power-Ups"));
    buttons.add(new MenuButton(width/2, height/2 + 150, "Network"));

    displayIncreasing = true;

    resetGame();

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

public void resetGame(){
    scene = 0;
    player = new Tank();
    enemy = new EnemyTank();
    plats = new Platforms();
    bar = new HealthBar(player.getHealth());
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    power = null;
    if (firstClient){
        plats.getPlats().get(0).setMove(true);
    }
}

public void draw(){
    background(236,236,236);
    switch(scene){
        case 0:
            drawMenu();
            break;
        case 1:
            drawGame();
            break;
        case 2:
            drawControls();
            break;
        case 3:
            drawPowers();
            break;
    }
}

public void drawMenu(){
    showButton();
    pushMatrix();
    fill(0);
    textSize(92);   
    int titleW = 600;
    int titleH = 270;
    image(titleText, width/2 - (titleW/2), 50, titleW, titleH);
    popMatrix();
    player.move(dir);
    player.gravity();
    player.bound();
    recentAngle = calculateArmAngle();
    player.setAngle(recentAngle);
    player.showBody();
    player.showArm();
}

public void showButton(){
    for (MenuButton m : buttons){
        m.setHighlight(false);
        if (mouseX > m.getX() && mouseX < m.getX() + m.getW()){
            if (mouseY > m.getY() && mouseY < m.getY() + m.getH())
                m.setHighlight(true);
        }
        m.show();
    }
}

public void drawControls(){
    image(controlImage, width/2 - 450, 0, 900, 900);
}

public void drawPowers(){
    if (displayIncreasing)
        powerDisplayW += 0.5f;
    else
        powerDisplayW -= 0.5f;
    if (powerDisplayW > 60 || powerDisplayW < 30)
        displayIncreasing = !displayIncreasing;

    fill(200, 0, 0);
    ellipse(800, 60, powerDisplayW, powerDisplayW);
    fill(0,0,200);
    ellipse(800, 220, powerDisplayW, powerDisplayW);
    fill(214, 123, 12);
    ellipse(880, 410, powerDisplayW, powerDisplayW);
    image(powerImage, width/2 - 450, 0, 900, 900);
}

public void drawGame(){
    //System.out.println(frameRate);
    player.move(dir);
    player.gravity();
    player.bound();

    //Calculate the angle of the arm.
    recentAngle = calculateArmAngle();
    player.setAngle(recentAngle);

    //Show the graphics
    bar.show();
    showPowerList();
    plats.showPlatforms();
    showPower();
    showAndBoundBullets();
    showEnemyBullets();
    enemy.showBody();
    player.showBody();
    player.showArm();
    checkHit();
    landPlats();
    hitPower();

    boolean reset = false;
    if (player.displayDead())
        reset = true;

    //Pack the appropriate coordinates into strings and send them.
    String loc = player.getX() + "," + player.getY() + "," + player.getAngle();
    if (player.isShield())
        loc += "," + "S";
    else
        loc += "," + "N";
    //If we are the first client we handle the position of the platform.
    if (firstClient)
        loc += "," + plats.getPlats().get(0).getX() + "," + plats.getPlats().get(0).getY();
    else
        loc += "," + "F" + "," + "F";
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

    if (reset){
        resetGame();
    }
}

public void showAndBoundBullets(){
    for (int i = 0; i < bullets.size(); i++){
        boolean removed = false;
        //Check to see if it has hit a platform.
        for (Platform p : plats.getPlats()){
            if (bullets.get(i).getX() > p.getX() && bullets.get(i).getX() < p.getX() + p.getW()){
                if (bullets.get(i).getY() > p.getY() && bullets.get(i).getY() < p.getY() + p.getH()){
                    removed = true;
                }
            }
        }
        if (removed){
            bullets.remove(i);
        }
        //Check to see if the bullet is out of bounds.
        else if (bullets.get(i).getY() > height || bullets.get(i).getY() < 0){
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
            if (b.getY() > player.getY() && b.getY() < player.getY() + player.getTankH()){
                //System.out.println("HIT!");
                if (player.takeHit())
                    bar.decreaseSize();
                break;
            }
        }
    }
}

public void showPower(){
    if (power != null)
        power.show();
}

public void hitPower(){
    if (power != null){
        if (power.getX() > player.getX() && power.getX() < player.getX() + player.getTankW()){
            if (power.getY() > player.getY() && power.getY() < player.getY() + player.getTankH()){
                if (power.getType() == 0)
                    power.usePower();
                else
                    player.givePower(power);
                try{
                    ByteBuffer powerBuff = ByteBuffer.wrap("0".getBytes());
                    dc.send(powerBuff, new InetSocketAddress(address, portNum));
                }
                catch(Exception e){
                    System.out.println("Exception in hitPower: " + e);
                }
                power = null;
            }
        }
    }
}

public void setPower(int type){
    if (type == 0)
        power = new PowerHealth(plats.getPlats().get(0), player, bar);
    else if (type == 1)
        power = new PowerShot(plats.getPlats().get(0), player, bar);
    else if (type == 2)
        power = new PowerShield(plats.getPlats().get(0), player, bar);

    else
        System.out.println("Inavlid type");


}

public void landPlats(){
    for (Platform p : plats.getPlats()){
        //Temp floats for important points on the tank.
        float playerCenter = player.getX() + player.getTankW()/2;
        float playerTempY = player.getY() + player.getTankH();
        //If we are traveling down.
        if (player.getVelocity() > 0){
            //Compare the Y values
            if (playerTempY > p.getY() && playerTempY < p.getY() + p.getH()){
                //Compare the X values.
                if (playerCenter > p.getX() && playerCenter < p.getX() + p.getW()){
                    player.resetVelocity();
                    player.resetCount();
                    player.setY(p.getY() - player.getTankH());
                }
            }
        }
    }
}

public void showPowerList(){
    int startX = width - 30;
    int startY = 30;
    for (Power p : player.getPowers()){
        fill(p.getColor());
        //stroke();
        ellipse(startX, startY, 30, 30);
        startX -= 40;
    }
}

public void showEnemyBullets(){
    for (EnemyBullet b : enemyBullets){
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
            message = message.trim();
            String[] coordinates = message.split(",");

            if (coordinates[0].equals("F")){
                firstClient = true;
                plats.getPlats().get(0).setMove(true);
            }
            else if (coordinates[0].equals("0")){
                power = null;
            }
            else if(coordinates[0].equals("1")){
                setPower(Integer.parseInt(coordinates[1]));
            }
            else{

                /*
                 *[0] Tank X pos.
                 *[1] Tank Y pos.
                 *[2] Tank arm Angle pos.
                 *[3] Player shield.
                 *[4] Plat X pos
                 *[5] Plat Y pos
                 *[6-X] Bullet X and Y pos.
                 */

                enemy.setX(Float.parseFloat(coordinates[0]));
                enemy.setY(Float.parseFloat(coordinates[1]));
                enemy.setAngle(Float.parseFloat(coordinates[2]));

                if (coordinates[3].equals("S"))
                    enemy.setShield(true);
                else
                    enemy.setShield(false);


                if (!firstClient){
                    plats.getPlats().get(0).setX(Float.parseFloat(coordinates[4]));
                    plats.getPlats().get(0).setY(Float.parseFloat(coordinates[5]));
                }

                int bulletCount = 0;
                for (int i = 6; i < coordinates.length; i++){
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
        dir = 3;
        holdingR = true;
    }
    if (keyCode == LEFT || key == 'a' || key == 'A'){
        dir = -3;
        holdingL = true;
    }
    //Jump for the player.
    if (keyCode == UP || key == 'w' || key == 'W')
        player.jump();

    //Add a bullet to the ArrayList when the player fires.
    if (key == ' '){
        if (bullets.size() < 3){
            if (speedCount >= 5){
                speedCount = 0;
                player.setFastBullet(false);
            }
            if (player.isFastBullet())
                speedCount++;

            //Calculate the x and y coordinates of the bullet before
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();

            if (player.isFastBullet())
                bullets.add(new Bullet(newX, newY, recentAngle, true));
            else
                bullets.add(new Bullet(newX, newY, recentAngle, false));
        }
    }
    if (keyCode == ENTER){
        player.usePower();
    }
}

public void mouseClicked(){
    if (scene == 0){
        if (mouseX > buttons.get(0).getX() && mouseX < buttons.get(0).getX() + buttons.get(0).getW()){
            if (mouseY > buttons.get(0).getY() && mouseY < buttons.get(0).getY() + buttons.get(0).getH())
                scene = 1;
        }
        if (mouseX > buttons.get(1).getX() && mouseX < buttons.get(1).getX() + buttons.get(1).getW()){
            if (mouseY > buttons.get(1).getY() && mouseY < buttons.get(1).getY() + buttons.get(1).getH())
                scene = 2;
        }
        if (mouseX > buttons.get(2).getX() && mouseX < buttons.get(2).getX() + buttons.get(2).getW()){
            if (mouseY > buttons.get(2).getY() && mouseY < buttons.get(2).getY() + buttons.get(2).getH())
                scene = 3;
        }
    }
    else if (scene == 2 || scene == 3){
        scene = 0;
    }

    else{
        if (bullets.size() < 3){
            if (bullets.size() < 3){
                if (speedCount >= 5){
                    speedCount = 0;
                    player.setFastBullet(false);
                }
                if (player.isFastBullet())
                    speedCount++;
            }

            //Calculate the x and y coordinates of the bullet before
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();
            if (player.isFastBullet())
                bullets.add(new Bullet(newX, newY, recentAngle, true));
            else
                bullets.add(new Bullet(newX, newY, recentAngle, false));
        }
    }
}
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
            fill(214, 123, 12);
        else
            fill(0);
        ellipse(pos.x, pos.y, 10, 10);
        pos.add(velocity.x * speed, velocity.y * speed);
        popMatrix();
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
public class HealthBar{
    private int size;
    private int w;
    private int incr;
    private int MAX_SIZE;

    public HealthBar(int h){
        incr = 2;
        size = h * incr;
        MAX_SIZE = h * incr;
        w = 20;

    }

    public void decreaseSize(){
        size -= incr;
        if (size < 0)
            size = 0;
    }

    public void increaseSize(int times){
        size += incr * times;
        if (size > MAX_SIZE)
                size = MAX_SIZE;
    }

    public void show(){
        pushMatrix();
        //if (size/incr > 100)

        fill(0,200,0);
        rect(50, 10, size, w);
        fill(0);
        textSize(20);
        text(size/2, 5 , 26 );
        popMatrix();
    }
}
public class MenuButton{
    private float w, h, x, y;
    private boolean highlight;
    private String text;

    public MenuButton(int x, int y, String t){
        w = 600;
        h = 40;
        this.x = x - w/2;
        this.y = y;
        text = t;
        highlight = false;
    }

    public void setHighlight(boolean b){
        highlight = b;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getW(){
        return w;
    }

    public float getH(){
        return h;
    }

    public void show(){
        pushMatrix();

        textSize(30);
        noStroke();
        if (!highlight){
            fill(0);
            rect(x, y, w, h);
            fill(236,236,236);
            text(text, x + (w/2 - textWidth(text)/2), y + (h/2 + textAscent()/2) - 5);
        }
        else{
            fill(236,236,236);
            rect(x, y, w, h);
            fill(0);
            text(text, x + (w/2 - textWidth(text)/2), y + (h/2 + textAscent()/2) - 5);
        }
        popMatrix();
    }


}
public class Platforms{
    private ArrayList<Platform> plats = new ArrayList<Platform>();

    public ArrayList<Platform> getPlats(){
        return plats;
    }

    public Platforms(){
        int midHeight = 25;
        int midWidth = 70;
        plats.add(new Platform(width/2 - midWidth/2, height/2, midWidth, midHeight));
        plats.add(new Platform(0, height/2 - 200, 200, 25));
        plats.add(new Platform(0, height/2 + 200, 200, 25));
        plats.add(new Platform(width - 200, height/2 + 200, 200, 25));
        plats.add(new Platform(width - 200, height/2 - 200, 200, 25));
    }

    public void showPlatforms(){
        for (Platform p : plats){
            p.show();
        }
    }
}

class Platform{
    private float x, y;
    private int w, h, speed;
    private boolean moving, right;

    public Platform(float tempX, float tempY, int tempW, int tempH){
        x = tempX;
        y = tempY;
        w = tempW;
        h = tempH;
        moving = false;
        right = true;
        speed = 2;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public void setMove(boolean m){
        moving = m;
    }

    public int getW(){
        return w;
    }

    public int getH(){
        return h;
    }

    public void show(){
        pushMatrix();
        fill(0);
        if (moving){
            if (right)
                x += speed;
            else
                x -= speed;
            if (x < 0 || x + w > width)
                right = !right;
        }
        rect(x, y, w, h);
        popMatrix();
    }
}
public class Power{
    private float x;
    private float y;
    private int type;
    private float size;
    private boolean increase;
    private int c;
    private Platform midPlat;
    private Tank p;
    private HealthBar h;
    private Bullet b;

    public Power(Platform mid, Tank p, HealthBar h){
        midPlat = mid;
        this.p = p;
        this.h = h;
        this.b = b;
        increase = true;
        size = 20;
        c = color(200, 0, 0);
        x = midPlat.getX() + (midPlat.getW()/2);
        y = midPlat.getY() - (midPlat.getH());
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public int getType(){
        return type;
    }

    private float getSize(){
        return size;
    }

    private Tank getTank(){
        return p;
    }

    private HealthBar getBar(){
        return h;
    }

    private void setColor(int tempC){
        c = tempC;
    }

    public int getColor(){
        return c;
    }

    private void setType(int t){
        type = t;
    }

    public void usePower(){

    }

    public void show(){
        pushMatrix();
        x = midPlat.getX() + (midPlat.getW()/2);
        y = midPlat.getY() - (midPlat.getH());
        if (increase)
            size += 0.3f;
        else
            size -= 0.3f;
        if (size > 40 || size < 20)
            increase = !increase;

        fill(c);
        ellipse(x, y, size, size);
        popMatrix();
    }
}

class PowerHealth extends Power{
    public PowerHealth(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        int healthColor = color(200, 0, 0);
        super.setColor(healthColor);
        super.setType(0);
    }

    public void usePower(){
        int tempHealth = super.getTank().getHealth() + 30;
        super.getTank().setHealth(tempHealth);
        super.getBar().increaseSize(30);
    }
}

class PowerShot extends Power{
    public PowerShot(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        int shotColor = color(214, 123, 12);
        super.setColor(shotColor);
        super.setType(1);
    }

    public void usePower(){
        //System.out.println("Power used");
        super.getTank().setFastBullet(true);
    }
}

class PowerShield extends Power{
    public PowerShield(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        int healthColor = color(0, 0, 200);
        super.setColor(healthColor);
        super.setType(2);
    }

    public void usePower(){
        super.getTank().giveShield();
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
    private int health = 100;
    //Time slot to make sure one bullet can't do more than one hit.
    private long pastTime = 0;
    //Image for the tank to draw.
    private PImage img = loadImage("tank1.png");
    //Boolean for the speed of the bullet
    private boolean speed = false;
    //Boolean for the shield of the tank.
    private boolean shield = false;

    private int shieldCount = 0;
    //List to hold the power ups.
    private ArrayList<Power> powerUps = new ArrayList<Power>();

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

    public float getY(){
        return y;
    }

    public float getVelocity(){
        return velocity;
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

    public boolean isFastBullet(){
        return speed;
    }

    public void setFastBullet(boolean b){
        speed = b;
    }

    public ArrayList<Power> getPowers(){
        return powerUps;
    }

    public void setHealth(int tempHealth){
        health = tempHealth;
        if (health > 10)
            health = 10;
    }

    public void givePower(Power pUp){
        powerUps.add(pUp);
    }

    public void usePower(){
        if (powerUps.size() > 0){
            powerUps.get(0).usePower();
            powerUps.remove(0);
        }
    }

    public void giveShield(){
        shield = true;
        shieldCount = 0;
    }

    public boolean isShield(){
        return shield;
    }

    public boolean takeHit(){
        //pastTime = System.currentTimeMillis();
        if (!shield){
            health--;
            return true;
        }
        if (shield){
            shieldCount++;
        }
        if (shield && shieldCount > 30){
            shield = false;
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

    public boolean displayDead(){
        if (health <= 0){
            x = -1000;
            y = -1000;
            return true;
        }
        return false;
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
        rotate(0);
        translate(x,y);
        rotate(armAngle);
        fill (106, 108, 0);
        rect(0, -armH/2, armW, armH);
    }

    public void showBody(){
        pushMatrix();
        rotate(0);
        image(img, x-(bodyW/2), y, bodyW, bodyH);
        if (shield){
            noFill();
            strokeWeight(3);
            stroke(0,0,200);
            ellipse(x, y, 140, 140);

        }
        popMatrix();
        noStroke();

    }
}
    public void settings() {  size(1300,900, P2D);  noSmooth(); }
    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "JumpingTanks" };
        if (passedArgs != null) {
          PApplet.main(concat(appletArgs, passedArgs));
        } else {
          PApplet.main(appletArgs);
        }
    }
}
