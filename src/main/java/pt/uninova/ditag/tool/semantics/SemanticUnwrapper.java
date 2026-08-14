package pt.uninova.ditag.tool.semantics;

import java.util.ArrayList;
import java.util.List;

import pt.uninova.ditag.tool.DTLogger;

public class SemanticUnwrapper {
	
	public static ArrayList<String> unwrapGroups(String str) {
        ArrayList<String> result = new ArrayList<>();
        
        unwrapGroupsHelper(str, result, 0);

        return result;
    }
	
    private static void unwrapGroupsHelper(String str, ArrayList<String> result, int index) {
        int startIndex = str.indexOf('{', index);

        if (startIndex == -1) {
            result.add(str);
            return;
        }

        int endIndex = str.indexOf('}', startIndex);
        String prefix = str.substring(0, startIndex);
        String suffix = str.substring(endIndex + 1);
        String[] elements = str.substring(startIndex + 1, endIndex).split(";");

        for (String element : elements) {
            String newString = prefix + "{" + element + "}" + suffix;
            unwrapGroupsHelper(newString, result, startIndex + 1);
        }
    }
    
    // *** *** //
    
    public static ArrayList<String> unwrapInnerAnnotations(ArrayList<String> list) {
        ArrayList<String> result = new ArrayList<>();
        unwrapInnerAnnotationsHelper(list, result);
        
        return result;
    }
    
    public static List<String> parse(String s) {
        List<StringBuilder> levels = new ArrayList<>();
        int d = 0;
        for (char c : s.toCharArray()) {
            if (c == '[') {
                d++;
            } else if (c == ']') {
                d--;
            } else {
                while (levels.size() <= d) levels.add(new StringBuilder());
                levels.get(d).append(c);
            }
        }
        return levels.stream().map(StringBuilder::toString).toList();
    }

    public static void unwrapInnerAnnotationsHelper(ArrayList<String> list, ArrayList<String> result) {
        for (String str : list) {
//            if (str.contains("[{") && str.contains("}]")) {
//                result.add(str);
//                continue;
//            }
            List<String> parsed = parse(str);
            for (int i = 0; i < parsed.size(); i++) {
                DTLogger.logger.fine("Level " + i + ": " + parsed.get(i));
            }
//
//            if (parsed.size() >= 2) {
//                String outer = parsed.get(0).trim(); 
//                String annotation = parsed.get(1).trim(); 
//
//                result.add(outer);
//                result.add(outer + "/" + annotation);
//            } else {
//                result.add(str);
//            }
        }
    }
	
}
