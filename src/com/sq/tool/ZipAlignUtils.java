package com.sq.tool;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipAlignUtils {

    public static void execute(String zipalignCmd, String apkPath, String alignApkPath) {
        if (zipalignCmd ==null || "".equals(zipalignCmd)) {
            zipalignCmd = " zipalign ";
        }

        LogUtil.d("--优化命令-->"+zipalignCmd);
        StringBuilder buffer = new StringBuilder();
        buffer.append(zipalignCmd+"  4 " + apkPath + " " + alignApkPath);
        try {
            Process process = Runtime.getRuntime().exec(buffer.toString());
            LogUtil.logProc(process);
            if (process.waitFor() != 0) {
                LogUtil.d("优化失败！！！");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static final int  BUFFER_SIZE = 2 * 1024;
    /**
     * zip解压
     * @param srcFile        zip源文件
     * @param destDirPath     解压后的目标文件夹
     * @throws RuntimeException 解压失败会抛出运行时异常
     */
    public static void unZip(File srcFile, String destDirPath) throws RuntimeException {
        long start = System.currentTimeMillis();
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw new RuntimeException(srcFile.getPath() + "所指文件不存在");
        }
        // 开始解压
        ZipFile zipFile = null;
        try {
            Pattern smaliClassPattern = Pattern.compile("(classes)([0-9])?.dex");
            zipFile = new ZipFile(srcFile);
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!smaliClassPattern.matcher(entry.getName()).find()){
                    continue;
                }
                System.out.println("解压" + entry.getName());
                // 如果是文件夹，就创建个文件夹
                if (entry.isDirectory()) {
                    String dirPath = destDirPath + "/" + entry.getName();
                    File dir = new File(dirPath);
                    dir.mkdirs();
                } else {
                    // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
                    File targetFile = new File(destDirPath + "/" + entry.getName());
                    // 保证这个文件的父文件夹必须要存在
                    if(!targetFile.getParentFile().exists()){
                        targetFile.getParentFile().mkdirs();
                    }
                    targetFile.createNewFile();
                    // 将压缩文件内容写入到这个文件中
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[BUFFER_SIZE];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    // 关流顺序，先打开的后关闭
                    fos.close();
                    is.close();
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("解压完成，耗时：" + (end - start) +" ms");
        } catch (Exception e) {
            throw new RuntimeException("unzip error from ZipUtils", e);
        } finally {
            if(zipFile != null){
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void updateZipFile(String inputZipName, String outPutZipName) {
        try {

            Pattern smaliClassPattern = Pattern.compile("(classes)([0-9])?.dex");
            ZipFile zipFile = new ZipFile(inputZipName);
            // 复制为新zip
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outPutZipName));
            // 遍历所有文件复制
            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry entryIn = (ZipEntry) e.nextElement();
                if (!smaliClassPattern.matcher(entryIn.getName()).find()) {
                    zos.putNextEntry(new ZipEntry(entryIn.getName()));
                    InputStream is = zipFile.getInputStream(entryIn);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        zos.write(buf, 0, (len < buf.length) ? len : buf.length);
                    }
                }
                // 当前文件需要复写
                else {
                    ZipEntry newEntry = new ZipEntry(entryIn.getName());
                    zos.putNextEntry(newEntry);
                    InputStream is = new FileInputStream(inputZipName.substring(0, inputZipName.lastIndexOf(File.separator)) + File.separator + "work" + File.separator + entryIn.getName());
                    byte[] buf = new byte[1024 * 5];
                    int len;
                    while ((len = (is.read(buf))) > 0) {
                        zos.write(buf, 0, (len < buf.length) ? len : buf.length);
                    }
                }
                zos.closeEntry();
            }
            zos.close();
        }catch (Exception e){
            LogUtil.e("update zip error:" + e);
        }
    }

    public static void toZip(String srcDir, String out, boolean KeepDirStructure)
            throws RuntimeException{

        long start = System.currentTimeMillis();
        ZipOutputStream zos = null ;
        try {
            zos = new ZipOutputStream(new FileOutputStream(out));
            File sourceFile = new File(srcDir);
            compress(sourceFile, zos, "", KeepDirStructure);
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) +" ms");
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils",e);
        }finally{
            if(zos != null){
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void compress(File sourceFile, ZipOutputStream zos, String name,
                                 boolean KeepDirStructure) throws Exception{
        byte[] buf = new byte[BUFFER_SIZE];
        if(sourceFile.isFile()){
            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // copy文件到zip输出流中
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1){
                zos.write(buf, 0, len);
            }
            // Complete the entry
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if(listFiles == null || listFiles.length == 0){
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                if(KeepDirStructure){
                    // 空文件夹的处理
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    // 没有文件，不需要文件的copy
                    zos.closeEntry();
                }

            }else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    if (KeepDirStructure) {
                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                        compress(file, zos, name + "/" + file.getName(),KeepDirStructure);
                    } else {
                        compress(file, zos, file.getName(),KeepDirStructure);
                    }

                }
            }
        }
    }
}
