import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * TIS-S9 验收工具：逐条打印 zip/jar 条目，并对主 jar 做"顶层目录必须不存在"的硬断言。
 *
 * 用法（JDK 21 单文件源码启动，无需先编译）：
 *   java tools/ZipArchive.java list          <jar>           逐条打印全部条目（序号 / 路径 / 压缩后 / 原始 / CRC32）
 *   java tools/ZipArchive.java tops          <jar>           只打印顶层路径段集合与计数
 *   java tools/ZipArchive.java assert-absent <jar> <seg>...  断言这些顶层段在任何条目里都不出现
 *   java tools/ZipArchive.java dump          <jar> <entry>   打印单个条目的文本内容
 *
 * 设计要点：不使用任何管道/过滤去"猜"结果 —— 清单由 ZipFile.entries() 原始遍历得到，
 * 断言失败以退出码 1 结束，成功以退出码 0 结束，供 S9-ACCEPT.md 直接引用。
 */
public final class ZipArchive {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: ZipArchive <list|tops|assert-absent|dump> <jar> [args...]");
            System.exit(2);
        }
        String mode = args[0];
        String jarPath = args[1];
        try (ZipFile zip = new ZipFile(jarPath)) {
            switch (mode) {
                case "list" -> list(zip, jarPath);
                case "tops" -> tops(zip, jarPath);
                case "assert-absent" -> {
                    List<String> forbidden = new ArrayList<>();
                    for (int i = 2; i < args.length; i++) {
                        forbidden.add(args[i]);
                    }
                    if (forbidden.isEmpty()) {
                        System.err.println("assert-absent needs at least one segment");
                        System.exit(2);
                    }
                    System.exit(assertAbsent(zip, jarPath, forbidden));
                }
                case "dump" -> {
                    if (args.length < 3) {
                        System.err.println("dump needs an entry name");
                        System.exit(2);
                    }
                    System.exit(dump(zip, jarPath, args[2]));
                }
                default -> {
                    System.err.println("unknown mode: " + mode);
                    System.exit(2);
                }
            }
        }
    }

    private static void list(ZipFile zip, String jarPath) {
        System.out.println("== entries of " + jarPath + " ==");
        int n = 0;
        int totalUncompressed = 0;
        var en = zip.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            n++;
            totalUncompressed += (int) Math.max(0, e.getSize());
            System.out.printf("%4d  %-72s  comp=%8d  size=%8d  crc=%08X%n",
                    n, e.getName(), e.getCompressedSize(), e.getSize(), e.getCrc() & 0xFFFFFFFFL);
        }
        System.out.printf("-- total entries = %d, uncompressed bytes = %d%n", n, totalUncompressed);
    }

    private static void tops(ZipFile zip, String jarPath) {
        Set<String> tops = new TreeSet<>();
        int n = 0;
        var en = zip.entries();
        while (en.hasMoreElements()) {
            n++;
            tops.add(topSegment(en.nextElement().getName()));
        }
        System.out.println("== top-level segments of " + jarPath + " ==");
        for (String t : tops) {
            System.out.println("  " + t);
        }
        System.out.printf("-- entries = %d, distinct top segments = %d: %s%n", n, tops.size(), tops);
    }

    private static int assertAbsent(ZipFile zip, String jarPath, List<String> forbidden) {
        Set<String> hits = new LinkedHashSet<>();
        List<String> offending = new ArrayList<>();
        var en = zip.entries();
        while (en.hasMoreElements()) {
            String name = en.nextElement().getName();
            String top = topSegment(name);
            if (forbidden.contains(top)) {
                hits.add(top);
                offending.add(name);
            }
        }
        System.out.println("== assert-absent on " + jarPath + " ==");
        System.out.println("   forbidden top-level segments: " + forbidden);
        System.out.println("   offending segments found   : " + (hits.isEmpty() ? "(none)" : hits));
        for (String s : offending) {
            System.out.println("   OFFENDING ENTRY: " + s);
        }
        if (hits.isEmpty()) {
            System.out.println("   RESULT: PASS - none of the forbidden top-level segments exist");
            return 0;
        }
        System.out.println("   RESULT: FAIL - " + offending.size() + " offending entries");
        return 1;
    }

    private static int dump(ZipFile zip, String jarPath, String entryName) throws Exception {
        ZipEntry e = zip.getEntry(entryName);
        if (e == null) {
            System.out.println("== dump " + entryName + " from " + jarPath + " ==");
            System.out.println("   RESULT: FAIL - entry not found");
            return 1;
        }
        byte[] bytes;
        try (InputStream in = zip.getInputStream(e)) {
            bytes = in.readAllBytes();
        }
        System.out.println("== dump " + entryName + " from " + jarPath + " (" + bytes.length + " bytes) ==");
        System.out.println(new String(bytes, StandardCharsets.UTF_8));
        System.out.println("== end of " + entryName + " ==");
        return 0;
    }

    private static String topSegment(String entryName) {
        String n = entryName.replace('\\', '/');
        int slash = n.indexOf('/');
        return slash < 0 ? n : n.substring(0, slash);
    }

    private ZipArchive() {
    }
}
