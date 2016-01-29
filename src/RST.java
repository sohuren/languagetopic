import org.fun4j.Cons;
import org.fun4j.compiler.Parser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class RST {

    static int sentNum = 0; 	//	keeps track of sentNum in paragraph
    
    static ArrayList<Tree> nodePointers = new ArrayList<>(); //keep track of pointers for each sentence in the tree.
    static Paragraph paragraph;
    static int index = 0;

    static void readRSTTree(String filename, Paragraph paragraph_) throws IOException {
        //read in RST tree
        ArrayList<String> lines = new ArrayList<>();
        try {

            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            do {
                line = br.readLine();
                if (line != null)
                    lines.add(line);
            } while (line != null);
        } catch (Exception e) {
            return;
        }

        sentNum = 0;
        nodePointers = new ArrayList<>();
        paragraph = paragraph_;
        index = 0;

        paragraph.rstTree = processTree(lines, null, 0);

    }


    static Tree processTree(ArrayList<String> lines, Tree parent, int depth) {
        
        String line = lines.get(index);
        Tree tree = new Tree(parent, depth);
        if (line.trim().startsWith("(")) {
            line = line.trim().substring(1);
            if (!line.contains("_!")) {
                tree.text = line;
                index++;
                tree.left = processTree(lines, tree, depth+1);
                tree.right = processTree(lines, tree, depth+1);
            } else {
                //deal with text
                String[] parts = line.split("!_|_!");
                tree.text = parts[0];
                tree.left = new Tree(parts[1], tree, depth+1);
                nodePointers.add(tree.left);

                if (parts[1].contains("<s>")) {
                    paragraph.sent2RSTNodePointers.add(nodePointers);
                    nodePointers = new ArrayList<>();
                    sentNum++;
                    assert sentNum == paragraph.sent2RSTNodePointers.size();
                }

                tree.right = new Tree(parts[3], tree, depth+1);
                nodePointers.add(tree.right);
                if (parts[3].contains("<s>")) {
                    paragraph.sent2RSTNodePointers.add(nodePointers);
                    nodePointers = new ArrayList<>();
                    sentNum++;
                    assert sentNum == paragraph.sent2RSTNodePointers.size();
                }
                index++;
            }
        } else {
            
        	//deal with text
            tree.text = line;
            nodePointers.add(tree);
            if (line.contains("<s>")) {
                paragraph.sent2RSTNodePointers.add(nodePointers);
                nodePointers = new ArrayList<>();
                sentNum++;
                assert sentNum == paragraph.sent2RSTNodePointers.size();
            }
            index++;
        }


        return tree;
    }



    static Tree getCommonAncestor(Tree x, Tree y) {

        assert x!=null && y != null;

        if(x.equals(y)) return x;

        if(x.depth == y.depth)
            return getCommonAncestor(x.parent, y.parent);

        if(x.depth > y.depth)
            return getCommonAncestor(x.parent, y);

        return getCommonAncestor(x, y.parent);

    }

}