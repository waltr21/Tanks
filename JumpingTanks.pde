Tank player;
ArrayList<Bullet> bullets = new ArrayList<Bullet>();
float recentAngle = 30;
int dir = 0;
int gravity = 10;
boolean holdingR, holdingL;
void setup(){
    size(800,800);
    //fullScreen();
    player = new Tank();
    frameRate(60);
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

    //Show the arm and body of the tank.
    showAndBoundBullets();
    player.showBody();
    player.showArm();

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

public float calculateArmAngle(){
    //Make a right triangle with the mouse and tank pos.
    float opp = (mouseY - player.getArmY());
    float adj = (mouseX - player.getArmX());
    //Handle divide m=by 0 errors.
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
        //Calculate the x and y coordinates of the bullet before
        float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();;
        float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();;
        bullets.add(new Bullet(newX, newY, recentAngle));
    }
}