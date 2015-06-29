package de.fosd.typechef.lexer;

import java.io.File;

public class SourceIdentifier {

    public static SourceIdentifier BASE_SOURCE = new SourceIdentifier((File) null);
    private File file;
    private String fileName;
    private String fileExtension;
    public static final String H_EXTENSION = "h";
    public static final String C_EXTENSION = "c";

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
        if (path.startsWith(File.separator)) {
            return new SourceIdentifier(path);
        } else {
            String[] tokens = path.split(File.separator);

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
