package com.sq;

import com.sq.bean.PublicXmlBean;
import com.sq.config.Config;
import com.sq.helper.PublicAndRHelper;
import com.sq.tool.DecodeUtil;
import com.sq.tool.FileUtil;
import com.sq.tool.LogUtil;

import com.sq.tool.values.ColorXmlProcessor;
import org.dom4j.DocumentException;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 修改apk资源id值工具
 */
public class ResTool {
    static Set<String> set;
    public static void main(String[] args) throws Exception {
        set = new HashSet<>();
        set.add(File.separator + "com.zbm.plugin_login".replace(".", File.separator));
        set.add(File.separator + "com.zbm.plugin_config".replace(".", File.separator));
        //获得配置
        File directory = new File("");
        String rootDir = directory.getCanonicalPath();
        Config config = new Config(
                rootDir + File.separator + "apktool.jar",
                rootDir + File.separator + "pluginapp-debug.apk",
                rootDir + File.separator + "work",
                "00500000");
        //处理
        handleResPkg(config);
    }

    public static void handleResPkg(Config config) {

        LogUtil.d("配置文件内容： " + config.toString());
        String apkPath = config.originalApkPath;
        LogUtil.d("apk路径为: " + apkPath);

        String tempApkPath = config.tempApkPath;
        LogUtil.d("反编译临时路径为: " + tempApkPath);

        File tempApkFile = new File(tempApkPath);
        if (tempApkFile.exists()) {
            FileUtil.delDir(tempApkFile);
        }
        tempApkFile.mkdirs();

        File sourceApk = new File(tempApkPath + File.separator + "dist");

        DecodeUtil decodeUtil = new DecodeUtil(config.apktoolPath);
        decodeUtil.decode(apkPath, tempApkPath);

        fixColorXmlPrivateRef(tempApkPath);

        //处理public逻辑
        handlePublicXml(tempApkPath, config.addBigValue);

        delSmaliClass(tempApkPath);

        decodeUtil.encode(tempApkPath);
    }

    /**
     * 处理xml文件
     */
    private static void handlePublicXml(String tempApkPath, String addBigValue) {
        String publicXmlPath = tempApkPath + File.separator + "res" + File.separator + "values" + File.separator + "public.xml";
        try {
            PublicXmlBean publicXmlBean = new PublicXmlBean(publicXmlPath);
            publicXmlBean.resetBigValue(addBigValue);
            publicXmlBean.flush();
            new PublicAndRHelper().handle(tempApkPath);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    private static void fixColorXmlPrivateRef(String tempApkPath){
        String colorXmlPath = tempApkPath + File.separator + "res" + File.separator + "values-v31" + File.separator + "colors.xml";
        File file = new File(colorXmlPath);
        if (!file.exists()) {
            LogUtil.i("无需修改color.xml文件");
            return;
        }
        try {
            new ColorXmlProcessor(colorXmlPath).flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void delSmaliClass(String apkWorkPath){
        Pattern smaliClassPattern = Pattern.compile("(smali)(_classes[0-9])?");
        File src = new File(apkWorkPath);
        List<File> fileList = new LinkedList<>();
        List<File> dirList = new LinkedList<>();
        File[] temp1 = src.listFiles();
        for (File file :temp1){
            if (file.getName().contains("smali")){
                fileList.add(file);
            }
        }
        LogUtil.e("####findAll:" + fileList);
        while (fileList.size() > 0){
            File dir = fileList.remove(0);
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++){
                File temp = files[i];
                if (temp.isDirectory()){
                    String[] ss = temp.getAbsolutePath().split("(smali)(_classes[0-9])?");
                    if (set.contains(ss[1])){
                        LogUtil.i("######9:忽略" + temp.getAbsolutePath());
                    }else {
                        fileList.add(temp);
                    }
                    LogUtil.i("#####8:" + Arrays.toString(ss));
                }else {
                    temp.delete();
                }
            }
            /**
             * 要注意smali目录不能清除
             */
            Matcher matcher = smaliClassPattern.matcher(dir.getName());
            if (!matcher.find()){
                dirList.add(dir);
            }
        }

        LogUtil.i("#####10:" + dirList);
        for (int i = dirList.size() - 1; i >= 0; i--) {
            dirList.get(i).delete();
        }
    }
}
