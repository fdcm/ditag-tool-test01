package pt.uninova.ditag.tool.translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.xml.sax.InputSource;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.CodeWriter;
import com.sun.tools.xjc.api.ClassNameAllocator;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.XJC;
import com.sun.codemodel.writer.SingleStreamCodeWriter;

public class SemanticPOJOCreator {

	public static String fromXSD(String xsdContent, String outClassName) throws IOException {
	    SchemaCompiler sc = XJC.createSchemaCompiler();
	    sc.forcePackageName("");
	    
	    sc.setClassNameAllocator(new ClassNameAllocator() {
	        public String assignClassName(String packageName, String className) {
	            return outClassName;
	        }
	    });

	    try (StringReader reader = new StringReader(xsdContent)) {
	        InputSource is = new InputSource(reader);
	        is.setSystemId("schema.xsd");

	        sc.parseSchema(is);
	        S2JJAXBModel model = sc.bind();
	        JCodeModel jCodeModel = model.generateCode(null, null);

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        CodeWriter codeWriter = new SingleStreamCodeWriter(baos);

	        jCodeModel.build(codeWriter);

	        String generatedCode = baos.toString();

	        generatedCode = Arrays.stream(generatedCode.split("\n"))
	            .map(line -> {
	                if (line.startsWith("public class")) {
	                    return line.replace("public class", "class");
	                } else {
	                    return line;
	                }
	            })
	            .filter(line -> !line.startsWith("import") && !line.contains("-----"))
	            .collect(Collectors.joining("\n"));

	        generatedCode = generatedCode.replaceAll(" ObjectFactory", " ObjectFactory_" + outClassName);
	        generatedCode = generatedCode.replaceAll("BigInteger ", "Integer ");
	        generatedCode = generatedCode.replaceAll("BigDecimal ", "float ");

	        return generatedCode;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
}

