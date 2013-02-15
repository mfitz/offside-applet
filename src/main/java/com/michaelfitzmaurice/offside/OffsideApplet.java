package com.michaelfitzmaurice.offside;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;

/**
 * An applet to provide an interactive, 2D visual explanation of football's
 * offside rule. The applet places a number of icons representing players
 * (attackers of one team and defenders of the other) on a football pitch, seen
 * from plan view.
 * <p>
 * The user has the ability to drag each of these players into any desired
 * position on the pitch, in order to create a mock-up of a situation that could
 * arise in a real football match. The onside/offside status of the play at that
 * point (were the ball to be played forward) can then be determined by clicking
 * a button. The applet marks the 'offside threshold' (the point beyond which
 * attackers must not advance if they are to remain onside) and reports the
 * status of the play.
 * <p>
 * The user also has the ability to draw arbitrary temporary shapes on the
 * screen (in the manner of Andy Gray on Sky Sports...).
 * 
 * @author Michael Fitzmaurice, April 2003
 */
public class OffsideApplet extends Applet implements ActionListener {
    
    // :TODO:
    //
    // - add a colour key indicating which team are attacking
    // - replace all hardcoded config & magic numbers
    // - make number and properties of players configurable via params
    // - highlight multiple offside players
    // - use a canvas in the centre of the applet so that image is not clipped

    private static final long serialVersionUID = 1L;
    
    // using jdk 1.2 style color constants for backwards compatibility
    private static final Color PLAYER_OUTLINE_COLOUR = Color.white;
    private static final Color OFFSIDE_BOUNDARY_COLOUR = Color.red;
    private static final Color ATTACKERS_SHIRT_COLOR = new Color(72, 100, 255);
    private static final Color DEFENDERS_SHIRT_COLOR = new Color(255, 25, 16);
    private static final Color GOALKEEPER_SHIRT_COLOR = new Color(10, 150, 10);

    private static final int PLAYER_HEIGHT = 30;
    private static final int PLAYER_WIDTH = 30;

    private Player[] defenders, attackers;
    private Player offsidePlayer;
    private Player selectedPlayer;

    private boolean drawOffsideThreshold;

    private int mouseXLocation, mouseYLocation;
    private int canvasHeight, canvasWidth;
    
    private Image pitchImage;
    private Button playBallButton;
    private Label messageLabel;
    
    public void init() {
        setLayout(new BorderLayout());

        canvasHeight = getBounds().height;
        canvasWidth = getBounds().width;

        // how wide is 1 / 10 of the applet? use this to position
        // players in different scetions around the pitch
        int tenPercentOfWidth = canvasWidth / 10;

        defenders = new Player[5];
        defenders[0] = new Player(DEFENDERS_SHIRT_COLOR, 5,
                tenPercentOfWidth, canvasHeight / 2);
        defenders[1] = new Player(DEFENDERS_SHIRT_COLOR, 3,
                tenPercentOfWidth * 3, canvasHeight / 3);
        defenders[2] = new Player(DEFENDERS_SHIRT_COLOR, 2,
                tenPercentOfWidth * 6, canvasHeight / 4);
        defenders[3] = new Player(DEFENDERS_SHIRT_COLOR, 4,
                tenPercentOfWidth * 8, canvasHeight / 2);
        defenders[4] = new Player(GOALKEEPER_SHIRT_COLOR, 1,
                (canvasWidth / 2) - PLAYER_WIDTH, PLAYER_HEIGHT);

        attackers = new Player[2];
        attackers[0] = new Player(ATTACKERS_SHIRT_COLOR, 9,
                tenPercentOfWidth * 4, canvasHeight / 2);
        attackers[1] = new Player(ATTACKERS_SHIRT_COLOR, 7,
                (canvasWidth / 2), canvasHeight / 3);

        this.addMouseListener(new MouseClickHandler());
        this.addMouseMotionListener(new MouseMotionHandler());

        // load up background image & scale to fit applet height & width
        String pathToPitchImage = getParameter("pitch-image");
        String codeBaseDir = getCodeBase().getPath().toString();
        System.out.println(codeBaseDir);
        File imageFile =  
            new File(getCodeBase().getPath().toString(), pathToPitchImage);
        if (imageFile.exists() == false) {
            // fail fast - applet doesn't make sense without the image
            throw new RuntimeException("Could not find image file " 
                                        + pathToPitchImage);
        }
        
        pitchImage = 
            getImage(getDocumentBase(), pathToPitchImage);
        pitchImage = 
            pitchImage.getScaledInstance(canvasWidth,
                                        canvasHeight, 
                                        Image.SCALE_DEFAULT);

        // set up GUI components
        messageLabel = new Label();
        messageLabel.setAlignment(Label.CENTER);
        messageLabel.setBackground(new Color(230, 230, 10));
        messageLabel.setForeground(OFFSIDE_BOUNDARY_COLOUR);
        playBallButton = new Button("Offside?");
        playBallButton.addActionListener(this);
        Panel buttonPanel = new Panel();
        buttonPanel.setBackground(Color.gray);
        buttonPanel.add(playBallButton);

        this.add(messageLabel, BorderLayout.NORTH);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void update(Graphics g) {
        // override update() to reduce flicker - no need to bother
        // redrawing the background on the on-screen Graphics object,
        // since the whole thing is copied from the off-screen Graphics
        // object once it is complete. To clear the on-screen background
        // first is the default behaviour - do not want this duplication
        paint(g);
    }

    public void paint(Graphics g) {
        // use double buffering to reduce flicker - draw
        // the overall picture in stages offscreen
        Image offScreenImage = createImage(canvasWidth, canvasHeight);
        Graphics offScreenGraphics = offScreenImage.getGraphics();
        this.drawPitch(offScreenGraphics);

        for (int i = 0; i < defenders.length; i++) {
            defenders[i].drawSelf(offScreenGraphics);
        }

        for (int i = 0; i < attackers.length; i++) {
            attackers[i].drawSelf(offScreenGraphics);
        }

        if (drawOffsideThreshold) {
            this.drawOffsideBoundary(offScreenGraphics);
            drawOffsideThreshold = false;
        }

        // copy the finished off-screen image onto the canvas in one go
        g.drawImage(offScreenImage, 0, 0, this);
    }

    /**
     * Helper method to draw a line on screen marking the threshold of the
     * offside region, given the current scene.
     */
    private void drawOffsideBoundary(Graphics g) {
        // get the location of the last defender (used later
        // on to find the second-to-last defender)
        int lastDefenderPosition = canvasHeight;

        for (int i = 0; i < defenders.length; i++) {
            Player defender = defenders[i];
            int defenderPosition = defender.getYPosition();

            if (defenderPosition < lastDefenderPosition) {
                lastDefenderPosition = defenderPosition;
            }
        }

        // offside threshold is the position of the second-to-last defender
        int offsideThreshold = canvasHeight;

        for (int i = 0; i < defenders.length; i++) {
            Player defender = defenders[i];
            int defenderPosition = defender.getYPosition();

            // does this player represent the offside threshold?
            if ((defenderPosition > lastDefenderPosition)
                    && (defenderPosition < offsideThreshold)) {
                offsideThreshold = defenderPosition;
            }
        }

        g.setColor(OFFSIDE_BOUNDARY_COLOUR);
        g.drawLine(0, offsideThreshold, canvasWidth, offsideThreshold);
    }

    private void drawPitch(Graphics g) {
        g.drawImage(pitchImage, 0, 0, this);
    }

    /**
     * Helper method to return the (most) offside attacking player given the
     * current scene. If no players are offside, null is returned.
     */
    private Player getOffsidePlayer() {
        Player offsideAttacker = null;

        // relative position of the foremost attacker in the Y dimension
        // determines onside/offside status - record this position
        Player foremostAttacker = attackers[0];
        int foremostAttackerPosition = foremostAttacker.getYPosition();

        for (int i = 1; i < attackers.length; i++) {
            Player attacker = attackers[i];
            int attackerPosition = attacker.getYPosition();

            // values of a lesser magnitude are further forward, since
            // the top of the screen is at 0 pixels in the Y plane, and
            // the goal line is at the top end of the scene
            if (attackerPosition < foremostAttackerPosition) {
                foremostAttacker = attacker;
                foremostAttackerPosition = attackerPosition;
            }
        }

        int numberOfGoalsideDefenders = 0;

        for (int i = 0; i < defenders.length; i++) {
            Player defender = defenders[i];
            int defenderPosition = defender.getYPosition();

            // level is considered onside (hence <=)
            if (defenderPosition <= foremostAttackerPosition)
                numberOfGoalsideDefenders++;
        }

        // there must be >= 2 defenders (goalkeeper counts) between the
        // foremost attacker and the goal-line in order for play to be onside
        if (numberOfGoalsideDefenders < 2)
            offsideAttacker = foremostAttacker;

        return offsideAttacker;
    }

    public void actionPerformed(ActionEvent e) {
        String message = "Play is onside. Nothin' to see here...";

        offsidePlayer = this.getOffsidePlayer();
        if (offsidePlayer != null) {
            message = offsidePlayer + " - OFFSIDE!";
        }

        messageLabel.setText(message);
        drawOffsideThreshold = true;

        repaint();
    }
    
    public String getAppletInfo() {
        return "Offside Applet version 1.0 by Michael Fitzmaurice";
    }

    /////////////// INNER CLASSES FOR MOUSE EVENT HANDLING ///////////////

    private class MouseClickHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            // the user has pressed the mouse button down but not released it -
            // this could be the start of an attempt to drag one of the player
            // icons to another location. In order to test whether or not this
            // is the case, the coordinates of the mouse pointer at the time of
            // the click must be compared to the coordinates of each player
            mouseXLocation = e.getX();
            mouseYLocation = e.getY();

            boolean foundSelection = false;

            for (int i = 0; i < defenders.length; i++) {
                Player player = defenders[i];

                if (player.isSelected(mouseXLocation, mouseYLocation)) {
                    foundSelection = true;
                    selectedPlayer = player;
                    break;
                }
            }

            if (!foundSelection) {
                for (int i = 0; i < attackers.length; i++) {
                    Player player = attackers[i];

                    if (player.isSelected(mouseXLocation, mouseYLocation)) {
                        foundSelection = true;
                        selectedPlayer = player;
                        break;
                    }
                }
            }

            if (!foundSelection) {
                selectedPlayer = null;
                messageLabel.setText("No player selected - switching to "
                        + "drawing mode");
            } else {
                messageLabel.setText("Moving " + selectedPlayer);
            }
        }
    }

    private class MouseMotionHandler extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            int newXLocation = e.getX();
            int newYLocation = e.getY();

            if (selectedPlayer != null) {
                // find out by how much the player should move
                int xMovement = newXLocation - mouseXLocation;
                int yMovement = newYLocation - mouseYLocation;
                int oldXPosition = selectedPlayer.getXPosition();
                int oldYPosition = selectedPlayer.getYPosition();

                selectedPlayer.setXPosition(oldXPosition + xMovement);
                selectedPlayer.setYPosition(oldYPosition + yMovement);

                // this calls update() automatically
                repaint();
            } else {
                // allow the user to draw (temporary) lines & shapes on
                // the screen if they have not selected a player to move
                Graphics g = getGraphics();
                g.setColor(Color.black);

                g.drawLine(mouseXLocation, mouseYLocation, newXLocation,
                        newYLocation);
            }

            mouseXLocation = newXLocation;
            mouseYLocation = newYLocation;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * An inner class to represent a player - essentially just an object that is
     * aware of:
     * <ul>
     * <li>the location on screen of the icon representing it
     * <li>how to draw this icon onto the screen on demand
     * </ul>
     */
    private class Player {
        
        // the thickness of the outer circle around each player
        private static final int BORDER_SIZE = 6;
        
        private int xPosition, yPosition, height, width;
        private int shirtNumber;
        private Color color;

        /**
         * Creates a new instance of <code>Player</code> with the desired
         * configuration
         * 
         * @param color
         *            The colour of this player's shirt
         * @param number
         *            The shirt number of this player
         * @param x
         *            The starting position of this player in the horizontal
         *            plane of the enclosing scene
         * @param y
         *            The starting position of this player in the vertical plane
         *            of the enclosing scene
         */
        Player(Color color, int number, int x, int y) {
            this.color = color;
            this.shirtNumber = number;
            this.xPosition = x;
            this.yPosition = y;
            this.height = PLAYER_HEIGHT;
            this.width = PLAYER_WIDTH;
        }

        /**
         * Paints this player onto the supplied Graphics context in the player's
         * current position within the scene
         * 
         * @param g
         *            The Graphics context on which to paint
         */
        public void drawSelf(Graphics g) {
            // draw the enclosing circle (the coloured border)
            g.setColor(PLAYER_OUTLINE_COLOUR);
            g.fillOval(xPosition, yPosition, width + BORDER_SIZE,
                    height + BORDER_SIZE);

            // draw the inner circle in the player's shirt colour
            g.setColor(color);
            g.fillOval(xPosition + (BORDER_SIZE / 2), yPosition
                    + (BORDER_SIZE / 2), width, height);

            // draw the player's shirt number in the outline colour
            g.setColor(PLAYER_OUTLINE_COLOUR);
            g.drawString("" + shirtNumber, xPosition + (width / 2),
                    yPosition + (height - (height / 4)));
        }

        /**
         * Tests whether or not a given location within the current scene falls
         * inside the boundaries of this player's on-screen icon
         * 
         * @param x
         *            The location within the x plane of the position to compare
         * @param y
         *            The location within the y plane of the position to compare
         * 
         * @return True if the supplied on-screen location falls within the
         *         boundaries of this player's on-screen icon
         */
        public boolean isSelected(int x, int y) {
            // both x & y coordinates must be within the on-screen
            // boundaries of the image representing this object
            boolean insideX = false;
            boolean insideY = false;

            insideX = (x >= xPosition) && (x <= (xPosition + width));

            if (insideX)
                insideY = (y >= yPosition) && (y <= (yPosition + height));

            return (insideX && insideY);
        }

        public int getXPosition() {
            return xPosition;
        }

        public void setXPosition(int x) {
            xPosition = x;
        }

        public int getYPosition() {
            return yPosition;
        }

        public void setYPosition(int y) {
            yPosition = y;
        }

        public String toString() {
            return "Player number " + shirtNumber;
        }
    }

}
