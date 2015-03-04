package de.fosd.typechef.lexer;

import java.io.File;

public class SourceIdentifier {

    public static SourceIdentifier BASE_SOURCE = new SourceIdentifier((File) null);
    private File file;
    private String fileName;
    private String fileExtension;
    private static final String H_EXTENSION = ".h";
    private static final String C_EXTENSION = ".c";

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

    public String getFileExtension() {
        return fileExtension;
    }

    public boolean isSourceFileSource() {
        return this.fileExtension != null && this.fileExtension.equals(C_EXTENSION);
    }

    public boolean isHeaderFileSource() {
        return this.fileExtension != null && this.fileExtension.equals(H_EXTENSION);
    }

    private void initialize() {
        if (file != null && file.isFile()) {
            String name = file.getName();
            int index = name.lastIndexOf('.');
            if (index != -1) {
                fileName = name.substring(0, index);
                fileExtension = name.substring(index);
            }
        }
    }

    @Override
    public String toString() {
        return "Identifier: " + this.file;
    }

    public boolean sameUnit(SourceIdentifier that) {
        return (this.fileName != null && that.fileName != null && this.fileName.equals(that.fileName) && this.file.getName().endsWith(C_EXTENSION) && that.file.getName().endsWith(H_EXTENSION));
    }
}
