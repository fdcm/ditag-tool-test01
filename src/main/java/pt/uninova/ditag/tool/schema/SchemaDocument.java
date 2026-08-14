package pt.uninova.ditag.tool.schema;

import java.io.File;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import java.io.StringReader;

public class SchemaDocument {
	
	private Document data;
	private String directory;
	
	// *** *** //
	
	public SchemaDocument() {
		super();
		
		this.data = null;
		this.directory = null;
	}

	public SchemaDocument(String content) {
	    super();
	    
	    try {
	        SAXBuilder saxBuilder = new SAXBuilder();
	        this.data = saxBuilder.build(new StringReader(content));
	    } catch (JDOMException | IOException e) {
	        e.printStackTrace();
	    }
	}
	
	// *** *** //
	
	public Document getData() {
		return data;
	}

	public void setData(Document data) {
		this.data = data;
	}
	
	// *** *** //
	
	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}
	
	public String getFileName() {
        String fileNameWithExtension = new File(this.directory).getName();
        int dotIndex = fileNameWithExtension.lastIndexOf('.');
		return (dotIndex == -1) ? fileNameWithExtension : fileNameWithExtension.substring(0, dotIndex);
	}
	
	public String getFileExtension() {
		String fileName = new File(this.directory).getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "N/A";
	}
	
}
