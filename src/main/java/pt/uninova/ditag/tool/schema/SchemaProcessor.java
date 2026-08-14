package pt.uninova.ditag.tool.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;

import pt.uninova.ditag.tool.DTLogger;

public final class SchemaProcessor {
	
	private SchemaProcessor() {}
	
	public static ArrayList<Element> getElements(Document doc, ArrayList<String> elementTypes) {
	    ArrayList<Element> elementList = new ArrayList<>(); 
	    if (doc != null)  {
	    	Element rootElement = doc.getRootElement();
	    	getElements(rootElement, elementTypes, elementList);
	    }
	    return elementList;
	}
	private static void getElements(Element element, ArrayList<String> elementTypes, ArrayList<Element> elementList) {
	    if (elementTypes.contains(element.getName())) {
	    	elementList.add(element);
	    }

	    List<Element> children = element.getChildren();
	    for (Element child : children) {
	    	getElements(child, elementTypes, elementList);
	    }
	}
	
	public static ArrayList<String> unwrapAnnotation(String str) {
		ArrayList<String> resultInner = new ArrayList<>();
		
        unwrapInnerAnnotations(str, resultInner);
        
        resultInner = resultInner.stream()
                .map(s -> s
                    // Replace /{ with [{ if not Already Preceded by [
                    .replaceAll("/(?<!\\[)\\{", "[{")
                    // Replace } with }] if not Already Followed by ]
                    .replaceAll("\\}(?!\\])", "}]")
                )
                .collect(Collectors.toCollection(ArrayList::new));
        
        DTLogger.logger.fine(resultInner.toString());
        return resultInner;
	}
	
//	public static void unwrapAllAnnotations(String str, List<String> result) {
//        int bracketStart = str.indexOf('[');
//        if (bracketStart == -1) {
//            result.add(str); // no annotation
//            return;
//        }
//
//        String basePath = str.substring(0, bracketStart);
//        int bracketEnd = findMatchingBracket(str, bracketStart);
//        String innerExpr = str.substring(bracketStart + 1, bracketEnd);
//        String after = str.substring(bracketEnd + 1);
//
//        // Clean and normalize tail
//        if (after.startsWith("/")) after = after.substring(1);
//        String fullOuter = basePath + "/" + after;
//
//        // Extract full inner path and path with value
//        int braceStart = innerExpr.indexOf('{');
//        int braceEnd = innerExpr.lastIndexOf('}');
//
//        if (braceStart == -1 || braceEnd == -1 || braceEnd < braceStart) {
//            throw new IllegalArgumentException("Missing or invalid value condition in annotation.");
//        }
//
//        String innerNoValue = innerExpr.substring(0, braceStart).replaceAll("/+$", "");
//        String innerWithValue = innerExpr.substring(0, braceEnd + 1).replaceAll("/+$", "");
//
//        // Add the three relevant pieces
//        result.add(fullOuter); // 1. /A/B
//        result.add(basePath + "/" + innerNoValue); // 2. /A/X/Y
//        result.add(basePath + "/" + innerWithValue); // 3. /A/X/Y{Z}
//    }
	
	public static void unwrapInnerAnnotations(String str, List<String> result) {
        int bracketStart = str.indexOf('[');
        if (bracketStart == -1) {
            result.add(str);
            return;
        }

        String basePath = str.substring(0, bracketStart);
        int bracketEnd = findMatchingBracket(str, bracketStart);
        String innerExpr = str.substring(bracketStart + 1, bracketEnd);
        String after = str.substring(bracketEnd + 1);

        if (after.startsWith("/")) after = after.substring(1);
        String fullOuter = basePath + (after == "" ? "" : "/") + after;
        result.add(fullOuter);
        unwrapInnerAnnotations(basePath + (innerExpr.startsWith("[{") ? "" : "/") + innerExpr, result);
	}
	
    private static int findMatchingBracket(String input, int start) {
        int depth = 0;
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1; // unmatched bracket
    }
}
