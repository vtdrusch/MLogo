/**
 * Javadoc Autogeneration
 * @author Vincent
 * 
 * Description
 * 
 */
package com.bme.mlogo;

import com.bme.logo.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.net.*;
import java.nio.file.*;
import java.awt.image.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import static com.bme.logo.Primitives.*;

public class MLogo implements ActionListener, KeyListener, ChangeListener, ComponentListener {
	static final String version = "Loko v0.5";
	static final String fileLoc = System.getProperty("user.dir");
	static URL base;
	private File saveFile;
	private String input = "";
	private String output = "";
	private int lineCount = 0;
	private JFrame frame;
	private JPanel pane;
	private TurtleGraphics t;
	private JTabbedPane tabbedPane1;
	private JPanel container;
	private JSplitPane splitPane;
	private JTextField listener;
	private JTextPane terminal;
	private JTextPane help;
	private TextEditor editor;
	private JComponent turtlePane;
	private Environment e;
	private int currentTab = 0;

	public MLogo(boolean interactive, boolean turtles, boolean trace){
		this.e = kernel();
		primitiveIO(e, trace);
		try				   { base = new URL("file:///" + fileLoc + "/docs/"); }
		catch(Exception ex){ ex.printStackTrace();							  }

		saveFile = new File("save/saveFile");
		saveFile.getParentFile().mkdirs();
		try{
			if(!saveFile.exists()){
				PrintWriter writer = new PrintWriter(saveFile);
				writer.print("");
			}
		}
		catch(IOException ex){
			ex.printStackTrace();
		}

		java.util.List<LWord> words = new ArrayList<LWord>(e.words());
		Collections.sort(words);
		for(LWord word : words) {
			generateDoc(word, false);
		}

		this.describe(e);

		t = new TurtleGraphics(e, 375, 315);
		repl(e, t);
	}

	public static void main(String[] a) {
		boolean interactive = true;
		boolean turtles     = false;
		boolean trace       = false;

		new MLogo(interactive, turtles, trace);
	}

	public String getVersion(){
		return MLogo.version;
	}

	public TurtleGraphics getTurtle(){
		return this.t;
	}

	public void setInput(String in){
		this.input = in;
	}

	public void setTitle(String title){
		this.frame.setTitle(title);
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
		//initialize the gui frame
		this.frame = new JFrame(version);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(1024, 768));

		//initialize the top level panel for element display
		this.pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		//create small turtle graphics display
		this.turtlePane = t.getTurtle();

		//set up first tabbed pane (terminal, large turtle graphics window)
		this.tabbedPane1 = new JTabbedPane();
		tabbedPane1.addChangeListener(this);

		this.editor = new TextEditor(this);
		JComponent tab0 = this.editor;

		//add editor tab to first tab pane
		this.tabbedPane1.add(tab0, 0);
		this.tabbedPane1.setTitleAt(0, "Editor");
		this.tabbedPane1.setToolTipTextAt(0, "Program Editor Window");

		//add large graphics tab to first tab pane
		this.container = new JPanel();
		//this.container.setLayout(new GridBagLayout());
		this.tabbedPane1.add(container, 1);
		this.tabbedPane1.setTitleAt(1, "Graphics");
		this.tabbedPane1.setToolTipTextAt(1, "Large Turtle Graphics Display");

		//set up terminal window
		this.terminal = new JTextPane();
		this.terminal.setEditable(false);
		this.terminal.setContentType("text/html");
		((HTMLDocument)this.terminal.getDocument()).setBase(base);
		this.terminal.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if(Desktop.isDesktopSupported()) {
						try{ Desktop.getDesktop().browse(e.getURL().toURI()); }
						catch(Exception ex){ insertText("Invalid link.", help); }
					}
				}
			}
		});

		//add terminal to scrolling pane
		JScrollPane termScroll = new JScrollPane(this.terminal);

		//add scrolling terminal pane and first tab pane to split pane
		this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane1, termScroll);
		splitPane.setOneTouchExpandable(true);
		splitPane.setDividerLocation(400);

		//set up second tabbed pane (tutor, modules, libraries)
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
						catch(Exception ex){ insertText("Cannot find HTML documentation for this word, please ensure that this word's " +
								"corresponding documentation is in the docs folder.", help); }
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

		frame.addComponentListener(this);
	}

	protected static JComponent makeTextPanel(String text) {
		JPanel panel = new JPanel(false);
		JLabel filler = new JLabel(text);
		filler.setHorizontalAlignment(JLabel.CENTER);
		panel.setLayout(new GridLayout(1, 1));
		panel.add(filler);
		return panel;
	}

	//ActionEvent method
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

	//KeyEvent methods
	public void keyPressed(KeyEvent e){ 
		Integer keyCode = e.getKeyCode();
		if(keyCode == 38){ listener.setText(output); }
	}

	public void keyReleased(KeyEvent e){ }

	public void keyTyped(KeyEvent e){ }

	//ChangeListener method
	public void stateChanged(ChangeEvent e){ 
		this.changeState();
	}

	//ComponentListener methods
	public void componentResized(ComponentEvent e){	
		this.changeState();
	}

	public void componentHidden(ComponentEvent e){ }

	public void componentShown(ComponentEvent e){ }

	public void componentMoved(ComponentEvent e){ }

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

	private void changeState(){
		int index = this.tabbedPane1.getSelectedIndex();
		GridBagConstraints c = new GridBagConstraints();
		Dimension dim = this.frame.getSize();

		if(index == 1){
			this.splitPane.setOneTouchExpandable(false);
			this.splitPane.setEnabled(false);
			this.splitPane.setDividerLocation(0.75);

			int width = this.tabbedPane1.getWidth();
			int height = this.splitPane.getDividerLocation();
			this.container.remove(this.turtlePane);
			this.pane.remove(this.turtlePane);
			this.t.resize(width, height);
			this.turtlePane = this.t.getTurtle();
			this.container.add(this.turtlePane);
			this.frame.setPreferredSize(dim);
			this.frame.pack();
		}
		else if(currentTab == 1){
			this.container.remove(this.turtlePane);

			this.splitPane.setOneTouchExpandable(true);
			this.splitPane.setEnabled(true);
			this.splitPane.setDividerLocation(0.5);

			this.t.resize(375, 315);
			this.turtlePane = t.getTurtle();
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.ipadx = 300;
			c.ipady = 300;
			c.gridx = 1;
			c.gridy = 0;
			c.gridheight = 1;
			c.weightx = 0.0;
			c.weighty = 0.0;
			this.pane.add(this.turtlePane, c);
			this.frame.setPreferredSize(dim);
			this.frame.pack();
		}
		this.currentTab = index;
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
			insertText("Unable to save to saveFile", help);
		}
	}

	public void generateDoc(LWord word, boolean regen){
		if(e.thing(word) instanceof LList){
			String filename;
			String args = " ";

			LList list = Primitives.list(e, word);
			if(list.arguments != null){ args += list.arguments.toString(); }
			else					  { args += "[]";					   }

			if(word.value.contains("?")){
				StringBuilder wordName = new StringBuilder(word.value);
				wordName.deleteCharAt(wordName.length()-1);
				filename = fileLoc + "/docs/is" + wordName.toString() + ".html";
			}
			else{
				filename = fileLoc + "/docs/" + word.value + ".html";
			}

			File doc = new File(filename);
			if(regen){
				try{
					Files.deleteIfExists(doc.toPath());
				}
				catch(Exception ex){
					insertText("Could not delete existing document for word: " + word.value + ".", help);
				}
			}

			doc.getParentFile().mkdirs();
			if(!doc.exists()){
				try{
					PrintWriter writer = new PrintWriter(doc);
					String desc = ((LList)e.thing(word)).description;
					if("".equals(desc)){ desc = "Description not found, please add a brief description of this word using the " +
							"<a href=\"describe.html\">describe</a> command."; }
					writer.println("<html lang=\"en\">\n" +
							"<head>\n" +
							"<title>" + word.value + "</title>\n" +
							"</head>\n" +
							"<body>\n" +
							"<h1>" + word.value + args + "</h1>\n" +
							"<hr>\n" +
							"<br>\n" +
							desc + "\n" +
							"</body>\n" +
							"</html>");
					writer.close();

				}
				catch(IOException e){
					insertText("Document generation failed for word: " + word.value + ".", help);
					e.printStackTrace();
				}
			}
		}
	}

	private void describe(Environment e){
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("args", "Takes a list input and returns the arguments associated with that list.");
		map.put("ascall", "Converts the input word to a call.");
		map.put("asname", "Covnerts the input word to a name.");
		map.put("asvalue", "Converts the input word to a value.");
		map.put("back", "Moves the turtle back by the number of units specified by the input.");
		map.put("bind", "Associates a list of arguments with another list. For example:<br><br>" +
				"bind ['x][print :x]<br><br>" +
				"returns the [print :x] list with the argument x bound to it. When used with the <a href=\"local.html\">local</a> command, " +
				"bind can be used to create lists with arguments and associate these lists with words so that they can be run later.");
		map.put("butfirst", "Description to be added later.");
		map.put("butlast", "Description to be added later.");
		map.put("clear", "Description to be added later.");
		map.put("clrhelp", "Description to be added later.");

		map.put("clrwindow", "Description to be added later.");
		map.put("describe", "Description to be added later.");
		map.put("dif", "Description to be added later.");
		map.put("edit", "Description to be added later.");
		map.put("equal", "Description to be added later.");
		map.put("erase", "Description to be added later.");
		map.put("first", "Description to be added later.");
		map.put("flatten", "Description to be added later.");
		map.put("forward", "Description to be added later.");
		map.put("fput", "Description to be added later.");

		map.put("genDocs", "Description to be added later.");
		map.put("getDesc", "Description to be added later.");
		map.put("isGreater", "Description to be added later.");
		map.put("help", "Description to be added later.");
		map.put("home", "Description to be added later.");
		map.put("desc?", "Description to be added later.");
		map.put("item", "Description to be added later.");
		map.put("join", "Description to be added later.");
		map.put("last", "Description to be added later.");
		map.put("left", "Description to be added later.");

		map.put("less?", "Description to be added later.");
		map.put("list?", "Description to be added later.");
		map.put("load", "Description to be added later.");
		map.put("local", "Description to be added later.");
		map.put("lput", "Description to be added later.");
		map.put("make", "Description to be added later.");
		map.put("member", "Description to be added later.");
		map.put("negate", "Description to be added later.");
		map.put("num?", "Description to be added later.");
		map.put("output", "Description to be added later.");

		map.put("pendown", "Description to be added later.");
		map.put("penup", "Description to be added later.");
		map.put("print", "Description to be added later.");
		map.put("println", "Description to be added later.");
		map.put("product", "Description to be added later.");
		map.put("quotient", "Description to be added later.");
		map.put("random", "Description to be added later.");
		map.put("readlist", "Description to be added later.");
		map.put("regenDocs", "Description to be added later.");
		map.put("remainder", "Description to be added later.");

		map.put("repeat", "Description to be added later.");
		map.put("right", "Description to be added later.");
		map.put("run", "Description to be added later.");
		map.put("save", "Description to be added later.");
		map.put("saveFile", "Description to be added later.");
		map.put("setcolor", "Description to be added later.");
		map.put("size", "Description to be added later.");
		map.put("stop", "Description to be added later.");
		map.put("sum", "Description to be added later.");
		map.put("thing", "Description to be added later.");

		map.put("trace", "Description to be added later.");
		map.put("unless", "Description to be added later.");
		map.put("version", "Description to be added later.");
		map.put("word?", "Description to be added later.");
		map.put("words", "Description to be added later.");

		java.util.List<LWord> words = new ArrayList<LWord>(e.words());
		Collections.sort(words);
		for(LWord word : words) {
			if(e.thing(word) instanceof LList){
				String value = ((LList)e.thing(word)).toString();
				if(value.contains("@")){
					((LList)e.thing(word)).description = map.get(word.toString());
				}
			}
		}
	}

	private void primitiveIO(Environment e, boolean trace) {
		final LWord a = new LWord(LWord.Type.Name, "word");
		final LWord b = new LWord(LWord.Type.Name, "filename");
		final LWord c = new LWord(LWord.Type.Name, "description");
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
					String type;
					if(word.type == LWord.Type.Call){ type = "call"; }
					else if(word.type == LWord.Type.Value){ type = "value"; }
					else if(word.type == LWord.Type.Name){ type = "name"; }
					else { type = "Prim"; }
					insertText(count.toString() + " " + word.toString() + " " + type, help); 
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
					String filename = word.value;

					if(word.value.contains("?")){
						StringBuilder wordName = new StringBuilder(word.value);
						wordName.deleteCharAt(wordName.length()-1);
						filename = "is" + wordName.toString();
					}

					if(e.thing(word) instanceof LNumber){
						args += ((Integer)((LNumber)e.thing(word)).value).toString();
					}
					if(e.thing(word) instanceof LList){
						LList list = Primitives.list(e, word);
						if(list.arguments != null){ args += list.arguments.toString(); }
						else					  { args += "[]";					   }
					}
					insertText("<a href=\"" + filename + ".html\">" + word.toString() + "</a>" + args, help);
					generateDoc(word, true);
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
				LAtom o = e.thing(b);
				String arg = o.toString();
				char[] chars = arg.toCharArray();
				StringBuilder filename = new StringBuilder();
				for(int i = 1; i < chars.length - 1; i += 1){ filename.append(chars[i]); }
				setInput(loadFile(filename.toString()));
			}
		}, b);
		e.bind(new LWord(LWord.Type.Prim, "save") {
			public void eval(Environment e) {
				LAtom o = e.thing(b);
				String arg = o.toString();
				char[] chars = arg.toCharArray();
				StringBuilder filename = new StringBuilder();
				for(int i = 1; i < chars.length - 1; i += 1){ filename.append(chars[i]); }
				String file = ("save/" + filename.toString());
				editor.saveFile(file);
			}
		}, b);
		e.bind(new LWord(LWord.Type.Prim, "saveEnv") {
			public void eval(Environment e) {
				saveEnv(e);
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "edit") {
			public void eval(Environment e) {
				LAtom o = e.thing(b);
				String arg = o.toString();
				char[] chars = arg.toCharArray();
				StringBuilder filename = new StringBuilder();
				for(int i = 1; i < chars.length - 1; i += 1){ filename.append(chars[i]); }
				String file = ("save/" + filename.toString());
				editor.openFile(file);
			}
		}, b);
		e.bind(new LWord(LWord.Type.Prim, "describe") {
			public void eval(Environment e) {
				if(!e.thing(a).toString().contains("@")){
					if(e.thing(a) instanceof LList){
						LAtom o = e.thing(c);
						String arg = o.toString();
						char[] chars = arg.toCharArray();
						StringBuilder desc = new StringBuilder();
						for(int i = 1; i < chars.length - 1; i += 1){ desc.append(chars[i]); }
						((LList)e.thing(a)).description = desc.toString(); 
					}
					else{
						insertText("Cannot describe non-list word.", help);
					}
				}
				else{
					insertText("Cannot redefine primitive word.", help);
				}
			}
		}, a, c);
		e.bind(new LWord(LWord.Type.Prim, "getDesc") {
			public void eval(Environment e) {
				if(a.type != LWord.Type.Call){
					LAtom o = e.thing(a);
					String desc = ((LList)o).description;
					if("".equals(desc)){ desc = "Description not found, please add a brief description of this word using the " +
							"<a href=\"describe.html\">describe</a> command."; }
					String in = input.split(":")[1];
					String output = in + " - " + desc;
					insertText(output, help);
				}
				else{
					insertText("Cannot describe non-list word.", help);
				}
			}
		}, a);
		e.bind(new LWord(LWord.Type.Prim, "genDocs") {
			public void eval(Environment e) {
				java.util.List<LWord> words = new ArrayList<LWord>(e.words());
				Collections.sort(words);
				for(LWord word : words) {
					generateDoc(word, false);
				}
				insertText("Documents generated in docs folder.", help);
			}
		});
		e.bind(new LWord(LWord.Type.Prim, "regenDocs") {
			public void eval(Environment e) {
				java.util.List<LWord> words = new ArrayList<LWord>(e.words());
				Collections.sort(words);
				for(LWord word : words) {
					generateDoc(word, true);
				}
				insertText("New documents generated in docs folder.", help);
			}
		});
	}
}

class TextEditor extends JPanel {
	private static final long serialVersionUID = 1L;
	private MLogo mlogo;
	private JTextArea editor = new JTextArea();
	private JFileChooser fileSelect = new JFileChooser(System.getProperty("user.dir") + "/save/");
	private String currentFile = "New File";
	private String input = "";
	private boolean changed = false;

	public TextEditor(MLogo m) {
		this.mlogo = m;
		this.editor.setTabSize(4);
		this.setLayout(new BorderLayout());

		JScrollPane scroll = new JScrollPane(editor);

		JToolBar toolBar = new JToolBar();
		toolBar.add(New);
		toolBar.add(Open);
		toolBar.add(Save);
		toolBar.add(SaveAs);

		toolBar.addSeparator();

		JButton cut = toolBar.add(Cut); 
		JButton	copy = toolBar.add(Copy); 
		JButton	paste = toolBar.add(Paste);

		toolBar.addSeparator();

		toolBar.add(Run); 
		toolBar.add(SaveRun);

		cut.setText("Cut");
		copy.setText("Copy");
		paste.setText("Paste");

		Save.setEnabled(false);
		SaveAs.setEnabled(false);

		editor.addKeyListener(k1);

		this.add(scroll, BorderLayout.CENTER);
		this.add(toolBar, BorderLayout.NORTH);

		mlogo.setTitle(mlogo.getVersion() + ", Editing: " + this.currentFile);
	}

	private KeyListener k1 = new KeyAdapter(){
		public void keyPressed(KeyEvent e) {
			changed = true;
			Save.setEnabled(true);
			SaveAs.setEnabled(true);
		}
	};

	Action New = new AbstractAction("New"){
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e){
			savePrev();
			editor.setText("");
			currentFile = "New File";
			mlogo.setTitle(mlogo.getVersion() + ", Editing: " + currentFile);
			changed = false;
		}
	};

	Action Open = new AbstractAction("Open"){
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			savePrev();
			if(fileSelect.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
				openFile(fileSelect.getSelectedFile().getAbsolutePath());
			}
			SaveAs.setEnabled(true);
		}
	};

	Action Save = new AbstractAction("Save"){
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if(!currentFile.equals("New File"))
				saveFile(currentFile);
			else
				saveFileAs();
		}
	};

	Action SaveAs = new AbstractAction("Save As"){
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			saveFileAs();
		}
	};

	Action Run = new AbstractAction("Run"){
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			input = editor.getText();
			mlogo.setInput(input);
		}
	};

	Action SaveRun = new AbstractAction("Save and Run"){
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			if(!currentFile.equals("New File"))
				saveFile(currentFile);
			else
				saveFileAs();

			input = editor.getText();
			mlogo.setInput(input);
		}
	};

	ActionMap m = editor.getActionMap();
	Action Cut = m.get(DefaultEditorKit.cutAction);
	Action Copy = m.get(DefaultEditorKit.copyAction);
	Action Paste = m.get(DefaultEditorKit.pasteAction);

	public String getInput(){
		return this.input;
	}

	public String getCurrentFile(){
		return this.currentFile;
	}

	public void openFile(String fileName){
		try {
			FileReader r = new FileReader(fileName);
			editor.read(r,null);
			r.close();
			currentFile = fileName;
			mlogo.setTitle(mlogo.getVersion() + ", Editing: " + this.currentFile);
			changed = false;
		}
		catch(IOException e) {
			Toolkit.getDefaultToolkit().beep();
			JOptionPane.showMessageDialog(this, "File not found: " + fileName);
		}
	}

	private void savePrev(){
		if(changed) {
			if(JOptionPane.showConfirmDialog(this, "Would you like to save "+ currentFile +" ?", "Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				saveFile(currentFile);
		}
	}

	public void saveFile(String fileName){
		try {
			FileWriter writer = new FileWriter(fileName);
			editor.write(writer);
			writer.close();
			currentFile = fileName;
			changed = false;
			mlogo.setTitle(mlogo.getVersion() + ", Editing: " + this.currentFile);
			Save.setEnabled(false);
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	private void saveFileAs() {
		if(fileSelect.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			saveFile(fileSelect.getSelectedFile().getAbsolutePath());
	}
}