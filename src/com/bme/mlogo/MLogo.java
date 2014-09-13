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
	static final String version = "Loko v1.0";
	static final String fileLoc = System.getProperty("user.dir");
	static URL docs;
	static URL mods;
	private File saveFile;
	private File[] moduleFiles;
	private String input = "";
	private String output = "";
	private int lineCount = 0;
	private JFrame frame;
	private TurtleGraphics t;
	private JTabbedPane tabbedPane1;
	private JTabbedPane tabbedPane2;
	private JPanel tContainer;
	private JSplitPane splitPane;
	private JTextField listener;
	private JTextPane terminal;
	private JTextPane help;
	private JTextPane modules;
	private TextEditor editor;
	private JComponent turtlePane;
	private Environment e;
	private int currentTab = 0;

	public MLogo(){
		e = kernel();
		primitiveIO(e);
		describe(e);

		try				   { docs = new URL("file:///" + fileLoc + "/docs/"); }
		catch(Exception ex){ ex.printStackTrace();							  }

		try				   { mods = new URL("file:///" + fileLoc + "/modules/"); }
		catch(Exception ex){ ex.printStackTrace();									}

		saveFile = new File("save/env_save");
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

		File modDirectory = new File(fileLoc + "/modules/");
		modDirectory.mkdirs();
		moduleFiles = new File[0];

		t = new TurtleGraphics(e, 380, 320);
		repl(e, t);
	}

	public static void main(String[] a) {
		new MLogo();
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
			int count = 0;
			try {
				while("".equals(input)){ 
					try{
						Thread.sleep(10);
						count += 1;
						if(count == 50){
							count = 0;
							if(tabbedPane2.getSelectedIndex() == 1){
								updateModules();
							}
						}
					}
					catch(InterruptedException e){ }
				}
				while(Parser.complete(input).size() > 0) {
					try{
						Thread.sleep(10);
						count += 1;
						if(count == 50){
							count = 0;
							if(tabbedPane2.getSelectedIndex() == 1){
								updateModules();
							}
						}
					}
					catch(InterruptedException e){ }
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
		this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.frame.setPreferredSize(new Dimension(1024, 768));
		this.frame.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		//create small graphics display
		this.turtlePane = t.getTurtle();

		//set up first tabbed pane (terminal, large turtle graphics window)
		this.tabbedPane1 = new JTabbedPane();
		tabbedPane1.addChangeListener(this);

		//create and add text editor to tab
		this.editor = new TextEditor(this);
		this.tabbedPane1.add(this.editor, 0);
		this.tabbedPane1.setTitleAt(0, "Editor");
		this.tabbedPane1.setToolTipTextAt(0, "Program Editor Window");

		//create and add container pane for the large graphics display to tab
		this.tContainer = new JPanel();
		this.tabbedPane1.add(this.tContainer, 1);
		this.tabbedPane1.setTitleAt(1, "Graphics");
		this.tabbedPane1.setToolTipTextAt(1, "Large Turtle Graphics Display");

		//set up terminal window
		this.terminal = new JTextPane();
		this.terminal.setEditable(false);
		this.terminal.setContentType("text/html");
		((HTMLDocument)this.terminal.getDocument()).setBase(docs);
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
		this.tabbedPane2 = new JTabbedPane();

		//create help window
		this.help = new JTextPane();
		help.setEditable(false);
		help.setContentType("text/html");
		((HTMLDocument)help.getDocument()).setBase(docs);
		help.addHyperlinkListener(new HyperlinkListener(){
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

		JComponent tab1 = new JScrollPane(this.help);
		this.tabbedPane2.add(tab1, 0);
		this.tabbedPane2.setTitleAt(0, "Teacher");
		this.tabbedPane2.setToolTipTextAt(0, "Digital tutor and help menu");

		this.modules = new JTextPane();
		modules.setEditable(false);
		modules.setContentType("text/html");
		((HTMLDocument)modules.getDocument()).setBase(mods);
		modules.addHyperlinkListener(new HyperlinkListener(){
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if(Desktop.isDesktopSupported()) {
						try{ Desktop.getDesktop().browse(e.getURL().toURI()); }
						catch(Exception ex){ insertText("Cannot load module file.", help); }
					}
				}
			}
		});

		JComponent tab2 = new JScrollPane(this.modules);
		this.tabbedPane2.add(tab2, 1);
		this.tabbedPane2.setTitleAt(1, "Modules");
		this.tabbedPane2.setToolTipTextAt(1, "Learning modules");

		JComponent tab3 = new JScrollPane();
		//tabbedPane2.addTab("Library", null, tab3, "Function library");

		tabbedPane2.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				if(tabbedPane2.getSelectedIndex() == 1){ updateModules(); }
			}
		});

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
		frame.add(splitPane, c); //split pane

		c.fill = GridBagConstraints.VERTICAL;
		c.ipadx = 300;
		c.ipady = 330;
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		frame.add(tabbedPane2, c); //tutor and lib pane

		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 0;
		c.ipady = 15;
		c.gridx = 0;
		c.gridy = 2;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		frame.add(listener, c); //user input pane

		c.fill = GridBagConstraints.BOTH;
		c.ipadx = 300;
		c.ipady = 300;
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		frame.add(turtlePane, c); //small turtle graphics pane

		frame.addComponentListener(this);
		frame.pack();
		frame.setVisible(true);
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
		else if(!listener.getText().contains("clear"))			   { insertText(">" + this.output, terminal);  }
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
			for(LAtom atom : e.trace) {
				insertText(String.format("<pre>\tin %s%n</pre>", atom), help);
			}
			this.input = "";
			this.lineCount = 0;
			env.reset();
		}
	}

	public void updateModules(){
		File dir = new File(fileLoc + "/modules/");
		boolean changed = false;
		dir.mkdirs();

		File[] files = dir.listFiles(new FilenameFilter(){
			public boolean accept(File f, String name){
				return name.endsWith("html");
			}
		});

		if(files.length != moduleFiles.length){ 
			changed = true; 
		}
		else{
			for(int i = 0; i < files.length; i += 1){
				if(!files[i].getName().equals(moduleFiles[i].getName())){ 
					changed = true;
					break;
				}
			}
		}

		if(files.length == 0){
			modules.setText("");
			insertText("No module files found, make sure any module files you want to use are stored in the 'modules' folder.", modules);
		}
		else if(changed){
			modules.setText("");
			for(File file: files){
				insertText("<a href=\"" + file.getName() + "\">" + file.getName().replaceFirst(".html", "") + "</a>", modules);
			}
		}

		moduleFiles = files;
	}

	private void changeState(){
		int index = this.tabbedPane1.getSelectedIndex();
		Dimension dim = this.frame.getSize();

		if(index == 1){
			this.splitPane.setOneTouchExpandable(false);
			this.splitPane.setEnabled(false);
			this.splitPane.setDividerLocation(0.75);

			int width = this.tabbedPane1.getWidth();
			int height = this.splitPane.getDividerLocation();
			this.tContainer.remove(this.turtlePane);
			this.frame.remove(this.turtlePane);
			this.t.resize(width, height);
			this.turtlePane = this.t.getTurtle();
			this.tContainer.add(this.turtlePane);
			this.frame.setPreferredSize(dim);
			this.frame.pack();
		}
		else if(currentTab == 1){
			this.tContainer.remove(this.turtlePane);

			this.splitPane.setOneTouchExpandable(true);
			this.splitPane.setEnabled(true);
			this.splitPane.setDividerLocation(0.5);

			this.t.resize(380, 320);
			this.turtlePane = t.getTurtle();
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.ipadx = 300;
			c.ipady = 300;
			c.gridx = 1;
			c.gridy = 0;
			c.gridheight = 1;
			c.weightx = 0.0;
			c.weighty = 0.0;
			this.frame.add(this.turtlePane, c);
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
							String desc = list.description;
							if(list.arguments != null){
								sourceText = "local " + word.toString() + " bind " + list.arguments.toString() + sourceText;
							}
							else{
								sourceText = "local " + word.toString() + " " + sourceText;
							}
							writer.println(sourceText);
							writer.println("describe [" + desc + "]");
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
		map.put("args", "Returns the arguments associated with an input list.");
		map.put("ascall", "Converts the input word to a call.");
		map.put("asname", "Covnerts the input word to a name.");
		map.put("asvalue", "Converts the input word to a value.");
		map.put("back", "Moves the turtle back by the input number of units.");
		map.put("bind", "Associates a list of arguments with another list. For example:<br><br>" +
				"bind ['x][print :x]<br><br>" +
				"returns the [print :x] list with the argument x bound to it. When used with the <a href=\"local.html\">local</a> command, " +
				"bind can be used to create lists with arguments and associate these lists with words so that they can be run later.");
		map.put("butfirst", "Returns the input list with all but the first element of that list.");
		map.put("butlast", "Returns the input list with all but the last element of that list.");
		map.put("clear", "Clears all messages printed to the terminal.");
		map.put("clrhelp", "Clears all messages printed to the help window.");

		map.put("clrturtle", "Clears all lines drawn to the graphics window.");
		map.put("describe", "Prints a description of the given word to the help window.");
		map.put("dif", "Returns the difference of 2 given numbers.");
		map.put("edit", "Loads a specified file to the editor pane for editing.");
		map.put("equal?", "Returns true if the two inputs are equal, false if they are not.");
		map.put("erase", "Deletes a user defined word. Note that system defined primitives cannot be erased.");
		map.put("first", "Returns the first element in the input list.");
		map.put("flatten", "Removes any sub-lists from the input list and inserts the contents of these sublists in the original list in their original " +
				"order. For example:<br><br>" +
				"[1 [2] 3 [[4 []] [] 5]]<br><br>" +
				"would become<br><br>" +
				"[1 2 3 4 5].");
		map.put("forward", "Moves the turtle forward by the input number of units.");
		map.put("fput", "Returns the input list with the input atom added to the front of the list.");

		map.put("genDocs", "Generates a help document for each word in the system word list with the word's name, arguments, and description. " +
				"System generated primitive words are all given descriptions automatically, user defined words need to be described by the user " +
				"after they are created using the <a href=\"describe.html\">describe</a> command<br><br>." +
				"This command will only generate documents for words that do not currently have a document file in the docs folder. To update existing " +
				"documentation, see the <a href=\"regenDocs.html\">regenDocs</a> command.");
		map.put("getDesc", "Prints the input word's name and description to the help window.");
		map.put("greater?", "Returns true if the first input is greater than the second input, false if it is not.");
		map.put("help", "Prints the full list of words to the help window along with their argument lists and links to these documentation files for " +
				"more information.");
		map.put("home", "Resets the turtle's position to the center of the drawing area (X0 Y0).");
		map.put("if", "Executes the input list if and only if the input conditions are true.");
		map.put("item", "Returns the item in the input list at the input number position in the list, starting at 0. Example:<br><br>" +
				"item 3 [1 2 3 4 5] returns 4<br><br>" +
				"Note that if the input number is greater than the length of the list, the empty list '[]' will be output instead. Example:<br><br>" +
				"item 5 [1 2 3 4 5] returns []");
		map.put("join", "Returns the combination of the two input lists as a single list. Example:<br><br>" +
				"join [1 [2 3] [4 [5 []]] [6 [7] 8 [] 9 [10]] returns [1 [2 3] [4 [5 []] 6 [7] 8 [] 9 [10]]");
		map.put("last", "Returns the last element in the input list.");
		map.put("left", "Turns the turtle left by the input number of degrees.");

		map.put("less?", "Returns true if the first input is less than the second input, false if it is not.");
		map.put("list?", "Returns true if the input is a list, false if it is not.");
		map.put("load", "Loads and runs the contents of the file specified by the input filename.");
		map.put("local", "Creates a new local variable with the input name and value.");
		map.put("lput", "Returns the input list with the input atom added to the back of the list.");
		map.put("make", "Creates a new local variable with the input name and value. If the input name already exists as a variable, make will" +
				"instead modify that value rather than creating a new variable.");
		map.put("member", "Searches a list for the input atom and returns a list containing that atom and all elements after it. Example:<br><br>" +
				"member 'elements [return all elements after atom] returns [elements after atom]<br><br>" +
				"If the input atom cannot be found in the list, the empty list '[]' will be output instead. Example:" +
				"member 6 [1 2 3 4 5] returns []");
		map.put("negate", "Returns the negative of the input number."); 
		map.put("num?", "Returns true if the input word is a number, false if it is not.");
		map.put("output", "Stops the current process and returns the given atom as output.");

		map.put("pendown", "Lowers the drawing pen so that lines will be drawn when the turtle moves.");
		map.put("penup", "Raises the drawing pen so that no lines will be drawn when the turtle moves.");
		map.put("print", "Prints the input to the terminal window.");
		map.put("println", "Prints an empty line to the terminal window.");
		map.put("product", "Returns the product of 2 given numbers.");
		map.put("quotient", "Returns the quotient of 2 given numbers.");
		map.put("random", "Returns a random element from a given list.");
		map.put("readlist", "Returns the previous line from the terminal as a list.");
		map.put("regenDocs", "Deletes any existing help documents and generates new documents for all words in the system word list with current definitions, " +
				"reflecting any changes made to a word's definitions since the document for that word was last generated.");
		map.put("remainder", "Returns the remainder of a division of 2 given numbers.");

		map.put("repeat", "Runs an input list a number of times specified by the input number.");
		map.put("right", "Turns the turtle right by the input number of degrees.");
		map.put("run", "Runs the input list as a set of instructions.");
		map.put("save", "Saves the contents of the editor window to a file specified by the input filename. Note that this will overwrite any information " +
				"already stored in the specified file if that file already exists.");
		map.put("saveEnv", "Saves any user defined words in the system to the file 'saveFile' to be loaded the next time Loko is run.");
		map.put("setcolor", "Sets the color of the turtle's drawing pen based on the 3 input values. These values are numbers from 0 to 255 and represent" +
				"the red, green, and blue color saturation in that order. Different combinations of these values produce a broad range of colors.<br><br>" +
				"Examples:<br>" +
				"White - 255, 255, 255<br>" +
				"Black - 0, 0, 0<br>" +
				"Orange - 255, 128, 0<br>" +
				"Yellow - 255, 255, 0<br>" +
				"Purple - 128, 0, 255<br>");
		map.put("size", "Returns the size (number of elements) of the input list.");
		map.put("stop", "Stops execution of the current process.");
		map.put("sum", "Returns the sum of 2 given numbers.");
		map.put("thing", "Returns the value associated with a given word.");

		map.put("trace", "Prints a list of the scopes in which the current procedure is being run, beginning with the most specific local scope and" +
				"ending with the global scope.");
		map.put("unless", "Executes the input list if and only if the input conditions are false.");
		map.put("version", "Prints the current Loko version number.");
		map.put("word?", "Returns true if the input is a word, false if it is not.");

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

	private void primitiveIO(Environment e) {
		final LWord a = new LWord(LWord.Type.Name, "word");
		final LWord b = new LWord(LWord.Type.Name, "filename");
		final LWord c = new LWord(LWord.Type.Name, "description");

		e.bind(new LWord(LWord.Type.Prim, "version") {
			public void eval(Environment e) {
				insertText(MLogo.version, terminal);
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
				insertText("Trace:<br>", help);
				for(LAtom s : e.trace()) {
					insertText(s.toString(), help);
				}
				insertText("'global", help);
				
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
				String term = "";
				try{ term = ((HTMLDocument)terminal.getDocument()).getText(0, terminal.getDocument().getLength()); }
				catch(BadLocationException ex){ ex.printStackTrace(); }

				Scanner in = new Scanner(term);
				Scanner next = new Scanner(term);
				if(next.hasNextLine()){ next.nextLine(); }
				else{ e.output(Parser.parse("")); }

				while(next.hasNextLine()){
					next.nextLine();
					String str = in.nextLine().replaceFirst(">", "");
					
					if(!next.hasNextLine()){
						e.output(Parser.parse(str));
					}
				}
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
				insertText("Click on a word to learn more about how it works.", help);
			}
		});

		e.bind(new LWord(LWord.Type.Prim, "clear") {
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
				if(a.type != LWord.Type.Call && e.thing(a) instanceof LList){
					LAtom o = e.thing(a);
					String desc = ((LList)o).description;
					if("".equals(desc)){ desc = "Description not found, please add a brief description of this word using the " +
							"<a href=\"describe.html\">describe</a> command."; }
					String in = input.split(":")[1];
					String output = in + " - " + desc;
					insertText(output, help);
				}
				else{
					insertText("Cannot describe non-list atom.", help);
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
		this.editor.setTabSize(2);
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