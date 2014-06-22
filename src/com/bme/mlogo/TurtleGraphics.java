package com.bme.mlogo;
import com.bme.logo.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import static com.bme.logo.Primitives.*;

public class TurtleGraphics {

	static final int WIDTH  = 375;
	static final int HEIGHT = 315;
	private final Image buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private final Turtle turtle = new Turtle();
	private final TurtlePanel turtlePanel = new TurtlePanel(turtle, buffer);
	private final Graphics g = buffer.getGraphics();
	private final Environment e;

	public TurtleGraphics(Environment e) {
		this.e = e;
		primitiveTurtle(e);
	}

	private void setup() {
		if (turtle.window == null) {
			synchronized(buffer) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, WIDTH, HEIGHT);
			}
			
			turtle.window = this.turtlePanel;
			turtle.window.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		}
	}

	protected JComponent getTurtle(){
		if(turtle.window == null){ setup(); }
		return turtle.window;
	}
	
	public void primitiveTurtle(Environment e) {
		final LWord a = new LWord(LWord.Type.Name, "argument1");
		final LWord b = new LWord(LWord.Type.Name, "argument2");
		final LWord c = new LWord(LWord.Type.Name, "argument3");

		e.bind(new LWord(LWord.Type.Prim, "forward") {
			public void eval(Environment e) {
				setup();
				turtle.goalDistance = -num(e, a);
				e.pause();
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "back") {
			public void eval(Environment e) {
				setup();
				turtle.goalDistance = num(e, a);
				e.pause();
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "left") {
			public void eval(Environment e) {
				setup();
				turtle.goalDegrees = -num(e, a);
				e.pause();
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "right") {
			public void eval(Environment e) {
				setup();
				turtle.goalDegrees = num(e, a);
				e.pause();
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "clear") {
			public void eval(Environment e) {
				setup();
				synchronized(buffer) {
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, WIDTH, HEIGHT);
				}
				turtle.window.repaint();
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "home") {
			public void eval(Environment e) {
				setup();
				synchronized(buffer) {
					turtle.degrees = 90;
					turtle.heading = 0;
					turtle.x = WIDTH  / 2;
					turtle.y = HEIGHT / 2;
					turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				}
				turtle.window.repaint();
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "penup") {
			public void eval(Environment e) {
				synchronized(buffer) {
					turtle.pendown = false;
					turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				}
				turtle.window.repaint();
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "pendown") {
			public void eval(Environment e) {
				synchronized(buffer) {
					turtle.pendown = true;
					turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				}
				turtle.window.repaint();
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "setcolor") {
			public void eval(Environment e) {
				synchronized(buffer) {
					turtle.pencolor = new Color(
						num(e, a) & 0xFF,
						num(e, b) & 0xFF,
						num(e, c) & 0xFF
					);
					turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				}
				turtle.window.repaint();
			}
		}, a, b, c);
	}

	public boolean update() {
		if (turtle.goalDegrees != 0) {
			int rotated = (int)(Math.signum(turtle.goalDegrees) *
				Math.min(30, Math.abs(turtle.goalDegrees)));
			turtle.goalDegrees -= rotated;
			turtle.degrees += rotated;
			if(turtle.degrees >= 90){ turtle.heading = Math.abs((turtle.degrees - 90) % 360);	    }
			else				    { turtle.heading = 360 - Math.abs((turtle.degrees - 90) % 360); }
			if(turtle.heading == 360){ turtle.heading -= 360; }
			turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
			turtle.window.repaint();
			return turtle.goalDegrees == 0;
		}
		if (turtle.goalDistance != 0) {
			int traveled = (int)(Math.signum(turtle.goalDistance) *
				Math.min(5, Math.abs(turtle.goalDistance)));
			turtle.goalDistance -= traveled;

			synchronized(buffer) {
				int ox = (int)turtle.x;
				int oy = (int)turtle.y;
				turtle.x += traveled * Math.cos(Math.toRadians(turtle.degrees));
				turtle.y += traveled * Math.sin(Math.toRadians(turtle.degrees));
				turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				if (turtle.pendown) { 
					g.setColor(turtle.pencolor);
					g.drawLine(ox, oy, (int)turtle.x, (int)turtle.y);
				}
			}
			turtle.window.repaint();
			return turtle.goalDistance == 0;
		}
		return true;
	}
}

class Turtle {
	TurtlePanel window = null;

	int goalDegrees;
	int goalDistance;
	
	int degrees = 90;
	int heading = 0;
	double x = TurtleGraphics.WIDTH  / 2;
	double y = TurtleGraphics.HEIGHT / 2;

	boolean pendown = true;
	Color pencolor = Color.GREEN;
}

class TurtlePanel extends JPanel {
	static final long serialVersionUID = 1;

	private final Turtle turtle;
	private final Image  buffer;
	private String penstat;
	private String pencolor;
	private String turtlepos;
	private String turtlehdg;
	
	public TurtlePanel(Turtle turtle, Image buffer) {
		setPreferredSize(new Dimension(TurtleGraphics.WIDTH, TurtleGraphics.HEIGHT));
		this.turtle = turtle;
		this.buffer = buffer;
		this.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
	}
	
	public void penStat(boolean pendown, Color pencolor, int degrees, double x, double y){
		if(pendown == true)    { this.penstat = "Pen: DOWN"; }
		else 				   { this.penstat = "Pen: UP";   }
		
		x -= TurtleGraphics.WIDTH/2;
		y -= TurtleGraphics.HEIGHT/2;
		if(y != 0){ y = -y; }
		String xpos = (new DecimalFormat("#.##")).format(x);
		String ypos = (new DecimalFormat("#.##")).format(y);
		this.pencolor = ("Color: " + turtle.pencolor.getRed() + "R " + turtle.pencolor.getGreen() + 
				"G " + turtle.pencolor.getBlue() + "B");
		this.turtlepos = ("Position: " + xpos + "X " + ypos + "Y");
		this.turtlehdg = ("Heading: " + ((Integer)degrees).toString() + " DEG");
	}

	public void paint(Graphics g2) {
		Graphics2D g = (Graphics2D)g2;
		synchronized(buffer) {
			g.drawImage(buffer, 0, 0, this);

			// draw turtle
			g.setColor(Color.GREEN);
			g.translate(turtle.x, turtle.y);
			g.rotate(Math.toRadians(turtle.degrees) - Math.PI/2);
			g.drawLine( 0, -10,  8, 10);
			g.drawLine( 0, -10, -8, 10);
			g.drawLine(-8,  10,  8, 10);
			
			//draw status info
			g.rotate(-Math.toRadians(turtle.degrees) + Math.PI/2);
			g.drawString(penstat, (float)(5-turtle.x), (float)(10-turtle.y));
			g.drawString(turtlepos, (float)(5-turtle.x), (float)(30-turtle.y));
			g.drawString(turtlehdg, (float)(5-turtle.x), (float)(40-turtle.y));
			g.setColor(turtle.pencolor);
			g.drawString(pencolor, (float)(5-turtle.x), (float)(20-turtle.y));
		}
	}
}