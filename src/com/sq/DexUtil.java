package com.sq;

import com.sq.tool.FileUtil;
import com.sq.tool.LogUtil;

import java.io.File;

/**
 * @author zhuxiaoxin
 * 编译，反编译
 * **/
public class DexUtil {

    private String bakSmaliPath, smaliPath;

    public DexUtil(String bakSmaliPath, String smaliPath) {
        this.bakSmaliPath = bakSmaliPath;
        this.smaliPath = smaliPath;
    }

    /**
     * 反编译
     **/
    public void dexToSmali(String dexPath, String tempPath) {
        LogUtil.d("--------start 反编译-------------");
        File file = new File(tempPath);
        if (file.exists()) {
            FileUtil.delDir(file);
        }
        file.mkdirs();
        StringBuilder builder = new StringBuilder();
        builder.append("java -jar ")
                .append(bakSmaliPath)
                .append(" d -o ")
                .append(tempPath)
                .append(" ")
                .append(dexPath);
        LogUtil.d("反编译命令:" + builder.toString());
        try {
            Process process = Runtime.getRuntime().exec(builder.toString());
            LogUtil.logProc(process);
            if (process.waitFor() != 0) {
                LogUtil.d("反编译dex失败 : exec cmd err..!!!!!!!");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d("反编译dex失败 : err..!!!!!!! ＝ " + e.toString());
            return;
        }
        LogUtil.d("---------end 反编译--------------");
    }

    /**
     * 回编译
     * @param tempPath
     */
    public void smaliToDex(String tempPath, String dexPath) {
        LogUtil.d("---------start 回编译--------------");
        File file = new File(dexPath);
        if (file.exists()) {
            file.delete();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("java -jar ")
                .append(smaliPath)
                .append(" a -o ")
                .append(dexPath)
                .append(" ")
                .append(tempPath);
        LogUtil.d("回编译命令:" + builder);
        try {
            Process process = Runtime.getRuntime().exec(builder.toString());
            LogUtil.logProc(process);
            if (process.waitFor() != 0) {
                LogUtil.d("回编译dex失败 : exec cmd err..!!!!!!!");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d("回编译dex失败 : err..!!!!!!! ＝ " + e.toString());
            return;
        }
        FileUtil.delDir(new File(tempPath));
        LogUtil.d("---------end 回编译--------------");
    }

}
