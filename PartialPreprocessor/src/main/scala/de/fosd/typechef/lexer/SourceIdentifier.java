package de.fosd.typechef.lexer;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.regex.Pattern;

public class SourceIdentifier {

    public static SourceIdentifier BASE_SOURCE = new SourceIdentifier((File) null);
    private File file;
    private String fileName;
    private String fileExtension;
    public static final String H_EXTENSION = "h";
    public static final String C_EXTENSION = "c";
    private static String ESCAPED_FILE_SEPARATOR = File.separator.replace("\\", "\\\\");
    private static Set<String> SEPARATORS = new HashSet<String>(Arrays.asList("/", ESCAPED_FILE_SEPARATOR));
    private static Pattern SEPARATOR_PATTERN;

    static {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for(String separator: SEPARATORS){
            if (first) {
                first = false;
            } else {
                builder.append("|");
            }
            builder.append(separator);
        }
        builder.append("]");
        SEPARATOR_PATTERN = Pattern.compile(builder.toString());
    }

    public SourceIdentifier(String path) {
        this(new File(path));
    }

    public SourceIdentifier(File file) {
        this.file = file;
        initialize();
    }

    public String getFileName() {
        return fileName;
    }

    public File getFile() {
        return file;
    }

    public static String normalize(String path){
        return SEPARATOR_PATTERN.matcher(path).replaceAll(ESCAPED_FILE_SEPARATOR);
    }
    public String getPath() {
        if (this.file == null) {
            return null;
        } else {
            return this.file.getPath();
        }
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public SourceIdentifier resolve(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return new SourceIdentifier(path);
        } else {
            String[] tokens = SEPARATOR_PATTERN.split(path);

            // when resolving a path start from the parent
            SourceIdentifier result = this.getParent();

            for (String token : tokens) {
                if (token.equals("..")) {
                    result = result.getParent();
                } else if (token.equals(".")) {
                    // do nothing
                } else {
                    result = result.getChild(token);
                }
            }

            return result;
        }
    }

    public boolean contains(SourceIdentifier that) {
        SourceIdentifier current = that;
        while (current != BASE_SOURCE) {
            if (this.equals(current)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    public SourceIdentifier getChild(String name) {
        if (this.file != null && this.file.isDirectory()) {
            for (File file : this.file.listFiles()) {
                if (file.getName().equals(name)) {
                    return new SourceIdentifier(file);
                }
            }
        }
        return BASE_SOURCE;
    }

    public boolean isSourceFileSource() {
        return this.fileExtension != null && this.fileExtension.equals(C_EXTENSION);
    }

    public boolean isHeaderFileSource() {
        return this.fileExtension != null && this.fileExtension.equals(H_EXTENSION);
    }

    public SourceIdentifier getParent() {
        if (this.file != null && this.file.getParentFile() != null) {
            return new SourceIdentifier(this.file.getParentFile());
        } else {
            return BASE_SOURCE;
        }
    }

    private void initialize() {
        if (file != null && file.isFile()) {
            String name = file.getName();
            int index = name.lastIndexOf('.');
            if (index != -1) {
                fileName = name.substring(0, index);
                fileExtension = name.substring(index + 1).toLowerCase();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        } else {
            SourceIdentifier that = (SourceIdentifier) obj;
            return (this.file == null && that.file == null) || (this.file != null && that.file != null && this.file.equals(that.file));
        }
    }

    @Override
    public int hashCode() {
        if (file != null) {
            return file.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public String toString() {
        return "Identifier: " + this.file;
    }

    public boolean sameUnit(SourceIdentifier that) {
        return (this.fileName != null && that.fileName != null && this.fileName.equals(that.fileName) && this.fileExtension.equals(C_EXTENSION) && that.fileExtension.equals(H_EXTENSION));
    }
}
