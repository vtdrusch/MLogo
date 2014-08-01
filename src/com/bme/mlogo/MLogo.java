package com.bme.mlogo;

import com.bme.logo.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.net.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.PlainDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import static com.bme.logo.Primitives.*;

public class MLogo implements ActionListener, KeyListener, ChangeListener {
	static final String version = "Loko v0.2.5";
	static final String instalLoc = "C:/Users/Avion/Documents/GitHub/MLogo/";
	static URL base;
	private File saveFile;
	private String input = "";
	private String output = "";
	private int lineCount = 0;
	private JFrame frame;
	private JPanel pane;
	private TurtleGraphics t;
	private JTabbedPane tabbedPane1;
	private JTextField listener;
	private JTextPane terminal;
	private JTextPane help;
	private JTextArea editor;
	private JComponent turtlePane;
	private Environment e;
	private boolean changeTab = true;
	private int currentTab = 0;

	public MLogo(boolean interactive, boolean turtles, boolean trace){
		this.e = kernel();
		primitiveIO(e, trace);
		try				   { base = new URL("file:///" + instalLoc + "docs/"); }
		catch(Exception ex){ ex.printStackTrace();							   }

		saveFile = new File("save/saveFile.txt");		
		t = new TurtleGraphics(e, 375, 315);
		repl(e, t);
	}

	public static void main(String[] a) {
		boolean interactive = true;
		boolean turtles     = false;
		boolean trace       = false;

		new MLogo(interactive, turtles, trace);
	}

	private void repl(Environment env, TurtleGraphics t) {
		this.initGUI(t);
		if(this.saveFile.length() != 0){ setInput(this.loadFile(saveFile.getName())); }

		insertText("<b>>Welcome to " + version + "!</b>", terminal);
		insertText(">Please see the teacher module in the lower right for more information.", terminal);
		insertText("Hello, I will be your digital tutor.", help);
		insertText("Type 'help' for a list of commands, type 'exit' to quit.", help);

		while(true) {
			boolean newInput = true;
			try {
				while("".equals(input)){ 
					try{ Thread.sleep(10); }
					catch(InterruptedException e){} 
				}
				while(Parser.complete(input).size() > 0) {
					try{ Thread.sleep(10); }
					catch(InterruptedException e){}
				}
				if ("exit".equals(input)) { this.saveEnv(env); break; }
				if (input.contains("load ")){ newInput = false; }
				runString(env, input, t);
				this.lineCount = 0;
				if(newInput){ this.input = ""; }
			}
			catch(SyntaxError e) {
				insertText(String.format("syntax error: %s", e.getMessage()), help);
				insertText(String.format("<pre>\t%s</pre>", e.line), help);
				String spacer = "<pre>\t";
				for(int z = 0; z < e.lineIndex; z++) {
					spacer += " ";
				}
				spacer += "^</pre>";
				insertText(spacer, help);
				this.lineCount = 0;
				this.input = "";
				env.reset();
			}
		}
		System.exit(0);
	}

	private void initGUI(TurtleGraphics t){
		this.frame = new JFrame(version);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(1024, 768));

		this.pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		//create turtle graphics display
		this.turtlePane = t.getTurtle();

		//set up first tabbed pane (terminal and large turtle graphics window)
		this.tabbedPane1 = new JTabbedPane();
		tabbedPane1.addChangeListener(this);

		this.editor = new JTextArea();
		this.editor.setTabSize(4);
		JComponent tab0 = this.editor;
		this.tabbedPane1.add(tab0, 0);
		this.tabbedPane1.setTitleAt(0, "Editor");
		this.tabbedPane1.setToolTipTextAt(0, "Program Editor Window");

		JComponent tab1 = new JPanel();
		this.tabbedPane1.add(tab1, 1);
		this.tabbedPane1.setTitleAt(1, "Graphics");
		this.tabbedPane1.setToolTipTextAt(1, "Large Turtle Graphics Display");

		this.terminal = new JTextPane();
		this.terminal.setEditable(false);
		this.terminal.setContentType("text/html");
		((HTMLDocument)this.terminal.getDocument()).setBase(base);
		this.terminal.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if(Desktop.isDesktopSupported()) {
						try{ Desktop.getDesktop().browse(e.getURL().toURI()); }
						catch(Exception ex){ System.out.println(ex); }
					}
				}
			}
		});

		//add terminal to scrolling pane
		JScrollPane termScroll = new JScrollPane(this.terminal);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane1, termScroll);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(400);
		//splitPane.getTopComponent().setMinimumSize(new Dimension());
		//splitPane.setDividerLocation(0);

		//set up second tabbed pane (tutor, modules, and libraries)
		JTabbedPane tabbedPane2 = new JTabbedPane();

		this.help = new JTextPane();
		this.help.setEditable(false);
		this.help.setContentType("text/html");
		((HTMLDocument)this.help.getDocument()).setBase(base);
		this.help.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if(Desktop.isDesktopSupported()) {
						try{ Desktop.getDesktop().browse(e.getURL().toURI()); }
						catch(Exception ex){ System.out.println(ex); }
					}
				}
			}
		});


		JComponent tab3 = new JScrollPane(this.help);
		tabbedPane2.addTab("Teacher", null, tab3, "Teaching module and help menu");

		JComponent tab4 = makeTextPanel("Panel 2");
		tabbedPane2.addTab("Modules", null, tab4, "Learning modules");

		JComponent tab5 = makeTextPanel("Panel 3");
		tabbedPane2.addTab("My Files", null, tab5, "User created Logo files");

		JComponent tab6 = makeTextPanel("Panel 4");
		tabbedPane2.addTab("Library", null, tab6, "Function library");

		//set up user input field
		this.listener = new JTextField("", 30);
		this.listener.addActionListener(this);
		this.listener.addKeyListener(this);

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
		pane.add(splitPane, c); //split pane

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

		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 300;
		c.ipady = 300;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(turtlePane, c); //small turtle graphics pane

		frame.add(pane);
		frame.pack();
		frame.setVisible(true);
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
		this.input += listener.getText();
		this.output = listener.getText();
		try{
			if(Parser.complete(input).size() > 0){ this.lineCount += 1; this.input += "\n"; }
		}
		catch(SyntaxError e){
			this.lineCount = 0;
		}
		if(this.lineCount > 1 && !"end".equals(listener.getText())){ insertText(">>" + this.output, terminal); }
		else													   { insertText(">" + this.output, terminal);  }
		listener.setText("");
	}

	public void keyPressed(KeyEvent e){ 
		Integer keyCode = e.getKeyCode();
		if(keyCode == 38){ listener.setText(output); }
	}

	public void keyReleased(KeyEvent e){ }

	public void keyTyped(KeyEvent e){ }

	public void stateChanged(ChangeEvent e){ 
		int index = this.tabbedPane1.getSelectedIndex();
		if(changeTab){
			if(index == 1){ 
				changeTab = false;
				this.pane.remove(turtlePane);
				this.tabbedPane1.remove(1);
				this.t.resize(this.tabbedPane1.getWidth(), this.tabbedPane1.getHeight());
				this.turtlePane = t.getTurtle();
				JComponent tab = this.turtlePane;
				this.tabbedPane1.add(tab, 1);
				this.tabbedPane1.setTitleAt(1, "Graphics");
				this.tabbedPane1.setToolTipTextAt(1, "Large Turtle Graphics Display");
				frame.pack();

				this.tabbedPane1.setSelectedIndex(1);
				this.changeTab = true;
			}
			else if(currentTab == 1){
				if(this.tabbedPane1.getTabCount() > 1){
					changeTab = false;
					this.tabbedPane1.remove(1);
					JComponent tab = new JPanel();
					this.tabbedPane1.add(tab, 1);
					this.tabbedPane1.setTitleAt(1, "Graphics");
					this.tabbedPane1.setToolTipTextAt(1, "Large Turtle Graphics Display");
					changeTab = true;
				}
				this.t.resize(375, 315);
				this.turtlePane = t.getTurtle();
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.ipadx = 300;
				c.ipady = 300;
				c.gridx = 1;
				c.gridy = 0;
				c.gridheight = 1;
				c.weightx = 0.0;
				c.weighty = 0.0;
				pane.add(turtlePane, c);
				frame.pack();
			}
		}
		this.currentTab = index;
	}

	public static void insertText(String s, JTextPane pane){
		HTMLDocument doc = (HTMLDocument)pane.getDocument();
		HTMLEditorKit editor = (HTMLEditorKit)pane.getEditorKit();
		try{ editor.insertHTML(doc, doc.getLength(), s, 0, 0, null); }
		catch(Exception e){ }
		pane.setCaretPosition(doc.getLength());
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
			this.lineCount = 0;
			env.reset();
		}
	}

	private String loadFile(String filename) {
		try {
			File fileIn = new File("save/" + filename);
			if(fileIn.length() == 0){
				insertText("Cannot load empty file.", help);
				return "";
			}
			Scanner in = new Scanner(fileIn);
			StringBuilder ret = new StringBuilder();
			while(in.hasNextLine()) {
				// this will conveniently convert platform-specific
				// newlines into an internal unix-style convention:
				ret.append(in.nextLine()+"\n");
			}
			in.close();
			// shave off the trailing newline we just inserted:
			ret.deleteCharAt(ret.length()-1);
			return ret.toString();
		}
		catch(IOException e) {
			insertText(String.format("Unable to load file '%s'.%n", filename), help);
			return null;
		}
	}

	//TODO
	private void saveEnv(Environment e){
		try{
			saveFile.getParentFile().mkdirs();

			PrintWriter writer = new PrintWriter(saveFile);
			java.util.List<LWord> words = new ArrayList<LWord>(e.words());
			Collections.sort(words);
			for(LWord word : words){
				if(e.thing(word) instanceof LNumber){ 
					writer.println("local " + word.toString() + " " + ((LNumber)e.thing(word)).value);
					writer.println();
				}
				if(e.thing(word) instanceof LWord){ 
					writer.println("local " + word.toString() + " '" + ((LWord)e.thing(word)).value);
					writer.println();
				}
				if(e.thing(word) instanceof LList){
					LList list = ((LList)e.thing(word));
					String sourceText = list.sourceText;
					if(!"".equals(sourceText)){
						writer.println(sourceText.replaceAll("\n", System.getProperty("line.separator")));
						writer.println();
					}
					else{
						sourceText = list.toString();
						if(!sourceText.contains("@")){
							if(list.arguments != null){
								sourceText = "local " + word.toString() + " bind " + list.arguments.toString() + sourceText;
							}
							else{
								sourceText = "local " + word.toString() + " " + sourceText;
							}
							writer.println(sourceText);
							writer.println();
						}
					}
				}
			}
			writer.close();
			insertText("Environment state saved.", help);
		}
		catch(IOException ex){
			insertText("Unable to save to saveFile.txt", help);
		}
	}

	public void setInput(String in){
		this.input = in;
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
				Integer count = 1;
				for(LWord word : words) {
					String type = "";
					if(e.thing(word) instanceof LList){ type = " list"; }
					if(e.thing(word) instanceof LWord){ type = " word"; }
					if(e.thing(word) instanceof LNumber){ type = " number"; }
					insertText(count.toString() + " " + word.toString() + type, help); 
					count += 1; 
				}
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
				for(LWord word : words) {
					String args = " ";
					if(e.thing(word) instanceof LList){
						LList list = Primitives.list(e, word);
						if(list.arguments != null){ args += list.arguments.toString(); }
						else					  { args += "[]";					   }
					}
					if(e.thing(word) instanceof LNumber){
						args += ((Integer)((LNumber)e.thing(word)).value).toString();
					}
					insertText("<a href=\"" + word + ".html\">" + word.toString() + "</a>" + args, help);
				}
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
		e.bind(new LWord(LWord.Type.Prim, "load") {
			public void eval(Environment e) {
				LAtom o = e.thing(a);
				String arg = o.toString();
				char[] chars = arg.toCharArray();
				StringBuilder filename = new StringBuilder();
				for(int i = 1; i < chars.length - 1; i += 1){ filename.append(chars[i]); }
				setInput(loadFile(filename.toString()));
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "save") {
			public void eval(Environment e) {
				saveEnv(e);
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "saveTo") {
			public void eval(Environment e) {
				LAtom o = e.thing(a);
				String arg = o.toString();
				char[] chars = arg.toCharArray();
				StringBuilder filename = new StringBuilder();
				for(int i = 1; i < chars.length - 1; i += 1){ filename.append(chars[i]); }
				File file = new File("save/" + filename.toString());
				
				try{
					file.getParentFile().mkdirs();
					
					PrintWriter writer = new PrintWriter(file);
					writer.println(editor.getText().replaceAll("\n", System.getProperty("line.separator")));
					writer.close();
				}
				catch(IOException ex){
					insertText("Could not write to file", help);
				}
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "edit") {
			public void eval(Environment e) {
				LAtom o = e.thing(a);
				String arg = o.toString();
				char[] chars = arg.toCharArray();
				StringBuilder filename = new StringBuilder();
				for(int i = 1; i < chars.length - 1; i += 1){ filename.append(chars[i]); }
				editor.setText(loadFile(filename.toString()));
			}
		}, a);
	}
}