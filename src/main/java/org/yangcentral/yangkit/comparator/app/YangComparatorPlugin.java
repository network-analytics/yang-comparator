package org.yangcentral.yangkit.comparator.app;

import org.yangcentral.yangkit.comparator.CompareType;
import org.yangcentral.yangkit.comparator.YangComparator;
import org.yangcentral.yangkit.comparator.YangCompareResult;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.compiler.FileSource;
import org.yangcentral.yangkit.compiler.Settings;
import org.yangcentral.yangkit.compiler.YangCompiler;
import org.yangcentral.yangkit.compiler.YangCompilerException;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.compiler.plugin.YangCompilerPlugin;
import org.yangcentral.yangkit.compiler.plugin.YangCompilerPluginParameter;
import org.yangcentral.yangkit.utils.file.FileUtil;
import org.yangcentral.yangkit.utils.xml.XmlWriter;

import java.io.IOException;
import java.util.*;

public class YangComparatorPlugin implements YangCompilerPlugin {

    @Override
    public void run(YangSchemaContext yangSchemaContext, YangCompiler yangCompiler,List<YangCompilerPluginParameter> list) throws YangCompilerException {
        CompareType compareType = null;
        String oldYangPath = null;
        String rulePath = null;
        String resultPath = null;
        for(YangCompilerPluginParameter parameter:list){
            //System.out.println("para name="+parameter.getName() + " para value="+parameter.getValue());
            if(parameter.getName().equals("old-yang")){
                oldYangPath = (String) parameter.getValue();
                List<String> yangPaths = new ArrayList<>();
                yangPaths.add(oldYangPath);
                yangCompiler.getBuildOption().addSource(new FileSource(yangPaths));
            } else if(parameter.getName().equals("settings")){
                String settingsPath = (String) parameter.getValue();
                try {
                    yangCompiler.setSettings(Settings.parse(FileUtil.readFile2String(settingsPath)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if(parameter.getName().equals("compare-type")){
                compareType = (CompareType) parameter.getValue();
            } else if(parameter.getName().equals("rule")){
                rulePath = (String) parameter.getValue();
            } else if(parameter.getName().equals("result")){
                resultPath = (String) parameter.getValue();
            }

        }
        if(oldYangPath == null){
            throw new YangCompilerException("missing mandatory parameter:old-yang");
        }
        if(compareType == null){
            throw new YangCompilerException("missing mandatory parameter:compare-type");
        }
        if(resultPath == null){
            throw new YangCompilerException("missing mandatory parameter:result");
        }
        YangSchemaContext oldSchemaContext = yangCompiler.buildSchemaContext();
        ValidatorResult oldResult = oldSchemaContext.validate();
        if(!oldResult.isOk()){
            throw new YangCompilerException("fail to validate the schema context of " + oldYangPath +
                    ".\n" + oldResult);
        }
        //System.out.println(oldSchemaContext.getValidateResult());
        YangComparator yangComparator = new YangComparator(oldSchemaContext,yangSchemaContext);
        try {
            List<YangCompareResult> results = yangComparator.compare(compareType,rulePath);
            boolean needCompatible = false;
            if(compareType == CompareType.COMPATIBLE_CHECK){
                needCompatible = true;
            }
            Document document = yangComparator.outputXmlCompareResult(results,needCompatible,compareType);
            //System.out.println(XmlWriter.transDom4jDoc2String(document));
            XmlWriter.writeDom4jDoc(document,resultPath);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e){
            for(StackTraceElement traceElement:e.getStackTrace()){
                System.out.println(traceElement);
            }

        }

    }
}
