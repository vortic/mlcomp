/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.ho.yaml.Yaml;

/**
 *
 * @author Victor
 */
public class General {

    public class FilePath {

        String path;

        public FilePath(String path) {
            this.path = path;
        }

        public String relativize(String newRoot) {
            String pathSeparator = System.getProperty("file.separator");

            String targetPath = new File(path).getAbsolutePath();
            String basePath = new File(newRoot).getAbsolutePath();

            // Normalize the paths
            String normalizedTargetPath = FilenameUtils.normalizeNoEndSeparator(targetPath);
            String normalizedBasePath = FilenameUtils.normalizeNoEndSeparator(basePath);

            // Undo the changes to the separators made by normalization
            if (pathSeparator.equals("/")) {
                normalizedTargetPath = FilenameUtils.separatorsToUnix(normalizedTargetPath);
                normalizedBasePath = FilenameUtils.separatorsToUnix(normalizedBasePath);

            } else if (pathSeparator.equals("\\")) {
                normalizedTargetPath = FilenameUtils.separatorsToWindows(normalizedTargetPath);
                normalizedBasePath = FilenameUtils.separatorsToWindows(normalizedBasePath);

            } else {
                throw new IllegalArgumentException("Unrecognised dir separator '" + pathSeparator + "'");
            }

            String[] base = normalizedBasePath.split(Pattern.quote(pathSeparator));
            String[] target = normalizedTargetPath.split(Pattern.quote(pathSeparator));

            // First get all the common elements. Store them as a string,
            // and also count how many of them there are.
            StringBuffer common = new StringBuffer();

            int commonIndex = 0;
            while (commonIndex < target.length && commonIndex < base.length
                    && target[commonIndex].equals(base[commonIndex])) {
                common.append(target[commonIndex] + pathSeparator);
                commonIndex++;
            }

            if (commonIndex == 0) {
                // No single common path element. This most
                // likely indicates differing drive letters, like C: and D:.
                // These paths cannot be relativized.
                throw new PathResolutionException("No common path element found for '" + normalizedTargetPath + "' and '" + normalizedBasePath
                        + "'");
            }

            // The number of directories we have to backtrack depends on whether the base is a file or a dir
            // For example, the relative path from
            //
            // /foo/bar/baz/gg/ff to /foo/bar/baz
            //
            // ".." if ff is a file
            // "../.." if ff is a directory
            //
            // The following is a heuristic to figure out if the base refers to a file or dir. It's not perfect, because
            // the resource referred to by this path may not actually exist, but it's the best I can do
            boolean baseIsFile = true;

            File baseResource = new File(normalizedBasePath);

            if (baseResource.exists()) {
                baseIsFile = baseResource.isFile();

            } else if (basePath.endsWith(pathSeparator)) {
                baseIsFile = false;
            }

            StringBuffer relative = new StringBuffer();

            if (base.length != commonIndex) {
                int numDirsUp = baseIsFile ? base.length - commonIndex - 1 : base.length - commonIndex;

                for (int i = 0; i < numDirsUp; i++) {
                    relative.append(".." + pathSeparator);
                }
            }
            relative.append(normalizedTargetPath.substring(common.length()));
            return relative.toString();
        }
    }

    static class PathResolutionException extends RuntimeException {

        PathResolutionException(String msg) {
            super(msg);
        }
    }

    static void changePath(String path) {
        String currPath = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", path);
            //execute block
            System.setProperty("user.dir", currPath);
        } catch (Exception e) {
            System.setProperty("user.dir", currPath);
            System.err.println(e);
        }
    }

    static void readStatus(String path) {
        path += "/status";

        try {
            File file = new File(path);
            Yaml.load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void writeStatus(Object map, String path) {
        path += "/status";
        try {
            File file = new File(path);
            Yaml.dump(map, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
