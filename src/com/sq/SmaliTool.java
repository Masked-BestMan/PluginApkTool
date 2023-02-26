package com.sq;

import com.sq.config.Config;
import com.sq.tool.FileUtil;
import com.sq.tool.LogUtil;
import com.sq.tool.ZipAlignUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 去除指定目录下smali文件的工具
 */
public class SmaliTool {
    static Set<String> set;//需要保留smali文件的目录集合
    public static void main(String[] args) throws Exception {
        set = new HashSet<>();
        set.add(File.separator + "com.zbm.plugin_config".replace(".", File.separator));
        File directory = new File("");
        String rootDir = directory.getCanonicalPath();
        Config config = new Config(
                rootDir + File.separator + "baksmali-2.5.2.jar",
                rootDir + File.separator + "pluginapp-debug.apk",
                rootDir + File.separator + "work",
                rootDir + File.separator + "smali-2.5.2.jar");

        handleApk(config);
    }

    public static void handleApk(Config config) {
        LogUtil.d("配置文件内容： " + config.toString());
        String apkPath = config.originalApkPath;
        LogUtil.d("apk路径为: " + apkPath);

        String tempApkPath = config.tempApkPath;
        LogUtil.d("解压临时路径为: " + tempApkPath);

        File tempApkFile = new File(tempApkPath);
        if (tempApkFile.exists()) {
            FileUtil.delDir(tempApkFile);
        }
        tempApkFile.mkdirs();

        ZipAlignUtils.unZip(new File(apkPath), tempApkPath);

        Pattern smaliClassPattern = Pattern.compile("(classes)([0-9])?.dex");
        File[] files = tempApkFile.listFiles();
        DexUtil dexUtil = new DexUtil(config.apktoolPath, config.addBigValue);
        List<File> list = new LinkedList<>();
        if (files != null) {
            for (File file : files) {
//                if (smaliClassPattern.matcher(file.getName()).find()){
                    String temp = tempApkPath + File.separator + file.getName().replace(".", "_");
                    dexUtil.dexToSmali(file.getAbsolutePath(), temp);
                    list.add(new File(temp));
//                }
            }
            List<File> newList = new LinkedList<>(list);
            delSmaliClass(list);

            for (File file: newList) {
                dexUtil.smaliToDex(file.getAbsolutePath(), tempApkPath + File.separator + file.getName().replace("_", "."));
            }

            ZipAlignUtils.updateZipFile(config.originalApkPath,  tempApkPath + File.separator + "pluginapp-debug.apk");
        }


    }


    private static void delSmaliClass(List<File> fileList){
        Pattern smaliClassPattern = Pattern.compile("(classes)[0-9]?(_dex)");
        List<File> dirList = new LinkedList<>();

        while (fileList.size() > 0){
            File dir = fileList.remove(0);
            File[] files = dir.listFiles();
            if (files == null){
                continue;
            }
            for (int i = 0; i < files.length; i++){
                File temp = files[i];
                if (temp.isDirectory()){
                    String[] ss = temp.getAbsolutePath().split("(classes)[0-9]?(_dex)");
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
