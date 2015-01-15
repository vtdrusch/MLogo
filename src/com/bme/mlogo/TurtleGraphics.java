package com.bme.mlogo;
import com.bme.logo.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import static com.bme.logo.Primitives.*;

public class TurtleGraphics implements MouseListener {

	static int WIDTH;
	static int HEIGHT;
	static final int WIDTH_OFFSCREEN = 2000;
	static final int HEIGHT_OFFSCREEN = 2000;
	static int X_OFFSET;
	static int Y_OFFSET;
	private BufferedImage buffer;
	private BufferedImage bufferOffscreen;
	private Image status;
	private Turtle turtle;
	private TurtlePanel turtlePanel;
	private Graphics g;
	private Graphics g2;

	public TurtleGraphics(Environment e, int width, int height) {
		TurtleGraphics.WIDTH = width;
		TurtleGraphics.HEIGHT = height;
		X_OFFSET = (WIDTH - WIDTH_OFFSCREEN) / 2;
		Y_OFFSET = (HEIGHT - HEIGHT_OFFSCREEN) / 2;
		buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		bufferOffscreen = new BufferedImage(WIDTH_OFFSCREEN, HEIGHT_OFFSCREEN, BufferedImage.TYPE_INT_ARGB);
		status = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		turtle = new Turtle();
		turtlePanel = new TurtlePanel(turtle, buffer, status);
		turtlePanel.addMouseListener(this);
		g = buffer.createGraphics();
		g2 = bufferOffscreen.createGraphics();
		
		synchronized(bufferOffscreen) {
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, WIDTH_OFFSCREEN, HEIGHT_OFFSCREEN);
		}
		
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

	public void resize(int width, int height){
		double x = turtle.x - TurtleGraphics.WIDTH / 2;
		double y = turtle.y - TurtleGraphics.HEIGHT / 2;
		
		if(!isEven(width)){
			width += 1;
		}
		if(!isEven(height)){
			height += 1;
		}
		
		TurtleGraphics.WIDTH = width;
		TurtleGraphics.HEIGHT = height;
		X_OFFSET = (WIDTH - WIDTH_OFFSCREEN) / 2;
		Y_OFFSET = (HEIGHT - HEIGHT_OFFSCREEN) / 2;

		BufferedImage img = bufferOffscreen;
		buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = buffer.createGraphics();
		try{
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.setBackground(Color.BLACK);
			g2d.clearRect(0, 0, WIDTH, HEIGHT);
			g2d.drawImage(img, X_OFFSET, Y_OFFSET, WIDTH_OFFSCREEN, HEIGHT_OFFSCREEN, null);
		}
		finally{
			g2d.dispose();
		}
		turtlePanel = new TurtlePanel(turtle, buffer, status);
		turtle.window = this.turtlePanel;

		turtle.x = WIDTH / 2;
		turtle.y = HEIGHT / 2;
		turtle.x += x;
		turtle.y += y;
		
		g = buffer.getGraphics();
		turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
	}
	
	private boolean isEven(int num){
		return ((num % 2) == 0);
	}

	protected JComponent getTurtle(){
		if(turtle.window == null){ setup(); }
		return turtle.window;
	}
	
	public void mouseClicked(MouseEvent e){ 
		//System.out.println(e.getX() + " " + e.getY());
	}
	
	public void mouseEntered(MouseEvent e){ }
	
	public void mouseExited(MouseEvent e){ }
	
	public void mousePressed(MouseEvent e){ }
	
	public void mouseReleased(MouseEvent e){ }

	public void primitiveTurtle(Environment e) {
		final LWord dist = new LWord(LWord.Type.Name, "distance");
		final LWord deg = new LWord(LWord.Type.Name, "degrees");
		final LWord r = new LWord(LWord.Type.Name, "red");
		final LWord gr = new LWord(LWord.Type.Name, "green");
		final LWord b = new LWord(LWord.Type.Name, "blue");

		e.bind(new LWord(LWord.Type.Prim, "forward") {
			public void eval(Environment e) {
				setup();
				turtle.goalDistance = -num(e, dist);
				e.pause();
			}
		}, dist);
		e.bind(new LWord(LWord.Type.Prim, "back") {
			public void eval(Environment e) {
				setup();
				turtle.goalDistance = num(e, dist);
				e.pause();
			}
		}, dist);
		e.bind(new LWord(LWord.Type.Prim, "left") {
			public void eval(Environment e) {
				setup();
				turtle.goalDegrees = -num(e, deg);
				e.pause();
			}
		}, deg);
		e.bind(new LWord(LWord.Type.Prim, "right") {
			public void eval(Environment e) {
				setup();
				turtle.goalDegrees = num(e, deg);
				e.pause();
			}
		}, deg);
		e.bind(new LWord(LWord.Type.Prim, "clrturtle") {
			public void eval(Environment e) {
				setup();
				synchronized(buffer) {
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, WIDTH, HEIGHT);
				}
				synchronized(bufferOffscreen) {
					g2.setColor(Color.BLACK);
					g2.fillRect(0, 0, WIDTH_OFFSCREEN, HEIGHT_OFFSCREEN);
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
							num(e, r) & 0xFF,
							num(e, gr) & 0xFF,
							num(e, b) & 0xFF
							);
					turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				}
				turtle.window.repaint();
			}
		}, r, gr, b);
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

			int ox = (int)turtle.x;
			int oy = (int)turtle.y;
			
			synchronized(buffer) {
				turtle.x += traveled * Math.cos(Math.toRadians(turtle.degrees));
				turtle.y += traveled * Math.sin(Math.toRadians(turtle.degrees));
				turtlePanel.penStat(turtle.pendown, turtle.pencolor, turtle.heading, turtle.x, turtle.y);
				if (turtle.pendown) {
					g.setColor(turtle.pencolor);
					g.drawLine(ox, oy, (int)turtle.x, (int)turtle.y);
				}
			}
			int x = (int)turtle.x - X_OFFSET;
			int y = (int)turtle.y - Y_OFFSET;
			synchronized(bufferOffscreen) {
				ox -= X_OFFSET;
				oy -= Y_OFFSET;
				if (turtle.pendown) {
					g2.setColor(turtle.pencolor);
					g2.drawLine(ox, oy, x, y);
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
	private final Image  status;
	private String penstat;
	private String pencolor;
	private String turtlepos;
	private String turtlehdg;

	public TurtlePanel(Turtle turtle, Image buffer, Image status) {
		setPreferredSize(new Dimension(TurtleGraphics.WIDTH, TurtleGraphics.HEIGHT));
		this.turtle = turtle;
		this.status = status;
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
		}
		synchronized(status){
			g.drawImage(status, 0, 0, this);

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