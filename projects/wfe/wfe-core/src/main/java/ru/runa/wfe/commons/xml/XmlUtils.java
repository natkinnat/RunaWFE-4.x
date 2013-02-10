package ru.runa.wfe.commons.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import ru.runa.wfe.commons.ClassLoaderUtil;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

/**
 * Unifies XML operations (Dom4j).
 * 
 * @author Dofs
 */
public class XmlUtils {

    public static Document parseWithoutValidation(String data) {
        return parseWithoutValidation(data.getBytes(Charsets.UTF_8));
    }

    public static Document parseWithoutValidation(byte[] data) {
        return parse(new ByteArrayInputStream(data), false, false, null);
    }

    public static Document parseWithoutValidation(InputStream in) {
        return parse(in, false, false, null);
    }

    public static Document parseWithXSDValidation(InputStream in, String xsdResourceName) {
        return parse(in, false, true, xsdResourceName);
    }

    public static Document parseWithXSDValidation(byte[] data, String xsdResourceName) {
        return parseWithXSDValidation(new ByteArrayInputStream(data), xsdResourceName);
    }

    public static Document parseWithXSDValidation(String data, String xsdResourceName) {
        return parseWithXSDValidation(data.getBytes(Charsets.UTF_8), xsdResourceName);
    }

    private static Document parse(InputStream in, boolean dtdValidation, boolean xsdValidation, String xsdResourceName) {
        try {
            SAXReader reader;
            if (xsdValidation) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                if (xsdResourceName != null) {
                    InputStream xsdInputStream = ClassLoaderUtil.getAsStreamNotNull(xsdResourceName, XmlUtils.class);
                    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
                    factory.setSchema(schemaFactory.newSchema(new Source[] { new StreamSource(xsdInputStream) }));
                } else {
                    factory.setValidating(true);
                }
                SAXParser parser = factory.newSAXParser();
                if (xsdResourceName == null) {
                    parser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
                }
                reader = new SAXReader(parser.getXMLReader());
            } else {
                reader = new SAXReader();
            }
            reader.setValidation(dtdValidation || (xsdValidation && xsdResourceName == null));
            reader.setErrorHandler(SimpleErrorHandler.getInstance());
            return reader.read(new BOMSkippingReader(new InputStreamReader(in, Charsets.UTF_8)));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static byte[] save(Node node) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XMLWriter writer = new XMLWriter(baos, OutputFormat.createPrettyPrint());
            writer.write(node);
            return baos.toByteArray();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public static String toString(Node node) {
        return new String(save(node), Charsets.UTF_8);
    }

    public static String serialize(Map<String, String> map) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("r");
        for (Entry<String, String> entry : map.entrySet()) {
            Element e = root.addElement(entry.getKey());
            e.addText(String.valueOf(entry.getValue()));
        }
        return toString(document);
    }

    public static Map<String, String> deserialize(String xml) {
        Map<String, String> result = Maps.newHashMap();
        Document document = parseWithoutValidation(xml);
        List<Element> elements = document.getRootElement().elements();
        for (Element element : elements) {
            result.put(element.getName(), element.getText());
        }
        return result;
    }
}
