import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;

Tank player;
EnemyTank enemy;
Platforms plats;
HealthBar bar;
Power power;
ArrayList<Bullet> bullets = new ArrayList<Bullet>();
ArrayList<EnemyBullet> enemyBullets = new ArrayList<EnemyBullet>();
float recentAngle = 30;
int dir = 0;
int gravity = 10;
boolean holdingR, holdingL;
DatagramChannel dc;
String address = "127.0.0.1";
int portNum = 8765;


void setup(){
    size(1300,900, P2D);
    noSmooth();

    player = new Tank();
    enemy = new EnemyTank();
    plats = new Platforms();
    bar = new HealthBar(player.getHealth());
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    power = new PowerHealth(plats.getPlats().get(0), player, bar);

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

void draw(){
    background(236,236,236);
    //System.out.println(frameRate);
    player.move(dir);
    player.gravity();
    player.bound();

    //Calculate the angle of the arm.
    recentAngle = calculateArmAngle();
    player.setAngle(recentAngle);

    //Show the arm and body of the tanks.
    bar.show();
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
                power = null;
            }
        }
    }
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

void keyReleased(){
    if (keyCode == RIGHT || key == 'd' || key == 'D')
        holdingR = false;

    if (keyCode == LEFT || key == 'a' || key == 'A')
        holdingL = false;

    //Check that we have both keys lifted up to stop moving.
    if (!holdingL && !holdingR)
        dir = 0;
}

void keyPressed(){
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
            //Calculate the x and y coordinates of the bullet before
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();;
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();;
            bullets.add(new Bullet(newX, newY, recentAngle));
        }
    }
}

void mouseClicked(){
    if (bullets.size() < 3){
        //Calculate the x and y coordinates of the bullet before
        float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();;
        float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();;
        bullets.add(new Bullet(newX, newY, recentAngle));
    }
}
