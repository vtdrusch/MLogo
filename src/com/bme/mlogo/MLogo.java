package com.bme.mlogo;

import com.bme.logo.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import static com.bme.logo.Primitives.*;

public class MLogo implements ActionListener {
	static final String version = "Loko v0.2";
	private String input = "";
	private static JFrame frame;
	private JTextField listener;
	private JTextPane terminal;
	private JTextPane help;
	
	public MLogo(boolean interactive, boolean turtles, boolean trace, java.util.List<String> args){
		Environment e = kernel();
		primitiveIO(e, trace);

		// the repl always loads turtle graphics primitives,
		// but they're strictly opt-in for batch mode.
		if (turtles) {
			TurtleGraphics t = new TurtleGraphics(e);
			for(String fileName : args) { runFile(e, fileName, t); }
			if (interactive) { repl(e, t); }
			else { System.exit(0); }
		}
		else {
			for(String fileName : args) { runFile(e, fileName, null); }
			if (interactive) {
				TurtleGraphics t = new TurtleGraphics(e);
				repl(e, t);
			}
		}
	}

	public static void main(String[] a) {
		java.util.List<String> args = new ArrayList<String>(Arrays.asList(a));

		boolean printHelp   = false;
		boolean interactive = true;
		boolean turtles     = false;
		boolean trace       = false;

		for(int z = args.size() - 1; z >= 0; z--) {
			if ("-h".equals(args.get(z))) { printHelp   = true; interactive = false; args.remove(z--); continue; }
			if ("-t".equals(args.get(z))) { turtles     = true; interactive = false; args.remove(z--); continue; }
			if ("-T".equals(args.get(z))) { trace       = true; interactive = false; args.remove(z--); continue; }
		}

		if (printHelp) {
			System.out.println(version);
			System.out.println("usage: MLogo [-hit] file ...");
			System.out.println();
			System.out.println(" h : print this help message");
			System.out.println(" t : enable turtle graphics during batch mode");
			System.out.println(" T : enable execution trace");
			System.out.println(" Default (no args): start an interactive REPL session");
			System.out.println();
		}
		
		new MLogo(interactive, turtles, trace, args);
	}

	private void repl(Environment env, TurtleGraphics t) {
		this.initGUI(t);
		insertText("<html><b>>" + version + "</b>", terminal);
		insertText(">type 'exit' to quit.", terminal);
		
		while(true) {
			try {
				while("".equals(input)){ 
					try{ Thread.sleep(10); }
					catch(InterruptedException e){} 
				}	
				if ("exit".equals(input)) { break; }
				runString(env, input, t);
				this.input = "";
			}
			catch(SyntaxError e) {
				insertText(String.format("syntax error: %s%n", e.getMessage()), help);
				insertText(String.format("\t%s%n\t", e.line), help);
				for(int z = 0; z < e.lineIndex; z++) {
					insertText(((Character)(e.line.charAt(z) == '\t' ? '\t' : ' ')).toString(), help);
				}
				insertText("^", help);
				this.input = "";
				env.reset();
			}
		}
		System.exit(0);
	}
	
	private void initGUI(TurtleGraphics t){
		JFrame frame = new JFrame(version);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(1024, 768));
		//frame.setResizable(false);
		
		JPanel pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		//set up first tabbed pane (terminal and large turtle graphics window)
		JTabbedPane tabbedPane1 = new JTabbedPane();
		
		this.terminal = new JTextPane();
		this.terminal.setEditable(false);
		this.terminal.setContentType("text/html");
		
		JComponent tab1 = new JScrollPane(this.terminal);
		tabbedPane1.addTab("Terminal", null, tab1, "Terminal output");
				
		JComponent tab2 = makeTextPanel("Turtle");
		tabbedPane1.addTab("Graphics", null, tab2, "Turtle graphics");
	
		//set up second tabbed pane (tutor, modules, and libraries)
		JTabbedPane tabbedPane2 = new JTabbedPane();
		
		this.help = new JTextPane();
		this.help.setEditable(false);
		this.help.setContentType("text/html");
		
		JComponent tab3 = new JScrollPane(this.help);
		tabbedPane2.addTab("Teacher", null, tab3, "Teaching module and help menu");
		this.help.setText("Welcome to Loko v0.2! I will be your digital tutor.\n" +
				"Type help for a list of commands.\n");
				
		JComponent tab4 = makeTextPanel("Panel 2");
		tabbedPane2.addTab("Modules", null, tab4, "Learning modules");
		
		JComponent tab5 = makeTextPanel("Panel 3");
		tabbedPane2.addTab("My Files", null, tab5, "User created Logo files");
		
		JComponent tab6 = makeTextPanel("Panel 4");
		tabbedPane2.addTab("Library", null, tab6, "Function library");
		
		//add small turtle graphics display
		JComponent turtlePane = t.getTurtle();
		
		//set up user input field
		this.listener = new JTextField("", 30);
		this.listener.addActionListener(this);
		
		//place components in gridbag layout
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 400;
		c.ipady = 400;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		pane.add(tabbedPane1, c); //terminal and large graphics pane
		
		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 300;
		c.ipady = 300;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(turtlePane, c); //small turtle graphics pane
		
		c.fill = GridBagConstraints.VERTICAL;
		c.ipadx = 300;
		c.ipady = 330;
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(tabbedPane2, c); //tutor and lib pane
		
		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 0;
		c.ipady = 15;
		c.gridx = 0;
		c.gridy = 2;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(listener, c); //user input pane
		
		frame.add(pane);
		frame.pack();
		frame.setVisible(true);
	}
	
	public static void insertText(String s, JTextPane pane){
		HTMLDocument doc = (HTMLDocument)pane.getDocument();
		HTMLEditorKit editor = (HTMLEditorKit)pane.getEditorKit();
		try{ editor.insertHTML(doc, doc.getLength(), s, 0, 0, null); }
		catch(Exception e){ }
		pane.setCaretPosition(doc.getLength());
	}

	protected static JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }
	
	public void actionPerformed(ActionEvent evt) {
		this.input = listener.getText();
		insertText(">" + this.input, terminal);
		listener.setText("");
	}
	
	private void runString(Environment env, String sourceText, TurtleGraphics t) {
		try {
			LList code = Parser.parse(sourceText);
			Interpreter.init(code, env);
			while(true) {
				// execute until the interpreter is paused
				if (!Interpreter.runUntil(env)) { return; }
				
				// update the display until animation is complete
				while(!t.update()) {
					try { Thread.sleep(1000 / 30); }
					catch(InterruptedException e) {}
				}
			}
		}
		catch(RuntimeError e) {
			insertText(String.format("runtime error: %s%n", e.getMessage()), help);
			//e.printStackTrace();
			for(LAtom atom : e.trace) {
				insertText(String.format("\tin %s%n", atom), help);
			}
			this.input = "";
			env.reset();
		}
	}

	private static void runFile(Environment env, String filename, TurtleGraphics t) {
		try {
			LList code = Parser.parse(loadFile(filename));
			if (t == null) {
				Interpreter.run(code, env);
				return;
			}
			Interpreter.init(code, env);
			while(true) {
				// execute until the interpreter is paused
				if (!Interpreter.runUntil(env)) { return; }
				
				// update the display until animation is complete
				while(!t.update()) {
					try { Thread.sleep(1000 / 30); }
					catch(InterruptedException e) {}
				}
			}
		}
		catch(SyntaxError e) {
			System.out.format("%d: syntax error: %s%n", e.lineNumber, e.getMessage());
			System.out.format("\t%s%n\t", e.line);
			for(int z = 0; z < e.lineIndex; z++) {
				System.out.print(e.line.charAt(z) == '\t' ? '\t' : ' ');
			}
			System.out.println("^");
			System.exit(1);
		}
		catch(RuntimeError e) {
			System.out.format("runtime error: %s%n", e.getMessage());
			for(LAtom atom : e.trace) {
				System.out.format("\tin %s%n", atom);
			}
			System.exit(1);
		}
	}

	private static String loadFile(String filename) {
		try {
			Scanner in = new Scanner(new File(filename));
			StringBuilder ret = new StringBuilder();
			while(in.hasNextLine()) {
				// this will conveniently convert platform-specific
				// newlines into an internal unix-style convention:
				ret.append(in.nextLine()+"\n");
			}
			// shave off the trailing newline we just inserted:
			ret.deleteCharAt(ret.length()-1);
			return ret.toString();
		}
		catch(IOException e) {
			System.err.format("Unable to load file '%s'.%n", filename);
			System.exit(1);
			return null;
		}
	}

	private void primitiveIO(Environment e, boolean trace) {
		final LWord a = new LWord(LWord.Type.Name, "argument1");
		final Scanner in = new Scanner(System.in);

		if (trace) {
			e.setTracer(new Tracer() {
				public void begin()  { System.out.println("tracer: begin."); }
				public void end()    { System.out.println("tracer: end.");   }
				//public void tick() { System.out.println("tracer: tick.");  }

				public void callPrimitive(String name, Map<LAtom, LAtom> args) {
					System.out.format("trace: PRIM %s%s%n",
						name,
						args.size() > 0 ? " " + args : ""
					);
				}
				public void call(String name, Map<LAtom, LAtom> args, boolean tail) {
					System.out.format("trace: CALL %s%s%s%n",
						name,
						args.size() > 0 ? " " + args : "",
						tail ? " (tail)" : ""
					);
				}
				public void output(String name, LAtom val, boolean implicit) {
					System.out.format("trace: RETURN %s- %s%s%n", name, val, implicit ? " (implicit)" : "");
				}
				public void stop(String name, boolean implicit) {
					System.out.format("trace: STOP %s%s%n", name, implicit ? " (implicit)" : "");
				}
			});
		}

		e.bind(new LWord(LWord.Type.Prim, "version") {
			public void eval(Environment e) {
				insertText(MLogo.version, terminal);
			}
		});

		e.bind(new LWord(LWord.Type.Prim, "words") {
			public void eval(Environment e) {
				java.util.List<LWord> words = new ArrayList<LWord>(e.words());
				Collections.sort(words);
				for(LWord word : words) { insertText(word.toString(), help); }
			}
		});

		e.bind(new LWord(LWord.Type.Prim, "erase") {
			public void eval(Environment e) {
				LWord key = word(e, a);
				// dereference the name to ensure that
				// it originally had a binding.
				// we don't care what it was.
				e.thing(key);
				e.erase(key);
			}
		}, a);

		e.bind(new LWord(LWord.Type.Prim, "trace") {
			public void eval(Environment e) {
				System.out.println("trace: ");
				for(LAtom s : e.trace()) {
					System.out.println("\t" + s);
				}
				System.out.println();
			}
		});

		e.bind(new LWord(LWord.Type.Prim, "print") {
			public void eval(Environment e) {
				insertText(">" + e.thing(a).toString(), terminal);
			}
		}, a);

		e.bind(new LWord(LWord.Type.Prim, "println") {
			public void eval(Environment e) {
				insertText(">", terminal);
			}
		});

		e.bind(new LWord(LWord.Type.Prim, "readlist") {
			public void eval(Environment e) {
				e.output(Parser.parse(in.nextLine()));
			}
		});
		
		e.bind(new LWord(LWord.Type.Prim, "help") {
			public void eval(Environment e) {
				java.util.List<LWord> words = new ArrayList<LWord>(e.words());
				Collections.sort(words);
				for(LWord word : words) { insertText(word.toString(), help); }
			}
		});
		
		e.bind(new LWord(LWord.Type.Prim, "clrwindow") {
			public void eval(Environment e) {
				terminal.setText("");
			}
		});
		
		e.bind(new LWord(LWord.Type.Prim, "clrhelp") {
			public void eval(Environment e) {
				help.setText("");
			}
		});
	}
}