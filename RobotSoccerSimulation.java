import java.awt.Graphics;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class RobotSoccerSimulation extends JPanel {
    private static final long serialVersionUID = -5228718339006830546L;

    private static final double WIDTH = 400;
    private static final double HEIGHT = 600;

    private double PLAYER_RADIUS = 15;
    private double ENEMY_RADIUS = 20;
    private double PLAYER_SPEED = 1.3;
    private double ENEMY_SPEED = 1.8;
    private static double FRICTION = 0.0009;

    RobotSoccerSimulation(double PLAYER_RADIUS, double ENEMY_RADIUS, double PLAYER_SPEED, double ENEMY_SPEED, double FRICTION) {
        this.PLAYER_RADIUS = PLAYER_RADIUS;
        this.ENEMY_RADIUS = ENEMY_RADIUS;
        this.PLAYER_SPEED = PLAYER_SPEED;
        this.ENEMY_SPEED = ENEMY_SPEED;
        RobotSoccerSimulation.FRICTION = FRICTION;
    }

    private volatile String endMessage;

    static class Ball {
        private double x;
        private double y;
        private double radius;
        private double speed;
        private Color color;

        Ball(double x, double y, double radius, double speed, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
            this.color = color;
        }

        void moveToward(double targetX, double targetY) {
            var dx = targetX - this.x;
            var dy = targetY - this.y;
            var v = this.speed / Math.hypot(dx, dy);
            this.x = constrain(this.x + v * dx, this.radius, WIDTH - this.radius);
            this.y = constrain(this.y + v * dy, this.radius, HEIGHT - this.radius);
        }

        private static double constrain(double value, double low, double high) {
            return Math.min(Math.max(low, value), high);
        }

        void applyFriction() {
            this.speed = constrain(this.speed - FRICTION, 0, Double.POSITIVE_INFINITY);
        }

        boolean inside(Goal goal) {
            return (
            this.x - this.radius > goal.x - goal.w / 2 &&
            this.x + this.radius < goal.x + goal.w / 2 &&
            this.y - this.radius > goal.y - goal.h / 2 &&
            this.y + this.radius < goal.y + goal.h / 2
            );
        }
    }

    private static Ball[] balls; 

    private static RobotSoccerSimulation[] game;

    private static class Goal {
        double x = WIDTH / 2;
        double y = 0;
        double w = 100;
        double h = 100;
    }

    private static Goal goal = new Goal();

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (var ball : balls) {
            g.setColor(ball.color);
            g.fillOval((int) (ball.x - ball.radius), (int) (ball.y - ball.radius), (int) ball.radius * 2,
                    (int) ball.radius * 2);
        }
        g.setColor(new Color(255, 255, 255, 128));
        g.fillRect((int) (goal.x - goal.w / 2), (int) (goal.y - goal.h / 2), (int) goal.w, (int) goal.h);
        if (endMessage != null) {
            g.setFont(new Font("Arial", Font.PLAIN, 50));
            g.setColor(Color.RED);
            g.drawString(endMessage, 30, (int) HEIGHT / 2);
        }
    }

    private void runTheAnimation() {
        while (endMessage == null) {
            for (var i = 0; i < balls.length; i++) {
                balls[i].applyFriction();
                balls[i].moveToward(i == 0 ? goal.x : balls[0].x, i == 0 ? goal.y : balls[0].y);
            }
            adjustIfCollisions();
            endSimulationIfNecessary();
            repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }
    }
    
    private void endSimulationIfNecessary() {
        if (balls[0].speed <= 0) {
            endMessage = "Oh no!";
        } else if (balls[0].inside(goal)) {
            endMessage = "GOOOOAAAAAL!!!";
        }
    }

    private void adjustIfCollisions() {
        for (var b1 : balls) {
            for (var b2 : balls) {
                if (b1 != b2) {
                    var dx = b2.x - b1.x;
                    var dy = b2.y - b1.y;
                    var dist = Math.hypot(dx, dy);
                    var overlap = b1.radius + b2.radius - dist;
                    if (overlap > 0) {
                        var adjustX = (overlap/2) * (dx / dist);
                        var adjustY = (overlap / 2) * (dy / dist);
                        b1.x -= adjustX;
                        b1.y -= adjustY;
                        b2.x += adjustX;
                        b2.y += adjustY;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {

       try {
            if (args.length != 5) {
                throw new IllegalArgumentException("Exactly five arguments required");
            }
            double PLAYER_RADIUS = Double.parseDouble(args[0]);
            double ENEMY_RADIUS = Double.parseDouble(args[1]);
            double PLAYER_SPEED = Double.parseDouble(args[2]);
            double ENEMY_SPEED = Double.parseDouble(args[3]);
            double FRICTION = Double.parseDouble(args[4]); 
            System.out.println(game(PLAYER_RADIUS, ENEMY_RADIUS, PLAYER_SPEED, ENEMY_SPEED, FRICTION));
            } catch (NumberFormatException e) {
                System.err.println("Arguments must all be integers");
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
         }

        SwingUtilities.invokeLater(() -> {
             balls = new Ball[] { 
                new Ball(0.0, HEIGHT, PLAYER_RADIUS, PLAYER_SPEED, Color.BLUE),
                new Ball(WIDTH * 0.25, 40, ENEMY_RADIUS, ENEMY_SPEED, Color.RED),
                new Ball(WIDTH * 0.75, 40, ENEMY_RADIUS, ENEMY_SPEED, Color.RED),
                new Ball(WIDTH / 2, HEIGHT / 2, ENEMY_RADIUS, ENEMY_SPEED, Color.RED) 
            };

             game = new RobotSoccerSimulation[] {
                 new game(PLAYER_RADIUS, ENEMY_RADIUS, PLAYER_SPEED, ENEMY_SPEED, FRICTION)
             };

            var panel = new RobotSoccerSimulation(PLAYER_RADIUS, ENEMY_RADIUS, PLAYER_SPEED, ENEMY_SPEED, FRICTION);
            panel.setBackground(Color.GREEN.brighter());
            var frame = new JFrame("Robotic Soccer");
            frame.setSize((int) WIDTH, (int) HEIGHT);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(panel, BorderLayout.CENTER);
            frame.setVisible(true);
            new Thread(() -> panel.runTheAnimation()).start();
        });
    }
