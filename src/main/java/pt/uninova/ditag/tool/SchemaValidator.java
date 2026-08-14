package pt.uninova.ditag.tool;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.xml.sax.SAXException;

public class SchemaValidator {
	
	public static Integer validate(String content) {
	    if (isValidXMLSchema(content)) {
	        return 1;
	    } else if (isValidJSONSchema(content)) {
	        return 2;
	    } else {
	        return 0;
	    }
	}
	
	private static boolean isValidXMLSchema(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new java.io.ByteArrayInputStream(content.getBytes()));
            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return false;
        }
    }

	private static boolean isValidJSONSchema(String content) {
        try {
            new JSONObject(new JSONTokener(content));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
