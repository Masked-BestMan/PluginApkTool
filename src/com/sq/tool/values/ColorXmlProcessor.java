package com.sq.tool.values;

import com.sq.tool.LogUtil;
import com.sq.tool.xml.XmlProcessor;
import org.dom4j.DocumentException;
import org.dom4j.Element;

public class ColorXmlProcessor extends XmlProcessor {
    public ColorXmlProcessor(String fileName) throws DocumentException {
        super(fileName);
        fixColorXml();
    }

    private void fixColorXml(){
        Element resourcesElement = getSelfDocument().getRootElement();
        for (Element colorElement : resourcesElement.elements()){
            String text = colorElement.getText();
            colorElement.setText(text.replace("@android:color", "@*android:color"));
            LogUtil.i("fixColorXml:" + text);
        }
    }
}
