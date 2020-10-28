import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

class TreeModel {
    public DefaultMutableTreeNode tree;
    public int num;
    public int previousNum;

    public TreeModel(DefaultMutableTreeNode tree, int num, int previousNum) {
        this.tree = tree;
        this.num = num;
        this.previousNum = previousNum;
    }
}

public class AntlrParser {
    private static JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private static JSplitPane splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private static JPanel panel = new JPanel(new BorderLayout());
    private static JTextArea txtArea = new JTextArea();
    private static JTextArea txtAreaConsole = new JTextArea();
    private static JPanel panelConsole = new JPanel(new BorderLayout());
    private static JPanel leftPanel = new JPanel(new BorderLayout());
    private static JPanel syntaxPanel = new JPanel(new BorderLayout());
    private static JButton btn = new JButton("CLICK");
    private static JTabbedPane tabsLeft = new JTabbedPane(JTabbedPane.TOP,
            JTabbedPane.SCROLL_TAB_LAYOUT);

    private static int previousNum = 0;

    private static ArrayList<TreeModel> treeModelList = new ArrayList<>();
    private static DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode("AST");
    private static JTable table;

    //When the value is false, the syntaxError method returns without displaying errors.
    private static final boolean REPORT_SYNTAX_ERRORS = true;

//    private String errorMsg = "";

    public AntlrParser() throws IOException {
        JFrame frame = new JFrame();
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        String text = readFile(new File("./Code/example.java"));
        Java8Lexer lexer = new Java8Lexer(new ANTLRInputStream(text));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java8Parser parser = new Java8Parser(tokens);

        txtArea.setText(readFile(new File("./Code/example.java")));
        splitPane.setRightComponent(new JScrollPane(txtArea));

        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    treeModelList.clear();

                    print(parseJava(txtArea.getText(), lexer, tokens, parser), false);

                    printTokens(txtArea.getText());

                    printTree();

                    errorsOutput(txtArea.getText());

                    DefaultTreeModel treeModel = new DefaultTreeModel(dmtn);
                    JTree tree = new JTree(treeModel);
                    syntaxPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
                    splitMain.setRightComponent(new JScrollPane(txtAreaConsole));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        tabsLeft.add("Syntax analyzer", syntaxPanel);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(btn, BorderLayout.NORTH);

        float k = 4e4f;


        panel.setPreferredSize(new Dimension(800, 400));

        splitMain.setLeftComponent(panel);
//        splitMain.setRightComponent(new JScrollPane(txtAreaConsole));

        frame.add(splitMain);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new AntlrParser();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void errorsOutput(String code) {
        Java8Lexer lexer = new Java8Lexer(CharStreams.fromString(code));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java8Parser parser = new Java8Parser(tokens);

        parser.removeErrorListeners();
        final List<String> errorMessages = new ArrayList<>();

        txtAreaConsole.setEnabled(false);

        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errorMessages.add(msg);
            }
        });

        parser.compilationUnit();

        if(errorMessages.size() != 0) {
            String errorTxt = "";
            for (String errorMessage : errorMessages) {
//                System.out.println(errorMessages.get(i));
                leftPanel.removeAll();
                syntaxPanel.removeAll();
                dmtn.removeAllChildren();
                errorTxt += errorMessage + "\n";
            }
            txtAreaConsole.setText( errorTxt );
            txtAreaConsole.setDisabledTextColor(new Color(255, 0, 0));
        } else {
            txtAreaConsole.setText("Successfully compiled");
            txtAreaConsole.setDisabledTextColor(new Color(0, 0, 0));
        }
    }

    private static String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, Charset.forName("UTF-8"));
    }

    private static void printTokens(String text) {
//        String text = readFile(new File("./Code/example.java"));
        Java8Lexer lexer = new Java8Lexer(new ANTLRInputStream(text));

        leftPanel.removeAll();
        List<? extends Token> tokenList = new ArrayList<>();
        tokenList = lexer.getAllTokens();

        Object[][] arrObj = new Object[tokenList.size()][4];
        String[] colName = { "Type", "Line start", "Line end", "Name" };

        for(int i = 0; i<tokenList.size(); i++){
//            System.out.println(token.getText() + " || " + token.getStartIndex() + " || " + token.getStopIndex() + " || " + Java8Lexer.returnVariableName(token.getType()) + "\n");
            arrObj[i][0] = tokenList.get(i).getText();
            arrObj[i][1] = tokenList.get(i).getStartIndex();
            arrObj[i][2] = tokenList.get(i).getStopIndex();
            arrObj[i][3] = Java8Lexer.returnVariableName(tokenList.get(i).getType());
        }
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(colName);

        for(int i = 0; i<arrObj.length; i++) {
            model.addRow(arrObj[i]);
        }

        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        tabsLeft.add("Tokens", leftPanel);

        splitPane.setLeftComponent(tabsLeft); // leftPanel
    }

    private static ParserRuleContext parseJava(String code, Java8Lexer lexer, CommonTokenStream tokens, Java8Parser parser) throws IOException {
        lexer = new Java8Lexer(new ANTLRInputStream(code));
        tokens = new CommonTokenStream(lexer);
        parser = new Java8Parser(tokens);

        return Objects.requireNonNull(parser.classDeclaration());
    }

    private static void print(RuleContext ctx, boolean verbose) {
        explore(ctx, verbose, 0);
    }

    private static void printTree() {
        syntaxPanel.removeAll();
        dmtn.removeAllChildren();

        for(int i = 0; i<treeModelList.size(); i++) {
            if(treeModelList.get(i).num == 0) {
                dmtn.add(treeModelList.get(i).tree);
            } else {
                int index = 0;
                for(int j = 0; j<i; j++) {
                    if(treeModelList.get(j).num == treeModelList.get(i).num-1) {
                        index = j;
                    }
                }
                treeModelList.get(index).tree.add( treeModelList.get(i).tree );
            }
        }
        dmtn.add(treeModelList.get(0).tree);
    }

    private static void explore(RuleContext ctx, boolean verbose, int indentation) {
        boolean toBeIgnored = !verbose && ctx.getChildCount() == 1
                && ctx.getChild(0) instanceof ParserRuleContext;
        if (!toBeIgnored) {
            int x = 0;
            String ruleName = Java8Parser.ruleNames[ctx.getRuleIndex()];
            for (int i = 0; i < indentation; i++) {
                x++;
//                System.out.print("  ");
            }
//            System.out.println(x + " - " + previousNum + " || " +ruleName + " " + ctx.getText());

            treeModelList.add(new TreeModel(new DefaultMutableTreeNode(ruleName + " " + ctx.getText()), x, previousNum));

            previousNum = x;
        }
        for (int i=0;i<ctx.getChildCount();i++) {
            ParseTree element = ctx.getChild(i);

            if (element instanceof RuleContext) {
                explore((RuleContext)element, verbose, indentation + (toBeIgnored ? 0 : 1));
            }
        }
    }
}
